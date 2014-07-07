package ca.bc.gov.open.cpf.api.domain;

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
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BusinessApplicationStatistics;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.module.ResourcePermission;

import com.revolsys.collection.ResultPager;
import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.identifier.Identifier;
import com.revolsys.data.io.RecordStore;
import com.revolsys.data.query.And;
import com.revolsys.data.query.Between;
import com.revolsys.data.query.Condition;
import com.revolsys.data.query.Equal;
import com.revolsys.data.query.In;
import com.revolsys.data.query.Or;
import com.revolsys.data.query.Q;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordUtil;
import com.revolsys.data.record.schema.Attribute;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.io.MapWriter;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.attribute.JdbcLongAttribute;
import com.revolsys.jdbc.io.JdbcRecordStore;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.CollectionUtil;

public class CpfDataAccessObject {
  private RecordStore recordStore;

  private RecordDefinition batchJobMetaData;

  private RecordDefinition batchJobExecutionGroupMetaData;

  private RecordDefinition batchJobResultMetaData;

  private RecordDefinition businessApplicationStatisticsMetaData;

  private RecordDefinition configPropertyMetaData;

  private RecordDefinition userAccountMetaData;

  private RecordDefinition userGroupMetaData;

  private RecordDefinition userGroupAccountXrefMetaData;

  private RecordDefinition userGroupPermissionMetaData;

  public CpfDataAccessObject() {
  }

