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

import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.BusinessApplicationStatistics;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.module.ResourcePermission;

import com.revolsys.collection.ResultPager;
import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.data.io.DataObjectStore;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.data.query.Condition;
import com.revolsys.gis.data.query.Conditions;
import com.revolsys.gis.data.query.MultipleCondition;
import com.revolsys.gis.data.query.Query;
import com.revolsys.io.MapWriter;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.io.json.JsonParser;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.attribute.JdbcLongAttribute;
import com.revolsys.jdbc.io.JdbcDataObjectStore;
import com.revolsys.util.CollectionUtil;

public class CpfDataAccessObject {

  private DataObjectStore dataStore;

  private DataObjectMetaData batchJobMetaData;

  private DataObjectMetaData batchJobExecutionGroupMetaData;

  private DataObjectMetaData batchJobResultMetaData;

  private DataObjectMetaData businessApplicationStatisticsMetaData;

  private DataObjectMetaData configPropertyMetaData;

  private DataObjectMetaData userAccountMetaData;

  private DataObjectMetaData userGroupMetaData;

  private DataObjectMetaData userGroupAccountXrefMetaData;

  private DataObjectMetaData userGroupPermissionMetaData;

  public CpfDataAccessObject() {
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int cancelBatchJob(final Long batchJobId) {
    final String username = getUsername();
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
        + "NUM_SCHEDULED_GROUPS  = 0, " + "NUM_COMPLETED_REQUESTS = 0, "
        + "NUM_FAILED_REQUESTS = NUM_SUBMITTED_REQUESTS,"
        + "STRUCTURED_INPUT_DATA = NULL, " + "WHEN_STATUS_CHANGED = ?, "
        + "JOB_STATUS = 'cancelled',"
        + "WHEN_UPDATED = ?, WHO_UPDATED = ? WHERE BATCH_JOB_ID = ?";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(dataSource, sql, now, now, username,
          batchJobId);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to reset started status", e);
      }
    }

