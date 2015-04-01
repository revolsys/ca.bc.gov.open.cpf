/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.api.domain;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import ca.bc.gov.open.cpf.api.scheduler.BusinessApplicationStatistics;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.impl.module.ResourcePermission;

import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.identifier.Identifier;
import com.revolsys.data.query.And;
import com.revolsys.data.query.Condition;
import com.revolsys.data.query.In;
import com.revolsys.data.query.Or;
import com.revolsys.data.query.Q;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.attribute.JdbcLongFieldDefinition;
import com.revolsys.jdbc.io.JdbcRecordStore;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.ui.web.controller.PathAliasController;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.Property;
import com.revolsys.util.WrappedException;

public class CpfDataAccessObject {
  public static String getUsername() {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    String username;
    if (authentication == null) {
      username = "SYSTEM";
      SecurityContextHolder.clearContext();
    } else {
      username = authentication.getName();
    }
    return username;
  }

  private RecordStore recordStore;

  private RecordDefinition batchJobRecordDefinition;

  private RecordDefinition batchJobResultRecordDefinition;

  private RecordDefinition businessApplicationStatisticsRecordDefinition;

  private RecordDefinition configPropertyRecordDefinition;

  private RecordDefinition userAccountRecordDefinition;

  private RecordDefinition userGroupRecordDefinition;

  private RecordDefinition userGroupAccountXrefRecordDefinition;

  private RecordDefinition userGroupPermissionRecordDefinition;

  private Map<Long, BatchJob> batchJobById = new HashMap<>();

  public CpfDataAccessObject() {
  }