  public boolean cancelBatchJob(final long jobId) {
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final String username = getUsername();
        final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
        final DataSource dataSource = jdbcRecordStore.getDataSource();
        final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
          + "NUM_SCHEDULED_GROUPS  = 0, " + "NUM_COMPLETED_REQUESTS = 0, "
          + "NUM_FAILED_REQUESTS = NUM_SUBMITTED_REQUESTS,"
          + "STRUCTURED_INPUT_DATA = NULL, " + "WHEN_STATUS_CHANGED = ?, "
          + "JOB_STATUS = 'cancelled',"
          + "WHEN_UPDATED = ?, WHO_UPDATED = ? WHERE BATCH_JOB_ID = ?";
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        if (JdbcUtils.executeUpdate(dataSource, sql, now, now, username, jobId) == 1) {
          try {
            deleteBatchJobResults(jobId);
          } finally {
            deleteBatchJobExecutionGroups(jobId);
          }
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

  @PreDestroy
  public void close() {
    this.recordStore = null;
    this.batchJobMetaData = null;
    this.batchJobExecutionGroupMetaData = null;
    this.batchJobResultMetaData = null;
    this.businessApplicationStatisticsMetaData = null;
    this.configPropertyMetaData = null;
    this.userAccountMetaData = null;
    this.userGroupMetaData = null;
    this.userGroupAccountXrefMetaData = null;
    this.userGroupPermissionMetaData = null;
  }

  public Record create(final String typeName) {
    return this.recordStore.create(typeName);
  }

  public Record createBatchJobExecutionGroup(final JobController jobController,
    final long batchJobId, final int groupSequenceNumber,
    final String structuredInputData, final int requestCount) {
    final Record batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, groupSequenceNumber, requestCount);
    jobController.setStructuredInputData(batchJobId, groupSequenceNumber,
      batchJobExecutionGroup, structuredInputData);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  public Record createBatchJobExecutionGroup(final JobController jobController,
    final long batchJobId, final int groupSequenceNumber,
    final String errorCode, final String errorMessage,
    final String errorDebugMessage) {
    final Map<String, Object> error = new HashMap<String, Object>();
    error.put("requestSequenceNumber", 1);
    error.put("errorCode", errorCode);
    error.put("errorMessage", errorMessage);
    error.put("errorDebugMessage", errorDebugMessage);
    final List<Map<String, Object>> resultDataItems = Collections.singletonList(error);

    final Map<String, Object> resultData = Collections.<String, Object> singletonMap(
      "items", resultDataItems);
    final Record batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, 1, 1);
    final String resultDataString = JsonMapIoFactory.toString(resultData);

    jobController.setStructuredResultData(batchJobId, groupSequenceNumber,
      batchJobExecutionGroup, resultDataString);

    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.STARTED_IND, 1);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.COMPLETED_IND, 1);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  protected Record createBatchJobExecutionGroup(final long batchJobId,
    final int groupSequenceNumber, final int requestCount) {
    final Record batchJobExecutionGroup = create(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.BATCH_JOB_ID,
      batchJobId);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.COMPLETED_IND, 0);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.STARTED_IND, 0);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.NUM_SUBMITTED_REQUESTS, requestCount);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.NUM_COMPLETED_REQUESTS, 0);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.NUM_FAILED_REQUESTS,
      0);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.SEQUENCE_NUMBER,
      groupSequenceNumber);
    return batchJobExecutionGroup;
  }

  public Record createBatchJobExecutionGroup(final long batchJobId,
    final int requestSequenceNumber, final String inputDataContentType,
    final Resource inputData) {
    final Record batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, requestSequenceNumber, 1);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE, inputDataContentType);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.INPUT_DATA,
      inputData);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  public Record createBatchJobExecutionGroup(final long batchJobId,
    final int requestSequenceNumber, final String inputDataContentType,
    final String inputDataUrl) {
    final Record batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, requestSequenceNumber, 1);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE, inputDataContentType);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.INPUT_DATA_URL,
      inputDataUrl);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  public Record createConfigProperty(final String environmentName,
    final String moduleName, final String componentName,
    final String propertyName, final Object propertyValue,
    final DataType propertyValueType) {
    final Record configProperty = create(ConfigProperty.CONFIG_PROPERTY);
    configProperty.setValue(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    configProperty.setValue(ConfigProperty.MODULE_NAME, moduleName);
    configProperty.setValue(ConfigProperty.COMPONENT_NAME, componentName);
    configProperty.setValue(ConfigProperty.PROPERTY_NAME, propertyName);
    configProperty.setValue(ConfigProperty.PROPERTY_VALUE_TYPE,
      propertyValueType.toString());

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

  public Record createUserAccount(final String userAccountClass,
    final String userAccountName, final String consumerKey,
    final String consumerSecret) {
    final Record userAccount = create(UserAccount.USER_ACCOUNT);

    userAccount.setValue(UserAccount.USER_NAME, userAccountName);
    userAccount.setValue(UserAccount.USER_ACCOUNT_CLASS, userAccountClass);
    userAccount.setValue(UserAccount.CONSUMER_KEY, consumerKey);
    userAccount.setValue(UserAccount.CONSUMER_SECRET, consumerSecret);
    userAccount.setValue(UserAccount.ACTIVE_IND, 1);

    write(userAccount);
    return userAccount;
  }

  public Record createUserGroup(final String moduleName,
    final String groupName, final String description) {
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

  public Record createUserGroupAccountXref(final Record userGroup,
    final Record userAccount) {
    final Identifier userGroupId = userGroup.getIdentifier();
    final Identifier userAccountId = userAccount.getIdentifier();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

    final Query query = Query.and(this.userGroupAccountXrefMetaData, filter);

    Record userGroupAccountXref = this.recordStore.queryFirst(query);
    if (userGroupAccountXref == null) {

      userGroupAccountXref = create(UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
      userGroupAccountXref.setValue(UserGroupAccountXref.USER_GROUP_ID,
        userGroupId);
      userGroupAccountXref.setValue(UserGroupAccountXref.USER_ACCOUNT_ID,
        userAccountId);

      write(userGroupAccountXref);
    }
    return userGroupAccountXref;
  }

  public Record createUserGroupPermission(final Record userGroup,
    final String groupName, final ResourcePermission permission) {
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
    deleteBatchJobExecutionGroups(batchJobId);

    final Query query = Query.equal(this.batchJobMetaData,
      BatchJob.BATCH_JOB_ID, batchJobId);
    return this.recordStore.delete(query);
  }

  public int deleteBatchJobExecutionGroups(final Long batchJobId) {
    final Query query = Query.equal(this.batchJobExecutionGroupMetaData,
      BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    return this.recordStore.delete(query);
  }

  public int deleteBatchJobResults(final Long batchJobId) {
    final Query query = Query.equal(this.batchJobResultMetaData,
      BatchJobResult.BATCH_JOB_ID, batchJobId);
    return this.recordStore.delete(query);
  }

  public int deleteBusinessApplicationStatistics(
    final Integer businessApplicationStatisticsId) {
    final Query query = Query.equal(this.businessApplicationStatisticsMetaData,
      BusinessApplicationStatistics.APPLICATION_STATISTIC_ID,
      businessApplicationStatisticsId);
    return this.recordStore.delete(query);
  }

  public int deleteConfigPropertiesForModule(final String moduleName) {
    final Query query = Query.equal(this.configPropertyMetaData,
      ConfigProperty.MODULE_NAME, moduleName);
    return this.recordStore.delete(query);
  }

  public void deleteUserAccount(final Record userAccount) {
    final Identifier userAccountId = userAccount.getIdentifier();
    final Query membersQuery = Query.equal(this.userGroupAccountXrefMetaData,
      UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);
    this.recordStore.delete(membersQuery);

    final String consumerKey = userAccount.getString(UserAccount.CONSUMER_KEY);
    final Query jobsQuery = Query.equal(this.batchJobMetaData,
      BatchJob.USER_ID, consumerKey);
    this.recordStore.delete(jobsQuery);

    delete(userAccount);
  }

  public void deleteUserGroup(final Record userGroup) {
    final Identifier userGroupId = userGroup.getIdentifier();

    final Query membersQuery = Query.equal(this.userGroupAccountXrefMetaData,
      UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    this.recordStore.delete(membersQuery);

    final Query permissionsQuery = Query.equal(
      this.userGroupPermissionMetaData, UserGroupPermission.USER_GROUP_ID,
      userGroupId);
    this.recordStore.delete(permissionsQuery);

    delete(userGroup);
  }

  public int deleteUserGroupAccountXref(final Record userGroup,
    final Record userAccount) {
    final Identifier userGroupId = userGroup.getIdentifier();
    final Identifier userAccountId = userAccount.getIdentifier();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

    final Query query = Query.and(this.userGroupAccountXrefMetaData, filter);
    return this.recordStore.delete(query);
  }

  public int deleteUserGroupsForModule(final String moduleName) {
    final Query query = Query.equal(this.userGroupMetaData,
      UserGroup.MODULE_NAME, moduleName);
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

  public Record getBatchJob(final long batchJobId) {
    return this.recordStore.load(BatchJob.BATCH_JOB, batchJobId);
  }

  public Record getBatchJob(final String consumerKey, final long batchJobId) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.USER_ID, consumerKey);
    filter.put(BatchJob.BATCH_JOB_ID, batchJobId);

    final Query query = Query.and(this.batchJobMetaData, filter);
    return this.recordStore.queryFirst(query);
  }

  public Record getBatchJobExecutionGroup(final long batchJobId,
    final long groupSequenceNumber) {
    final Query query = new Query(
      BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP);
    query.and(Q.equal(BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId));
    query.and(Q.equal(BatchJobExecutionGroup.SEQUENCE_NUMBER,
      groupSequenceNumber));
    return this.recordStore.queryFirst(query);
  }

  public List<Long> getBatchJobIds(final String businessApplicationName,
    final String jobStatus) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    filter.put(BatchJob.JOB_STATUS, jobStatus);
    final Query query = Query.and(this.batchJobMetaData, filter);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    final Reader<Record> batchJobs = this.recordStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final Record batchJob : batchJobs) {
        final Long batchJobId = RecordUtil.getLong(batchJob,
          BatchJob.BATCH_JOB_ID);
        batchJobIds.add(batchJobId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  public List<Long> getBatchJobIdsToSchedule(
    final String businessApplicationName) {
    final Query query = Query.equal(this.batchJobMetaData,
      BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    // TODO move to scheduling groups
    query.setWhereCondition(Q.sql("JOB_STATUS IN ('requestsCreated', 'processing') AND "
      + "NUM_SUBMITTED_GROUPS > 0 AND "
      + "NUM_SCHEDULED_GROUPS + NUM_COMPLETED_GROUPS < NUM_SUBMITTED_GROUPS "));
    query.addOrderBy(BatchJob.NUM_SCHEDULED_GROUPS, true);
    query.addOrderBy(BatchJob.LAST_SCHEDULED_TIMESTAMP, true);
    query.addOrderBy(BatchJob.BATCH_JOB_ID, true);
    final Reader<Record> batchJobs = this.recordStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final Record batchJob : batchJobs) {
        final Long batchJobId = RecordUtil.getLong(batchJob,
          BatchJob.BATCH_JOB_ID);
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

  public Record getBatchJobResult(final long batchJobId,
    final long sequenceNumber) {
    final And where = Q.and(Q.equal(BatchJobResult.BATCH_JOB_ID, batchJobId),
      Q.equal(BatchJobResult.SEQUENCE_NUMBER, sequenceNumber));
    final Query query = new Query(BatchJobResult.BATCH_JOB_RESULT, where);
    return this.recordStore.queryFirst(query);
  }

  public List<Record> getBatchJobResults(final long batchJobId) {
    final Query query = Query.equal(this.batchJobResultMetaData,
      BatchJobResult.BATCH_JOB_ID, batchJobId);
    query.setAttributeNames(BatchJobResult.ALL_EXCEPT_BLOB);
    query.addOrderBy(BatchJobResult.SEQUENCE_NUMBER, true);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getBatchJobsForUser(final String consumerKey) {
    final Query query = Query.equal(this.batchJobMetaData, BatchJob.USER_ID,
      consumerKey);
    query.addOrderBy(BatchJob.BATCH_JOB_ID, false);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getBatchJobsForUserAndApplication(
    final String consumerKey, final String businessApplicationName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.USER_ID, consumerKey);
    filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    final Query query = Query.and(this.batchJobMetaData, filter);

    query.addOrderBy(BatchJob.BATCH_JOB_ID, false);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getConfigPropertiesForAllModules(
    final String environmentName, final String componentName,
    final String propertyName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    filter.put(ConfigProperty.PROPERTY_NAME, propertyName);
    final Query query = Query.and(this.configPropertyMetaData, filter);

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
    final Query query = Query.and(this.configPropertyMetaData, filter);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getConfigPropertiesForModule(
    final String environmentName, final String moduleName,
    final String componentName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    final Query query = Query.and(this.configPropertyMetaData, filter);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public Record getConfigProperty(final String environmentName,
    final String moduleName, final String componentName,
    final String propertyName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    filter.put(ConfigProperty.PROPERTY_NAME, propertyName);
    final Query query = Query.and(this.configPropertyMetaData, filter);
    return this.recordStore.queryFirst(query);
  }

  public Long getNonExecutingGroupSequenceNumber(final Long batchJobId) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    filter.put(BatchJobExecutionGroup.STARTED_IND, 0);
    filter.put(BatchJobExecutionGroup.COMPLETED_IND, 0);
    filter.put(BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    filter.put(BatchJobExecutionGroup.STARTED_IND, 0);
    filter.put(BatchJobExecutionGroup.COMPLETED_IND, 0);
    final Query query = Query.and(this.batchJobExecutionGroupMetaData, filter);
    query.setAttributeNames(BatchJobExecutionGroup.SEQUENCE_NUMBER);
    query.addOrderBy(BatchJobExecutionGroup.SEQUENCE_NUMBER, true);
    query.setLimit(1);
    final Record batchJobExecutionGroup = this.recordStore.queryFirst(query);
    if (batchJobExecutionGroup == null) {
      return null;
    } else {
      return RecordUtil.getLong(batchJobExecutionGroup,
        BatchJobExecutionGroup.SEQUENCE_NUMBER);
    }
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
    final Query query = new Query(this.batchJobMetaData);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    final Condition[] conditions = {
      new In(BatchJob.JOB_STATUS, "resultsCreated", "downloadInitiated",
        "cancelled"),
      Q.lessThan(
            this.batchJobMetaData.getAttribute(BatchJob.WHEN_STATUS_CHANGED),
        keepUntilTimestamp)
    };
    query.setWhereCondition(new And(conditions));
    final Reader<Record> batchJobs = this.recordStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final Record batchJob : batchJobs) {
        final Long batchJobId = RecordUtil.getLong(batchJob,
          BatchJob.BATCH_JOB_ID);
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
    final Query query = Query.equal(this.userAccountMetaData,
      UserAccount.CONSUMER_KEY, consumerKey);
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
    final Query query = Query.and(this.userAccountMetaData, filter);
    return this.recordStore.queryFirst(query);
  }

  public ResultPager<Record> getUserAccountsForUserGroup(final Record userGroup) {
    final Condition equal = Q.equal(UserGroupAccountXref.USER_GROUP_ID,
      userGroup.getIdentifier());
    final Query query = new Query(UserAccount.USER_ACCOUNT, equal);
    query.setFromClause("CPF.CPF_USER_ACCOUNTS T"
      + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_ACCOUNT_ID = X.USER_ACCOUNT_ID");

    final ResultPager<Record> pager = this.recordStore.page(query);
    return pager;
  }

  public List<Record> getUserAccountsLikeName(final String name) {
    if (StringUtils.hasText(name)) {
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
    final Query query = Query.equal(this.userGroupMetaData,
      UserGroup.USER_GROUP_NAME, groupName);
    return this.recordStore.queryFirst(query);
  }

  public Record getUserGroup(final String moduleName, final String groupName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroup.MODULE_NAME, moduleName);
    filter.put(UserGroup.USER_GROUP_NAME, groupName);
    final Query query = Query.and(this.userGroupMetaData, filter);
    return this.recordStore.queryFirst(query);
  }

  public Record getUserGroupPermission(final List<String> userGroupNames,
    final String moduleName, final String resourceClass,
    final String resourceId, final String actionName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroup.USER_GROUP_NAME, userGroupNames);
    filter.put("T." + UserGroupPermission.MODULE_NAME, moduleName);
    filter.put(UserGroupPermission.RESOURCE_CLASS, resourceClass);
    filter.put(UserGroupPermission.RESOURCE_ID, resourceId);
    filter.put(UserGroupPermission.ACTION_NAME, actionName);
    final Query query = Query.and(this.userGroupPermissionMetaData, filter);
    query.setFromClause("CPF.CPF_USER_GROUP_PERMISSIONS T"
      + " JOIN CPF.CPF_USER_GROUPS G ON T.USER_GROUP_ID = G.USER_GROUP_ID");
    return this.recordStore.queryFirst(query);
  }

  public List<Record> getUserGroupPermissions(final Record userGroup,
    final String moduleName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupPermission.USER_GROUP_ID, userGroup.getIdentifier());
    filter.put(UserGroupPermission.MODULE_NAME, moduleName);
    final Query query = Query.and(this.userGroupPermissionMetaData, filter);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<Record> getUserGroupsForModule(final String moduleName) {
    final Query query = Query.equal(this.userGroupMetaData,
      UserGroup.MODULE_NAME, moduleName);
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

    query.setWhereCondition(Q.equal(new JdbcLongAttribute("X.USER_ACCOUNT_ID"),
      userAccount.getIdentifier()));
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      final List<Record> groups = reader.read();
      return new LinkedHashSet<Record>(groups);
    } finally {
      reader.close();
    }
  }

  public String getUsername() {
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

  public boolean hasBatchJobUnexecutedJobs(final long batchJobId) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      // TODO move to scheduling groups
      final String sql = "SELECT NUM_SUBMITTED_GROUPS - NUM_COMPLETED_GROUPS - NUM_SCHEDULED_GROUPS"
        + " FROM CPF.CPF_BATCH_JOBS WHERE BATCH_JOB_ID = ?";
      try {
        return JdbcUtils.selectInt(dataSource, sql, batchJobId) <= 0;
      } catch (final IllegalArgumentException e) {
        return false;
      }
    }
    return false;
  }

  private void insertStatistics(final BusinessApplicationStatistics statistics,
    final String businessApplicationName, final String durationType,
    final Date startTime, final String valuesString) {
    Integer databaseId;
    final Record applicationStatistics;
    databaseId = ((Number)this.recordStore.createPrimaryIdValue(BusinessApplicationStatistics.APPLICATION_STATISTICS)).intValue();
    applicationStatistics = this.recordStore.create(BusinessApplicationStatistics.APPLICATION_STATISTICS);
    applicationStatistics.setValue(
      BusinessApplicationStatistics.APPLICATION_STATISTIC_ID, databaseId);
    applicationStatistics.setValue(
      BusinessApplicationStatistics.BUSINESS_APPLICATION_NAME,
      businessApplicationName);
    applicationStatistics.setValue(
      BusinessApplicationStatistics.START_TIMESTAMP, startTime);
    applicationStatistics.setValue(BusinessApplicationStatistics.DURATION_TYPE,
      durationType);
    applicationStatistics.setValue(
      BusinessApplicationStatistics.STATISTIC_VALUES, valuesString);
    this.recordStore.insert(applicationStatistics);
    statistics.setDatabaseId(databaseId);
  }

  public boolean isBatchJobCompleted(final long batchJobId) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "SELECT NUM_SUBMITTED_REQUESTS - NUM_COMPLETED_REQUESTS - NUM_FAILED_REQUESTS FROM CPF.CPF_BATCH_JOBS WHERE BATCH_JOB_ID = ?";
      try {
        return JdbcUtils.selectInt(dataSource, sql, batchJobId) <= 0;
      } catch (final IllegalArgumentException e) {
        return false;
      }
    }
    return false;
  }

  protected boolean postProcessWriteError(final MapWriter errorMapWriter,
    final Map<String, Object> resultMap) {
    boolean written;
    final String errorCode = (String)resultMap.get("errorCode");
    final Number requestSequenceNumber = (Number)resultMap.get("requestSequenceNumber");
    final String errorMessage = (String)resultMap.get("errorMessage");
    final Map<String, String> errorMap = new LinkedHashMap<String, String>();
    errorMap.put("sequenceNumber",
      StringConverterRegistry.toString(requestSequenceNumber));
    errorMap.put("errorCode", errorCode);
    errorMap.put("errorMessage", errorMessage);
    errorMapWriter.write(errorMap);
    written = true;
    return written;
  }

  @SuppressWarnings("unchecked")
  protected void postProcessWriteStructuredResult(
    final com.revolsys.io.Writer<Record> structuredDataWriter,
    final RecordDefinition resultMetaData,
    final Map<String, Object> defaultProperties,
    final Map<String, Object> resultData) {
    final List<Map<String, Object>> results = (List<Map<String, Object>>)resultData.get("results");
    final Number sequenceNumber = (Number)resultData.get("requestSequenceNumber");
    int i = 1;
    for (final Map<String, Object> structuredResultMap : results) {
      final Record structuredResult = RecordUtil.getObject(resultMetaData,
        structuredResultMap);

      final Map<String, Object> properties = (Map<String, Object>)structuredResultMap.get("customizationProperties");
      if (properties != null && !properties.isEmpty()) {
        structuredDataWriter.setProperties(properties);
      }

      structuredResult.put("sequenceNumber", sequenceNumber);
      structuredResult.put("resultNumber", i);
      structuredDataWriter.write(structuredResult);
      if (properties != null && !properties.isEmpty()) {

        structuredDataWriter.clearProperties();
        structuredDataWriter.setProperties(defaultProperties);
      }
      i++;
    }
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
        insertStatistics(statistics, businessApplicationName, durationType,
          startTime, valuesString);
      } else if (statistics.isModified()) {
        applicationStatistics = this.recordStore.load(
          BusinessApplicationStatistics.APPLICATION_STATISTICS, databaseId);
        if (applicationStatistics == null) {
          insertStatistics(statistics, businessApplicationName, durationType,
            startTime, valuesString);
        } else {
          applicationStatistics.setValue(
            BusinessApplicationStatistics.STATISTIC_VALUES, valuesString);
          this.recordStore.update(applicationStatistics);
        }
      }

      statistics.setModified(false);
    }
  }

  public boolean setBatchJobCompleted(final long batchJobId) {
    final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
    final DataSource dataSource = jdbcRecordStore.getDataSource();

    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
      + "NUM_COMPLETED_GROUPS = NUM_SUBMITTED_GROUPS, NUM_SCHEDULED_GROUPS = 0, STRUCTURED_INPUT_DATA = NULL, JOB_STATUS = 'resultsCreated', COMPLETED_TIMESTAMP = ?, WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ? "
      + "WHERE JOB_STATUS IN ('creatingRequests','creatingResults') AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      return JdbcUtils.executeUpdate(dataSource, sql, now, now, now,
        getUsername(), batchJobId) == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set job completed " + batchJobId, e);
    }
  }

  public boolean setBatchJobDownloaded(final long batchJobId) {
    final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
    final DataSource dataSource = jdbcRecordStore.getDataSource();

    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
      + "JOB_STATUS = 'downloadInitiated', WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ? "
      + "WHERE JOB_STATUS = 'resultsCreated' AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      final String username = getUsername();
      return JdbcUtils.executeUpdate(dataSource, sql, now, now, username,
        batchJobId) == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set job downloaded " + batchJobId,
        e);
    }
  }

  public void setBatchJobExecutionGroupsStarted(final Long batchJobId,
    final Long sequenceNumber) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOB_EXECUTION_GROUPS SET STARTED_IND = 1 WHERE BATCH_JOB_ID = ? AND SEQUENCE_NUMBER = ?";
      try {
        JdbcUtils.executeUpdate(dataSource, sql, batchJobId, sequenceNumber);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to set started status", e);
      }
    }
  }

  public boolean setBatchJobFailed(final long batchJobId) {
    final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
    final DataSource dataSource = jdbcRecordStore.getDataSource();

    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
      + "NUM_COMPLETED_REQUESTS = 0, NUM_FAILED_REQUESTS = NUM_SUBMITTED_REQUESTS, JOB_STATUS = 'resultsCreated', WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ? "
      + "WHERE JOB_STATUS = 'creatingRequests' AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      final String username = getUsername();
      return JdbcUtils.executeUpdate(dataSource, sql, now, now, username,
        batchJobId) == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set started status", e);
    }
  }

  public boolean setBatchJobRequestsCreated(final long batchJobId,
    final int numSubmittedRequests, final int numFailedRequests,
    final int groupSize, final int numGroups) {
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
        final DataSource dataSource = jdbcRecordStore.getDataSource();
        final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
          + "NUM_SUBMITTED_REQUESTS = ?, "//
          + "NUM_FAILED_REQUESTS = ?, "//
          + "GROUP_SIZE = ?, "//
          + "NUM_SUBMITTED_GROUPS = ?, "//
          + "STRUCTURED_INPUT_DATA = NULL, "//
          + "JOB_STATUS = 'requestsCreated', "//
          + "LAST_SCHEDULED_TIMESTAMP = ?, "//
          + "WHEN_STATUS_CHANGED = ?, "//
          + "WHEN_UPDATED = ?, "//
          + "WHO_UPDATED = ? "//
          + "WHERE JOB_STATUS IN ('creatingRequests') AND BATCH_JOB_ID = ?";
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final boolean result = JdbcUtils.executeUpdate(dataSource, sql,
          numSubmittedRequests, numFailedRequests, groupSize, numGroups, now,
          now, now, getUsername(), batchJobId) == 1;
        return result;
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public boolean setBatchJobRequestsFailed(final long batchJobId,
    final int numSubmittedRequests, final int numFailedRequests,
    final int groupSize, final int numGroups) {
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
        final DataSource dataSource = jdbcRecordStore.getDataSource();
        final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
          + "NUM_SUBMITTED_REQUESTS = ?, "//
          + "NUM_FAILED_REQUESTS = ?, "//
          + "GROUP_SIZE = ?, "//
          + "NUM_SUBMITTED_GROUPS = ?, "//
          + "STRUCTURED_INPUT_DATA = NULL, "//
          + "JOB_STATUS = 'processed', "//
          + "LAST_SCHEDULED_TIMESTAMP = ?, "//
          + "WHEN_STATUS_CHANGED = ?, "//
          + "WHEN_UPDATED = ?, "//
          + "WHO_UPDATED = ? "//
          + "WHERE JOB_STATUS IN ('creatingRequests') AND BATCH_JOB_ID = ?";
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final boolean result = JdbcUtils.executeUpdate(dataSource, sql,
          numSubmittedRequests, numFailedRequests, groupSize, numGroups, now,
          now, now, getUsername(), batchJobId) == 1;
        return result;
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public boolean setBatchJobStatus(final long batchJobId,
    final String oldJobStatus, final String newJobStatus) {
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
        final DataSource dataSource = jdbcRecordStore.getDataSource();
        final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ?, JOB_STATUS = ? WHERE JOB_STATUS = ? AND BATCH_JOB_ID = ?";
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final String username = getUsername();
        final int count = JdbcUtils.executeUpdate(dataSource, sql, now, now,
          username, newJobStatus, oldJobStatus, batchJobId);
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
      if (StringUtils.hasText(stringValue)) {
        object.setValue(ConfigProperty.PROPERTY_VALUE, stringValue);
      } else {
        object.setValue(ConfigProperty.PROPERTY_VALUE, null);
      }
    }
  }

  public void setRecordStore(final RecordStore recordStore) {
    this.recordStore = recordStore;
    this.batchJobMetaData = recordStore.getRecordDefinition(BatchJob.BATCH_JOB);
    this.batchJobExecutionGroupMetaData = recordStore.getRecordDefinition(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP);
    this.batchJobResultMetaData = recordStore.getRecordDefinition(BatchJobResult.BATCH_JOB_RESULT);
    this.businessApplicationStatisticsMetaData = recordStore.getRecordDefinition(BusinessApplicationStatistics.APPLICATION_STATISTICS);
    this.configPropertyMetaData = recordStore.getRecordDefinition(ConfigProperty.CONFIG_PROPERTY);
    this.userAccountMetaData = recordStore.getRecordDefinition(UserAccount.USER_ACCOUNT);
    this.userGroupMetaData = recordStore.getRecordDefinition(UserGroup.USER_GROUP);
    this.userGroupPermissionMetaData = recordStore.getRecordDefinition(UserGroupPermission.USER_GROUP_PERMISSION);
    this.userGroupAccountXrefMetaData = recordStore.getRecordDefinition(UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
  }

  /**
   * Increment the number of scheduled groups by 1
   *
   * @param batchJobId The BatchJob identifier.
   * @param timestamp The timestamp.
   */
  public int updateBatchJobAddScheduledGroupCount(final long batchJobId,
    final Timestamp timestamp) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "NUM_SCHEDULED_GROUPS = NUM_SCHEDULED_GROUPS + 1,"
        + " WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = 'SYSTEM' "
        + "WHERE BATCH_JOB_ID = ? AND JOB_STATUS = 'processing'";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(dataSource, sql, now, now, batchJobId);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable update counts: " + sql, e);
      }
    }

    return 0;
  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  public void updateBatchJobExecutionGroupFromResponse(
    final JobController jobController, final String workerId,
    final BatchJobRequestExecutionGroup group, final long sequenceNumber,
    final Object groupResultObject, final int successCount, final int errorCount) {
    if (groupResultObject != null) {
      final long batchJobId = group.getBatchJobId();
      final Record batchJobExecutionGroup = getBatchJobExecutionGroup(
        batchJobId, sequenceNumber);
      if (0 == batchJobExecutionGroup.getInteger(BatchJobExecutionGroup.COMPLETED_IND)) {

        final String resultData;
        if (groupResultObject instanceof Map) {
          final Map<String, Object> groupResults = (Map)groupResultObject;
          resultData = JsonMapIoFactory.toString(groupResults);
        } else {
          resultData = groupResultObject.toString();
        }
        batchJobExecutionGroup.setValue(BatchJobExecutionGroup.COMPLETED_IND, 1);
        jobController.setStructuredResultData(batchJobId, sequenceNumber,
          batchJobExecutionGroup, resultData);
        final int numCompletedRequests = batchJobExecutionGroup.getInteger(BatchJobExecutionGroup.NUM_COMPLETED_REQUESTS)
          + successCount;
        batchJobExecutionGroup.setValue(
          BatchJobExecutionGroup.NUM_COMPLETED_REQUESTS, numCompletedRequests);
        final int numFailedRequests = batchJobExecutionGroup.getInteger(BatchJobExecutionGroup.NUM_FAILED_REQUESTS)
          + errorCount;
        batchJobExecutionGroup.setValue(
          BatchJobExecutionGroup.NUM_FAILED_REQUESTS, numFailedRequests);
        write(batchJobExecutionGroup);
      }
    }
  }

  public int updateBatchJobGroupCompleted(final Long batchJobId,
    final int numCompletedRequests, final int numFailedRequests,
    final int numGroups) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "NUM_SCHEDULED_GROUPS = NUM_SCHEDULED_GROUPS - ?,"
        + "NUM_COMPLETED_GROUPS = NUM_COMPLETED_GROUPS + ?,"
        + "NUM_COMPLETED_REQUESTS = NUM_COMPLETED_REQUESTS + ?, "
        + "NUM_FAILED_REQUESTS = NUM_FAILED_REQUESTS + ? "
        + "WHERE BATCH_JOB_ID = ? AND JOB_STATUS = 'processing'";
      try (
        Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
        try {
          final int result = JdbcUtils.executeUpdate(dataSource, sql,
            numGroups, numGroups, numCompletedRequests, numFailedRequests,
            batchJobId);
          return result;
        } catch (final Throwable e) {
          throw transaction.setRollbackOnly(e);
        }
      }
    }

    return 0;
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
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "JOB_STATUS = 'processed', WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = 'SYSTEM' "
        + "WHERE JOB_STATUS = 'processing' AND BUSINESS_APPLICATION_NAME = ? AND NUM_COMPLETED_REQUESTS + NUM_FAILED_REQUESTS = NUM_SUBMITTED_REQUESTS";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(dataSource, sql, now, now,
          businessApplicationName);
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
  public int updateBatchJobStatus(final String newStatus,
    final String oldStatus, final String businessApplicationName) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "JOB_STATUS = ?, WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = 'SYSTEM' "
        + "WHERE JOB_STATUS = ? AND BUSINESS_APPLICATION_NAME = ?";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(dataSource, sql, newStatus, now, now,
          oldStatus, businessApplicationName);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to update status: " + sql, e);
      }
    }
    return 0;
  }

  public int updateJobUserId(final String oldUserId, final String newUserId) {
    final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
    final DataSource dataSource = jdbcRecordStore.getDataSource();
    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET USER_ID = ? WHERE USER_ID = ?";
    try {
      return JdbcUtils.executeUpdate(dataSource, sql, newUserId, oldUserId);
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to change jobs for user rename", e);
    }
  }

  public int updateResetBatchJobExecutingGroups(
    final String businessApplicationName) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "NUM_SCHEDULED_GROUPS = 0,"
        + " NUM_COMPLETED_REQUESTS = COALESCE((SELECT SUM(NUM_COMPLETED_REQUESTS) FROM CPF.CPF_BATCH_JOB_EXECUTION_GROUPS G WHERE BJ.BATCH_JOB_ID = G.BATCH_JOB_ID AND COMPLETED_IND = 1), 0),"
        + " NUM_FAILED_REQUESTS = COALESCE((SELECT SUM(NUM_FAILED_REQUESTS) FROM CPF.CPF_BATCH_JOB_EXECUTION_GROUPS G WHERE BJ.BATCH_JOB_ID = G.BATCH_JOB_ID AND COMPLETED_IND = 1), 0),"
        + " NUM_COMPLETED_GROUPS = COALESCE((SELECT COUNT(SEQUENCE_NUMBER) FROM CPF.CPF_BATCH_JOB_EXECUTION_GROUPS G WHERE BJ.BATCH_JOB_ID = G.BATCH_JOB_ID AND COMPLETED_IND = 1), 0),"
        + " WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = 'SYSTEM' "
        + "WHERE JOB_STATUS = 'processing' AND BUSINESS_APPLICATION_NAME = ?";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(dataSource, sql, now, now,
          businessApplicationName);
      } catch (final Throwable e) {
        throw new RuntimeException(
          "Unable to update batch job executing groups count: " + sql, e);
      }
    }

    return 0;
  }

  public int updateResetGroupsForRestart(final String businessApplicationName) {
    if (this.recordStore instanceof JdbcRecordStore) {
      final JdbcRecordStore jdbcRecordStore = (JdbcRecordStore)this.recordStore;
      final DataSource dataSource = jdbcRecordStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOB_EXECUTION_GROUPS SET STARTED_IND = 0 WHERE STARTED_IND = 1 AND COMPLETED_IND = 0 AND BATCH_JOB_ID IN (SELECT BATCH_JOB_ID FROM CPF.CPF_BATCH_JOBS WHERE BUSINESS_APPLICATION_NAME = ?)";
      try {
        return JdbcUtils.executeUpdate(dataSource, sql, businessApplicationName);
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

        if (recordDefinition.getIdAttributeIndex() != -1
          && record.getIdentifier() == null) {
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

  public int writeGroupResults(final JobController jobController,
    final long batchJobId, final int startIndex, final int endIndex,
    final BusinessApplication application,
    final RecordDefinition resultMetaData, final MapWriter errorResultWriter,
    final com.revolsys.io.Writer<Record> structuredResultWriter,
    final Map<String, Object> defaultProperties) {
    int mask = 0;
    try (
      Transaction transaction = createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final Attribute sequenceNumberAttribute = this.batchJobExecutionGroupMetaData.getAttribute(BatchJobExecutionGroup.SEQUENCE_NUMBER);
        final Between between = Q.between(sequenceNumberAttribute, startIndex,
          endIndex);
        final Equal batchJobIdEqual = Q.equal(
          BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
        final And whereCondition = Q.and(batchJobIdEqual, between);
        final Query query = new Query(this.batchJobExecutionGroupMetaData,
          whereCondition);
        query.setOrderByColumns(BatchJobExecutionGroup.SEQUENCE_NUMBER);
        query.setAttributeNames(BatchJobExecutionGroup.SEQUENCE_NUMBER,
          BatchJobExecutionGroup.STRUCTURED_RESULT_DATA);

        try (
          final Reader<Record> reader = getRecordStore().query(query);) {
          for (final Record batchJobExecutionGroup : reader) {
            final Long groupSequenceNumber = batchJobExecutionGroup.getLong(BatchJobExecutionGroup.SEQUENCE_NUMBER);
            final Map<String, Object> resultDataMap = jobController.getStructuredResultData(
              batchJobId, groupSequenceNumber, batchJobExecutionGroup);

            if (resultDataMap != null) {
              @SuppressWarnings("unchecked")
              final List<Map<String, Object>> resultDataList = (List<Map<String, Object>>)resultDataMap.get("items");
              for (final Map<String, Object> resultData : resultDataList) {
                final Map<String, Object> resultMap = resultData;
                if (resultMap.containsKey("errorCode")) {
                  postProcessWriteError(errorResultWriter, resultMap);
                  mask |= 4;
                } else if (!application.isPerRequestResultData()) {
                  postProcessWriteStructuredResult(structuredResultWriter,
                    resultMetaData, defaultProperties, resultData);
                  mask |= 2;
                }
              }

              // String resultDataString;
              // if (resultRecord instanceof Clob) {
              // try {
              // final Clob clob = (Clob)resultRecord;
              // resultDataString =
              // FileUtil.getString(clob.getCharacterStream());
              // } catch (final SQLException e) {
              // throw new RuntimeException("Unable to read clob", e);
              // }
              // } else if (resultRecord instanceof java.io.Reader) {
              // final java.io.Reader resultDataReader =
              // (java.io.Reader)resultRecord;
              // resultDataString = FileUtil.getString(resultDataReader);
              // } else {
              // resultDataString = resultRecord.toString();
              // }
              // if (resultDataString.charAt(0) != '{') {
              // resultDataString = Compress.inflateBase64(resultDataString);
              // }

            }
          }
        } catch (final Throwable e) {
          throw new RuntimeException("Unable to read results. batchJobId="
            + batchJobId + "\t" + startIndex + " <= SEQUENCE_NUMBER <= "
            + endIndex, e);
        }
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }

    return mask;
  }
}
