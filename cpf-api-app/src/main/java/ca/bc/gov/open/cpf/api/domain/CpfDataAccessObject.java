package ca.bc.gov.open.cpf.api.domain;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

import ca.bc.gov.open.cpf.api.scheduler.BusinessApplicationStatistics;
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
import com.revolsys.gis.data.query.Query;
import com.revolsys.io.Reader;
import com.revolsys.io.Writer;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.jdbc.attribute.JdbcLongAttribute;
import com.revolsys.jdbc.io.JdbcDataObjectStore;
import com.revolsys.util.CollectionUtil;

public class CpfDataAccessObject {

  private DataObjectStore dataStore;

  public DataObject create(final String typeName) {
    return dataStore.create(typeName);
  }

  public DataObject createBatchJobRequest(final long batchJobId,
    final int requestSequenceNumber, final String structuredInputData) {
    final DataObject batchJobRequest = create(BatchJobRequest.BATCH_JOB_REQUEST);
    batchJobRequest.setValue(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    batchJobRequest.setValue(BatchJobRequest.COMPLETED_IND, 0);
    batchJobRequest.setValue(BatchJobRequest.STARTED_IND, 0);
    batchJobRequest.setValue(BatchJobRequest.REQUEST_SEQUENCE_NUMBER,
      requestSequenceNumber);
    batchJobRequest.setValue(BatchJobRequest.STRUCTURED_INPUT_DATA,
      structuredInputData);
    write(batchJobRequest);
    return batchJobRequest;
  }

  public DataObject createBatchJobRequest(final long batchJobId,
    final int requestSequenceNumber, final String inputDataContentType,
    final Resource inputData) {
    final DataObject batchJobRequest = create(BatchJobRequest.BATCH_JOB_REQUEST);
    batchJobRequest.setValue(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    batchJobRequest.setValue(BatchJobRequest.COMPLETED_IND, 0);
    batchJobRequest.setValue(BatchJobRequest.STARTED_IND, 0);
    batchJobRequest.setValue(BatchJobRequest.REQUEST_SEQUENCE_NUMBER,
      requestSequenceNumber);
    batchJobRequest.setValue(BatchJobRequest.INPUT_DATA_CONTENT_TYPE,
      inputDataContentType);
    batchJobRequest.setValue(BatchJobRequest.INPUT_DATA, inputData);
    write(batchJobRequest);
    return batchJobRequest;
  }

  public DataObject createBatchJobRequest(final long batchJobId,
    final int requestSequenceNumber, final String inputDataContentType,
    final String inputDataUrl) {
    final DataObject batchJobRequest = create(BatchJobRequest.BATCH_JOB_REQUEST);
    batchJobRequest.setValue(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    batchJobRequest.setValue(BatchJobRequest.COMPLETED_IND, 0);
    batchJobRequest.setValue(BatchJobRequest.STARTED_IND, 0);
    batchJobRequest.setValue(BatchJobRequest.REQUEST_SEQUENCE_NUMBER,
      requestSequenceNumber);
    batchJobRequest.setValue(BatchJobRequest.INPUT_DATA_CONTENT_TYPE,
      inputDataContentType);
    batchJobRequest.setValue(BatchJobRequest.INPUT_DATA_URL, inputDataUrl);
    write(batchJobRequest);
    return batchJobRequest;
  }

  public DataObject createBatchJobRequest(final long batchJobId,
    final int requestSequenceNumber, final String errorCode,
    final String errorMessage, final String errorDebugMessage) {
    final DataObject batchJobRequest = create(BatchJobRequest.BATCH_JOB_REQUEST);
    batchJobRequest.setValue(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    batchJobRequest.setValue(BatchJobRequest.REQUEST_SEQUENCE_NUMBER,
      requestSequenceNumber);
    batchJobRequest.setValue(BatchJobRequest.ERROR_CODE, errorCode);
    batchJobRequest.setValue(BatchJobRequest.ERROR_MESSAGE, errorMessage);
    if (errorDebugMessage != null) {
      batchJobRequest.setValue(
        BatchJobRequest.ERROR_DEBUG_MESSAGE,
        errorDebugMessage.substring(0,
          Math.max(4000, errorDebugMessage.length())));
    }
    batchJobRequest.setValue(BatchJobRequest.STARTED_IND, 1);
    batchJobRequest.setValue(BatchJobRequest.COMPLETED_IND, 1);
    write(batchJobRequest);
    return batchJobRequest;
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

    final Query query = new Query(UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
    query.addFilter(UserGroupAccountXref.USER_GROUP_ID, userGroupId.longValue());
    query.addFilter(UserGroupAccountXref.USER_ACCOUNT_ID,
      userAccountId.longValue());

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
    deleteBatchJobRequests(batchJobId);

    final Query query = new Query(BatchJob.BATCH_JOB);
    query.addFilter(BatchJob.BATCH_JOB_ID, batchJobId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteBatchJobRequests(final Long batchJobId) {
    final Query query = new Query(BatchJobRequest.BATCH_JOB_REQUEST);
    query.addFilter(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteBatchJobResults(final Long batchJobId) {
    final Query query = new Query(BatchJobResult.BATCH_JOB_RESULT);
    query.addFilter(BatchJobResult.BATCH_JOB_ID, batchJobId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteBusinessApplicationStatistics(
    final Integer businessApplicationStatisticsId) {
    final Query query = new Query(
      BusinessApplicationStatistics.APPLICATION_STATISTICS);
    query.addFilter(BusinessApplicationStatistics.APPLICATION_STATISTIC_ID,
      businessApplicationStatisticsId);
    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteConfigPropertiesForModule(final String moduleName) {
    final Query query = new Query(ConfigProperty.CONFIG_PROPERTY);
    query.addFilter(ConfigProperty.MODULE_NAME, moduleName);
    return dataStore.delete(query);
  }

  public void deleteUserAccount(final DataObject userAccount) {
    final Number userAccountId = userAccount.getIdValue();

    final Query membersQuery = new Query(
      UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
    membersQuery.addFilter(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);
    dataStore.delete(membersQuery);

    delete(userAccount);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void deleteUserGroup(final DataObject userGroup) {
    final Number userGroupId = userGroup.getIdValue();

    final Query membersQuery = new Query(
      UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
    membersQuery.addFilter(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    dataStore.delete(membersQuery);

    final Query permissionsQuery = new Query(
      UserGroupPermission.USER_GROUP_PERMISSION);
    permissionsQuery.addFilter(UserGroupPermission.USER_GROUP_ID, userGroupId);
    dataStore.delete(permissionsQuery);

    delete(userGroup);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteUserGroupAccountXref(final DataObject userGroup,
    final DataObject userAccount) {
    final Number userGroupId = userGroup.getIdValue();
    final Number userAccountId = userAccount.getIdValue();
    final Query query = new Query(UserGroupAccountXref.USER_GROUP_ACCOUNT_XREF);
    query.addFilter(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
    query.addFilter(UserGroupAccountXref.USER_ACCOUNT_ID, userAccountId);

    return dataStore.delete(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int deleteUserGroupsForModule(final String moduleName) {
    final Query query = new Query(UserGroup.USER_GROUP);
    query.addFilter(UserGroup.MODULE_NAME, moduleName);
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
    final Query query = new Query(BatchJob.BATCH_JOB);
    query.addFilter(BatchJob.USER_ID, consumerKey);
    query.addFilter(BatchJob.BATCH_JOB_ID, batchJobId);
    return dataStore.queryFirst(query);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public List<Long> getBatchJobIds(final String businessApplicationName,
    final String jobStatus) {
    final Query query = new Query(BatchJob.BATCH_JOB);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    query.addFilter(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    query.addFilter(BatchJob.JOB_STATUS, jobStatus);
    final Reader<DataObject> batchJobs = dataStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final DataObject batchJob : batchJobs) {
        final Long batchJobId = DataObjectUtil.getLong(batchJob,
          BatchJobRequest.BATCH_JOB_ID);
        batchJobIds.add(batchJobId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  public List<Long> getBatchJobIdsToSchedule(
    final String businessApplicationName) {
    final Query query = new Query(BatchJob.BATCH_JOB);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    query.addFilter(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
    query.setWhereClause("JOB_STATUS IN ('requestsCreated', 'processing') AND NUM_SUBMITTED_REQUESTS > 0 AND NUM_EXECUTING_REQUESTS + NUM_FAILED_REQUESTS + NUM_COMPLETED_REQUESTS < NUM_SUBMITTED_REQUESTS ");
    query.addOrderBy(BatchJob.NUM_EXECUTING_REQUESTS, true);
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
  public DataObject getBatchJobRequest(final long batchJobRequestId) {
    return dataStore.load(BatchJobRequest.BATCH_JOB_REQUEST, batchJobRequestId);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJobRequestLocked(final long batchJobRequestId) {
    return dataStore.load(BatchJobRequest.BATCH_JOB_REQUEST, batchJobRequestId);
  }

  public List<DataObject> getBatchJobRequests(final List<Long> requestIds) {
    final Query query = new Query(BatchJobRequest.BATCH_JOB_REQUEST);
    query.setWhereClause("BATCH_JOB_REQUEST_ID IN ("
      + CollectionUtil.toString(requestIds) + ")");
    final Reader<DataObject> batchJobRequests = dataStore.query(query);
    try {
      return batchJobRequests.read();
    } finally {
      batchJobRequests.close();
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public DataObject getBatchJobResult(final long batchJobResultId) {
    return dataStore.load(BatchJobResult.BATCH_JOB_RESULT, batchJobResultId);
  }

  public List<DataObject> getBatchJobResults(final long batchJobId) {
    final Query query = new Query(BatchJobResult.BATCH_JOB_RESULT);
    query.setAttributeNames(BatchJobResult.ALL_EXCEPT_BLOB);
    query.addFilter(BatchJobResult.BATCH_JOB_ID, batchJobId);
    query.addOrderBy(BatchJobResult.BATCH_JOB_RESULT_ID, true);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getBatchJobsForUser(final String consumerKey) {
    final Query query = new Query(BatchJob.BATCH_JOB);
    query.addFilter(BatchJob.USER_ID, consumerKey);
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
    final Query query = new Query(BatchJob.BATCH_JOB);
    query.addFilter(BatchJob.USER_ID, consumerKey);
    query.addFilter(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
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
    final Query query = new Query(ConfigProperty.CONFIG_PROPERTY);
    query.addFilter(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    query.addFilter(ConfigProperty.COMPONENT_NAME, componentName);
    query.addFilter(ConfigProperty.PROPERTY_NAME, propertyName);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getConfigPropertiesForComponent(
    final String moduleName, final String componentName) {
    final Query query = new Query(ConfigProperty.CONFIG_PROPERTY);
    query.addFilter(ConfigProperty.MODULE_NAME, moduleName);
    query.addFilter(ConfigProperty.COMPONENT_NAME, componentName);
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
    final Query query = new Query(ConfigProperty.CONFIG_PROPERTY);
    query.addFilter(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    query.addFilter(ConfigProperty.MODULE_NAME, moduleName);
    query.addFilter(ConfigProperty.COMPONENT_NAME, componentName);
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
    final Query query = new Query(ConfigProperty.CONFIG_PROPERTY);
    query.addFilter(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    query.addFilter(ConfigProperty.MODULE_NAME, moduleName);
    query.addFilter(ConfigProperty.COMPONENT_NAME, componentName);
    query.addFilter(ConfigProperty.PROPERTY_NAME, propertyName);
    return dataStore.queryFirst(query);
  }

  public DataObjectStore getDataStore() {
    return dataStore;
  }

  public Reader<DataObject> getErrorResultDataRequests(final long batchJobId) {
    final Query query = new Query(BatchJobRequest.BATCH_JOB_REQUEST);
    query.addFilter(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    query.addFilter(BatchJobRequest.COMPLETED_IND, 1);
    query.setWhereClause("ERROR_CODE IS NOT NULL");

    query.addOrderBy(BatchJobRequest.REQUEST_SEQUENCE_NUMBER, true);
    final Reader<DataObject> reader = dataStore.query(query);
    return reader;
  }

  public List<Long> getNonExecutingRequestIds(final int numRequests,
    final Long batchJobId) {
    final Query query = new Query(BatchJobRequest.BATCH_JOB_REQUEST);
    query.setAttributeNames(BatchJobRequest.BATCH_JOB_REQUEST_ID);
    query.addFilter(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    query.addFilter(BatchJobRequest.STARTED_IND, 0);
    query.addFilter(BatchJobRequest.COMPLETED_IND, 0);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      final List<DataObject> requests = CollectionUtil.subList(reader,
        numRequests);
      final List<Long> requestIds = new ArrayList<Long>();
      for (final DataObject batchJobRequest : requests) {
        final Long batchJobRequestId = DataObjectUtil.getLong(batchJobRequest,
          BatchJobRequest.BATCH_JOB_REQUEST_ID);
        requestIds.add(batchJobRequestId);
      }
      return requestIds;
    } finally {
      reader.close();
    }
  }

  /**
   * Get all the jobs that are either marked for deletion, or that have had a
   * status change timestamp less than the passed timestamp.
   * 
   * @param expiryTimeStamp The timestamp of the maximum age of the completed
   *          jobs to be retained.
   * @return The batch job ids.
   */
  public List<Long> getOldBatchJobIds(final Timestamp keepUntilTimestamp) {
    final Query query = new Query(BatchJob.BATCH_JOB);
    query.setAttributeNames(BatchJob.BATCH_JOB_ID);
    query.setWhereClause("JOB_STATUS  = 'markedForDeletion' OR (JOB_STATUS IN ('resultsCreated', 'downloadInitiated') AND WHEN_STATUS_CHANGED < ?)");
    query.addParameter(keepUntilTimestamp);
    final Reader<DataObject> batchJobs = dataStore.query(query);
    try {
      final List<Long> batchJobIds = new ArrayList<Long>();
      for (final DataObject batchJob : batchJobs) {
        final Long batchJobRequestId = DataObjectUtil.getLong(batchJob,
          BatchJobRequest.BATCH_JOB_REQUEST_ID);
        batchJobIds.add(batchJobRequestId);
      }
      return batchJobIds;
    } finally {
      batchJobs.close();
    }
  }

  public Reader<DataObject> getStructuredResultDataRequests(
    final long batchJobId) {
    final Query query = new Query(BatchJobRequest.BATCH_JOB_REQUEST);
    query.addFilter(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    query.addFilter(BatchJobRequest.COMPLETED_IND, 1);
    query.addFilter(BatchJobRequest.ERROR_CODE, null);
    query.setWhereClause("STRUCTURED_RESULT_DATA IS NOT NULL");

    query.addOrderBy(BatchJobRequest.REQUEST_SEQUENCE_NUMBER, true);
    final Reader<DataObject> reader = dataStore.query(query);
    return reader;
  }

  /**
   * Get the user with the specified consumer key.
   * 
   * @param consumerKey The external user class.
   * @return The user account if it exists, null otherwise.
   */
  public DataObject getUserAccount(final String consumerKey) {
    final Query query = new Query(UserAccount.USER_ACCOUNT);
    query.addFilter(UserAccount.CONSUMER_KEY, consumerKey);
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
    final Query query = new Query(UserAccount.USER_ACCOUNT);
    query.addFilter(UserAccount.USER_ACCOUNT_CLASS, userClass);
    query.addFilter(UserAccount.USER_NAME, userName);
    return dataStore.queryFirst(query);
  }

  public ResultPager<DataObject> getUserAccountsForUserGroup(
    final DataObject userGroup) {
    final Query query = new Query(UserAccount.USER_ACCOUNT);
    query.setFromClause("CPF.CPF_USER_ACCOUNTS T"
      + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_ACCOUNT_ID = X.USER_ACCOUNT_ID");
    query.addFilter(UserGroupAccountXref.USER_GROUP_ID, userGroup.getIdValue());

    final ResultPager<DataObject> pager = dataStore.page(query);
    return pager;
  }

  public List<DataObject> getUserAccountsLikeName(final String name) {
    if (StringUtils.hasText(name)) {
      final Query query = new Query(UserAccount.USER_ACCOUNT);
      final String likeName = "%" + name.toUpperCase() + "%";
      query.setWhereClause("UPPER(CONSUMER_KEY) like ? OR UPPER(USER_NAME) like ?");
      query.addParameter(likeName);
      query.addParameter(likeName);
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
    final Query query = new Query(UserGroup.USER_GROUP);
    query.addFilter(UserGroup.USER_GROUP_NAME, groupName);
    return dataStore.queryFirst(query);
  }

  public DataObject getUserGroup(final String moduleName, final String groupName) {
    final Query query = new Query(UserGroup.USER_GROUP);
    query.addFilter(UserGroup.MODULE_NAME, moduleName);
    query.addFilter(UserGroup.USER_GROUP_NAME, groupName);
    return dataStore.queryFirst(query);
  }

  public DataObject getUserGroupPermission(final List<String> userGroupNames,
    final String moduleName, final String resourceClass,
    final String resourceId, final String actionName) {
    final Query query = new Query(UserGroupPermission.USER_GROUP_PERMISSION);
    query.setFromClause("CPF.CPF_USER_GROUP_PERMISSIONS T"
      + " JOIN CPF.CPF_USER_GROUPS G ON T.USER_GROUP_ID = G.USER_GROUP_ID");
    query.addFilter(UserGroup.USER_GROUP_NAME, userGroupNames);
    query.addFilter("T." + UserGroupPermission.MODULE_NAME, moduleName);
    query.addFilter(UserGroupPermission.RESOURCE_CLASS, resourceClass);
    query.addFilter(UserGroupPermission.RESOURCE_ID, resourceId);
    query.addFilter(UserGroupPermission.ACTION_NAME, actionName);
    return dataStore.queryFirst(query);
  }

  public List<DataObject> getUserGroupPermissions(final DataObject userGroup,
    final String moduleName) {
    final Query query = new Query(UserGroupPermission.USER_GROUP_PERMISSION);
    query.addFilter(UserGroupPermission.USER_GROUP_ID, userGroup.getIdValue());
    query.addFilter(UserGroupPermission.MODULE_NAME, moduleName);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      return reader.read();
    } finally {
      reader.close();
    }
  }

  public List<DataObject> getUserGroupsForModule(final String moduleName) {
    final Query query = new Query(UserGroup.USER_GROUP);
    query.addFilter(UserGroup.MODULE_NAME, moduleName);
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
    query.setWhereClause("X.USER_ACCOUNT_ID = ?");
    query.addParameter(userAccount.getIdValue(), new JdbcLongAttribute("", 0,
      0, true, null));
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      final List<DataObject> groups = reader.read();
      return new LinkedHashSet<DataObject>(groups);
    } finally {
      reader.close();
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public boolean hasBatchJobUnexecutedJobs(final long batchJobId) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "SELECT NUM_SUBMITTED_REQUESTS - NUM_COMPLETED_REQUESTS - NUM_FAILED_REQUESTS - NUM_EXECUTING_REQUESTS FROM CPF.CPF_BATCH_JOBS WHERE BATCH_JOB_ID = ?";
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

  public void setBatchJobRequestsStarted(final List<Long> batchJobRequestIds) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOB_REQUESTS SET STARTED_IND = 1 WHERE BATCH_JOB_REQUEST_ID IN ("
        + CollectionUtil.toString(batchJobRequestIds) + ")";
      try {
        JdbcUtils.executeUpdate(dataSource, sql);
      } catch (final SQLException e) {
        throw new RuntimeException("Unable to set started status", e);
      }
    }
  }

  public void setBatchJobStatus(final DataObject batchJob,
    final String jobStatus) {
    if (!jobStatus.equals(BatchJob.JOB_STATUS)) {
      batchJob.setValue(BatchJob.JOB_STATUS, jobStatus);
      final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      batchJob.setValue(BatchJob.WHEN_STATUS_CHANGED, timestamp);
      write(batchJob);
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

  }

  public int updateBatchJobExecutionCounts(final Long batchJobId,
    final int numExecutingRequests, final int numCompletedRequests,
    final int numFailedRequests) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "NUM_EXECUTING_REQUESTS = NUM_EXECUTING_REQUESTS + ?, "
        + "NUM_COMPLETED_REQUESTS = NUM_COMPLETED_REQUESTS + ?, "
        + "NUM_FAILED_REQUESTS = NUM_FAILED_REQUESTS + ? "
        + "WHERE BATCH_JOB_ID = ?";
      try {
        return JdbcUtils.executeUpdate(dataSource, sql, numExecutingRequests,
          numCompletedRequests, numFailedRequests, batchJobId);
      } catch (final SQLException e) {
        throw new RuntimeException("Unable to reset started status", e);
      }
    }

    return 0;
  }

  /**
   * Update the request execution/failed counts for all the {@link BatchJob}s
   * which are in the processing or requestsCreated status.
   * 
   * @param businessApplicationNames The business application names to update
   *          the status for.
   * @return The number of records updated.
   */
  public int updateBatchJobExecutionCounts(final String businessApplicationName) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "NUM_EXECUTING_REQUESTS = 0, "
        + "NUM_COMPLETED_REQUESTS = ( SELECT COUNT(*) FROM CPF.CPF_BATCH_JOB_REQUESTS BJRQ WHERE BJRQ.BATCH_JOB_ID = BJ.BATCH_JOB_ID AND COMPLETED_IND = 1 AND ERROR_CODE IS NULL), "
        + "NUM_FAILED_REQUESTS = ( SELECT COUNT(*) FROM CPF.CPF_BATCH_JOB_REQUESTS BJRQ WHERE BJRQ.BATCH_JOB_ID = BJ.BATCH_JOB_ID AND COMPLETED_IND = 1 AND ERROR_CODE IS NOT NULL) "
        + "WHERE JOB_STATUS IN ('processing', 'requestsCreated') AND BUSINESS_APPLICATION_NAME = ?";
      try {
        return JdbcUtils.executeUpdate(dataSource, sql, businessApplicationName);
      } catch (final SQLException e) {
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
   * @param businessApplicationNames The business application names to update
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
      } catch (final SQLException e) {
        throw new RuntimeException("Unable to update status: " + sql, e);
      }
    }

    return 0;
  }

  /**
   * Increase the numExecutingRequests and set the
   * mostRecentRequestScheduledTimestamp for the BatchJob.
   * 
   * @param batchJobId The BatchJob identifier.
   * @param numExecutingRequests The number of requests to increase the
   *          numExecutingRequests by.
   * @param timestamp The timestamp.
   */
  public int updateBatchJobStartRequestExecution(final long batchJobId,
    final int numExecutingRequests, final Timestamp timestamp) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOBS BJ SET "
        + "NUM_EXECUTING_REQUESTS = NUM_EXECUTING_REQUESTS + ?, WHEN_STATUS_CHANGED = ?, WHEN_UPDATED = ?, WHO_UPDATED = 'SYSTEM' "
        + "WHERE BATCH_JOB_ID = ?";
      try {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return JdbcUtils.executeUpdate(dataSource, sql, numExecutingRequests,
          now, now, batchJobId);
      } catch (final SQLException e) {
        throw new RuntimeException("Unable update counts: " + sql, e);
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
   * @param businessApplicationNames The list of business application names.
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
      } catch (final SQLException e) {
        throw new RuntimeException("Unable to update status: " + sql, e);
      }
    }
    return 0;
  }

  public int updateResetRequestsForRestart(final String businessApplicationName) {
    if (dataStore instanceof JdbcDataObjectStore) {
      final JdbcDataObjectStore jdbcDataStore = (JdbcDataObjectStore)dataStore;
      final DataSource dataSource = jdbcDataStore.getDataSource();
      final String sql = "UPDATE CPF.CPF_BATCH_JOB_REQUESTS SET STARTED_IND = 0 WHERE STARTED_IND = 1 AND COMPLETED_IND = 0 AND BATCH_JOB_ID IN (SELECT BATCH_JOB_ID FROM CPF.CPF_BATCH_JOBS WHERE BUSINESS_APPLICATION_NAME = ?)";
      try {
        return JdbcUtils.executeUpdate(dataSource, sql, businessApplicationName);
      } catch (final SQLException e) {
        throw new RuntimeException("Unable to reset started status", e);
      }
    }

    return 0;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void write(final DataObject object) {
    final Writer<DataObject> writer = dataStore.getWriter();
    try {
      final SecurityContext securityContext = SecurityContextHolder.getContext();
      final Authentication authentication = securityContext.getAuthentication();
      String consumerKey;
      if (authentication == null) {
        consumerKey = "SYSTEM";
        SecurityContextHolder.clearContext();
      } else {
        consumerKey = authentication.getName();
      }
      final Timestamp time = new Timestamp(System.currentTimeMillis());
      switch (object.getState()) {
        case New:
          final DataObjectMetaData metaData = object.getMetaData();

          if (metaData.getIdAttributeIndex() != -1
            && object.getIdValue() == null) {
            final Object id = dataStore.createPrimaryIdValue(metaData.getPath());
            object.setIdValue(id);
          }
          object.setValue("WHO_CREATED", consumerKey);
          object.setValue("WHEN_CREATED", time);
          object.setValue("WHO_UPDATED", consumerKey);
          object.setValue("WHEN_UPDATED", time);
        break;
        case Persisted:
          object.setValue("WHO_UPDATED", consumerKey);
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
}