    final Query query = Query.equal(batchJobMetaData, BatchJob.BATCH_JOB_ID,
      batchJobId);
    return dataStore.delete(query);
  }

  public DataObject create(final String typeName) {
    return dataStore.create(typeName);
  }

  protected DataObject createBatchJobExecutionGroup(final long batchJobId,
    final int requestSequenceNumber, final int requestCount) {
    final DataObject batchJobExecutionGroup = create(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP);
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
      requestSequenceNumber);
    return batchJobExecutionGroup;
  }

  public DataObject createBatchJobExecutionGroup(final long batchJobId,
    final int requestSequenceNumber, final String structuredInputData,
    final int requestCount) {
    final DataObject batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, requestSequenceNumber, requestCount);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.STRUCTURED_INPUT_DATA, structuredInputData);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  public DataObject createBatchJobExecutionGroup(final long batchJobId,
    final int requestSequenceNumber, final String inputDataContentType,
    final Resource inputData) {
    final DataObject batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, requestSequenceNumber, 1);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE, inputDataContentType);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.INPUT_DATA,
      inputData);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  public DataObject createBatchJobExecutionGroup(final long batchJobId,
    final int requestSequenceNumber, final String inputDataContentType,
    final String inputDataUrl) {
    final DataObject batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, requestSequenceNumber, 1);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE, inputDataContentType);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.INPUT_DATA_URL,
      inputDataUrl);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  public DataObject createBatchJobExecutionGroup(final long batchJobId,
    final int requestSequenceNumber, final String errorCode,
    final String errorMessage, final String errorDebugMessage) {
    final Map<String, Object> error = new HashMap<String, Object>();
    error.put("requestSequenceNumber", 1);
    error.put("errorCode", errorCode);
    error.put("errorMessage", errorMessage);
    error.put("errorDebugMessage", errorDebugMessage);
    final List<Map<String, Object>> resultData = Collections.singletonList(error);

    final DataObject batchJobExecutionGroup = createBatchJobExecutionGroup(
      batchJobId, 1, 1);
    batchJobExecutionGroup.setValue(
      BatchJobExecutionGroup.STRUCTURED_RESULT_DATA, resultData);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.STARTED_IND, 1);
    batchJobExecutionGroup.setValue(BatchJobExecutionGroup.COMPLETED_IND, 1);
    write(batchJobExecutionGroup);
    return batchJobExecutionGroup;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject createConfigProperty(final String environmentName,
    final String moduleName, final String componentName,
    final String propertyName, final Object propertyValue,
    final DataType propertyValueType) {
    final DataObject configProperty = create(ConfigProperty.CONFIG_PROPERTY);
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
    return (T)dataStore.createPrimaryIdValue(typeName);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject createUserAccount(final String userAccountClass,
    final String userAccountName, final String consumerKey,
    final String consumerSecret) {
    final DataObject userAccount = create(UserAccount.USER_ACCOUNT);

    userAccount.setValue(UserAccount.USER_NAME, userAccountName);
    userAccount.setValue(UserAccount.USER_ACCOUNT_CLASS, userAccountClass);
    userAccount.setValue(UserAccount.CONSUMER_KEY, consumerKey);
    userAccount.setValue(UserAccount.CONSUMER_SECRET, consumerSecret);
    userAccount.setValue(UserAccount.ACTIVE_IND, 1);

    write(userAccount);
    return userAccount;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject createUserGroup(final String moduleName,
    final String groupName, final String description) {
    DataObject userGroup = getUserGroup(moduleName, groupName);
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

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject createUserGroupAccountXref(final DataObject userGroup,
    final DataObject userAccount) {
    final Number userGroupId = userGroup.getIdValue();
    final Number userAccountId = userAccount.getIdValue();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

    final Query query = Query.and(userGroupAccountXrefMetaData, filter);

    DataObject userGroupAccountXref = dataStore.queryFirst(query);
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

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject createUserGroupPermission(final DataObject userGroup,
    final String groupName, final ResourcePermission permission) {
    final DataObject userGroupPermission = create(UserGroup.USER_GROUP);

    userGroupPermission.setValue(UserGroup.MODULE_NAME, userGroup);
    userGroupPermission.setValue(UserGroup.USER_GROUP_NAME, groupName);
    userGroupPermission.setValue(UserGroup.DESCRIPTION, permission);

    write(userGroupPermission);
    return userGroup;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void delete(final DataObject object) {
    dataStore.delete(object);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteBatchJob(final Long batchJobId) {
    deleteBatchJobResults(batchJobId);
    deleteBatchJobExecutionGroups(batchJobId);

    final Query query = Query.equal(batchJobMetaData, BatchJob.BATCH_JOB_ID,
      batchJobId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteBatchJobExecutionGroups(final Long batchJobId) {
    final Query query = Query.equal(batchJobExecutionGroupMetaData,
      BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteBatchJobResults(final Long batchJobId) {
    final Query query = Query.equal(batchJobResultMetaData,
      BatchJobResult.BATCH_JOB_ID, batchJobId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteBusinessApplicationStatistics(
    final Integer businessApplicationStatisticsId) {
    final Query query = Query.equal(businessApplicationStatisticsMetaData,
      BusinessApplicationStatistics.APPLICATION_STATISTIC_ID,
      businessApplicationStatisticsId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteConfigPropertiesForModule(final String moduleName) {
    final Query query = Query.equal(configPropertyMetaData,
      ConfigProperty.MODULE_NAME, moduleName);
    return dataStore.delete(query);
  }

  public void deleteUserAccount(final DataObject userAccount) {
    final Number userAccountId = userAccount.getIdValue();
    final Query membersQuery = Query.equal(userGroupAccountXrefMetaData,
      UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);
    dataStore.delete(membersQuery);

    delete(userAccount);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void deleteUserGroup(final DataObject userGroup) {
    final Number userGroupId = userGroup.getIdValue();

    final Query membersQuery = Query.equal(userGroupAccountXrefMetaData,
      UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    dataStore.delete(membersQuery);

    final Query permissionsQuery = Query.equal(userGroupPermissionMetaData,
      UserGroupPermission.USER_GROUP_ID, userGroupId);
    dataStore.delete(permissionsQuery);

    delete(userGroup);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteUserGroupAccountXref(final DataObject userGroup,
    final DataObject userAccount) {
    final Number userGroupId = userGroup.getIdValue();
    final Number userAccountId = userAccount.getIdValue();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

    final Query query = Query.and(userGroupAccountXrefMetaData, filter);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteUserGroupsForModule(final String moduleName) {
    final Query query = Query.equal(userGroupMetaData, UserGroup.MODULE_NAME,
      moduleName);
    int i = 0;
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      for (final DataObject userGroup : reader) {
        deleteUserGroup(userGroup);
        i++;
      }
    } finally {
      reader.close();
    }
    return i;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJob(final long batchJobId) {
    return dataStore.load(BatchJob.BATCH_JOB, batchJobId);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJob(final String consumerKey, final long batchJobId) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.USER_ID, consumerKey);
    filter.put(BatchJob.BATCH_JOB_ID, batchJobId);

    final Query query = Query.and(batchJobMetaData, filter);
    return dataStore.queryFirst(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJobExecutionGroup(
    final long batchJobExecutionGroupId) {
    return dataStore.load(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP,
      batchJobExecutionGroupId);
  }

  public Reader<DataObject> getBatchJobExecutionGroupIds(final long batchJobId) {
    final Query query = Query.equal(batchJobExecutionGroupMetaData,
      BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    query.setAttributeNames(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP_ID);
    query.addOrderBy(BatchJobExecutionGroup.SEQUENCE_NUMBER, true);
    final Reader<DataObject> reader = dataStore.query(query);
    return reader;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJobExecutionGroupLocked(
    final long batchJobExecutionGroupId) {
    return dataStore.load(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP,
      batchJobExecutionGroupId);
  }

  public List<DataObject> getBatchJobExecutionGroups(
    final List<Long> batchJobExecutionGroupIds) {
    final Query query = new Query(batchJobExecutionGroupMetaData);
    query.setWhereCondition(Conditions.in(
      BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP_ID,
      batchJobExecutionGroupIds));
    final Reader<DataObject> batchJobExecutionGroups = dataStore.query(query);
    try {
      return batchJobExecutionGroups.read();
    } finally {
      batchJobExecutionGroups.close();
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public List<Long> getBatchJobIds(final String businessApplicationName,
    final String jobStatus) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    filter.put(BatchJob.JOB_STATUS, jobStatus);
    final Query query = Query.and(batchJobMetaData, filter);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    final Reader<DataObject> batchJobs = dataStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final DataObject batchJob : batchJobs) {
        final Long batchJobId = DataObjectUtil.getLong(batchJob,
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
    final Query query = Query.equal(batchJobMetaData,
      BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    // TODO move to scheduling groups
    query.setWhereCondition(Conditions.sql("JOB_STATUS IN ('requestsCreated', 'processing') AND "
      + "NUM_SUBMITTED_GROUPS > 0 AND "
      + "NUM_SCHEDULED_GROUPS + NUM_COMPLETED_GROUPS < NUM_SUBMITTED_GROUPS "));
    query.addOrderBy(BatchJob.NUM_SCHEDULED_GROUPS, true);
    query.addOrderBy(BatchJob.LAST_SCHEDULED_TIMESTAMP, true);
    query.addOrderBy(BatchJob.BATCH_JOB_ID, true);
    final Reader<DataObject> batchJobs = dataStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final DataObject batchJob : batchJobs) {
        final Long batchJobId = DataObjectUtil.getLong(batchJob,
          BatchJob.BATCH_JOB_ID);
        batchJobIds.add(batchJobId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJobLocked(final long batchJobId) {
    return dataStore.load(BatchJob.BATCH_JOB, batchJobId);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJobResult(final long batchJobResultId) {
    return dataStore.load(BatchJobResult.BATCH_JOB_RESULT, batchJobResultId);
  }

  public List<DataObject> getBatchJobResults(final long batchJobId) {
    final Query query = Query.equal(batchJobResultMetaData,
      BatchJobResult.BATCH_JOB_ID, batchJobId);
    query.setAttributeNames(BatchJobResult.ALL_EXCEPT_BLOB);
    query.addOrderBy(BatchJobResult.BATCH_JOB_RESULT_ID, true);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getBatchJobsForUser(final String consumerKey) {
    final Query query = Query.equal(batchJobMetaData, BatchJob.USER_ID,
      consumerKey);
    query.addOrderBy(BatchJob.BATCH_JOB_ID, false);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getBatchJobsForUserAndApplication(
    final String consumerKey, final String businessApplicationName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJob.USER_ID, consumerKey);
    filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    final Query query = Query.and(batchJobMetaData, filter);

    query.addOrderBy(BatchJob.BATCH_JOB_ID, false);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getConfigPropertiesForAllModules(
    final String environmentName, final String componentName,
    final String propertyName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    filter.put(ConfigProperty.PROPERTY_NAME, propertyName);
    final Query query = Query.and(configPropertyMetaData, filter);

    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getConfigPropertiesForComponent(
    final String moduleName, final String componentName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    final Query query = Query.and(configPropertyMetaData, filter);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getConfigPropertiesForModule(
    final String environmentName, final String moduleName,
    final String componentName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    final Query query = Query.and(configPropertyMetaData, filter);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public DataObject getConfigProperty(final String environmentName,
    final String moduleName, final String componentName,
    final String propertyName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME, componentName);
    filter.put(ConfigProperty.PROPERTY_NAME, propertyName);
    final Query query = Query.and(configPropertyMetaData, filter);
    return dataStore.queryFirst(query);
  }

  public DataObjectStore getDataStore() {
    return dataStore;
  }

  public Long getNonExecutingRequestId(final Long batchJobId) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    filter.put(BatchJobExecutionGroup.STARTED_IND, 0);
    filter.put(BatchJobExecutionGroup.COMPLETED_IND, 0);
    filter.put(BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    filter.put(BatchJobExecutionGroup.STARTED_IND, 0);
    filter.put(BatchJobExecutionGroup.COMPLETED_IND, 0);
    final Query query = Query.and(batchJobExecutionGroupMetaData, filter);
    query.setAttributeNames(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP_ID);
    query.addOrderBy(BatchJobExecutionGroup.SEQUENCE_NUMBER, true);
    query.setLimit(1);
    final DataObject batchJobExecutionGroup = dataStore.queryFirst(query);
    if (batchJobExecutionGroup == null) {
      return null;
    } else {
      return DataObjectUtil.getLong(batchJobExecutionGroup,
        BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP_ID);
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
    final Query query = new Query(batchJobMetaData);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    query.setWhereCondition(Conditions.and(Conditions.in(BatchJob.JOB_STATUS,
      "resultsCreated", "downloadInitiated", "cancelled"), Conditions.lessThan(
      BatchJob.WHEN_STATUS_CHANGED, keepUntilTimestamp)));
    final Reader<DataObject> batchJobs = dataStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final DataObject batchJob : batchJobs) {
        final Long batchJobId = DataObjectUtil.getLong(batchJob,
          BatchJob.BATCH_JOB_ID);
        batchJobIds.add(batchJobId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  /**
   * Get the user with the specified consumer key.
   * 
   * @param consumerKey The external user class.
   * @return The user account if it exists, null otherwise.
   */
  public DataObject getUserAccount(final String consumerKey) {
    final Query query = Query.equal(userAccountMetaData,
      UserAccount.CONSUMER_KEY, consumerKey);
    return dataStore.queryFirst(query);
  }

  /**
   * Get the user with the specified external user class and external user name.
   * 
   * @param userClass The external user class.
   * @param userName The external user name.
   * @return The user account if it exists, null otherwise.
   */
  public DataObject getUserAccount(final String userClass, final String userName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserAccount.USER_ACCOUNT_CLASS, userClass);
    filter.put(UserAccount.USER_NAME, userName);
    final Query query = Query.and(userAccountMetaData, filter);
    return dataStore.queryFirst(query);
  }

  public ResultPager<DataObject> getUserAccountsForUserGroup(
    final DataObject userGroup) {
    final Condition equal = Conditions.equal(
      UserGroupAccountXref.USER_GROUP_ID, userGroup.getIdValue());
    final Query query = new Query(UserAccount.USER_ACCOUNT, equal);
    query.setFromClause("CPF.CPF_USER_ACCOUNTS T"
      + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_ACCOUNT_ID = X.USER_ACCOUNT_ID");

    final ResultPager<DataObject> pager = dataStore.page(query);
    return pager;
  }

  public List<DataObject> getUserAccountsLikeName(final String name) {
    if (StringUtils.hasText(name)) {
      final Condition consumerKeyLike = Conditions.likeUpper(
        UserAccount.CONSUMER_KEY, name);
      final Condition userNameLike = Conditions.likeUpper(
        UserAccount.USER_NAME, name);
      final MultipleCondition or = Conditions.or(consumerKeyLike, userNameLike);
      final Query query = new Query(UserAccount.USER_ACCOUNT, or);
      final Reader<DataObject> reader = dataStore.query(query);
      try {
        return CollectionUtil.subList(reader, 20);
      } finally {
        reader.close();
      }
    } else {
      return Collections.emptyList();
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getUserGroup(final long userGroupId) {
    return dataStore.load(UserGroup.USER_GROUP, userGroupId);
  }

  public DataObject getUserGroup(final String groupName) {
    final Query query = Query.equal(userGroupMetaData,
      UserGroup.USER_GROUP_NAME, groupName);
    return dataStore.queryFirst(query);
  }

  public DataObject getUserGroup(final String moduleName, final String groupName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroup.MODULE_NAME, moduleName);
    filter.put(UserGroup.USER_GROUP_NAME, groupName);
    final Query query = Query.and(userGroupMetaData, filter);
    return dataStore.queryFirst(query);
  }

  public DataObject getUserGroupPermission(final List<String> userGroupNames,
    final String moduleName, final String resourceClass,
    final String resourceId, final String actionName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroup.USER_GROUP_NAME, userGroupNames);
    filter.put("T." + UserGroupPermission.MODULE_NAME, moduleName);
    filter.put(UserGroupPermission.RESOURCE_CLASS, resourceClass);
    filter.put(UserGroupPermission.RESOURCE_ID, resourceId);
    filter.put(UserGroupPermission.ACTION_NAME, actionName);
    final Query query = Query.and(userGroupPermissionMetaData, filter);
    query.setFromClause("CPF.CPF_USER_GROUP_PERMISSIONS T"
      + " JOIN CPF.CPF_USER_GROUPS G ON T.USER_GROUP_ID = G.USER_GROUP_ID");
    return dataStore.queryFirst(query);
  }

  public List<DataObject> getUserGroupPermissions(final DataObject userGroup,
    final String moduleName) {
    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(UserGroupPermission.USER_GROUP_ID, userGroup.getIdValue());
    filter.put(UserGroupPermission.MODULE_NAME, moduleName);
    final Query query = Query.and(userGroupPermissionMetaData, filter);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getUserGroupsForModule(final String moduleName) {
    final Query query = Query.equal(userGroupMetaData, UserGroup.MODULE_NAME,
      moduleName);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public Set<DataObject> getUserGroupsForUserAccount(
    final DataObject userAccount) {
    final Query query = new Query(UserGroup.USER_GROUP);
    query.setFromClause("CPF.CPF_USER_GROUPS T"
      + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_GROUP_ID = X.USER_GROUP_ID");

    query.setWhereCondition(Conditions.equal(new JdbcLongAttribute(
      "X.USER_ACCOUNT_ID"), userAccount.getIdValue()));
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      final List<DataObject> groups = reader.read();
      return new LinkedHashSet<DataObject>(groups);
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

  @Transactional(propagation = Propagation.REQUIRED)
  public boolean hasBatchJobUnexecutedJobs(final long batchJobId) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
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
    final DataObject applicationStatistics;
    databaseId = ((Number)dataStore.createPrimaryIdValue(BusinessApplicationStatistics.APPLICATION_STATISTICS)).intValue();
    applicationStatistics = dataStore.create(BusinessApplicationStatistics.APPLICATION_STATISTICS);
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
    dataStore.insert(applicationStatistics);
    statistics.setDatabaseId(databaseId);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public boolean isBatchJobCompleted(final long batchJobId) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
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
    errorMap.put("sequenceNumber", requestSequenceNumber.toString());
    errorMap.put("errorCode", errorCode);
    errorMap.put("errorMessage", errorMessage);
    errorMapWriter.write(errorMap);
    written = true;
    return written;
  }

  @SuppressWarnings("unchecked")
  protected void postProcessWriteStructuredResult(
    final com.revolsys.io.Writer<DataObject> structuredDataWriter,
    final DataObjectMetaData resultMetaData,
    final Map<String, Object> defaultProperties,
    final Map<String, Object> resultData) {
    final List<Map<String, Object>> results = (List<Map<String, Object>>)resultData.get("results");
    final Number sequenceNumber = (Number)resultData.get("requestSequenceNumber");
    int i = 1;
    for (final Map<String, Object> structuredResultMap : results) {
      final DataObject structuredResult = DataObjectUtil.getObject(
        resultMetaData, structuredResultMap);

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

  @Transactional(propagation = Propagation.REQUIRED)
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

      final DataObject applicationStatistics;
      if (databaseId == null) {
        insertStatistics(statistics, businessApplicationName, durationType,
          startTime, valuesString);
      } else if (statistics.isModified()) {
        applicationStatistics = dataStore.load(
          BusinessApplicationStatistics.APPLICATION_STATISTICS, databaseId);
        if (applicationStatistics == null) {
          insertStatistics(statistics, businessApplicationName, durationType,
            startTime, valuesString);
        } else {
          applicationStatistics.setValue(
            BusinessApplicationStatistics.STATISTIC_VALUES, valuesString);
          dataStore.update(applicationStatistics);
        }
      }

      statistics.setModified(false);
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public boolean setBatchJobCompleted(final long batchJobId) {
    final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
    final DataSource dataSource = jdbcDataStore.getDataSource();

    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
      + "NUM_COMPLETED_GROUPS = NUM_SUBMITTED_GROUPS, NUM_SCHEDULED_GROUPS = 0, STRUCTURED_INPUT_DATA = NULL, JOB_STATUS = 'resultsCreated', COMPLETED_TIMESTAMP = ?, LAST_SCHEDULED_TIMESTAMP = NULL, WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ? "
      + "WHERE JOB_STATUS IN ('creatingRequests','creatingResults') AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      return JdbcUtils.executeUpdate(dataSource, sql, now, now, now,
        getUsername(), batchJobId) == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set job completed " + batchJobId, e);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean setBatchJobDownloaded(final long batchJobId) {
    final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
    final DataSource dataSource = jdbcDataStore.getDataSource();

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

  public void setBatchJobExecutionGroupsStarted(
    final Long batchJobExecutionGroupId) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOB_EXECUTION_GROUPS SET STARTED_IND = 1 WHERE BATCH_JOB_EXECUTION_GROUP_ID = ?";
      try {
        JdbcUtils.executeUpdate(dataSource, sql, batchJobExecutionGroupId);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to set started status", e);
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public boolean setBatchJobFailed(final long batchJobId) {
    final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
    final DataSource dataSource = jdbcDataStore.getDataSource();

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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean setBatchJobResultsCreated(final long batchJobId,
    final int numSubmittedRequests, final int groupSize, final int numGroups) {
    final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
    final DataSource dataSource = jdbcDataStore.getDataSource();
    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET "
      + "NUM_SUBMITTED_REQUESTS = ?, GROUP_SIZE = ?, NUM_SUBMITTED_GROUPS = ?, STRUCTURED_INPUT_DATA = NULL, JOB_STATUS = 'requestsCreated', LAST_SCHEDULED_TIMESTAMP = ?, WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ? "
      + "WHERE JOB_STATUS IN ('creatingRequests') AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      return JdbcUtils.executeUpdate(dataSource, sql, numSubmittedRequests,
        groupSize, numGroups, now, now, now, getUsername(), batchJobId) == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set started status", e);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean setBatchJobStatus(final long batchJobId,
    final String oldJobStatus, final String newJobStatus) {
    final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
    final DataSource dataSource = jdbcDataStore.getDataSource();
    final String sql = "UPDATE CPF.CPF_BATCH_JOBS SET WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = ?, JOB_STATUS = ? WHERE JOB_STATUS = ? AND BATCH_JOB_ID = ?";
    try {
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      final String username = getUsername();
      final int count = JdbcUtils.executeUpdate(dataSource, sql, now, now,
        username, newJobStatus, oldJobStatus, batchJobId);
      return count == 1;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to set started status", e);
    }
  }

  public void setConfigPropertyValue(final DataObject object, final Object value) {
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

  public void setDataStore(final DataObjectStore dataStore) {
    this.dataStore = dataStore;
    this.batchJobMetaData = dataStore.getMetaData(BatchJob.BATCH_JOB);
    this.batchJobExecutionGroupMetaData = dataStore.getMetaData(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP);
    this.batchJobResultMetaData = dataStore.getMetaData(BatchJobResult.BATCH_JOB_RESULT);
    this.businessApplicationStatisticsMetaData = dataStore.getMetaData(BusinessApplicationStatistics.APPLICATION_STATISTICS);
    this.configPropertyMetaData = dataStore.getMetaData(ConfigProperty.CONFIG_PROPERTY);
    this.userAccountMetaData = dataStore.getMetaData(UserAccount.USER_ACCOUNT);
    this.userGroupMetaData = dataStore.getMetaData(UserGroup.USER_GROUP);
    this.userGroupPermissionMetaData = dataStore.getMetaData(UserGroupPermission.USER_GROUP_PERMISSION);
    this.userGroupAccountXrefMetaData = dataStore.getMetaData(UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
  }

  /**
   * Increment the number of scheduled groups by 1
   * 
   * @param batchJobId The BatchJob identifier.
   * @param timestamp The timestamp.
   */
  public int updateBatchJobAddScheduledGroupCount(final long batchJobId,
    final Timestamp timestamp) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
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

  @SuppressWarnings("unchecked")
  @Transactional(propagation = Propagation.REQUIRED)
  public void updateBatchJobExecutionGroupFromResponse(final String workerId,
    final BatchJobRequestExecutionGroup group,
    final Long batchJobExecutionGroupId,
    final List<Map<String, Object>> results, final int successCount,
    final int errorCount) {
    if (results != null) {
      for (final Map<String, Object> result : results) {
        final List<Map<String, Object>> logRecords = (List<Map<String, Object>>)result.remove("logRecords");
        BatchJobService.logGroup(workerId, group, logRecords,
          "Request Execution", "Application Log", Collections.singletonMap(
            "batchJobExecutionGroupId", batchJobExecutionGroupId));
      }

      final DataObject batchJobExecutionGroup = getBatchJobExecutionGroupLocked(batchJobExecutionGroupId);
      if (0 == batchJobExecutionGroup.getInteger(BatchJobExecutionGroup.COMPLETED_IND)) {
        final String resultData = JsonMapIoFactory.toString(results);
        batchJobExecutionGroup.setValue(BatchJobExecutionGroup.COMPLETED_IND, 1);
        batchJobExecutionGroup.setValue(
          BatchJobExecutionGroup.STRUCTURED_RESULT_DATA, resultData);
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int updateBatchJobGroupCompleted(final Long batchJobId,
    final int numCompletedRequests, final int numFailedRequests,
    final int numGroups) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "NUM_SCHEDULED_GROUPS = NUM_SCHEDULED_GROUPS - ?,"
        + "NUM_COMPLETED_GROUPS = NUM_COMPLETED_GROUPS + ?,"
        + "NUM_COMPLETED_REQUESTS = NUM_COMPLETED_REQUESTS + ?, "
        + "NUM_FAILED_REQUESTS = NUM_FAILED_REQUESTS + ? "
        + "WHERE BATCH_JOB_ID = ? AND JOB_STATUS = 'processing'";
      try {
        return JdbcUtils.executeUpdate(dataSource, sql, numGroups, numGroups,
          numCompletedRequests, numFailedRequests, batchJobId);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to reset started status", e);
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
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
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
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
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

  public int updateResetRequestsForRestart(final String businessApplicationName) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOB_EXECUTION_GROUPS SET STARTED_IND = 0 WHERE STARTED_IND = 1 AND COMPLETED_IND = 0 AND BATCH_JOB_ID IN (SELECT BATCH_JOB_ID FROM CPF.CPF_BATCH_JOBS WHERE BUSINESS_APPLICATION_NAME = ?)";
      try {
        return JdbcUtils.executeUpdate(dataSource, sql, businessApplicationName);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to reset started status", e);
      }
    }

    return 0;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void write(final DataObject object) {
    final Writer<DataObject> writer = dataStore.getWriter();
    try {
      final String username = getUsername();
      final Timestamp time = new Timestamp(System.currentTimeMillis());
      switch (object.getState()) {
        case New:
          final DataObjectMetaData metaData = object.getMetaData();

          if (metaData.getIdAttributeIndex() != -1
            && object.getIdValue() == null) {
            final Object id = dataStore.createPrimaryIdValue(metaData.getPath());
            object.setIdValue(id);
          }
          object.setValue("WHO_CREATED", username);
          object.setValue("WHEN_CREATED", time);
          object.setValue("WHO_UPDATED", username);
          object.setValue("WHEN_UPDATED", time);
        break;
        case Persisted:
          object.setValue("WHO_UPDATED", username);
          object.setValue("WHEN_UPDATED", time);
        break;
        default:
        break;
      }
      writer.write(object);
    } finally {
      writer.close();
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int writeGroupResult(final long batchJobExecutionGroupId,
    final BusinessApplication application,
    final DataObjectMetaData resultMetaData, final MapWriter errorResultWriter,
    final com.revolsys.io.Writer<DataObject> structuredResultWriter,
    final Map<String, Object> defaultProperties) {
    int mask = 0;
    final Query query = Query.equal(batchJobExecutionGroupMetaData,
      BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP_ID,
      batchJobExecutionGroupId);
    query.setAttributeNames(BatchJobExecutionGroup.STRUCTURED_RESULT_DATA);
    final Reader<DataObject> reader = getDataStore().query(query);
    try {
      for (final DataObject batchJobExecutionGroup : reader) {
        final Object resultDataObject = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.STRUCTURED_RESULT_DATA);

        if (resultDataObject != null) {
          final Map<String, Object> resultDataMap = JsonParser.read(resultDataObject);
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
        }
      }
    } finally {
      reader.close();
    }
    return mask;
  }
}
