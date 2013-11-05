package ca.bc.gov.open.cpf.api.scheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.security.service.AuthorizationService;
import ca.bc.gov.open.cpf.api.security.service.AuthorizationServiceUserSecurityServiceFactory;
import ca.bc.gov.open.cpf.client.api.ErrorCode;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.impl.log.AppLogUtil;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEvent;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEventListener;
import ca.bc.gov.open.cpf.plugin.impl.security.SecurityServiceFactory;

import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.io.DataObjectStore;
import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.io.MapReaderDataObjectReader;
import com.revolsys.gis.data.model.ArrayDataObject;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.AttributeProperties;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.query.Query;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.MapReaderFactory;
import com.revolsys.io.MapWriter;
import com.revolsys.io.MapWriterFactory;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.io.Reader;
import com.revolsys.io.csv.CsvMapWriter;
import com.revolsys.io.html.XhtmlMapWriter;
import com.revolsys.io.json.JsonDataObjectIoFactory;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.io.kml.Kml22Constants;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.channel.NamedChannelBundle;
import com.revolsys.spring.InputStreamResource;
import com.revolsys.spring.InvokeMethodAfterCommit;
import com.revolsys.transaction.SendToChannelAfterCommit;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.UrlUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.valid.IsValidOp;

public class BatchJobService implements ModuleEventListener {
  protected static Map<String, String> getBusinessApplicationParameters(
    final DataObject batchJob) {
    final String jobParameters = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_PARAMS);
    final Map<String, String> parameters = JsonMapIoFactory.toMap(jobParameters);
    return parameters;
  }

  public static Map<String, Object> getGroupLogData(
    final BatchJobRequestExecutionGroup group) {
    final String groupId = group.getId();
    final long batchJobId = group.getBatchJobId();
    final String businessApplicationName = group.getBusinessApplicationName();
    final Map<String, Object> appLogData = new LinkedHashMap<String, Object>();
    appLogData.put("businessApplicationName", businessApplicationName);
    appLogData.put("batchJobId", batchJobId);
    appLogData.put("groupId", groupId);
    return appLogData;
  }

  protected static Map<String, BusinessApplicationStatistics> getStatistics(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId,
    final String businessApplicationName) {
    Map<String, BusinessApplicationStatistics> statistics = statisticsByAppAndId.get(businessApplicationName);
    if (statistics == null) {
      statistics = new HashMap<String, BusinessApplicationStatistics>();
      statisticsByAppAndId.put(businessApplicationName, statistics);
    }
    return statistics;
  }

  protected static BusinessApplicationStatistics getStatistics(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId,
    final String businessApplicationName, final String statisticsId) {
    final Map<String, BusinessApplicationStatistics> statisticsById = getStatistics(
      statisticsByAppAndId, businessApplicationName);
    BusinessApplicationStatistics statistics = statisticsById.get(statisticsId);
    if (statistics == null) {
      statistics = new BusinessApplicationStatistics(businessApplicationName,
        statisticsId);
      statisticsById.put(statisticsId, statistics);
    }
    return statistics;
  }

  public static Map<String, Object> toMap(final DataObject batchJob,
    final String jobUrl, final long timeUntilNextCheck) {
    try {
      final Map<String, Object> jobMap = new NamedLinkedHashMap<String, Object>(
        "BatchJob");
      jobMap.put("id", new URI(jobUrl));
      jobMap.put("consumerKey", batchJob.getValue(BatchJob.USER_ID));
      jobMap.put("businessApplicationName",
        batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME));
      jobMap.put("businessApplicationVersion",
        batchJob.getValue(BatchJob.BUSINESS_APPLICATION_VERSION));
      final Map<String, String> parameters = getBusinessApplicationParameters(batchJob);
      for (final Entry<String, String> param : parameters.entrySet()) {
        jobMap.put(param.getKey(), param.getValue());
      }
      jobMap.put("jobStatus", batchJob.getValue(BatchJob.JOB_STATUS));

      jobMap.put("secondsToWaitForStatusCheck", timeUntilNextCheck);

      jobMap.put("numSubmittedRequests",
        batchJob.getValue(BatchJob.NUM_SUBMITTED_REQUESTS));
      jobMap.put("numCompletedRequests",
        batchJob.getValue(BatchJob.NUM_COMPLETED_REQUESTS));
      jobMap.put("numFailedRequests",
        batchJob.getValue(BatchJob.NUM_FAILED_REQUESTS));
      jobMap.put("resultDataContentType",
        batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE));
      if (batchJob.getValue(BatchJob.COMPLETED_TIMESTAMP) != null) {
        String path;
        if (jobUrl.endsWith("/")) {
          path = jobUrl + "results/";
        } else {
          path = jobUrl + "/results/";
        }

        jobMap.put("resultsUrl", new URI(path));
      }
      return jobMap;
    } catch (final URISyntaxException e) {
      throw new IllegalArgumentException(jobUrl + " is not a valid URI", e);
    }
  }

  private StatisticsProcess statisticsProcess;

  private AuthorizationService authorizationService;

  private String baseUrl;

  private BusinessApplicationRegistry businessApplicationRegistry;

  /** The email address messages are sent from. */
  private String fromEmail;

  private NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = new NamedChannelBundle<BatchJobRequestExecutionGroup>();

  /** The class used to send email. */
  private JavaMailSender mailSender;

  private long maxWorkerWaitTime = 60 * 1000;

  private Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId = new HashMap<String, Map<String, BusinessApplicationStatistics>>();

  private BatchJobPostProcess postProcess;

  private BatchJobPreProcess preProcess;

  private boolean running;

  private BatchJobScheduler scheduler;

  private SecurityServiceFactory securityServiceFactory;

  private Map<String, String> userClassBaseUrls;

  private final Map<String, Integer> connectedWorkerCounts = new HashMap<String, Integer>();

  private CpfDataAccessObject dataAccessObject;

  private DataObjectStore dataStore;

  private final Map<String, Worker> workersById = new TreeMap<String, Worker>();

  private final Map<Long, Set<BatchJobRequestExecutionGroup>> groupsByJobId = new HashMap<Long, Set<BatchJobRequestExecutionGroup>>();

  private final Set<Long> preprocesedJobIds = new HashSet<Long>();

  /**
   * Generate an error result for the job, update the job counts and status, and
   * back out any add job requests that have already been added.
   * 
   * @param validationErrorCode The failure error code.
   */
  private boolean addJobValidationError(final long batchJobId,
    final ErrorCode validationErrorCode,
    final String validationErrorDebugMessage,
    final String validationErrorMessage) {

    dataAccessObject.deleteBatchJobExecutionGroups(batchJobId);

    final String errorFormat = "text/csv";
    final StringWriter errorWriter = new StringWriter();

    String newErrorMessage = validationErrorMessage;
    if (validationErrorMessage.equals("")) {
      newErrorMessage = validationErrorCode.getDescription();
    }
    final MapWriter errorMapWriter = new CsvMapWriter(errorWriter);
    final Map<String, String> errorResultMap = new HashMap<String, String>();
    errorResultMap.put("Code", validationErrorCode.name());
    errorResultMap.put("Message", newErrorMessage);
    errorMapWriter.write(errorResultMap);
    try {
      final byte[] errorBytes = errorWriter.toString().getBytes("UTF-8");
      createBatchJobResult(batchJobId, BatchJobResult.ERROR_RESULT_DATA,
        errorFormat, errorBytes);
    } catch (final UnsupportedEncodingException e) {
    }

    dataAccessObject.setBatchJobFailed(batchJobId);
    LoggerFactory.getLogger(BatchJobService.class).debug(
      validationErrorDebugMessage);
    return false;
  }

  protected void addStatisticRollUp(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId,
    final String businessApplicationName, final String statisticsId,
    final Map<String, ? extends Object> values) {
    if (statisticsId != null) {
      final BusinessApplicationStatistics statistics = getStatistics(
        statisticsByAppAndId, businessApplicationName, statisticsId);
      statistics.addStatistics(values);
      final String parentStatisticsId = statistics.getParentId();
      addStatisticRollUp(statisticsByAppAndId, businessApplicationName,
        parentStatisticsId, values);
    }
  }

  public void addStatistics(final BusinessApplication businessApplication,
    final Map<String, Object> values) {
    values.put("businessApplicationName", businessApplication.getName());
    values.put("time", new Date(System.currentTimeMillis()));
    sendStatistics(values);
  }

  protected void addStatistics(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId,
    final BusinessApplicationStatistics statistics) {
    final String statisticsId = statistics.getId();
    final String businessApplicationName = statistics.getBusinessApplicationName();
    final Map<String, BusinessApplicationStatistics> statisticsById = getStatistics(
      statisticsByAppAndId, businessApplicationName);
    final BusinessApplicationStatistics previousStatistics = statisticsById.get(statisticsId);
    if (previousStatistics == null) {
      statisticsById.put(statisticsId, statistics);
    } else {
      previousStatistics.addStatistics(statistics);
      if (previousStatistics.getDatabaseId() == null) {
        final Integer databaseId = statistics.getDatabaseId();
        previousStatistics.setDatabaseId(databaseId);
      }
    }
    addStatisticRollUp(statisticsByAppAndId, businessApplicationName,
      statistics.getParentId(), statistics.toMap());
  }

  public boolean cancelBatchJob(final long batchJobId) {
    synchronized (preprocesedJobIds) {
      preprocesedJobIds.remove(batchJobId);
    }
    Set<BatchJobRequestExecutionGroup> groups;
    synchronized (groupsByJobId) {
      groups = groupsByJobId.remove(batchJobId);
    }
    if (groups != null) {
      for (final BatchJobRequestExecutionGroup group : groups) {
        group.cancel();
      }
    }
    final boolean cancelled = dataAccessObject.cancelBatchJob(batchJobId) == 1;
    if (cancelled) {
      try {
        dataAccessObject.deleteBatchJobResults(batchJobId);
      } finally {
        dataAccessObject.deleteBatchJobExecutionGroups(batchJobId);
      }
    }
    return cancelled;
  }

  public void cancelGroup(final Worker worker, final String groupId) {
    if (groupId != null) {
      final BatchJobRequestExecutionGroup group = worker.removeExecutingGroup(groupId);
      if (group != null) {
        LoggerFactory.getLogger(BatchJobService.class).info(
          "Rescheduling group " + group.getBatchJobId() + " " + groupId);
        removeGroup(group);
        group.resetId();
        schedule(group);
      }
    }
  }

  public boolean canDeleteStatistic(
    final BusinessApplicationStatistics statistics, final Date currentTime) {
    final String durationType = statistics.getDurationType();

    if (durationType.equals(BusinessApplicationStatistics.YEAR)) {
      return false;
    } else {
      final String parentDurationType = statistics.getParentDurationType();
      final String parentId = statistics.getParentId();
      final String currentParentId = BusinessApplicationStatistics.getId(
        parentDurationType, currentTime);
      return parentId.compareTo(currentParentId) < 0;
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected void collateAllStatistics() {
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId = new HashMap<String, Map<String, BusinessApplicationStatistics>>();

    collateInMemoryStatistics(statisticsByAppAndId);

    collateDatabaseStatistics(statisticsByAppAndId);

    saveStatistics(statisticsByAppAndId);

    setStatisticsByAppAndId(statisticsByAppAndId);
  }

  private void collateDatabaseStatistics(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId) {
    final Query query = new Query(
      BusinessApplicationStatistics.APPLICATION_STATISTICS);
    query.addOrderBy(BusinessApplicationStatistics.START_TIMESTAMP, true);
    final Reader<DataObject> reader = dataStore.query(query);
    try {
      for (final DataObject dbStatistics : reader) {
        boolean delete = false;
        final Date startTime = dbStatistics.getValue(BusinessApplicationStatistics.START_TIMESTAMP);
        final String durationType = dbStatistics.getValue(BusinessApplicationStatistics.DURATION_TYPE);
        final String businessApplicationName = dbStatistics.getValue(BusinessApplicationStatistics.BUSINESS_APPLICATION_NAME);
        final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
        if (businessApplication != null) {
          final String statisticsId = BusinessApplicationStatistics.getId(
            durationType, startTime);
          final String valuesString = dbStatistics.getValue(BusinessApplicationStatistics.STATISTIC_VALUES);
          if (StringUtils.hasText(valuesString)) {
            final Map<String, Object> values = JsonMapIoFactory.toObjectMap(valuesString);
            if (values.isEmpty()) {
              delete = true;
            } else {
              final BusinessApplicationStatistics statistics = getStatistics(
                statisticsByAppAndId, businessApplicationName, statisticsId);

              final Integer databaseId = dbStatistics.getInteger(BusinessApplicationStatistics.APPLICATION_STATISTIC_ID);
              final Integer previousDatabaseId = statistics.getDatabaseId();
              if (previousDatabaseId == null) {
                statistics.setDatabaseId(databaseId);
                statistics.addStatistics(values);
                final String parentStatisticsId = statistics.getParentId();
                addStatisticRollUp(statisticsByAppAndId,
                  businessApplicationName, parentStatisticsId, values);
              } else if (!databaseId.equals(previousDatabaseId)) {
                statistics.addStatistics(values);
                final String parentStatisticsId = statistics.getParentId();
                addStatisticRollUp(statisticsByAppAndId,
                  businessApplicationName, parentStatisticsId, values);
                delete = true;
              }
            }
          } else {
            delete = true;
          }

          if (delete) {
            dataStore.delete(dbStatistics);
          }
        }
      }
    } finally {
      reader.close();
    }
  }

  private void collateInMemoryStatistics(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId) {
    final Map<String, Map<String, BusinessApplicationStatistics>> oldStatistics = getStatisticsByAppAndId();
    for (final Map<String, BusinessApplicationStatistics> statsById : oldStatistics.values()) {
      for (final BusinessApplicationStatistics statistics : statsById.values()) {
        final String durationType = statistics.getDurationType();
        if (durationType.equals(BusinessApplicationStatistics.HOUR)) {
          final Integer databaseId = statistics.getDatabaseId();
          if (databaseId == null || statistics.isModified()) {
            addStatistics(statisticsByAppAndId, statistics);
          }
        }
      }
    }
  }

  public void collateStatistics() {
    final Map<String, ?> values = Collections.singletonMap(
      StatisticsProcess.COLLATE, Boolean.TRUE);
    sendStatistics(values);
  }

  protected boolean createBatchJobExecutionGroup(final Long batchJobId,
    final int sequenceNumber, final DataObjectMetaData requestMetaData,
    final List<Map<String, Object>> group) {
    final String structuredInputDataString = JsonDataObjectIoFactory.toString(
      requestMetaData, group);
    synchronized (preprocesedJobIds) {
      if (preprocesedJobIds.contains(batchJobId)) {
        dataAccessObject.createBatchJobExecutionGroup(batchJobId,
          sequenceNumber, structuredInputDataString, group.size());
        return true;
      } else {
        return false;
      }
    }
  }

  public void createBatchJobResult(final long batchJobId,
    final String resultDataType, final String contentType, final Object data) {
    if (data != null) {
      if (data instanceof File) {
        final File file = (File)data;
        if (file.length() == 0) {
          return;
        }
      }
      try {
        final DataObject result = dataAccessObject.create(BatchJobResult.BATCH_JOB_RESULT);
        result.setValue(BatchJobResult.BATCH_JOB_ID, batchJobId);
        result.setValue(BatchJobResult.BATCH_JOB_RESULT_TYPE, resultDataType);
        result.setValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE, contentType);
        result.setValue(BatchJobResult.RESULT_DATA, data);
        dataAccessObject.write(result);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to save result data", e);
      }
    }
  }

  public void createBatchJobResultOpaque(final long batchJobId,
    final long sequenceNumber, final String contentType, final Object data) {
    final Number batchJobResultId = dataAccessObject.createId(BatchJobResult.BATCH_JOB_RESULT);
    final DataObject result = dataAccessObject.create(BatchJobResult.BATCH_JOB_RESULT);
    result.setValue(BatchJobResult.BATCH_JOB_RESULT_ID, batchJobResultId);
    result.setValue(BatchJobResult.BATCH_JOB_ID, batchJobId);
    result.setValue(BatchJobResult.SEQUENCE_NUMBER, sequenceNumber);
    result.setValue(BatchJobResult.BATCH_JOB_RESULT_TYPE,
      BatchJobResult.OPAQUE_RESULT_DATA);
    result.setValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE, contentType);
    result.setValue(BatchJobResult.RESULT_DATA, data);
    dataAccessObject.write(result);
  }

  /**
   * Create the error BatchJobResult for a BatchJob. This will only be created
   * if there were any errors.
   * 
   * @param batchJobId The DataObject identifier.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void createResults(final long batchJobId) {
    final DataObject batchJob = dataAccessObject.getBatchJobLocked(batchJobId);
    final String resultFormat = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

    final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
    final BusinessApplication application = getBusinessApplication(businessApplicationName);
    final DataObjectMetaData resultMetaData = application.getResultMetaData();

    final String fileExtension = IoFactoryRegistry.getFileExtension(resultFormat);
    File structuredResultFile = null;

    File errorFile = null;
    Writer errorWriter = null;
    MapWriter errorResultWriter = null;
    com.revolsys.io.Writer<DataObject> structuredResultWriter = null;
    try {
      errorFile = FileUtil.createTempFile("errors", ".csv");
      errorWriter = new FileWriter(errorFile);
      errorResultWriter = new CsvMapWriter(errorWriter);

      structuredResultFile = FileUtil.createTempFile("result", "."
        + fileExtension);
      structuredResultWriter = createStructuredResultWriter(batchJob,
        batchJobId, application, structuredResultFile, resultMetaData,
        resultFormat);
      final Map<String, Object> defaultProperties = new HashMap<String, Object>(
        structuredResultWriter.getProperties());

      boolean hasErrors = false;
      boolean hasResults = false;
      try {
        final Reader<DataObject> requests = dataAccessObject.getBatchJobExecutionGroupIds(batchJobId);
        try {
          for (final DataObject batchJobExecutionGroup : requests) {
            final long batchJobExecutionGroupId = batchJobExecutionGroup.getLong(BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP_ID);
            final int mask = dataAccessObject.writeGroupResult(
              batchJobExecutionGroupId, application, resultMetaData,
              errorResultWriter, structuredResultWriter, defaultProperties);
            if ((mask & 4) > 0) {
              hasErrors = true;
            }
            if ((mask & 2) > 0) {
              hasResults = true;
            }
          }
        } finally {
          requests.close();
        }
      } finally {
        if (structuredResultWriter != null) {
          try {
            structuredResultWriter.close();
          } catch (final Throwable e) {
            LoggerFactory.getLogger(BatchJobService.class).error(
              "Unable to close structured result writer", e);
          }
        }
        if (errorResultWriter != null) {
          try {
            errorResultWriter.close();
          } catch (final Throwable e) {
            LoggerFactory.getLogger(BatchJobService.class).error(
              "Unable to close error result writer", e);
          }
        }
        FileUtil.closeSilent(errorWriter);
      }
      if (hasResults) {
        createBatchJobResult(batchJobId, BatchJobResult.STRUCTURED_RESULT_DATA,
          resultFormat, structuredResultFile);
      }
      if (hasErrors) {
        createBatchJobResult(batchJobId, BatchJobResult.ERROR_RESULT_DATA,
          "text/csv", errorFile);
      }
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to save results", e);
    } finally {
      InvokeMethodAfterCommit.invoke(errorFile, "delete");
      InvokeMethodAfterCommit.invoke(structuredResultFile, "delete");
    }
  }

  protected com.revolsys.io.Writer<DataObject> createStructuredResultWriter(
    final DataObject batchJob, final long batchJobId,
    final BusinessApplication application, final File structuredResultFile,
    final DataObjectMetaData resultMetaData, final String resultFormat) {
    final Map<String, ? extends Object> businessApplicationParameters = getBusinessApplicationParameters(batchJob);
    final GeometryFactory geometryFactory = getGeometryFactory(
      resultMetaData.getGeometryFactory(), businessApplicationParameters);
    final String title = "Job " + batchJobId + " Result";

    final FileSystemResource resource = new FileSystemResource(
      structuredResultFile);
    return createStructuredResultWriter(resource, application, resultMetaData,
      resultFormat, title, geometryFactory);
  }

  public com.revolsys.io.Writer<DataObject> createStructuredResultWriter(
    final org.springframework.core.io.Resource resource,
    final BusinessApplication application,
    final DataObjectMetaData resultMetaData, final String resultFormat,
    final String title, final GeometryFactory geometryFactory) {
    final IoFactoryRegistry ioFactory = IoFactoryRegistry.getInstance();
    final DataObjectWriterFactory writerFactory = ioFactory.getFactoryByMediaType(
      DataObjectWriterFactory.class, resultFormat);
    final com.revolsys.io.Writer<DataObject> dataObjectWriter = writerFactory.createDataObjectWriter(
      resultMetaData, resource);
    dataObjectWriter.setProperty(Kml22Constants.STYLE_URL_PROPERTY, baseUrl
      + "/kml/defaultStyle.kml#default");
    dataObjectWriter.setProperty(IoConstants.TITLE_PROPERTY, title);
    dataObjectWriter.setProperty("htmlCssStyleUrl", baseUrl
      + "/css/default.css");

    dataObjectWriter.setProperty(IoConstants.GEOMETRY_FACTORY, geometryFactory);
    dataObjectWriter.setProperties(application.getProperties());
    dataObjectWriter.open();
    return dataObjectWriter;
  }

  @PreDestroy
  public void destory() {
    running = false;
    authorizationService = null;
    businessApplicationRegistry = null;
    connectedWorkerCounts.clear();
    dataAccessObject = null;
    dataStore = null;
    groupsByJobId.clear();
    if (groupsToSchedule != null) {
      groupsToSchedule.close();
      groupsToSchedule = null;
    }
    mailSender = null;
    if (postProcess != null) {
      postProcess.getIn().writeDisconnect();
      postProcess = null;
    }
    preprocesedJobIds.clear();
    if (preProcess != null) {
      preProcess.getIn().writeDisconnect();
      preProcess = null;
    }
    if (scheduler != null) {
      scheduler.getIn().writeDisconnect();
      scheduler = null;
    }
    securityServiceFactory = null;
    statisticsByAppAndId.clear();
    if (statisticsProcess != null) {
      statisticsProcess.getIn().writeDisconnect();
      statisticsProcess = null;
    }
    userClassBaseUrls.clear();
    workersById.clear();
  }

  public AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public BatchJobRequestExecutionGroup getBatchJobRequestExecutionGroup(
    final String workerId, final String groupId) {
    final Worker worker = getWorker(workerId);
    if (worker == null) {
      return null;
    } else {
      return worker.getExecutingGroup(groupId);
    }
  }

  public BusinessApplication getBusinessApplication(final String name) {
    return businessApplicationRegistry.getBusinessApplication(name);
  }

  public BusinessApplication getBusinessApplication(final String name,
    final String version) {
    return businessApplicationRegistry.getBusinessApplication(name);
  }

  public List<String> getBusinessApplicationNames() {
    return businessApplicationRegistry.getBusinessApplicationNames();
  }

  public PluginAdaptor getBusinessApplicationPlugin(
    final BusinessApplication businessApplication) {
    return businessApplicationRegistry.getBusinessApplicationPlugin(businessApplication);
  }

  public PluginAdaptor getBusinessApplicationPlugin(
    final String businessApplicationName,
    final String businessApplicationVersion) {
    return businessApplicationRegistry.getBusinessApplicationPlugin(
      businessApplicationName, businessApplicationVersion);
  }

  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return businessApplicationRegistry;
  }

  public List<BusinessApplication> getBusinessApplications() {
    return businessApplicationRegistry.getBusinessApplications();
  }

  public CpfDataAccessObject getDataAccessObject() {
    return dataAccessObject;
  }

  public DataObjectStore getDataStore() {
    return dataStore;
  }

  /**
   * Get the email address messages are sent from.
   * 
   * @return The email address messages are sent from.
   */
  public String getFromEmail() {
    return fromEmail;
  }

  public GeometryFactory getGeometryFactory(
    final GeometryFactory geometryFactory,
    final Map<String, ? extends Object> parameters) {
    if (geometryFactory == null) {
      return null;
    } else {
      final int srid = CollectionUtil.getInteger(parameters, "resultSrid",
        geometryFactory.getSRID());
      final int numAxis = CollectionUtil.getInteger(parameters,
        "resultNumAxis", geometryFactory.getNumAxis());
      final double scaleXY = CollectionUtil.getDouble(parameters,
        "resultScaleFactorXy", geometryFactory.getScaleXY());
      final double scaleZ = CollectionUtil.getDouble(parameters,
        "resultScaleFactorZ", geometryFactory.getScaleZ());
      return GeometryFactory.getFactory(srid, numAxis, scaleXY, scaleZ);
    }
  }

  /**
   * Get a buffered reader for the job's input data. The input Data may be a
   * remote URL or a CLOB field.
   * 
   * @return BufferedReader or null if unable to connect to data
   */
  private InputStream getJobInputDataStream(final DataObject batchJob) {
    final String inputDataUrlString = batchJob.getValue(BatchJob.STRUCTURED_INPUT_DATA_URL);
    if (inputDataUrlString != null && !inputDataUrlString.equals("")) {
      try {
        final URL inputDataUrl = new URL(inputDataUrlString);
        return inputDataUrl.openStream();
      } catch (final IOException e) {
        LoggerFactory.getLogger(BatchJobService.class).error(
          "Unable to open stream: " + inputDataUrlString, e);
      }
    } else {
      try {
        final Blob inputData = batchJob.getValue(BatchJob.STRUCTURED_INPUT_DATA);
        if (inputData != null) {
          return inputData.getBinaryStream();
        }
      } catch (final SQLException e) {
        LoggerFactory.getLogger(BatchJobService.class).error(
          "Unable to open stream: " + inputDataUrlString, e);
      }
    }

    return null;
  }

  /**
   * Get the class used to send email.
   * 
   * @return The class used to send email.
   */
  public JavaMailSender getMailSender() {
    return mailSender;
  }

  public Module getModule(final String moduleName) {
    return businessApplicationRegistry.getModule(moduleName);
  }

  public Map<String, Object> getNextBatchJobRequestExecutionGroup(
    final String workerId, final List<String> moduleNames) {
    final long startTime = System.currentTimeMillis();
    final long endTime = startTime + maxWorkerWaitTime;
    final Map<String, Object> response = new HashMap<String, Object>();
    if (running) {

      BatchJobRequestExecutionGroup group = null;
      try {
        do {
          final List<String> workerModuleNames = getWorkerModuleNames(workerId,
            moduleNames);
          final long waitTime = Math.min(10000, endTime - startTime);
          if (waitTime > 0) {
            group = groupsToSchedule.read(waitTime, workerModuleNames);
          }
        } while (group == null && System.currentTimeMillis() < endTime);
      } catch (final ClosedException e) {
      }

      if (running && group != null && !group.isCancelled()) {
        final BusinessApplication businessApplication = group.getBusinessApplication();
        final String businessApplicationName = businessApplication.getName();
        final Module module = businessApplication.getModule();
        final String moduleName = group.getModuleName();
        final Date startedDate = module.getStartedDate();
        final Worker worker = getWorker(workerId);
        if (worker == null || startedDate == null) {
          schedule(group);
        } else {
          final long moduleStartTime = startedDate.getTime();

          response.put("workerId", workerId);
          response.put("moduleName", moduleName);
          response.put("moduleTime", moduleStartTime);
          response.put("businessApplicationName", businessApplicationName);
          response.put("logLevel", businessApplication.getLogLevel());

          if (worker.isModuleLoaded(moduleName, moduleStartTime)) {
            group.setExecutionStartTime(System.currentTimeMillis());
            final String groupId = group.getId();
            final Long batchJobId = group.getBatchJobId();

            response.put("batchJobId", batchJobId);
            response.put("groupId", groupId);
            if (businessApplication.isInfoLogEnabled()) {
              businessApplication.getLog().info(
                "Execution Start workerId=" + workerId + ", batchJobId="
                  + batchJobId + ", groupId=" + groupId);
            }
            response.put("consumerKey", group.getconsumerKey());
            worker.addExecutingGroup(moduleName + ":" + moduleStartTime, group);
          } else {
            response.put("action", "loadModule");
            schedule(group);
          }
        }
      }
    }
    return response;
  }

  public Object getNonEmptyValue(final Map<String, ? extends Object> map,
    final String key) {
    final Object value = map.get(key);
    if (value == null) {
      return null;
    } else {
      final String result = value.toString().trim();
      if (StringUtils.hasText(result)) {
        return value;
      } else {
        return null;
      }
    }
  }

  public BatchJobPostProcess getPostProcess() {
    return postProcess;
  }

  public BatchJobPreProcess getPreProcess() {
    return preProcess;
  }

  public SecurityService getSecurityService(final Module module,
    final String consumerKey) {
    return securityServiceFactory.getSecurityService(module, consumerKey);
  }

  public BusinessApplicationStatistics getStatistics(
    final String businessApplicationName, final String statisticsId) {
    synchronized (statisticsByAppAndId) {
      return getStatistics(statisticsByAppAndId, businessApplicationName,
        statisticsId);
    }
  }

  public Map<String, Map<String, BusinessApplicationStatistics>> getStatisticsByAppAndId() {
    return statisticsByAppAndId;
  }

  public List<BusinessApplicationStatistics> getStatisticsList(
    final String businessApplicationName) {
    synchronized (statisticsByAppAndId) {
      final Map<String, BusinessApplicationStatistics> statisticsById = getStatistics(
        statisticsByAppAndId, businessApplicationName);
      return new ArrayList<BusinessApplicationStatistics>(
        statisticsById.values());
    }
  }

  public Map<String, String> getUserClassBaseUrls() {
    return userClassBaseUrls;
  }

  public Worker getWorker(final String workerId) {
    synchronized (workersById) {
      final Worker worker = workersById.get(workerId);
      return worker;
    }
  }

  public List<String> getWorkerModuleNames(final String workerId,
    List<String> moduleNames) {
    if (moduleNames == null || moduleNames.isEmpty()) {
      moduleNames = businessApplicationRegistry.getModuleNames();
    }
    final Worker worker = getWorker(workerId);
    if (worker != null) {
      final Set<String> excludedModules = worker.getExcludedOrLoadingModules();
      if (excludedModules != null) {
        for (final String moduleNameTime : new ArrayList<String>(
          excludedModules)) {
          final int index = moduleNameTime.lastIndexOf(':');
          final String moduleName = moduleNameTime.substring(0, index);
          final long moduleTime = Long.valueOf(moduleNameTime.substring(index + 1));

          final Module module = businessApplicationRegistry.getModule(moduleName);
          if (module == null) {
            worker.removeExcludedModule(moduleNameTime);
          } else {
            if (module.getStartedDate().getTime() > moduleTime) {
              worker.removeExcludedModule(moduleNameTime);
            } else {
              moduleNames.remove(moduleName);
            }
          }
        }
      }
    }
    return moduleNames;
  }

  public List<Worker> getWorkers() {
    return new ArrayList<Worker>(workersById.values());
  }

  @PostConstruct
  public void init() {
    running = true;
    securityServiceFactory = new AuthorizationServiceUserSecurityServiceFactory(
      authorizationService);
    businessApplicationRegistry.addModuleEventListener(securityServiceFactory);
    dataStore = dataAccessObject.getDataStore();
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public void moduleChanged(final ModuleEvent event) {
    final String action = event.getAction();
    final Module module = event.getModule();
    final String moduleName = module.getName();
    synchronized (module) {
      if (action.equals(ModuleEvent.STOP)) {
        groupsToSchedule.remove(moduleName);
        for (final Worker worker : getWorkers()) {
          final String moduleNameAndTime = moduleName + ":"
            + module.getStartedTime();
          worker.cancelExecutingGroups(moduleNameAndTime);
        }
      }
      final List<String> businessApplicationNames = module.getBusinessApplicationNames();
      for (final String businessApplicationName : businessApplicationNames) {
        synchronized (businessApplicationName.intern()) {
          if (action.equals(ModuleEvent.STOP)) {
            groupsToSchedule.remove(businessApplicationName);
          } else if (action.equals(ModuleEvent.START)) {
            resetProcessingBatchJobs(moduleName, businessApplicationName);
            resetCreatingRequestsBatchJobs(moduleName, businessApplicationName);
            resetCreatingResultsBatchJobs(moduleName, businessApplicationName);
            scheduleFromDatabase(moduleName, businessApplicationName);
            if (preProcess != null) {
              preProcess.scheduleFromDatabase(moduleName,
                businessApplicationName);
            }
            if (postProcess != null) {
              postProcess.scheduleFromDatabase(moduleName,
                businessApplicationName);
            }
          }
        }
      }
    }

  }

  public void postProcess(final long batchJobId) {
    if (postProcess != null) {
      SendToChannelAfterCommit.send(postProcess.getIn(), batchJobId);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postProcessBatchJob(final long batchJobId, final long time,
    final long lastChangedTime) {
    final com.revolsys.io.Writer<DataObject> writer = dataStore.getWriter();
    try {
      final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
      final StopWatch stopWatch = new StopWatch();
      stopWatch.start();

      final long numRequests = DataObjectUtil.getInteger(batchJob,
        BatchJob.NUM_SUBMITTED_REQUESTS);
      final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
      final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
      AppLog log;
      if (businessApplication == null) {
        log = new AppLog(businessApplicationName);
      } else {
        log = businessApplication.getLog();
      }
      if (log.isInfoEnabled()) {
        log.info("Job post-process - Start, batchJobId=" + batchJobId);
      }

      final Map<String, Object> postProcessScheduledStatistics = new HashMap<String, Object>();
      postProcessScheduledStatistics.put("postProcessScheduledJobsTime", time
        - lastChangedTime);
      postProcessScheduledStatistics.put("postProcessScheduledJobsCount", 1);
      addStatistics(businessApplication, postProcessScheduledStatistics);

      createResults(batchJobId);

      if (dataAccessObject.setBatchJobCompleted(batchJobId)) {
        sendNotification(batchJobId, batchJob);
      }
      final long numCompletedRequests = DataObjectUtil.getInteger(batchJob,
        BatchJob.NUM_COMPLETED_REQUESTS);
      final long numFailedRequests = DataObjectUtil.getInteger(batchJob,
        BatchJob.NUM_FAILED_REQUESTS);
      if (log.isInfoEnabled()) {
        AppLogUtil.infoAfterCommit(log, "Job post-process - End", stopWatch);
        AppLogUtil.infoAfterCommit(log, "Job completed -End");
      }
      final Timestamp whenCreated = batchJob.getValue(BatchJob.WHEN_CREATED);

      final Map<String, Object> postProcessStatistics = new HashMap<String, Object>();

      postProcessStatistics.put("postProcessedTime", stopWatch);
      postProcessStatistics.put("postProcessedJobsCount", 1);
      postProcessStatistics.put("postProcessedRequestsCount", numRequests);

      postProcessStatistics.put("completedJobsCount", 1);
      postProcessStatistics.put("completedRequestsCount", numCompletedRequests
        + numFailedRequests);
      postProcessStatistics.put("completedFailedRequestsCount",
        numFailedRequests);
      postProcessStatistics.put("completedTime", System.currentTimeMillis()
        - whenCreated.getTime());

      InvokeMethodAfterCommit.invoke(this, "addStatistics",
        businessApplication, postProcessStatistics);
    } finally {
      writer.close();
    }
  }

  public void preProcess(final long batchJobId) {
    if (preProcess != null) {
      SendToChannelAfterCommit.send(preProcess.getIn(), batchJobId);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void preProcessBatchJob(final Long batchJobId, final long time,
    final long lastChangedTime) {
    synchronized (preprocesedJobIds) {
      preprocesedJobIds.add(batchJobId);
    }
    try {
      dataAccessObject.deleteBatchJobExecutionGroups(batchJobId);
      int numSubmittedRequests = 0;
      final StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
      final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
      final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
      if (businessApplication == null) {
        throw new IllegalArgumentException("Cannot find business application: "
          + businessApplicationName);
      }
      final AppLog log = businessApplication.getLog();
      if (log.isInfoEnabled()) {
        log.info("Job pre-process - Start, batchJobId=" + batchJobId);
      }
      try {
        final int maxGroupSize = businessApplication.getNumRequestsPerWorker();
        int numGroups = 0;
        boolean valid = true;
        final Map<String, Object> preProcessScheduledStatistics = new HashMap<String, Object>();
        preProcessScheduledStatistics.put("preProcessScheduledJobsCount", 1);
        preProcessScheduledStatistics.put("preProcessScheduledJobsTime", time
          - lastChangedTime);

        InvokeMethodAfterCommit.invoke(this, "addStatistics",
          businessApplication, preProcessScheduledStatistics);

        addStatistics(businessApplication, preProcessScheduledStatistics);

        final InputStream inputDataStream = getJobInputDataStream(batchJob);
        int numFailedRequests = batchJob.getInteger(BatchJob.NUM_FAILED_REQUESTS);
        try {
          final Map<String, String> jobParameters = getBusinessApplicationParameters(batchJob);

          final String inputContentType = batchJob.getValue(BatchJob.INPUT_DATA_CONTENT_TYPE);
          final String resultContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
          if (!businessApplication.getInputDataContentTypes().containsKey(
            inputContentType)) {
            valid = addJobValidationError(batchJobId,
              ErrorCode.BAD_INPUT_DATA_TYPE, "", "");
          } else if (!businessApplication.getResultDataContentTypes()
            .containsKey(resultContentType)) {
            valid = addJobValidationError(batchJobId,
              ErrorCode.BAD_RESULT_DATA_TYPE, "", "");
          } else if (inputDataStream == null) {
            valid = addJobValidationError(batchJobId,
              ErrorCode.INPUT_DATA_UNREADABLE, "", "");
          } else {
            final DataObjectMetaData requestMetaData = businessApplication.getRequestMetaData();
            try {
              final MapReaderFactory factory = IoFactoryRegistry.getInstance()
                .getFactoryByMediaType(MapReaderFactory.class, inputContentType);
              if (factory == null) {
                valid = addJobValidationError(batchJobId,
                  ErrorCode.INPUT_DATA_UNREADABLE, inputContentType,
                  "Media type not supported");
              } else {
                final InputStreamResource resource = new InputStreamResource(
                  "in", inputDataStream);
                final Reader<Map<String, Object>> mapReader = factory.createMapReader(resource);
                if (mapReader == null) {
                  valid = addJobValidationError(batchJobId,
                    ErrorCode.INPUT_DATA_UNREADABLE, inputContentType,
                    "Media type not supported");
                } else {
                  final Reader<DataObject> inputDataReader = new MapReaderDataObjectReader(
                    requestMetaData, mapReader);

                  final int commitInterval = 100;
                  final PlatformTransactionManager transactionManager = dataStore.getTransactionManager();
                  final TransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
                    TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                  TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
                  try {
                    List<Map<String, Object>> group = new ArrayList<Map<String, Object>>();
                    for (final Map<String, Object> inputDataRecord : inputDataReader) {
                      if (group.size() == maxGroupSize) {
                        numGroups++;
                        if (createBatchJobExecutionGroup(batchJobId, numGroups,
                          requestMetaData, group)) {
                          group = new ArrayList<Map<String, Object>>();
                          if (numGroups % commitInterval == 0) {
                            transactionManager.commit(status);
                            status = transactionManager.getTransaction(transactionDefinition);
                          }
                        } else {
                          transactionManager.rollback(status);
                          return;
                        }
                      }
                      numSubmittedRequests++;
                      final DataObject requestParemeters = processParameters(
                        batchJob, businessApplication, numSubmittedRequests,
                        jobParameters, inputDataRecord);
                      if (requestParemeters == null) {
                        numFailedRequests++;
                      } else {
                        requestParemeters.setValue("requestSequenceNumber",
                          numSubmittedRequests);
                        group.add(requestParemeters);
                      }

                    }
                    numGroups++;
                    if (createBatchJobExecutionGroup(batchJobId, numGroups,
                      requestMetaData, group)) {
                    } else {
                      transactionManager.rollback(status);
                      return;
                    }
                  } catch (final Throwable e) {
                    transactionManager.rollback(status);
                    ExceptionUtil.throwUncheckedException(e);
                  }
                  transactionManager.commit(status);
                }

                FileUtil.closeSilent(inputDataStream);

                final int maxRequests = businessApplication.getMaxRequestsPerJob();
                if (numSubmittedRequests == 0) {
                  valid = addJobValidationError(batchJobId,
                    ErrorCode.INPUT_DATA_UNREADABLE, "No records specified",
                    String.valueOf(numSubmittedRequests));
                } else if (numSubmittedRequests > maxRequests) {
                  valid = addJobValidationError(batchJobId,
                    ErrorCode.TOO_MANY_REQUESTS, null,
                    String.valueOf(numSubmittedRequests));
                }
              }
            } catch (final Throwable e) {
              LoggerFactory.getLogger(getClass()).error(
                "Error pre-processing job " + batchJobId, e);
              final StringWriter errorDebugMessage = new StringWriter();
              e.printStackTrace(new PrintWriter(errorDebugMessage));
              valid = addJobValidationError(batchJobId,
                ErrorCode.ERROR_PROCESSING_REQUEST,
                errorDebugMessage.toString(), e.getMessage());
            }
          }
        } finally {
          FileUtil.closeSilent(inputDataStream);
        }

        if (!valid || numSubmittedRequests == numFailedRequests) {
          valid = false;
          createResults(batchJobId);
          dataAccessObject.setBatchJobCompleted(batchJobId);
        } else if (dataAccessObject.setBatchJobResultsCreated(batchJobId,
          numSubmittedRequests, numFailedRequests, maxGroupSize, numGroups)) {
          schedule(businessApplicationName, batchJobId);
        }
        final Map<String, Object> preProcessStatistics = new HashMap<String, Object>();
        preProcessStatistics.put("preProcessedTime", stopWatch);
        preProcessStatistics.put("preProcessedJobsCount", 1);
        preProcessStatistics.put("preProcessedRequestsCount",
          numSubmittedRequests);

        InvokeMethodAfterCommit.invoke(this, "addStatistics",
          businessApplication, preProcessStatistics);

        if (!valid) {
          final Timestamp whenCreated = batchJob.getValue(BatchJob.WHEN_CREATED);

          final Map<String, Object> jobCompletedStatistics = new HashMap<String, Object>();

          jobCompletedStatistics.put("completedJobsCount", 1);
          jobCompletedStatistics.put("completedRequestsCount",
            numFailedRequests);
          jobCompletedStatistics.put("completedFailedRequestsCount",
            numFailedRequests);
          jobCompletedStatistics.put("completedTime",
            System.currentTimeMillis() - whenCreated.getTime());

          InvokeMethodAfterCommit.invoke(this, "addStatistics",
            businessApplication, jobCompletedStatistics);

          // numCompletedRequests is 0 + numFailedFailed
          if (log.isInfoEnabled()) {
            AppLogUtil.infoAfterCommit(log, "Job completed - End, batchJobId="
              + batchJobId);
          }
        }
      } finally {
        if (log.isInfoEnabled()) {
          AppLogUtil.infoAfterCommit(log, "Job pre-process - End, batchJobId="
            + batchJobId);
        }
      }
    } finally {
      synchronized (preprocesedJobIds) {
        preprocesedJobIds.remove(batchJobId);
      }
    }
  }

  private DataObject processParameters(final DataObject batchJob,
    final BusinessApplication businessApplication,
    final int requestSequenceNumber, final Map<String, String> jobParameters,
    final Map<String, Object> inputDataRecord) {
    final long batchJobId = DataObjectUtil.getInteger(batchJob,
      BatchJob.BATCH_JOB_ID);
    final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
    final DataObject requestParameters = new ArrayDataObject(requestMetaData);
    for (final Attribute attribute : requestMetaData.getAttributes()) {
      boolean jobParameter = false;
      final String parameterName = attribute.getName();
      Object parameterValue = null;
      if (businessApplication.isRequestParameter(parameterName)) {
        parameterValue = getNonEmptyValue(inputDataRecord, parameterName);
      }
      if (parameterValue == null
        && businessApplication.isJobParameter(parameterName)) {
        jobParameter = true;
        parameterValue = getNonEmptyValue(jobParameters, parameterName);
      }
      if (parameterValue == null) {
        if (attribute.isRequired()) {
          dataAccessObject.createBatchJobExecutionGroup(batchJobId,
            requestSequenceNumber,
            ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription(),
            ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription() + " "
              + parameterName, null);
          return null;
        }
      } else if (!businessApplication.isRequestAttributeValid(parameterName,
        parameterValue)) {
        dataAccessObject.createBatchJobExecutionGroup(batchJobId,
          requestSequenceNumber,
          ErrorCode.INVALID_PARAMETER_VALUE.getDescription(),
          ErrorCode.INVALID_PARAMETER_VALUE.getDescription() + " "
            + parameterName + "=" + parameterValue, null);
        return null;
      } else {
        try {
          final String sridString = jobParameters.get("srid");
          setStructuredInputDataValue(sridString, requestParameters, attribute,
            parameterValue, !jobParameter);
        } catch (final IllegalArgumentException e) {
          final StringWriter errorOut = new StringWriter();
          e.printStackTrace(new PrintWriter(errorOut));
          dataAccessObject.createBatchJobExecutionGroup(batchJobId,
            requestSequenceNumber,
            ErrorCode.INVALID_PARAMETER_VALUE.getDescription(),
            ErrorCode.INVALID_PARAMETER_VALUE.getDescription() + " "
              + parameterName + " " + e.getMessage(), errorOut.toString());
          return null;
        }
      }
    }
    return requestParameters;
  }

  protected void removeGroup(final BatchJobRequestExecutionGroup group) {
    final long batchJobId = group.getBatchJobId();
    synchronized (groupsByJobId) {
      final Set<BatchJobRequestExecutionGroup> groups = groupsByJobId.get(batchJobId);
      if (groups != null) {
        groups.remove(group);
        if (groups.isEmpty()) {
          groupsByJobId.remove(batchJobId);
        }
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resetCreatingRequestsBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final int numCleanedJobs = dataAccessObject.updateBatchJobStatus(
      BatchJob.SUBMITTED, BatchJob.CREATING_REQUESTS, businessApplicationName);
    if (numCleanedJobs > 0) {
      final AppLog log = new AppLog(businessApplicationName);
      log.info("Job status reset to submitted=" + numCleanedJobs);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resetCreatingResultsBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final int numCleanedJobs = dataAccessObject.updateBatchJobStatus(
      BatchJob.PROCESSED, BatchJob.CREATING_RESULTS, businessApplicationName);
    if (numCleanedJobs > 0) {
      final AppLog log = new AppLog(businessApplicationName);
      log.info("Job status reset to processed,count=" + numCleanedJobs);
    }
  }

  public void resetHungWorkers() {
    final Timestamp lastIdleTime = new Timestamp(System.currentTimeMillis()
      - (maxWorkerWaitTime * 2));
    ArrayList<String> workerIds;
    synchronized (workersById) {
      workerIds = new ArrayList<String>(workersById.keySet());
    }
    for (final String workerId : workerIds) {
      if (!connectedWorkerCounts.containsKey(workerId)) {
        Worker worker = getWorker(workerId);
        if (worker != null) {
          final Timestamp workerTimestamp = worker.getLastConnectTime();
          if (workerTimestamp == null || workerTimestamp.before(lastIdleTime)) {
            synchronized (workersById) {
              worker = workersById.remove(workerId);
            }
            final Map<String, BatchJobRequestExecutionGroup> groupsById = worker.getExecutingGroupsById();
            if (groupsById != null) {
              for (final BatchJobRequestExecutionGroup group : groupsById.values()) {
                final String groupId = group.getId();
                if (LoggerFactory.getLogger(BatchJobService.class)
                  .isDebugEnabled()) {
                  LoggerFactory.getLogger(BatchJobService.class).debug(
                    "Rescheduling group " + groupId + " from worker "
                      + workerId);
                }
                group.resetId();
                schedule(group);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Reset all BatchJobs and their BatchJobExecutionGroups for the specified business
   * applications which are currently in the processing state so that they can
   * be rescheduled.
   * 
   * @param moduleName The name of the module.
   * @param businessApplicationName The name of the business applications to
   *          reset the jobs for.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resetProcessingBatchJobs(final String moduleName,
    final String businessApplicationName) {

    groupsToSchedule.remove(businessApplicationName);

    final AppLog log = new AppLog(businessApplicationName);

    final int numCleanedRequests = dataAccessObject.updateResetGroupsForRestart(businessApplicationName);
    if (numCleanedRequests > 0) {
      log.info("Groups cleaned for restart, count=" + numCleanedRequests);
    }
    final int numCleanedJobs = dataAccessObject.updateBatchJobProcessedStatus(businessApplicationName);
    if (numCleanedJobs > 0) {
      log.info("Jobs status for restart, count=" + numCleanedJobs);
    }
  }

  protected void saveAllStatistics() {
    List<String> businessApplicationNames;
    synchronized (statisticsByAppAndId) {
      businessApplicationNames = new ArrayList<String>(
        statisticsByAppAndId.keySet());
    }
    saveStatistics(businessApplicationNames);
  }

  protected void saveStatistics(
    final Collection<String> businessApplicationNames) {
    for (final String businessApplicationName : businessApplicationNames) {
      Map<String, BusinessApplicationStatistics> statisticsById;
      synchronized (statisticsByAppAndId) {
        statisticsById = statisticsByAppAndId.remove(businessApplicationName);
      }
      if (statisticsById != null) {
        for (final BusinessApplicationStatistics statistics : statisticsById.values()) {
          final String durationType = statistics.getDurationType();
          if (durationType.equals(BusinessApplicationStatistics.HOUR)) {
            dataAccessObject.saveStatistics(statistics);
          }
        }
      }
    }
  }

  protected void saveStatistics(
    final Map<String, BusinessApplicationStatistics> statsById,
    final Date currentTime) {
    for (final Iterator<BusinessApplicationStatistics> iterator = statsById.values()
      .iterator(); iterator.hasNext();) {
      final BusinessApplicationStatistics statistics = iterator.next();
      if (canDeleteStatistic(statistics, currentTime)) {
        iterator.remove();
        final Integer databaseId = statistics.getDatabaseId();
        if (databaseId != null) {
          dataAccessObject.deleteBusinessApplicationStatistics(databaseId);
        }
      } else {
        final String durationType = statistics.getDurationType();
        final String currentId = BusinessApplicationStatistics.getId(
          durationType, currentTime);
        if (!currentId.equals(statistics.getId())) {
          dataAccessObject.saveStatistics(statistics);
        } else {
          final Integer databaseId = statistics.getDatabaseId();
          if (databaseId != null) {
            dataAccessObject.deleteBusinessApplicationStatistics(databaseId);
          }
        }
      }
    }
  }

  protected void saveStatistics(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId) {
    final Date currentTime = new Date(System.currentTimeMillis());
    for (final Map<String, BusinessApplicationStatistics> statsById : statisticsByAppAndId.values()) {
      saveStatistics(statsById, currentTime);
    }
  }

  public void schedule(final BatchJobRequestExecutionGroup group) {
    if (running) {
      final long batchJobId = group.getBatchJobId();
      synchronized (groupsByJobId) {
        Set<BatchJobRequestExecutionGroup> groups = groupsByJobId.get(batchJobId);
        if (groups == null) {
          groups = new LinkedHashSet<BatchJobRequestExecutionGroup>();
          groupsByJobId.put(batchJobId, groups);
        }
        groups.add(group);
      }
      final BusinessApplication businessApplication = group.getBusinessApplication();
      final Module module = businessApplication.getModule();
      final String name = module.getName();
      groupsToSchedule.write(name, group);
    }
  }

  public void schedule(final String businessApplicationName,
    final long batchJobId) {
    if (scheduler != null) {
      final BatchJobScheduleInfo jobInfo = new BatchJobScheduleInfo(
        businessApplicationName, batchJobId, BatchJobScheduleInfo.SCHEDULE);
      SendToChannelAfterCommit.send(scheduler.getIn(), jobInfo);
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public boolean scheduleBatchJobExecutionGroups(final Long batchJobId) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final long start = System.currentTimeMillis();
    final Timestamp startTime = new Timestamp(start);
    final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
    dataAccessObject.setBatchJobStatus(batchJobId, BatchJob.REQUESTS_CREATED,
      BatchJob.PROCESSING);
    if (batchJob == null) {
      return false;
    } else {
      final String consumerKey = batchJob.getValue(BatchJob.USER_ID);

      final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
      final String businessApplicationVersion = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_VERSION);

      final BusinessApplication businessApplication = getBusinessApplication(
        businessApplicationName, businessApplicationVersion);
      if (businessApplication == null) {
        return false;
      } else {
        final AppLog log = businessApplication.getLog();
        if (log.isInfoEnabled()) {
          log.info("Job schedule - Start, batchJobId=" + batchJobId);
        }
        try {
          final Map<String, String> businessApplicationParameterMap = getBusinessApplicationParameters(batchJob);
          final String resultDataContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

          final Long batchJobExecutionGroupId = dataAccessObject.getNonExecutingRequestId(batchJobId);

          if (batchJobExecutionGroupId == null) {
            return false;
          } else {
            dataAccessObject.setBatchJobExecutionGroupsStarted(batchJobExecutionGroupId);

            dataAccessObject.updateBatchJobAddScheduledGroupCount(batchJobId,
              startTime);

            final BatchJobRequestExecutionGroup group = new BatchJobRequestExecutionGroup(
              consumerKey, batchJobId, businessApplication,
              businessApplicationParameterMap, resultDataContentType,
              new Timestamp(System.currentTimeMillis()),
              batchJobExecutionGroupId);

            InvokeMethodAfterCommit.invoke(this, "schedule", group);

            final Map<String, Object> statistics = new HashMap<String, Object>();
            statistics.put("executeScheduledTime", stopWatch);
            statistics.put("executeScheduledGroupsCount", 1);

            InvokeMethodAfterCommit.invoke(this, "addStatistics",
              businessApplication, statistics);
            return true;
          }
        } finally {
          if (log.isInfoEnabled()) {
            AppLogUtil.infoAfterCommit(log, "Job schedule - End, batchJobId="
              + batchJobId, stopWatch);
          }
        }
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void scheduleFromDatabase() {
    for (final Module module : businessApplicationRegistry.getModules()) {
      if (module.isStarted()) {
        final String moduleName = module.getName();
        for (final String businessApplicationName : module.getBusinessApplicationNames()) {
          scheduleFromDatabase(moduleName, businessApplicationName);
        }
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void scheduleFromDatabase(final String jobStatus) {
    for (final Module module : businessApplicationRegistry.getModules()) {
      if (module.isEnabled()) {
        final String moduleName = module.getName();
        for (final BusinessApplication businessApplication : module.getBusinessApplications()) {
          final String businessApplicationName = businessApplication.getName();
          scheduleFromDatabase(moduleName, businessApplicationName, jobStatus);
        }
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void scheduleFromDatabase(final String moduleName,
    final String businessApplicationName) {
    synchronized (businessApplicationName.intern()) {
      final List<Long> batchJobIds = dataAccessObject.getBatchJobIdsToSchedule(businessApplicationName);
      for (final Long batchJobId : batchJobIds) {
        new AppLog(businessApplicationName).info("Schedule from database, batchJobId="
          + batchJobId);
        schedule(businessApplicationName, batchJobId);
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void scheduleFromDatabase(final String moduleName,
    final String businessApplicationName, final String jobStatus) {
    final AppLog log = new AppLog(businessApplicationName);
    try {
      final List<Long> batchJobIds = dataAccessObject.getBatchJobIds(
        businessApplicationName, jobStatus);
      for (final Long batchJobId : batchJobIds) {
        if (jobStatus.equals(BatchJob.SUBMITTED)) {
          log.info("Pre-process from database, batchJobId=" + batchJobId);
          preProcess.schedule(batchJobId);
        } else if (jobStatus.equals(BatchJob.PROCESSED)) {
          log.info("Post-process from database, batchJobId=" + batchJobId);
          postProcess.schedule(batchJobId);
        }
      }
    } catch (final Throwable t) {
      log.error("Unable to schedule from database", t);
    }
  }

  public void scheduleSaveStatistics(final List<String> businessApplicationNames) {
    final Map<String, Object> values = new HashMap<String, Object>();
    values.put(StatisticsProcess.SAVE, Boolean.TRUE);
    values.put("businessApplicationNames", businessApplicationNames);
    sendStatistics(values);
  }

  /**
   * @param batchJobId The DataObject identifier.
   * @param batchJob The BatchJob.
   */
  public void sendNotification(final Long batchJobId, final DataObject batchJob) {
    String notificationUrl = batchJob.getValue(BatchJob.NOTIFICATION_URL);
    if (StringUtils.hasText(notificationUrl)) {
      try {
        String baseUrl = this.baseUrl;
        if (userClassBaseUrls != null && !userClassBaseUrls.isEmpty()) {
          final String consumerKey = batchJob.getValue(BatchJob.USER_ID);
          final DataObject userAccount = dataAccessObject.getUserAccount(consumerKey);
          final String userClass = userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS);
          if (userClassBaseUrls.containsKey(userClass)) {
            baseUrl = userClassBaseUrls.get(userClass);
          }
        }
        final String propertiesText = batchJob.getValue(BatchJob.PROPERTIES);
        String batchJobUrl = baseUrl;
        if (StringUtils.hasText(propertiesText)) {
          final String webServicePrefix = JsonMapIoFactory.toMap(propertiesText)
            .get("webServicePrefix");
          if (StringUtils.hasText(webServicePrefix)) {
            batchJobUrl += webServicePrefix;
          }
        }
        batchJobUrl += "/ws/users/" + batchJob.getValue(BatchJob.USER_ID)
          + "/jobs/" + batchJobId;
        final Map<String, Object> jobMap = toMap(batchJob, batchJobUrl, 0);
        final String subject = "CPF Job " + batchJobId + " status";

        URI notificationUri = new URI(notificationUrl);
        final String scheme = notificationUri.getScheme();
        if (scheme != null) {
          if (scheme.equals("mailto")) {
            final StringWriter bodyOut = new StringWriter();

            final MapWriter writer = new XhtmlMapWriter(bodyOut);
            writer.setProperty("title", subject);
            writer.write(jobMap);
            writer.close();

            final MimeMessage message = mailSender.createMimeMessage();

            final MimeMessageHelper messageHelper = new MimeMessageHelper(
              message);
            messageHelper.setTo(notificationUri.getSchemeSpecificPart());
            messageHelper.setSubject(subject);
            messageHelper.setFrom(fromEmail);
            messageHelper.setText(bodyOut.toString(), true);
            mailSender.send(message);
          } else if (scheme.equals("http") || scheme.equals("https")) {
            notificationUrl = UrlUtil.getUrl(notificationUrl,
              Collections.singletonMap("batchJobUrl", batchJobUrl));
            notificationUri = new URI(notificationUrl);
            final StringWriter bodyOut = new StringWriter();
            final String contentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
            final IoFactoryRegistry ioFactory = IoFactoryRegistry.getInstance();
            final MapWriterFactory writerFactory = ioFactory.getFactoryByMediaType(
              MapWriterFactory.class, contentType);
            if (writerFactory == null) {
              LoggerFactory.getLogger(BatchJobService.class).error(
                "Media type not supported for DataObject #" + batchJobId
                  + " to " + contentType);
            } else {
              final MapWriter writer = writerFactory.getWriter(bodyOut);
              writer.setProperty("title", subject);
              writer.write(jobMap);
              writer.close();
              final HttpEntity body = new StringEntity(bodyOut.toString());
              final HttpClient client = new DefaultHttpClient();
              final HttpPost request = new HttpPost(notificationUri);
              request.setHeader("Content-type", contentType);
              request.setEntity(body);
              final HttpResponse response = client.execute(request);
              final HttpEntity entity = response.getEntity();
              try {
                final StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() >= 400) {
                  LoggerFactory.getLogger(BatchJobService.class).error(
                    "Unable to send notification for DataObject #" + batchJobId
                      + " to " + notificationUrl + " response=" + statusLine);
                }
              } finally {
                final InputStream content = entity.getContent();
                FileUtil.closeSilent(content);
              }
            }
          }
        }
      } catch (final Throwable e) {
        LoggerFactory.getLogger(BatchJobService.class).error(
          "Unable to send notification for DataObject #" + batchJobId + " to "
            + notificationUrl, e);
      }
    }
  }

  private void sendStatistics(final Map<String, ?> values) {
    if (statisticsProcess != null) {
      final Channel<Map<String, ? extends Object>> in = statisticsProcess.getIn();
      if (!in.isClosed()) {
        in.write(values);
      }
    }
  }

  public void setAuthorizationService(
    final AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void setBatchJobExecutingCounts(final String businessApplicationName,
    final Long batchJobId, final Set<BatchJobRequestExecutionGroup> groups) {
    synchronized (businessApplicationName.intern()) {
      int numCompletedRequests = 0;
      int numFailedRequests = 0;

      int numGroups = 0;
      for (final BatchJobRequestExecutionGroup group : groups) {
        numCompletedRequests += group.getNumCompletedRequests();
        numFailedRequests += group.getNumFailedRequests();
        numGroups++;
      }
      dataAccessObject.updateBatchJobGroupCompleted(batchJobId,
        numCompletedRequests, numFailedRequests, numGroups);
    }

    if (dataAccessObject.isBatchJobCompleted(batchJobId)) {
      dataAccessObject.setBatchJobStatus(batchJobId, BatchJob.PROCESSING,
        BatchJob.PROCESSED);
      postProcess(batchJobId);
    } else if (dataAccessObject.hasBatchJobUnexecutedJobs(batchJobId)) {
      schedule(businessApplicationName, batchJobId);
    }
  }

  @SuppressWarnings("unchecked")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void setBatchJobExecutionGroupResults(final String workerId,
    final String groupId, final Map<String, Object> results) {
    final com.revolsys.io.Writer<DataObject> writer = dataStore.getWriter();
    try {
      final Worker worker = getWorker(workerId);
      if (worker != null) {
        final BatchJobRequestExecutionGroup group = worker.removeExecutingGroup(groupId);
        if (group != null && !group.isCancelled()) {
          synchronized (group) {
            final long batchJobId = group.getBatchJobId();
            final BusinessApplication businessApplication = group.getBusinessApplication();
            final String businessApplicationName = group.getBusinessApplicationName();
            final String moduleName = group.getModuleName();

            final List<Map<String, Object>> groupResults = (List<Map<String, Object>>)results.get("results");
            final long groupExecutedTime = CollectionUtil.getLong(results,
              "groupExecutedTime");
            final long applicationExecutedTime = CollectionUtil.getLong(
              results, "applicationExecutedTime");
            final int successCount = CollectionUtil.getInteger(results,
              "successCount");
            final int errorCount = CollectionUtil.getInteger(results,
              "errorCount");

            final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
            if (batchJob != null) {
              if (groupResults != null && !groupResults.isEmpty()) {
                final Long batchJobExecutionGroupId = group.getBatchJobExecutionGroupId();
                dataAccessObject.updateBatchJobExecutionGroupFromResponse(
                  workerId, group, batchJobExecutionGroupId, groupResults,
                  successCount, errorCount);
              }
            }
            updateGroupStatistics(group, businessApplication, moduleName,
              applicationExecutedTime, groupExecutedTime, successCount,
              errorCount);
            scheduler.groupFinished(businessApplicationName, batchJobId, group);
            removeGroup(group);
          }
        }
      }
    } finally {
      writer.close();
    }
  }

  public void setBusinessApplicationRegistry(
    final BusinessApplicationRegistry businessApplicationRegistry) {
    this.businessApplicationRegistry = businessApplicationRegistry;
  }

  @Resource(name = "cpfDataAccessObject")
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

  /**
   * Set the email address messages are sent from.
   * 
   * @param fromEmail The email address messages are sent from.
   */
  public void setFromEmail(final String fromEmail) {
    this.fromEmail = fromEmail;
  }

  /**
   * Set the class used to send email.
   * 
   * @param mailSender The class used to send email.
   */
  public void setMailSender(final JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void setMaxWorkerWaitTime(final long maxWorkerWaitTime) {
    this.maxWorkerWaitTime = maxWorkerWaitTime * 1000;
  }

  public void setPostProcess(final BatchJobPostProcess postProcess) {
    this.postProcess = postProcess;
    postProcess.getIn().writeConnect();
  }

  public void setPreProcess(final BatchJobPreProcess preProcess) {
    this.preProcess = preProcess;
    preProcess.getIn().writeConnect();
  }

  public void setScheduler(final BatchJobScheduler scheduler) {
    this.scheduler = scheduler;
  }

  private void setStatisticsByAppAndId(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId) {
    this.statisticsByAppAndId = statisticsByAppAndId;
  }

  public void setStatisticsProcess(final StatisticsProcess statisticsProcess) {
    this.statisticsProcess = statisticsProcess;
    statisticsProcess.getIn().writeConnect();
  }

  public void setStructuredInputDataValue(final String sridString,
    final Map<String, Object> requestParemeters, final Attribute attribute,
    Object parameterValue, final boolean setValue) {
    final DataType dataType = attribute.getType();
    final Class<?> dataClass = dataType.getJavaClass();
    if (Geometry.class.isAssignableFrom(dataClass)) {
      if (parameterValue != null) {
        final GeometryFactory geometryFactory = attribute.getProperty(AttributeProperties.GEOMETRY_FACTORY);
        Geometry geometry;
        if (parameterValue instanceof Geometry) {
          geometry = (Geometry)parameterValue;
        } else {
          String wkt = parameterValue.toString();
          if (StringUtils.hasText(wkt)) {
            wkt = wkt.trim();
          }
          if (wkt.startsWith("http")) {
            wkt = UrlUtil.getContent(wkt + "/feature.wkt?srid=3005");
            if (!wkt.startsWith("SRID")) {
              wkt = "SRID=3005;" + wkt;
            }
          }
          try {
            if (StringUtils.hasText(sridString)) {
              final int srid = Integer.parseInt(sridString);
              final GeometryFactory sourceGeometryFactory = GeometryFactory.getFactory(srid);
              geometry = sourceGeometryFactory.createGeometry(wkt, false);
            } else {
              geometry = geometryFactory.createGeometry(wkt, false);
            }
          } catch (final Throwable t) {
            throw new IllegalArgumentException("invalid WKT geometry", t);
          }
        }
        if (geometryFactory != GeometryFactory.getFactory()) {
          geometry = geometryFactory.createGeometry(geometry);
        }
        final Boolean validateGeometry = attribute.getProperty(AttributeProperties.VALIDATE_GEOMETRY);
        if (geometry.getSRID() == 0) {
          throw new IllegalArgumentException(
            "does not have a coordinate system (SRID) specified");
        }
        if (validateGeometry == true) {
          final IsValidOp validOp = new IsValidOp(geometry);
          if (!validOp.isValid()) {
            throw new IllegalArgumentException(validOp.getValidationError()
              .getMessage());
          }
        }
        parameterValue = geometry;
      }
    } else if (Number.class.isAssignableFrom(dataClass)) {
      if (!(parameterValue instanceof Number)) {
        parameterValue = new BigDecimal(parameterValue.toString().trim());
      }
    }
    if (setValue) {
      requestParemeters.put(attribute.getName(), parameterValue);
    }
  }

  public void setUserClassBaseUrls(final Map<String, String> userClassBaseUrls) {
    this.userClassBaseUrls = userClassBaseUrls;
  }

  public void setWorkerConnected(final String workerId, final boolean connected) {
    synchronized (connectedWorkerCounts) {
      Integer count = connectedWorkerCounts.get(workerId);
      if (count == null) {
        count = 0;
      }
      if (connected) {
        connectedWorkerCounts.put(workerId, count + 1);
      } else {
        count = count - 1;
        if (count <= 0) {
          connectedWorkerCounts.remove(workerId);
        } else {
          connectedWorkerCounts.put(workerId, count);
        }
      }

      synchronized (workersById) {
        Worker worker = workersById.get(workerId);
        if (worker == null) {
          worker = new Worker(businessApplicationRegistry, workerId);
          if (connected) {
            workersById.put(workerId, worker);
          }
        }
        final long time = System.currentTimeMillis();
        final Timestamp lastConnectTime = new Timestamp(time);
        worker.setLastConnectTime(lastConnectTime);
      }
    }
  }

  protected void updateGroupStatistics(
    final BatchJobRequestExecutionGroup group,
    final BusinessApplication businessApplication, final String moduleName,
    final long applicationExecutedTime, final long groupExecutedTime,
    final int successCount, final int errorCount) {
    final Map<String, Object> appExecutedStatistics = new HashMap<String, Object>();
    appExecutedStatistics.put("applicationExecutedGroupsCount", 1);
    appExecutedStatistics.put("applicationExecutedRequestsCount", successCount
      + errorCount);
    appExecutedStatistics.put("applicationExecutedFailedRequestsCount",
      errorCount);
    appExecutedStatistics.put("applicationExecutedTime",
      applicationExecutedTime);
    appExecutedStatistics.put("executedTime", groupExecutedTime);

    addStatistics(businessApplication, appExecutedStatistics);

    final long executionStartTime = group.getExecutionStartTime();
    final long durationInMillis = System.currentTimeMillis()
      - executionStartTime;
    final AppLog log = businessApplication.getLog();
    if (log.isInfoEnabled()) {
      log.info("Group Execution- End, batchJobId=" + group.getBatchJobId()
        + ", groupId=" + group.getId() + ", time=" + durationInMillis);
    }
    final Map<String, Object> executedStatistics = new HashMap<String, Object>();
    executedStatistics.put("executedGroupsCount", 1);
    executedStatistics.put("executedRequestsCount", successCount + errorCount);
    executedStatistics.put("executedTime", durationInMillis);

    InvokeMethodAfterCommit.invoke(this, "addStatistics", businessApplication,
      executedStatistics);

    group.setNumCompletedRequests(successCount);
    group.setNumFailedRequests(errorCount);
  }

  public void updateWorkerExecutingGroups(final Worker worker,
    final List<String> executingGroupIds) {
    final long minStartTime = System.currentTimeMillis() - 60 * 1000;
    final List<BatchJobRequestExecutionGroup> executingGroups = worker.getExecutingGroups();
    for (final BatchJobRequestExecutionGroup executionGroup : executingGroups) {
      if (executionGroup.getExecutionStartTime() < minStartTime) {
        final String groupId = executionGroup.getId();
        if (!executingGroupIds.contains(groupId)) {
          cancelGroup(worker, groupId);
        }
      }
    }
  }
}