  public boolean cancelBatchJob(final long jobId) {
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final String username = getUsername();
        final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
        final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET " //
          + "COMPLETED_GROUP_RANGE = null, "//
          + "COMPLETED_REQUEST_RANGE = null, "//
          + "FAILED_REQUEST_RANGE = NUM_SUBMITTED_REQUESTS,"
          + "WHEN_STATUS_CHANGED = ?, " //
          + "JOB_STATUS = 'cancelled',"
          + "WHEN_UPDATED = ?, WHO_UPDATED = ? WHERE BATCH_JOB_ID = ?";
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        if (JdbcUtils.executeUpdate(jdbcRecordStore, sql, now, now, username, jobId) == 1) {
          deleteBatchJobResults(jobId);
          return true;
        } else {
          return false;
        }
      } catch (final Throwable e) {
        transaction.setRollbackOnly();
        throw new RuntimeException("Unable to cencel jobId=" + jobId, e);
      }
    }
  }

  public void clearBatchJob(final long batchJobId) {
    synchronized (this.batchJobById) {
      this.batchJobById.remove(batchJobId);
    }
  }

  @PreDestroy
  public void close() {
    this.batchJobById = null;
    this.batchJobRecordDefinition = null;
    this.batchJobResultRecordDefinition = null;
    this.businessApplicationStatisticsRecordDefinition = null;
    this.configPropertyRecordDefinition = null;
    this.recordStore = null;
    this.userAccountRecordDefinition = null;
    this.userGroupRecordDefinition = null;
    this.userGroupAccountXrefRecordDefinition = null;
    this.userGroupPermissionRecordDefinition = null;
  }

  public Record create(final String typeName) {
    return this.recordStore.create(typeName);
  }

  public BatchJob createBatchJob() {
    final Record record = create(BatchJob.BATCH_JOB);
    final long batchJobId = ((Number)this.recordStore.createPrimaryIdValue(BatchJob.BATCH_JOB)).intValue();
    record.setIdValue(batchJobId);
    final String prefix = PathAliasController.getAlias();
    if (prefix != null) {
      final Map<String, String> properties = new HashMap<String, String>();
      properties.put("webServicePrefix", prefix);
      record.setValue(BatchJob.PROPERTIES, JsonMapIoFactory.toString(properties));
    }
    record.setValue(BatchJob.JOB_STATUS, BatchJobStatus.SUBMITTED);
    record.setValue(BatchJob.WHEN_STATUS_CHANGED, new Timestamp(System.currentTimeMillis()));
    record.setValue(BatchJob.NUM_SUBMITTED_GROUPS, 0);
    record.setValue(BatchJob.GROUP_SIZE, 1);

    final BatchJob batchJob = new BatchJob(record);
    this.batchJobById.put(batchJobId, batchJob);
    return batchJob;
  }

  public void createBatchJobExecutionGroup(final JobController jobController,
    final long batchJobId, final int groupSequenceNumber, final String errorCode,
    final String errorMessage, final String errorDebugMessage) {
    final Map<String, Object> error = new HashMap<String, Object>();
    error.put("i", 1);
    error.put("errorCode", errorCode);
    error.put("errorMessage", errorMessage);
    error.put("errorDebugMessage", errorDebugMessage);
    final List<Map<String, Object>> resultDataItems = Collections.singletonList(error);

    final Map<String, Object> resultData = Collections.<String, Object> singletonMap("items",
      resultDataItems);
    final String resultDataString = JsonMapIoFactory.toString(resultData);

    // TODO errors jobController.setStructuredResultData(batchJobId,
    // groupSequenceNumber, batchJobExecutionGroup,
    // resultDataString);
  }

  public Record createConfigProperty(final String environmentName, final String moduleName,
    final String componentName, final String propertyName, final Object propertyValue,
    final DataType propertyValueType) {
    final Record configProperty = create(ConfigProperty.CONFIG_PROPERTY);
    configProperty.setValue(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    configProperty.setValue(ConfigProperty.MODULE_NAME, moduleName);
    configProperty.setValue(ConfigProperty.COMPONENT_NAME, componentName);
    configProperty.setValue(ConfigProperty.PROPERTY_NAME, propertyName);
    configProperty.setValue(ConfigProperty.PROPERTY_VALUE_TYPE, propertyValueType.toString());

    setConfigPropertyValue(configProperty, propertyValue);
    write(configProperty);
    return configProperty;
  }

  @SuppressWarnings("unchecked")
  public <T> T createId(final String typeName) {
    return (T)this.recordStore.createPrimaryIdValue(typeName);
  }

  public Transaction createTransaction(final Propagation propagation) {
    final PlatformTransactionManager transactionManager = getTransactionManager();
    return new Transaction(transactionManager, propagation);
  }

  public Record createUserAccount(final String userAccountClass, final String userAccountName,
    final String consumerKey, final String consumerSecret) {
    final Record userAccount = create(UserAccount.USER_ACCOUNT);

    userAccount.setValue(UserAccount.USER_NAME, userAccountName);
    userAccount.setValue(UserAccount.USER_ACCOUNT_CLASS, userAccountClass);
    userAccount.setValue(UserAccount.CONSUMER_KEY, consumerKey);
    userAccount.setValue(UserAccount.CONSUMER_SECRET, consumerSecret);
    userAccount.setValue(UserAccount.ACTIVE_IND, 1);

    write(userAccount);
    return userAccount;
  }

  public Record createUserGroup(final String moduleName, final String groupName,
    final String description) {
    Record userGroup = getUserGroup(moduleName, groupName);
    if (userGroup == null) {
      userGroup = create(UserGroup.USER_GROUP);
      userGroup.setValue(UserGroup.MODULE_NAME, moduleName);
      userGroup.setValue(UserGroup.USER_GROUP_NAME, groupName);
      userGroup.setValue(UserGroup.DESCRIPTION, description);
      userGroup.setValue(UserGroup.ACTIVE_IND, 1);
      write(userGroup);
    }
    return userGroup;
  }

  public Record createUserGroupAccountXref(final Record userGroup, final Record userAccount) {
    final Identifier userGroupId = userGroup.getIdentifier();
    final Identifier userAccountId = userAccount.getIdentifier();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

    final Query query = Query.and(this.userGroupAccountXrefRecordDefinition, filter);

    Record userGroupAccountXref = this.recordStore.queryFirst(query);
    if (userGroupAccountXref == null) {

      userGroupAccountXref = create(UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
      userGroupAccountXref.setValue(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
      userGroupAccountXref.setValue(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

      write(userGroupAccountXref);
    }
    return userGroupAccountXref;
  }

  public Record createUserGroupPermission(final Record userGroup, final String groupName,
    final ResourcePermission permission) {
    final Record userGroupPermission = create(UserGroup.USER_GROUP);

    userGroupPermission.setValue(UserGroup.MODULE_NAME, userGroup);
    userGroupPermission.setValue(UserGroup.USER_GROUP_NAME, groupName);
    userGroupPermission.setValue(UserGroup.DESCRIPTION, permission);

    write(userGroupPermission);
    return userGroup;
  }

  public void delete(final Record object) {
    this.recordStore.delete(object);
  }

  public int deleteBatchJob(final Long batchJobId) {
    deleteBatchJobResults(batchJobId);

    final Query query = Query.equal(this.batchJobRecordDefinition, BatchJob.BATCH_JOB_ID,
      batchJobId);
    return this.recordStore.delete(query);
  }

  public int deleteBatchJobResults(final Long batchJobId) {
    final Query query = Query.equal(this.batchJobResultRecordDefinition,
      BatchJobResult.BATCH_JOB_ID, batchJobId);
    return this.recordStore.delete(query);
  }

  public int deleteBusinessApplicationStatistics(final Integer businessApplicationStatisticsId) {
    final Query query = Query.equal(this.businessApplicationStatisticsRecordDefinition,
      BusinessApplicationStatistics.APPLICATION_STATISTIC_ID, businessApplicationStatisticsId);
    return this.recordStore.delete(query);
  }

  public int deleteConfigPropertiesForModule(final String moduleName) {
    final Query query = Query.equal(this.configPropertyRecordDefinition,
      ConfigProperty.MODULE_NAME, moduleName);
    return this.recordStore.delete(query);
  }

  public void deleteUserAccount(final Record userAccount) {
    final Identifier userAccountId = userAccount.getIdentifier();
    final Query membersQuery = Query.equal(this.userGroupAccountXrefRecordDefinition,
      UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);
    this.recordStore.delete(membersQuery);

    final String consumerKey = userAccount.getString(UserAccount.CONSUMER_KEY);
    final Query jobsQuery = Query.equal(this.batchJobRecordDefinition, BatchJob.USER_ID,
      consumerKey);
    this.recordStore.delete(jobsQuery);

    delete(userAccount);
  }

  public void deleteUserGroup(final Record userGroup) {
    final Identifier userGroupId = userGroup.getIdentifier();

    final Query membersQuery = Query.equal(this.userGroupAccountXrefRecordDefinition,
      UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    this.recordStore.delete(membersQuery);

    final Query permissionsQuery = Query.equal(this.userGroupPermissionRecordDefinition,
      UserGroupPermission.USER_GROUP_ID, userGroupId);
    this.recordStore.delete(permissionsQuery);

    delete(userGroup);
  }

  public int deleteUserGroupAccountXref(final Record userGroup, final Record userAccount) {
    final Identifier userGroupId = userGroup.getIdentifier();
    final Identifier userAccountId = userAccount.getIdentifier();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

    final Query query = Query.and(this.userGroupAccountXrefRecordDefinition, filter);
    return this.recordStore.delete(query);
  }

  public int deleteUserGroupsForModule(final String moduleName) {
    final Query query = Query.equal(this.userGroupRecordDefinition, UserGroup.MODULE_NAME,
      moduleName);
    int i = 0;
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      for (final Record userGroup : reader) {
        deleteUserGroup(userGroup);
        i++;
      }
    } finally {
      reader.close();
    }
    return i;
  }

  public synchronized BatchJob getBatchJob(final Long batchJobId) {
    if (batchJobId == null) {
      return null;
    } else {
      synchronized (this.batchJobById) {
        BatchJob batchJob = this.batchJobById.get(batchJobId);
        if (batchJob == null) {
          final Record record = this.recordStore.load(BatchJob.BATCH_JOB, batchJobId);
          if (record != null) {
            batchJob = new BatchJob(record);
            this.batchJobById.put(batchJobId, batchJob);
          }
        }
        return batchJob;
      }
    }
  }

  public BatchJob getBatchJob(final Record record) {
    if (record == null) {
      return null;
    } else if (record instanceof BatchJob) {
      return (BatchJob)record;

    } else {
      synchronized (this.batchJobById) {
        final Long batchJobId = record.getLong(BatchJob.BATCH_JOB_ID);
        if (batchJobId == null) {
          return null;
        } else {
          BatchJob batchJob = this.batchJobById.get(batchJobId);
          if (batchJob == null) {
            batchJob = new BatchJob(record);
            this.batchJobById.put(batchJobId, batchJob);
          }
          return batchJob;
        }
      }
    }
  }

  public Record getBatchJob(final String consumerKey, final long batchJobId) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.USER_ID, consumerKey);
    filter.put(BatchJob.BATCH_JOB_ID, batchJobId);

    final Query query = Query.and(this.batchJobRecordDefinition, filter);
    return this.recordStore.queryFirst(query);
  }

  public long getBatchJobFileSize(final long jobId, final String path, final int sequenceNumber) {
    final Query query = new Query(BatchJobFile.BATCH_JOB_FILE);
    query.and(Q.equal(BatchJobFile.BATCH_JOB_ID, jobId));
    query.and(Q.equal(BatchJobFile.PATH, path));
    query.and(Q.equal(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber));
    final Record file = this.recordStore.queryFirst(query);
    if (file != null) {
      try {
        final Blob resultData = file.getValue(BatchJobFile.DATA);
        return resultData.length();
      } catch (final SQLException e) {
        throw new WrappedException(e);
      }
    }
    return 0;
  }

  public InputStream getBatchJobFileStream(final long jobId, final String path,
    final int sequenceNumber) {
    final Query query = new Query(BatchJobFile.BATCH_JOB_FILE);
    query.and(Q.equal(BatchJobFile.BATCH_JOB_ID, jobId));
    query.and(Q.equal(BatchJobFile.PATH, path));
    query.and(Q.equal(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber));
    final Record file = this.recordStore.queryFirst(query);
    if (file != null) {
      try {
        final Blob resultData = file.getValue(BatchJobFile.DATA);
        return resultData.getBinaryStream();
      } catch (final SQLException e) {
        throw new WrappedException(e);
      }
    }
    return null;
  }

  public List<Long> getBatchJobIds(final String businessApplicationName, final String jobStatus) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    filter.put(BatchJob.JOB_STATUS, jobStatus);
    final Query query = Query.and(this.batchJobRecordDefinition, filter);
    query.setFieldNames(BatchJob.BATCH_JOB_ID);
    final Reader<Record> batchJobs = this.recordStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final Record batchJob : batchJobs) {
        final Long batchJobId = batchJob.getLong(BatchJob.BATCH_JOB_ID);
        batchJobIds.add(batchJobId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  public List<Long> getBatchJobIdsToSchedule(final String businessApplicationName) {
    final Query query = new Query(this.batchJobRecordDefinition);
    query.setFieldNames(BatchJob.BATCH_JOB_ID);
    // TODO move to scheduling groups
    query.setWhereCondition(Q.sql(
      "JOB_STATUS IN ( 'processing') AND "
        + "NUM_SUBMITTED_GROUPS > 0 AND "
        + "COMPLETED_GROUP_RANGE <> concat('1~', NUM_SUBMITTED_GROUPS) AND BUSINESS_APPLICATION_NAME = ?",
      businessApplicationName));
    query.addOrderBy(BatchJob.LAST_SCHEDULED_TIMESTAMP, true);
    query.addOrderBy(BatchJob.BATCH_JOB_ID, true);
    final Reader<Record> batchJobs = this.recordStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final Record batchJob : batchJobs) {
        final Long batchJobId = batchJob.getLong(BatchJob.BATCH_JOB_ID);
        batchJobIds.add(batchJobId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  public Record getBatchJobLocked(final long batchJobId) {
    return this.recordStore.load(BatchJob.BATCH_JOB, batchJobId);
  }

  public Record getBatchJobResult(final long batchJobId, final long sequenceNumber) {
    final And where = Q.and(Q.equal(BatchJobResult.BATCH_JOB_ID, batchJobId),
      Q.equal(BatchJobResult.SEQUENCE_NUMBER, sequenceNumber));
    final Query query = new Query(BatchJobResult.BATCH_JOB_RESULT, where);
    return this.recordStore.queryFirst(query);
  }

  public List<Record> getBatchJobResults(final long batchJobId) {
    final Query query = Query.equal(this.batchJobResultRecordDefinition,
      BatchJobResult.BATCH_JOB_ID, batchJobId);
    query.setFieldNames(BatchJobResult.ALL_EXCEPT_BLOB);
    query.addOrderBy(BatchJobResult.SEQUENCE_NUMBER, true);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getBatchJobsForUser(final String consumerKey) {
    final Query query = Query.equal(this.batchJobRecordDefinition, BatchJob.USER_ID, consumerKey);
    query.addOrderBy(BatchJob.BATCH_JOB_ID, false);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getBatchJobsForUserAndApplication(final String consumerKey,
    final String businessApplicationName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.USER_ID, consumerKey);
    filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    final Query query = Query.and(this.batchJobRecordDefinition, filter);

    query.addOrderBy(BatchJob.BATCH_JOB_ID, false);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getConfigPropertiesForAllModules(final String environmentName,
    final String componentName, final String propertyName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    filter.put(ConfigProperty.PROPERTY_NAME, propertyName);
    final Query query = Query.and(this.configPropertyRecordDefinition, filter);

    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getConfigPropertiesForComponent(final String moduleName,
    final String componentName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    final Query query = Query.and(this.configPropertyRecordDefinition, filter);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getConfigPropertiesForModule(final String environmentName,
    final String moduleName, final String componentName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    final Query query = Query.and(this.configPropertyRecordDefinition, filter);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public Record getConfigProperty(final String environmentName, final String moduleName,
    final String componentName, final String propertyName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    filter.put(ConfigProperty.PROPERTY_NAME, propertyName);
    final Query query = Query.and(this.configPropertyRecordDefinition, filter);
    return this.recordStore.queryFirst(query);
  }

  /**
   * Get all the jobs that are either marked for deletion, or that have had a
   * status change timestamp less than the passed timestamp.
   *
   * @param keepUntilTimestamp The timestamp of the maximum age of the completed
   *          jobs to be retained.
   * @return The batch job ids.
   */
  public List<Long> getOldBatchJobIds(final Timestamp keepUntilTimestamp) {
    final Query query = new Query(this.batchJobRecordDefinition);
    query.setFieldNames(BatchJob.BATCH_JOB_ID);
    final And and = new And(new In(BatchJob.JOB_STATUS, BatchJobStatus.RESULTS_CREATED,
      BatchJobStatus.DOWNLOAD_INITIATED, BatchJobStatus.CANCELLED), Q.lessThan(
      this.batchJobRecordDefinition.getField(BatchJob.WHEN_STATUS_CHANGED), keepUntilTimestamp));
    query.setWhereCondition(and);
    final Reader<Record> batchJobs = this.recordStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final Record batchJob : batchJobs) {
        final Long batchJobId = batchJob.getLong(BatchJob.BATCH_JOB_ID);
        batchJobIds.add(batchJobId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  public RecordStore getRecordStore() {
    return this.recordStore;
  }

  public PlatformTransactionManager getTransactionManager() {
    return this.recordStore.getTransactionManager();
  }

  /**
   * Get the user with the specified consumer key.
   *
   * @param consumerKey The external user class.
   * @return The user account if it exists, null otherwise.
   */
  public Record getUserAccount(final String consumerKey) {
    final Query query = Query.equal(this.userAccountRecordDefinition, UserAccount.CONSUMER_KEY,
      consumerKey);
    return this.recordStore.queryFirst(query);
  }

  /**
   * Get the user with the specified external user class and external user name.
   *
   * @param userClass The external user class.
   * @param userName The external user name.
   * @return The user account if it exists, null otherwise.
   */
  public Record getUserAccount(final String userClass, final String userName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserAccount.USER_ACCOUNT_CLASS, userClass);
    filter.put(UserAccount.USER_NAME, userName);
    final Query query = Query.and(this.userAccountRecordDefinition, filter);
    return this.recordStore.queryFirst(query);
  }

  public List<Record> getUserAccountsLikeName(final String name) {
    if (Property.hasValue(name)) {
      final Condition consumerKeyLike = Q.iLike(UserAccount.CONSUMER_KEY, name);
      final Condition userNameLike = Q.iLike(UserAccount.USER_NAME, name);
      final Condition[] conditions = {
        consumerKeyLike, userNameLike
      };
      final Or or = new Or(conditions);
      final Query query = new Query(UserAccount.USER_ACCOUNT, or);
      final Reader<Record> reader = this.recordStore.query(query);
      try {
        return CollectionUtil.subList(reader, 20);
      } finally {
        reader.close();
      }
    } else {
      return Collections.emptyList();
    }
  }

  public Record getUserGroup(final long userGroupId) {
    return this.recordStore.load(UserGroup.USER_GROUP, userGroupId);
  }

  public Record getUserGroup(final String groupName) {
    final Query query = Query.equal(this.userGroupRecordDefinition, UserGroup.USER_GROUP_NAME,
      groupName);
    return this.recordStore.queryFirst(query);
  }

  public Record getUserGroup(final String moduleName, final String groupName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroup.MODULE_NAME, moduleName);
    filter.put(UserGroup.USER_GROUP_NAME, groupName);
    final Query query = Query.and(this.userGroupRecordDefinition, filter);
    return this.recordStore.queryFirst(query);
  }

  public Record getUserGroupPermission(final List<String> userGroupNames, final String moduleName,
    final String resourceClass, final String resourceId, final String actionName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroup.USER_GROUP_NAME, userGroupNames);
    filter.put("T." + UserGroupPermission.MODULE_NAME, moduleName);
    filter.put(UserGroupPermission.RESOURCE_CLASS, resourceClass);
    filter.put(UserGroupPermission.RESOURCE_ID, resourceId);
    filter.put(UserGroupPermission.ACTION_NAME, actionName);
    final Query query = Query.and(this.userGroupPermissionRecordDefinition, filter);
    query.setFromClause("CPF.CPF_USER_GROUP_PERMISSIONS T"
      + " JOIN CPF.CPF_USER_GROUPS G ON T.USER_GROUP_ID = G.USER_GROUP_ID");
    return this.recordStore.queryFirst(query);
  }

  public List<Record> getUserGroupPermissions(final Record userGroup, final String moduleName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupPermission.USER_GROUP_ID, userGroup.getIdentifier());
    filter.put(UserGroupPermission.MODULE_NAME, moduleName);
    final Query query = Query.and(this.userGroupPermissionRecordDefinition, filter);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getUserGroupsForModule(final String moduleName) {
    final Query query = Query.equal(this.userGroupRecordDefinition, UserGroup.MODULE_NAME,
      moduleName);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public Set<Record> getUserGroupsForUserAccount(final Record userAccount) {
    final Query query = new Query(UserGroup.USER_GROUP);
    query.setFromClause("CPF.CPF_USER_GROUPS T"
      + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_GROUP_ID = X.USER_GROUP_ID");

    query.setWhereCondition(Q.equal(new JdbcLongFieldDefinition("X.USER_ACCOUNT_ID"),
      userAccount.getIdentifier()));
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      final List<Record> groups = reader.read();
      return new LinkedHashSet<Record>(groups);
    } finally {
      reader.close();
    }
  }

  public boolean hasBatchJobUnexecutedJobs(final long batchJobId) {
    final BatchJob batchJob = getBatchJob(batchJobId);
    if (batchJob == null) {
      return false;
    } else {
      return batchJob.hasAvailableGroup();
    }
  }

  private void insertStatistics(final BusinessApplicationStatistics statistics,
    final String businessApplicationName, final String durationType, final Date startTime,
    final String valuesString) {
    Integer databaseId;
    final Record applicationStatistics;
    databaseId = ((Number)this.recordStore.createPrimaryIdValue(BusinessApplicationStatistics.APPLICATION_STATISTICS)).intValue();
    applicationStatistics = this.recordStore.create(BusinessApplicationStatistics.APPLICATION_STATISTICS);
    applicationStatistics.setValue(BusinessApplicationStatistics.APPLICATION_STATISTIC_ID,
      databaseId);
    applicationStatistics.setValue(BusinessApplicationStatistics.BUSINESS_APPLICATION_NAME,
      businessApplicationName);
    applicationStatistics.setValue(BusinessApplicationStatistics.START_TIMESTAMP, startTime);
    applicationStatistics.setValue(BusinessApplicationStatistics.DURATION_TYPE, durationType);
    applicationStatistics.setValue(BusinessApplicationStatistics.STATISTIC_VALUES, valuesString);
    this.recordStore.insert(applicationStatistics);
    statistics.setDatabaseId(databaseId);
  }

  public void saveStatistics(final BusinessApplicationStatistics statistics) {
    final Integer databaseId = statistics.getDatabaseId();
    final String businessApplicationName = statistics.getBusinessApplicationName();
    final String durationType = statistics.getDurationType();
    final Date startTime = statistics.getStartTime();

    final Map<String, Long> values = statistics.toMap();
    if (values.isEmpty()) {
      if (databaseId != null) {
        deleteBusinessApplicationStatistics(databaseId);
      }
    } else {
      final String valuesString = JsonMapIoFactory.toString(values);

      final Record applicationStatistics;
      if (databaseId == null) {
        insertStatistics(statistics, businessApplicationName, durationType, startTime, valuesString);
      } else if (statistics.isModified()) {
        applicationStatistics = this.recordStore.load(
          BusinessApplicationStatistics.APPLICATION_STATISTICS, databaseId);
        if (applicationStatistics == null) {
          insertStatistics(statistics, businessApplicationName, durationType, startTime,
            valuesString);
        } else {
          applicationStatistics.setValue(BusinessApplicationStatistics.STATISTIC_VALUES,
            valuesString);
          this.recordStore.update(applicationStatistics);
        }
      }

      statistics.setModified(false);
    }
  }

  public boolean setBatchJobDownloaded(final long batchJobId) {
    final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;

    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
      + "JOB_STATUS = 'downloadInitiated', WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ? "
      + "WHERE JOB_STATUS = 'resultsCreated' AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      final String username = getUsername();
      return JdbcUtils.executeUpdate(jdbcRecordStore, sql, now, now, username, batchJobId) == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set job downloaded " + batchJobId, e);
    }
  }

  public boolean setBatchJobFailed(final long batchJobId) {
    final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;

    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
      + "COMPLETED_REQUEST_RANGE = null, "//
      + "FAILED_REQUEST_RANGE = concat('1~', NUM_SUBMITTED_REQUESTS), "//
      + "JOB_STATUS = 'resultsCreated', COMPLETED_TIMESTAMP = ?, WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ? "
      + "WHERE JOB_STATUS = 'creatingRequests' AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      final String username = getUsername();
      return JdbcUtils.executeUpdate(jdbcRecordStore, sql, now, now, now, username, batchJobId) == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set started status", e);
    }
  }

  public boolean setBatchJobRequestsFailed(final long batchJobId, final int numSubmittedRequests,
    final int numFailedRequests, final int groupSize, final int numGroups) {
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
        final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET " //
          + "NUM_SUBMITTED_REQUESTS = ?, "//
          + "FAILED_REQUEST_RANGE = ?, "//
          + "GROUP_SIZE = ?, "//
          + "NUM_SUBMITTED_GROUPS = ?, "//
          + "JOB_STATUS = 'processed', "//
          + "LAST_SCHEDULED_TIMESTAMP = ?, "//
          + "WHEN_STATUS_CHANGED = ?, "//
          + "WHEN_UPDATED = ?, "//
          + "WHO_UPDATED = ? "//
          + "WHERE JOB_STATUS IN ('creatingRequests') AND BATCH_JOB_ID = ?";
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final boolean result = JdbcUtils.executeUpdate(jdbcRecordStore, sql, numSubmittedRequests,
          numFailedRequests, groupSize, numGroups, now, now, now, getUsername(), batchJobId) == 1;
        return result;
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public boolean setBatchJobStatus(final long batchJobId, final String oldJobStatus,
    final String newJobStatus) {
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
        final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ?, JOB_STATUS = ? WHERE JOB_STATUS = ? AND BATCH_JOB_ID = ?";
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final String username = getUsername();
        final int count = JdbcUtils.executeUpdate(jdbcRecordStore, sql, now, now, username,
          newJobStatus, oldJobStatus, batchJobId);
        return count == 1;
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void setConfigPropertyValue(final Record object, final Object value) {
    if (value == null) {
      object.setValue(ConfigProperty.PROPERTY_VALUE, value);
    } else {
      final String stringValue;
      final String valueType = object.getValue(ConfigProperty.PROPERTY_VALUE_TYPE);
      final DataType dataType = DataTypes.getType(QName.valueOf(valueType));
      if (dataType == null) {
        stringValue = value.toString();
      } else {
        @SuppressWarnings("unchecked")
        final Class<Object> dataTypeClass = (Class<Object>)dataType.getJavaClass();
        final StringConverter<Object> converter = StringConverterRegistry.getInstance()
          .getConverter(dataTypeClass);
        if (converter == null) {
          stringValue = value.toString();
        } else {
          stringValue = converter.toString(value);
        }
      }
      if (Property.hasValue(stringValue)) {
        object.setValue(ConfigProperty.PROPERTY_VALUE, stringValue);
      } else {
        object.setValue(ConfigProperty.PROPERTY_VALUE, null);
      }
    }
  }

  public void setRecordStore(final RecordStore recordStore) {
    this.recordStore = recordStore;
    this.batchJobRecordDefinition = recordStore.getRecordDefinition(BatchJob.BATCH_JOB);
    this.batchJobResultRecordDefinition = recordStore.getRecordDefinition(BatchJobResult.BATCH_JOB_RESULT);
    this.businessApplicationStatisticsRecordDefinition = recordStore.getRecordDefinition(BusinessApplicationStatistics.APPLICATION_STATISTICS);
    this.configPropertyRecordDefinition = recordStore.getRecordDefinition(ConfigProperty.CONFIG_PROPERTY);
    this.userAccountRecordDefinition = recordStore.getRecordDefinition(UserAccount.USER_ACCOUNT);
    this.userGroupRecordDefinition = recordStore.getRecordDefinition(UserGroup.USER_GROUP);
    this.userGroupPermissionRecordDefinition = recordStore.getRecordDefinition(UserGroupPermission.USER_GROUP_PERMISSION);
    this.userGroupAccountXrefRecordDefinition = recordStore.getRecordDefinition(UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
  }

  /**
   * Update the status to processed of all the {@link BatchJob}s which are in
   * the processed status and have the request execution/failed counts equal to
   * the number of submitted requests.
   *
   * @param businessApplicationName The business application names to update
   *          the status for.
   * @return The number of records updated.
   */
  public int updateBatchJobProcessedStatus(final String businessApplicationName) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "JOB_STATUS = 'processed', WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = 'SYSTEM' "
        + "WHERE JOB_STATUS = 'processing' AND BUSINESS_APPLICATION_NAME = ? AND COMPLETED_REQUEST_RANGE + FAILED_REQUEST_RANGE = NUM_SUBMITTED_REQUESTS";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(jdbcRecordStore, sql, now, now, businessApplicationName);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to update status: " + sql, e);
      }
    }

    return 0;
  }

  /**
   * Set the status to the newStatus for all BatchJob objects for the list of
   * businessApplicationNames which have the oldStatus.
   *
   * @param newStatus The status to change the jobs to.
   * @param oldStatus The status of jobs to update.
   * @param businessApplicationName The list of business application names.
   * @return The number of BatchJobs updated.
   */
  public int updateBatchJobStatus(final String newStatus, final String oldStatus,
    final String businessApplicationName) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "JOB_STATUS = ?, WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = 'SYSTEM' "
        + "WHERE JOB_STATUS = ? AND BUSINESS_APPLICATION_NAME = ?";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(jdbcRecordStore, sql, newStatus, now, now, oldStatus,
          businessApplicationName);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to update status: " + sql, e);
      }
    }
    return 0;
  }

  public int updateJobUserId(final String oldUserId, final String newUserId) {
    final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET USER_ID = ? WHERE USER_ID = ?";
    try {
      return JdbcUtils.executeUpdate(jdbcRecordStore, sql, newUserId, oldUserId);
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to change jobs for user rename", e);
    }
  }

  public int updateResetGroupsForRestart(final String businessApplicationName) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final String sql = "UPDATE CPF.CPF_BATCH_JOB_EXECUTION_GROUPS SET STARTED_IND = 0 WHERE STARTED_IND = 1 AND COMPLETED_IND = 0 AND BATCH_JOB_ID IN (SELECT BATCH_JOB_ID FROM CPF.CPF_BATCH_JOBS WHERE BUSINESS_APPLICATION_NAME = ?)";
      try {
        return JdbcUtils.executeUpdate(jdbcRecordStore, sql, businessApplicationName);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to reset started status", e);
      }
    }

    return 0;
  }

  public void write(final Record record) {
    try (
      final Writer<Record> writer = this.recordStore.getWriter()) {
      write(writer, record);
    }
  }

  protected void write(final Writer<Record> writer, final Record record) {
    final String username = getUsername();
    final Timestamp time = new Timestamp(System.currentTimeMillis());
    switch (record.getState()) {
      case New:
        final RecordDefinition recordDefinition = record.getRecordDefinition();

        if (recordDefinition.getIdFieldIndex() != -1 && record.getIdentifier() == null) {
          final Object id = this.recordStore.createPrimaryIdValue(recordDefinition.getPath());
          record.setIdValue(id);
        }
        record.setValue(Common.WHO_CREATED, username);
        record.setValue(Common.WHEN_CREATED, time);
        record.setValue(Common.WHO_UPDATED, username);
        record.setValue(Common.WHEN_UPDATED, time);
      break;
      case Modified:
        record.setValue(Common.WHO_UPDATED, username);
        record.setValue(Common.WHEN_UPDATED, time);
      break;
      default:
      break;
    }
    writer.write(record);
  }

}
