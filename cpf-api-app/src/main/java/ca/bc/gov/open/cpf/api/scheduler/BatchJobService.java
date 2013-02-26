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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobRequest;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.security.service.AuthorizationService;
import ca.bc.gov.open.cpf.api.security.service.AuthorizationServiceUserSecurityServiceFactory;
import ca.bc.gov.open.cpf.client.api.ErrorCode;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.impl.log.ModuleLog;
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
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.UrlUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.valid.IsValidOp;

public class BatchJobService implements ModuleEventListener {
  /** The logger. */
  private static final Logger LOG = LoggerFactory.getLogger(BatchJobService.class);

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
      final String jobParameters = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_PARAMS);
      final Map<String, String> parameters = JsonMapIoFactory.toMap(jobParameters);
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

  private final long maxWorkerPingTime = 60 * 1000;

  private final long maxWorkerWaitTime = maxWorkerPingTime;

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

  /**
   * Generate an error result for the job, update the job counts and status, and
   * back out any add job requests that have already been added.
   * 
   * @param validationErrorCode The failure error code.
   */
  private void addJobValidationError(final DataObject batchJob,
    final ErrorCode validationErrorCode,
    final String validationErrorDebugMessage,
    final String validationErrorMessage) {
    final long batchJobId = DataObjectUtil.getInteger(batchJob,
      BatchJob.BATCH_JOB_ID);

    dataAccessObject.deleteBatchJobRequests(batchJobId);

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

    batchJob.setValue(BatchJob.NUM_COMPLETED_REQUESTS, 0);
    batchJob.setValue(BatchJob.NUM_FAILED_REQUESTS,
      DataObjectUtil.getInteger(batchJob, BatchJob.NUM_SUBMITTED_REQUESTS));
    if (!BatchJob.MARKED_FOR_DELETION.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
      batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.RESULTS_CREATED);
    }
    dataAccessObject.write(batchJob);
    LOG.debug(validationErrorDebugMessage);
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

  public void cancelGroup(final Worker worker, final String groupId) {
    if (groupId != null) {
      final BatchJobRequestExecutionGroup group = worker.removeExecutingGroup(groupId);
      if (group != null) {
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

  public void createBatchJobResult(final long batchJobId,
    final String resultDataType, final String contentType, final Object data) {
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

  public void createBatchJobResultOpaque(final long batchJobId,
    final long batchJobRequestId, final String contentType, final Object data) {
    final DataObject request = dataAccessObject.getBatchJobRequestLocked(batchJobRequestId);
    if (request.getValue(BatchJobResult.BATCH_JOB_RESULT_ID) == null) {
      final Number batchJobResultId = dataAccessObject.createId(BatchJobResult.BATCH_JOB_RESULT);
      final DataObject result = dataAccessObject.create(BatchJobResult.BATCH_JOB_RESULT);
      result.setValue(BatchJobResult.BATCH_JOB_RESULT_ID, batchJobResultId);
      result.setValue(BatchJobResult.BATCH_JOB_ID, batchJobId);
      result.setValue(BatchJobResult.BATCH_JOB_REQUEST_ID, batchJobRequestId);
      result.setValue(BatchJobResult.BATCH_JOB_RESULT_TYPE,
        BatchJobResult.OPAQUE_RESULT_DATA);
      result.setValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE, contentType);
      result.setValue(BatchJobResult.RESULT_DATA, data);
      dataAccessObject.write(result);

      request.setValue(BatchJobRequest.BATCH_JOB_RESULT_ID, batchJobResultId);
      dataAccessObject.write(request);
    }

  }

  /**
   * Create the error BatchJobResult for a BatchJob. This will only be created
   * if there were any errors.
   * 
   * @param batchJobId The DataObject identifier.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void createErrorResults(final long batchJobId) {
    dataAccessObject.getBatchJobLocked(batchJobId);
    final Reader<DataObject> errorResults = dataAccessObject.getErrorResultDataRequests(batchJobId);
    try {
      final String errorFormat = "text/csv";
      final File file = FileUtil.createTempFile("result", ".csv");
      try {
        boolean written = false;
        final Writer writer = new FileWriter(file);
        try {
          final MapWriter mapWriter = new CsvMapWriter(writer);
          try {
            for (final DataObject request : errorResults) {
              final String errorCode = request.getValue(BatchJobRequest.ERROR_CODE);
              final Map<String, String> errorMap = new LinkedHashMap<String, String>();
              errorMap.put("sequenceNumber",
                request.getValue(BatchJobRequest.REQUEST_SEQUENCE_NUMBER)
                  .toString());
              errorMap.put("errorCode", errorCode);
              final String errorMessage = request.getValue(BatchJobRequest.ERROR_MESSAGE);
              errorMap.put("errorMessage", errorMessage);
              mapWriter.write(errorMap);
              written = true;
            }
          } finally {
            mapWriter.close();
          }
        } finally {
          FileUtil.closeSilent(writer);
        }
        if (written) {
          createBatchJobResult(batchJobId, BatchJobResult.ERROR_RESULT_DATA,
            errorFormat, file);
        }
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to save result data", e);
      } finally {
        file.delete();
      }
    } finally {
      errorResults.close();
    }
  }

  /**
   * Create the structured BatchJobResult for a BatchJob. This will only be
   * created if there were any structured results.
   * 
   * @param batchJobId The DataObject identifier.
   */
  @SuppressWarnings("unchecked")
  @Transactional(propagation = Propagation.REQUIRED)
  public void createStructuredResults(final long batchJobId) {
    final DataObject batchJob = dataAccessObject.getBatchJobLocked(batchJobId);
    final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
    final BusinessApplication application = getBusinessApplication(businessApplicationName);
    if (!application.isPerRequestResultData()) {
      final Reader<DataObject> structuredResults = dataAccessObject.getStructuredResultDataRequests(batchJobId);
      try {
        final String resultFormat = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

        final IoFactoryRegistry ioFactory = IoFactoryRegistry.getInstance();
        final DataObjectWriterFactory writerFactory = ioFactory.getFactoryByMediaType(
          DataObjectWriterFactory.class, resultFormat);
        final String fileExtension = writerFactory.getFileExtension(resultFormat);
        final File file = FileUtil.createTempFile("result", "." + fileExtension);
        try {
          final FileSystemResource resource = new FileSystemResource(file);
          final DataObjectMetaData resultMetaData = application.getResultMetaData();
          final com.revolsys.io.Writer<DataObject> dataObjectWriter = writerFactory.createDataObjectWriter(
            resultMetaData, resource);
          dataObjectWriter.setProperty(Kml22Constants.STYLE_URL_PROPERTY,
            baseUrl + "/kml/defaultStyle.kml#default");
          dataObjectWriter.setProperty(IoConstants.TITLE_PROPERTY, "Job "
            + batchJobId + " Result");
          dataObjectWriter.setProperty("htmlCssStyleUrl", baseUrl
            + "/css/default.css");
          dataObjectWriter.setProperties(application.getProperties());
          boolean written = false;
          try {
            final Map<String, Object> defaultProperties = new HashMap<String, Object>(
              dataObjectWriter.getProperties());
            for (final DataObject batchJobRequest : structuredResults) {
              final String structuredResultData = batchJobRequest.getString(BatchJobRequest.STRUCTURED_RESULT_DATA);
              final List<Map<String, Object>> results = JsonMapIoFactory.toMapList(structuredResultData);
              final Integer sequenceNumber = DataObjectUtil.getInteger(
                batchJobRequest, BatchJobRequest.REQUEST_SEQUENCE_NUMBER);
              int i = 1;
              for (final Map<String, Object> structuredResultMap : results) {
                final DataObject structuredResult = DataObjectUtil.getObject(
                  resultMetaData, structuredResultMap);

                final Map<String, Object> properties = (Map<String, Object>)structuredResultMap.get("customizationProperties");
                if (properties != null && !properties.isEmpty()) {
                  dataObjectWriter.setProperties(properties);
                }

                structuredResult.put("sequenceNumber", sequenceNumber);
                structuredResult.put("resultNumber", i);
                dataObjectWriter.write(structuredResult);
                if (properties != null && !properties.isEmpty()) {

                  dataObjectWriter.clearProperties();
                  dataObjectWriter.setProperties(defaultProperties);
                }
                i++;
              }
              written = true;
            }
          } finally {
            dataObjectWriter.close();
          }

          if (written) {
            createBatchJobResult(batchJobId,
              BatchJobResult.STRUCTURED_RESULT_DATA, resultFormat, file);
          }
        } catch (final Throwable e) {
          throw new RuntimeException("Unable to save result data", e);
        } finally {
          InvokeMethodAfterCommit.invoke(file, "delete");
        }
      } finally {
        structuredResults.close();
      }
    }
  }

  public void deleteLogFiles() {
    final File logDirectory = businessApplicationRegistry.getLogDirectory();
    final Calendar cal = new GregorianCalendar();
    cal.set(Calendar.HOUR, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    cal.add(Calendar.DAY_OF_MONTH, -7);
    final Date date = cal.getTime();
    LOG.info("Start: Deleting log files < " + date);
    FileUtil.deleteFilesOlderThan(logDirectory, date);
    LOG.info("End: Deleting log files < " + date);
  }

  @PreDestroy
  public void destory() {
    running = false;
    if (preProcess != null) {
      preProcess.getIn().writeDisconnect();
      preProcess = null;
    }
    if (postProcess != null) {
      postProcess.getIn().writeDisconnect();
      postProcess = null;
    }
    if (scheduler != null) {
      scheduler.getIn().writeDisconnect();
      scheduler = null;
    }
    if (groupsToSchedule != null) {
      groupsToSchedule.close();
      groupsToSchedule = null;
    }
    if (statisticsProcess != null) {
      statisticsProcess.getIn().writeDisconnect();
      statisticsProcess = null;
    }
    dataStore = null;
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
        LOG.error("Unable to open stream: " + inputDataUrlString, e);
      }
    } else {
      try {
        final Blob inputData = batchJob.getValue(BatchJob.STRUCTURED_INPUT_DATA);
        if (inputData != null) {
          return inputData.getBinaryStream();
        }
      } catch (final SQLException e) {
        LOG.error("Unable to open stream: " + inputDataUrlString, e);
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
    final String workerId, List<String> moduleNames) {
    final Map<String, Object> response = new HashMap<String, Object>();
    if (running) {
      moduleNames = getWorkerModuleNames(workerId, moduleNames);

      BatchJobRequestExecutionGroup group = null;
      try {
        group = groupsToSchedule.read(maxWorkerWaitTime, moduleNames);
      } catch (final ClosedException e) {
      }

      if (running && group != null) {
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
              ModuleLog.info(moduleName, "Execution", "Start",
                new LinkedHashMap<String, Object>(response));
            }
            response.put("consumerKey", group.getconsumerKey());
            response.put("numRequests", group.getNumBatchJobRequests());
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
      final Set<String> excludedModules = worker.getExcludedModules();
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
        for (Worker worker : getWorkers()) {
          String moduleNameAndTime = moduleName + ":" + module.getStartedTime();
          List<BatchJobRequestExecutionGroup> cancelledGroups = worker.cancelExecutingGroups(moduleNameAndTime);
          for (BatchJobRequestExecutionGroup cancelledGroup : cancelledGroups) {
            cancelledGroup.resetId();
            schedule(cancelledGroup);
          }
        }
      }
      final List<String> businessApplicationNames = module.getBusinessApplicationNames();
      for (final String businessApplicationName : businessApplicationNames) {
        if (action.equals(ModuleEvent.START)) {
          resetProcessingBatchJobs(moduleName, businessApplicationName);
          resetCreatingRequestsBatchJobs(moduleName, businessApplicationName);
          resetCreatingResultsBatchJobs(moduleName, businessApplicationName);
          scheduleFromDatabase(moduleName, businessApplicationName);
          if (preProcess != null) {
            preProcess.scheduleFromDatabase(moduleName, businessApplicationName);
          }
          if (postProcess != null) {
            postProcess.scheduleFromDatabase(moduleName,
              businessApplicationName);
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
      String moduleName = "Unknown";
      if (businessApplication != null) {
        final Module module = businessApplication.getModule();
        moduleName = module.getName();

      }
      final Map<String, Object> logData = new LinkedHashMap<String, Object>();
      logData.put("businessApplicationName", businessApplicationName);
      logData.put("batchJobId", batchJobId);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.info(moduleName, "Job post-process", "Start", logData);
      }

      final Map<String, Object> postProcessScheduledStatistics = new HashMap<String, Object>();
      postProcessScheduledStatistics.put("postProcessScheduledJobsTime", time
        - lastChangedTime);
      postProcessScheduledStatistics.put("postProcessScheduledJobsCount", 1);
      addStatistics(businessApplication, postProcessScheduledStatistics);

      createErrorResults(batchJobId);
      createStructuredResults(batchJobId);

      final long resultsGeneratedTime = setBatchJobCompleted(batchJobId);
      if (resultsGeneratedTime != -1) {
        sendNotification(batchJobId, batchJob);
      }
      final long numCompletedRequests = DataObjectUtil.getInteger(batchJob,
        BatchJob.NUM_COMPLETED_REQUESTS);
      final long numFailedRequests = DataObjectUtil.getInteger(batchJob,
        BatchJob.NUM_FAILED_REQUESTS);
      logData.put("numCompletedRequests", numCompletedRequests);
      logData.put("numFailedRequests", numFailedRequests);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.infoAfterCommit(moduleName, "Job post-process", "End",
          logData);
        ModuleLog.infoAfterCommit(moduleName, "Job completed", "End", logData);
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

  /**
   * Process a single newly submitted batch job.
   * 
   * @param lastChangedTime
   * @param time
   * @return
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void preProcessBatchJob(final Long batchJobId, final long time,
    final long lastChangedTime) {
    final com.revolsys.io.Writer<DataObject> writer = dataStore.getWriter();
    try {
      final StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      final DataObject batchJob = dataAccessObject.getBatchJobLocked(batchJobId);
      final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
      final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
      if (businessApplication == null) {
        throw new IllegalArgumentException("Cannot find business application: "
          + businessApplicationName);
      }
      final Module module = businessApplication.getModule();
      final String moduleName = module.getName();
      final Map<String, Object> logData = new LinkedHashMap<String, Object>();
      logData.put("businessApplicationName", businessApplicationName);
      logData.put("batchJobId", batchJobId);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.info(moduleName, "Job pre-process", "Start", logData);
      }
      final Map<String, Object> preProcessScheduledStatistics = new HashMap<String, Object>();
      preProcessScheduledStatistics.put("preProcessScheduledJobsCount", 1);
      preProcessScheduledStatistics.put("preProcessScheduledJobsTime", time
        - lastChangedTime);

      InvokeMethodAfterCommit.invoke(this, "addStatistics",
        businessApplication, preProcessScheduledStatistics);

      addStatistics(businessApplication, preProcessScheduledStatistics);

      final InputStream inputDataStream = getJobInputDataStream(batchJob);
      final long numFailedRequests = batchJob.getLong(BatchJob.NUM_FAILED_REQUESTS);
      try {
        final String appParams = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_PARAMS);
        final Map<String, String> jobParameters = JsonMapIoFactory.toMap(appParams);

        final String inputContentType = batchJob.getValue(BatchJob.INPUT_DATA_CONTENT_TYPE);
        final String resultContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
        if (!businessApplication.getInputDataContentTypes().containsKey(
          inputContentType)) {
          addJobValidationError(batchJob, ErrorCode.BAD_INPUT_DATA_TYPE, "", "");
        } else if (!businessApplication.getResultDataContentTypes()
          .containsKey(resultContentType)) {
          addJobValidationError(batchJob, ErrorCode.BAD_RESULT_DATA_TYPE, "",
            "");
        } else if (inputDataStream == null) {
          addJobValidationError(batchJob, ErrorCode.INPUT_DATA_UNREADABLE, "",
            "");
        } else {
          final DataObjectMetaData requestMetaData = businessApplication.getRequestMetaData();
          try {
            int requestSequenceNumber = 0;
            final MapReaderFactory factory = IoFactoryRegistry.getInstance()
              .getFactoryByMediaType(MapReaderFactory.class, inputContentType);
            if (factory == null) {
              addJobValidationError(batchJob, ErrorCode.INPUT_DATA_UNREADABLE,
                inputContentType, "Media type not supported");
            } else {
              final InputStreamResource resource = new InputStreamResource(
                "in", inputDataStream);
              final Reader<Map<String, Object>> mapReader = factory.createMapReader(resource);
              if (mapReader == null) {
                addJobValidationError(batchJob,
                  ErrorCode.INPUT_DATA_UNREADABLE, inputContentType,
                  "Media type not supported");
              } else {
                final Reader<DataObject> inputDataReader = new MapReaderDataObjectReader(
                  requestMetaData, mapReader);

                for (final Map<String, Object> inputDataRecord : inputDataReader) {
                  requestSequenceNumber++;
                  final DataObject requestParemeters = processParameters(
                    batchJob, businessApplication, requestSequenceNumber,
                    jobParameters, inputDataRecord);
                  if (requestParemeters == null) {
                    batchJob.setValue(BatchJob.NUM_FAILED_REQUESTS,
                      numFailedRequests + 1);
                  } else {
                    final String structuredInputDataString = JsonDataObjectIoFactory.toString(requestParemeters);
                    dataAccessObject.createBatchJobRequest(batchJobId,
                      requestSequenceNumber, structuredInputDataString);
                  }
                }
              }

              FileUtil.closeSilent(inputDataStream);

              batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS,
                requestSequenceNumber);
              final int maxRequests = businessApplication.getMaxRequestsPerJob();
              if (requestSequenceNumber == 0) {
                addJobValidationError(batchJob,
                  ErrorCode.INPUT_DATA_UNREADABLE, "No records specified",
                  String.valueOf(requestSequenceNumber));
              } else if (requestSequenceNumber > maxRequests) {
                addJobValidationError(batchJob, ErrorCode.TOO_MANY_REQUESTS,
                  null, String.valueOf(requestSequenceNumber));
              }
            }
          } catch (final Throwable e) {
            final StringWriter errorDebugMessage = new StringWriter();
            e.printStackTrace(new PrintWriter(errorDebugMessage));
            addJobValidationError(batchJob, ErrorCode.ERROR_PROCESSING_REQUEST,
              errorDebugMessage.toString(), e.getMessage());
          }
        }
      } finally {
        FileUtil.closeSilent(inputDataStream);
      }
      final long numSubmittedRequests = batchJob.getLong(BatchJob.NUM_SUBMITTED_REQUESTS);
      if (numSubmittedRequests == numFailedRequests) {
        createErrorResults(batchJobId);
        if (!BatchJob.MARKED_FOR_DELETION.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
          batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.RESULTS_CREATED);

          final long completeTime = System.currentTimeMillis();
          final Timestamp now = new Timestamp(completeTime);
          batchJob.setValue(BatchJob.COMPLETED_TIMESTAMP, now);
          batchJob.setValue(BatchJob.LAST_SCHEDULED_TIMESTAMP, now);
        }
      }
      // Clear the input data as it is no longer required
      batchJob.setValue(BatchJob.STRUCTURED_INPUT_DATA, null);
      final String jobStatus = batchJob.getValue(BatchJob.JOB_STATUS);
      if (!BatchJob.RESULTS_CREATED.equals(jobStatus)
        && numSubmittedRequests > 0) {
        if (BatchJob.CREATING_REQUESTS.equals(jobStatus)) {
          final Timestamp now = new Timestamp(time);
          batchJob.setValue(BatchJob.LAST_SCHEDULED_TIMESTAMP, now);
          if (!BatchJob.MARKED_FOR_DELETION.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
            batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.REQUESTS_CREATED);
          }
          batchJob.setValue(BatchJob.WHEN_STATUS_CHANGED, now);
        }
        schedule(businessApplicationName, batchJobId);
      }
      logData.put("numSubmittedRequests", numSubmittedRequests);
      logData.put("numFailedRequests", numFailedRequests);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.infoAfterCommit(moduleName, "Job pre-process", "End",
          stopWatch, logData);
      }

      final Map<String, Object> preProcessStatistics = new HashMap<String, Object>();
      preProcessStatistics.put("preProcessedTime", stopWatch);
      preProcessStatistics.put("preProcessedJobsCount", 1);
      preProcessStatistics.put("preProcessedRequestsCount", stopWatch);

      InvokeMethodAfterCommit.invoke(this, "addStatistics",
        businessApplication, preProcessStatistics);

      if (BatchJob.RESULTS_CREATED.equals(jobStatus)
        || numSubmittedRequests == 0) {
        final Timestamp whenCreated = batchJob.getValue(BatchJob.WHEN_CREATED);
        final long numCompletedRequests = batchJob.getLong(BatchJob.NUM_COMPLETED_REQUESTS);

        final Map<String, Object> jobCompletedStatistics = new HashMap<String, Object>();

        jobCompletedStatistics.put("completedJobsCount", 1);
        jobCompletedStatistics.put("completedRequestsCount",
          numCompletedRequests + numFailedRequests);
        jobCompletedStatistics.put("completedFailedRequestsCount",
          numFailedRequests);
        jobCompletedStatistics.put("completedTime", System.currentTimeMillis()
          - whenCreated.getTime());

        InvokeMethodAfterCommit.invoke(this, "addStatistics",
          businessApplication, jobCompletedStatistics);

        logData.put("numCompletedRequests", numCompletedRequests);
        logData.put("numFailedRequests", numFailedRequests);
        if (businessApplication.isInfoLogEnabled()) {
          ModuleLog.infoAfterCommit(moduleName, "Job completed", "End", logData);
        }
      }
      dataAccessObject.write(batchJob);
    } finally {
      writer.close();
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
      final String parameterName = attribute.getName();
      Object parameterValue = null;
      if (businessApplication.isRequestParameter(parameterName)) {
        parameterValue = getNonEmptyValue(inputDataRecord, parameterName);
      }
      if (parameterValue == null
        && businessApplication.isJobParameter(parameterName)) {
        parameterValue = getNonEmptyValue(jobParameters, parameterName);
      }
      if (parameterValue == null) {
        if (attribute.isRequired()) {
          dataAccessObject.createBatchJobRequest(batchJobId,
            requestSequenceNumber,
            ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription(),
            ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription() + " "
              + parameterName, null);
          return null;
        }
      } else if (!businessApplication.isRequestAttributeValid(parameterName,
        parameterValue)) {
        dataAccessObject.createBatchJobRequest(batchJobId,
          requestSequenceNumber,
          ErrorCode.INVALID_PARAMETER_VALUE.getDescription(),
          ErrorCode.INVALID_PARAMETER_VALUE.getDescription() + " "
            + parameterName + "=" + parameterValue, null);
        return null;
      } else {
        try {
          final String sridString = jobParameters.get("srid");
          setStructuredInputDataValue(sridString, requestParameters, attribute,
            parameterValue);
        } catch (final IllegalArgumentException e) {
          final StringWriter errorOut = new StringWriter();
          e.printStackTrace(new PrintWriter(errorOut));
          dataAccessObject.createBatchJobRequest(batchJobId,
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resetCreatingRequestsBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final int numCleanedJobs = dataAccessObject.updateBatchJobStatus(
      BatchJob.SUBMITTED, BatchJob.CREATING_REQUESTS, businessApplicationName);
    if (numCleanedJobs > 0) {
      ModuleLog.info(moduleName, businessApplicationName,
        "Job status reset to submitted",
        Collections.singletonMap("count", numCleanedJobs));
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resetCreatingResultsBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final int numCleanedJobs = dataAccessObject.updateBatchJobStatus(
      BatchJob.PROCESSED, BatchJob.CREATING_RESULTS, businessApplicationName);
    if (numCleanedJobs > 0) {
      ModuleLog.info(moduleName, businessApplicationName,
        "Job status reset to processed",
        Collections.singletonMap("count", numCleanedJobs));
    }
  }

  public void resetHungWorkers() {
    final Timestamp lastIdleTime = new Timestamp(System.currentTimeMillis()
      - maxWorkerPingTime);
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
                if (LOG.isDebugEnabled()) {
                  LOG.debug("Rescheduling group " + groupId + " from worker "
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
   * Reset all BatchJobs and their BatchJobRequests for the specified business
   * applications which are currently in the processing state so that they can
   * be rescheduled.
   * 
   * @param moduleName
   * @param businessApplicationNames The names of the business applications to
   *          reset the jobs for.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resetProcessingBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final int numCleanedRequests = dataAccessObject.updateResetRequestsForRestart(businessApplicationName);
    if (numCleanedRequests > 0) {
      ModuleLog.info(moduleName, businessApplicationName,
        "Request cleaned for restart",
        Collections.singletonMap("count", numCleanedRequests));
    }
    final int numResetJobCounts = dataAccessObject.updateBatchJobExecutionCounts(businessApplicationName);
    if (numResetJobCounts > 0) {
      ModuleLog.info(moduleName, businessApplicationName,
        "Job execution counts for restart",
        Collections.singletonMap("count", numResetJobCounts));
    }
    final int numCleanedJobs = dataAccessObject.updateBatchJobProcessedStatus(businessApplicationName);
    if (numCleanedJobs > 0) {
      ModuleLog.info(moduleName, businessApplicationName,
        "Job status for restart",
        Collections.singletonMap("count", numCleanedJobs));
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
  public boolean scheduleBatchJobRequests(final Long batchJobId) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final long start = System.currentTimeMillis();
    final Timestamp startTime = new Timestamp(start);
    final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
    if (batchJob == null) {
      return false;
    } else {
      final String consumerKey = batchJob.getValue(BatchJob.USER_ID);

      final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
      final String businessApplicationVersion = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_VERSION);
      setBatchJobStatus(batchJobId, BatchJob.REQUESTS_CREATED,
        BatchJob.PROCESSING, start);

      final BusinessApplication businessApplication = getBusinessApplication(
        businessApplicationName, businessApplicationVersion);
      final Module module = businessApplication.getModule();
      final String moduleName = module.getName();
      final Map<String, Object> logData = new LinkedHashMap<String, Object>();
      logData.put("businessApplicationName", businessApplicationName);
      logData.put("batchJobId", batchJobId);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.info(moduleName, "Job schedule", "Start", logData);
      }
      try {
        final String appParams = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_PARAMS);
        final Map<String, String> businessApplicationParameterMap = JsonMapIoFactory.toMap(appParams);
        final String resultDataContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

        final int numRequestsPerWorker = businessApplication.getNumRequestsPerWorker();

        final List<Long> batchJobRequestIds = dataAccessObject.getNonExecutingRequestIds(
          numRequestsPerWorker, batchJobId);

        if (batchJobRequestIds.isEmpty()) {
          return false;
        } else {
          dataAccessObject.setBatchJobRequestsStarted(batchJobRequestIds);

          dataAccessObject.updateBatchJobStartRequestExecution(batchJobId,
            batchJobRequestIds.size(), startTime);

          final BatchJobRequestExecutionGroup group = new BatchJobRequestExecutionGroup(
            consumerKey, batchJobId, businessApplication,
            businessApplicationParameterMap, resultDataContentType,
            new Timestamp(System.currentTimeMillis()));
          for (final Long batchJobRequestId : batchJobRequestIds) {
            group.addBatchJobRequestId(batchJobRequestId);
          }

          InvokeMethodAfterCommit.invoke(this, "schedule", group);
          logData.put("groupId", group.getId());
          logData.put("numRequests", batchJobRequestIds.size());
          final long numBatchJobRequests = group.getNumBatchJobRequests();

          final Map<String, Object> statistics = new HashMap<String, Object>();
          statistics.put("executeScheduledTime", stopWatch);
          statistics.put("executeScheduledGroupsCount", 1);
          statistics.put("executeScheduledRequestsCount", numBatchJobRequests);

          InvokeMethodAfterCommit.invoke(this, "addStatistics",
            businessApplication, statistics);
          return true;
        }
      } finally {
        if (businessApplication.isInfoLogEnabled()) {
          if (businessApplication.isInfoLogEnabled()) {
            ModuleLog.infoAfterCommit(moduleName, "Job schedule", "End",
              stopWatch, logData);
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
    final List<Long> batchJobIds = dataAccessObject.getBatchJobIdsToSchedule(businessApplicationName);
    for (final Long batchJobId : batchJobIds) {
      final Map<String, Long> parameters = Collections.singletonMap(
        "batchJobId", batchJobId);
      ModuleLog.info(moduleName, businessApplicationName,
        "Schedule from database", parameters);
      schedule(businessApplicationName, batchJobId);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void scheduleFromDatabase(final String moduleName,
    final String businessApplicationName, final String jobStatus) {
    try {
      final List<Long> batchJobIds = dataAccessObject.getBatchJobIds(
        businessApplicationName, jobStatus);
      for (final Long batchJobId : batchJobIds) {
        if (jobStatus.equals(BatchJob.SUBMITTED)) {
          ModuleLog.info(moduleName, businessApplicationName,
            "Pre-process from database",
            Collections.singletonMap("batchJobId", batchJobId));
          preProcess.schedule(batchJobId);
        } else if (jobStatus.equals(BatchJob.PROCESSED)) {
          ModuleLog.info(moduleName, businessApplicationName,
            "Post-process from database",
            Collections.singletonMap("batchJobId", batchJobId));
          postProcess.schedule(batchJobId);
        }
      }
    } catch (final Throwable t) {
      final String trace = ExceptionUtil.toString(t);
      ModuleLog.error(moduleName, businessApplicationName,
        "Unable to schedule from database",
        Collections.singletonMap("exception", trace));
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
              LOG.error("Media type not supported for DataObject #"
                + batchJobId + " to " + contentType);
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
                  LOG.error("Unable to send notification for DataObject #"
                    + batchJobId + " to " + notificationUrl + " response="
                    + statusLine);
                }
              } finally {
                final InputStream content = entity.getContent();
                FileUtil.closeSilent(content);
              }
            }
          }
        }
      } catch (final Throwable e) {
        LOG.error("Unable to send notification for DataObject #" + batchJobId
          + " to " + notificationUrl, e);
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

  /**
   * Set the status of the DataObject to requestsCreated if the status is
   * creatingRequests.
   * 
   * @param batchJobId The id of the BatchJob.
   * @return True if the status was set, false otherwise.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public long setBatchJobCompleted(final Long batchJobId) {
    final DataObject batchJob = dataAccessObject.getBatchJobLocked(batchJobId);
    if (BatchJob.CREATING_RESULTS.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
      final long time = System.currentTimeMillis();
      final Timestamp now = new Timestamp(time);
      batchJob.setValue(BatchJob.COMPLETED_TIMESTAMP, now);
      if (!BatchJob.MARKED_FOR_DELETION.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
        batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.RESULTS_CREATED);
      }
      batchJob.setValue(BatchJob.LAST_SCHEDULED_TIMESTAMP, now);
      dataAccessObject.write(batchJob);
      return time;
    } else {
      return -1;
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void setBatchJobExecutingCounts(final String businessApplicationName,
    final Long batchJobId, final Set<BatchJobRequestExecutionGroup> groups) {

    for (final BatchJobRequestExecutionGroup group : groups) {
      final int numExecutingRequests = group.getNumBatchJobRequests();
      final int numCompletedRequests = group.getNumCompletedRequests();
      final int numFailedRequests = group.getNumFailedRequests();
      dataAccessObject.updateBatchJobExecutionCounts(batchJobId,
        -numExecutingRequests, numCompletedRequests, numFailedRequests);
    }

    if (dataAccessObject.isBatchJobCompleted(batchJobId)) {
      final long time = System.currentTimeMillis();
      setBatchJobStatus(batchJobId, BatchJob.PROCESSING, BatchJob.PROCESSED,
        time);
      postProcess(batchJobId);
    } else if (dataAccessObject.hasBatchJobUnexecutedJobs(batchJobId)) {
      schedule(businessApplicationName, batchJobId);
    }
  }

  @SuppressWarnings("unchecked")
  public void setBatchJobExecutionGroupResults(final String workerId,
    final String groupId, final Map<String, Object> results) {
    final com.revolsys.io.Writer<DataObject> writer = dataStore.getWriter();
    try {
      final Worker worker = getWorker(workerId);
      if (worker != null) {
        final BatchJobRequestExecutionGroup group = worker.removeExecutingGroup(groupId);
        if (group != null) {
          synchronized (group) {
            final BusinessApplication businessApplication = group.getBusinessApplication();
            final String businessApplicationName = group.getBusinessApplicationName();
            final String moduleName = group.getModuleName();
            final Map<String, Object> logData = new LinkedHashMap<String, Object>();
            final long batchJobId = group.getBatchJobId();
            logData.put("businessApplicationName", businessApplicationName);
            logData.put("workerId", workerId);
            logData.put("batchJobId", batchJobId);
            logData.put("groupId", groupId);
            final long numBatchJobRequests = group.getNumBatchJobRequests();
            logData.put("numRequests", numBatchJobRequests);

            final List<Map<String, Object>> groupLogRecords = (List<Map<String, Object>>)results.get("logRecords");
            if (groupLogRecords != null && !groupLogRecords.isEmpty()) {
              final Map<String, Object> appLogData = new LinkedHashMap<String, Object>();
              appLogData.put("batchJobId", batchJobId);
              appLogData.put("logRecords", groupLogRecords);
              if (businessApplication.isInfoLogEnabled()) {
                ModuleLog.info(moduleName, "Group Execution",
                  "Application Log", appLogData);
              }
            }

            final List<Map<String, Object>> groupResults = (List<Map<String, Object>>)results.get("results");
            if (groupResults != null && !groupResults.isEmpty()) {
              final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
              if (batchJob != null) {
                final List<Long> batchJobRequestIds = group.getBatchJobRequestIds();
                final long gridEndTime = System.currentTimeMillis();
                new Timestamp(gridEndTime);

                long totalApplicationExecutionTime = 0;
                int numCompletedRequests = 0;
                int numFailedRequests = 0;
                for (final Map<String, Object> requestResult : groupResults) {
                  final Long requestId = ((Number)requestResult.get("requestId")).longValue();
                  if (batchJobRequestIds.contains(requestId)) {

                    final int requestStatus = updateBatchJobRequestFromResponse(
                      requestId, requestResult);
                    Number requestExecutionTime = (Number)requestResult.get("requestExecutionTime");
                    if (requestStatus == 0) {
                      if (requestExecutionTime != null) {
                        totalApplicationExecutionTime += requestExecutionTime.longValue();
                      }
                      numCompletedRequests++;
                    } else if (requestStatus == 1) {
                      if (requestExecutionTime != null) {
                        totalApplicationExecutionTime += requestExecutionTime.longValue();
                      }
                      numFailedRequests++;
                    }
                    final List<Map<String, Object>> logRecords = (List<Map<String, Object>>)requestResult.get("logRecords");
                    if (logRecords != null && !logRecords.isEmpty()) {
                      final Map<String, Object> appLogData = new LinkedHashMap<String, Object>();
                      appLogData.put("batchJobId", batchJobId);
                      appLogData.put("requestId", requestId);
                      appLogData.put("logRecords", logRecords);
                      ModuleLog.info(moduleName, "Request Execution",
                        "Application Log", appLogData);
                    }
                  }
                }

                final Map<String, Object> appExecutedStatistics = new HashMap<String, Object>();
                appExecutedStatistics.put("applicationExecutedGroupsCount", 1);
                appExecutedStatistics.put("applicationExecutedRequestsCount",
                  numCompletedRequests + numFailedRequests);
                appExecutedStatistics.put(
                  "applicationExecutedFailedRequestsCount", numFailedRequests);
                appExecutedStatistics.put("executedTime",
                  totalApplicationExecutionTime);

                addStatistics(businessApplication, appExecutedStatistics);

                group.setNumCompletedRequests(numCompletedRequests);
                group.setNumFailedRequests(numFailedRequests);
              }
              final long executionStartTime = group.getExecutionStartTime();
              if (businessApplication.isInfoLogEnabled()) {
                ModuleLog.info(moduleName, "Execution", "End",
                  System.currentTimeMillis() - executionStartTime, logData);
              }
              final Map<String, Object> executedStatistics = new HashMap<String, Object>();
              executedStatistics.put("executedGroupsCount", 1);
              executedStatistics.put("executedRequestsCount",
                numBatchJobRequests);
              executedStatistics.put("executedTime", System.currentTimeMillis()
                - executionStartTime);

              InvokeMethodAfterCommit.invoke(this, "addStatistics",
                businessApplication, executedStatistics);

              scheduler.groupFinished(businessApplicationName, batchJobId,
                group);
            } else {
              group.resetId();
              schedule(group);
            }
          }
        }
      }
    } finally {
      writer.close();
    }
  }

  /**
   * Set the status of a DataObject if it is in the expected status.
   * 
   * @param batchJobId The id of the BatchJob.
   * @param expectedStatus The current expected status.
   * @param newStatus The new status to set.
   * @return True if the status was set, false otherwise.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public boolean setBatchJobStatus(final Long batchJobId,
    final String expectedStatus, final String newStatus) {
    final long time = System.currentTimeMillis();
    return setBatchJobStatus(batchJobId, expectedStatus, newStatus, time) != -1;
  }

  /**
   * Set the status of a DataObject if it is in the expected status.
   * 
   * @param batchJobId The id of the BatchJob.
   * @param expectedStatus The current expected status.
   * @param newStatus The new status to set.
   * @param time The time in milliseconds.
   * @return True if the status was set, false otherwise.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public long setBatchJobStatus(final Long batchJobId,
    final String expectedStatus, final String newStatus, final long time) {
    final DataObject batchJob = dataAccessObject.getBatchJobLocked(batchJobId);
    if (batchJob != null
      && expectedStatus.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
      final Timestamp startTimestamp = batchJob.getValue(BatchJob.WHEN_STATUS_CHANGED);
      final long startTime = startTimestamp.getTime();
      if (!BatchJob.MARKED_FOR_DELETION.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
        batchJob.setValue(BatchJob.JOB_STATUS, newStatus);
      }
      batchJob.setValue(BatchJob.WHEN_STATUS_CHANGED, new Timestamp(time));
      dataAccessObject.write(batchJob);
      return startTime;
    } else {
      return -1;
    }
  }

  public void setBusinessApplicationRegistry(
    final BusinessApplicationRegistry businessApplicationRegistry) {
    this.businessApplicationRegistry = businessApplicationRegistry;
    businessApplicationRegistry.addModuleEventListener(this);
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
    Object parameterValue) {
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
    requestParemeters.put(attribute.getName(), parameterValue);
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

  /**
   * Update the associated DataObject and BatchJobRequest record with the
   * response data.
   * 
   * @param requestId
   * @param result An individual Response to a Request sent to a Business
   *          Application.
   * @return True if the request was successful, false if it has errors.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public int updateBatchJobRequestFromResponse(final Long requestId,
    final Map<String, Object> result) {
    final DataObject batchJobRequest = dataAccessObject.getBatchJobRequestLocked(requestId);
    if (DataObjectUtil.getBoolean(batchJobRequest,
      BatchJobRequest.COMPLETED_IND)) {
      LOG.error("*** PROCESSING ERROR *** request " + requestId
        + " is being reprocessed");
    }
    String errorMessage = (String)result.get("errorMessage");
    ErrorCode errorCode = null;
    final String errorCodeString = (String)result.get("errorCode");
    if (errorCodeString != null) {
      errorCode = ErrorCode.valueOf(errorCodeString);
    }
    int status = 0;
    if (errorCode == null) {
      if (Boolean.TRUE == result.get("perRequestResultData")) {
        final String resultDataUrl = batchJobRequest.getValue(BatchJobRequest.RESULT_DATA_URL);
        final Number batchJobResultId = batchJobRequest.getValue(BatchJobRequest.BATCH_JOB_RESULT_ID);
        if (batchJobResultId == null && resultDataUrl == null) {
          errorCode = ErrorCode.BAD_RESPONSE_DATA;
          errorMessage = "Missing resultData or resultDataUrl";
        }

      } else {
        final String resultData = (String)result.get("results");
        if (StringUtils.hasText(resultData)) {
          batchJobRequest.setValue(BatchJobRequest.STRUCTURED_RESULT_DATA,
            resultData);
        } else {
          errorCode = ErrorCode.BAD_RESPONSE_DATA;
        }
      }
      status = 0;
    } else {
      status = 1;
    }
    if (errorCode != null && errorMessage == null) {
      errorMessage = errorCode.getDescription();
    }
    if (errorCode == ErrorCode.RECOVERABLE_EXCEPTION) {
      batchJobRequest.setValue(BatchJobRequest.STARTED_IND, 0);
      status = 2;
    } else {
      batchJobRequest.setValue(BatchJobRequest.ERROR_CODE, errorCode);
      String errorTrace = (String)result.get("errorTrace");
      if (StringUtils.hasText(errorTrace)) {
        if (errorTrace.length() > 4000) {
          errorTrace = errorTrace.substring(0, 4000);
        }
      }
      batchJobRequest.setValue(BatchJobRequest.ERROR_DEBUG_MESSAGE, errorTrace);
      if (StringUtils.hasText(errorMessage)) {
        if (errorMessage.length() > 4000) {
          errorMessage = errorMessage.substring(0, 4000);
        }
      }
      batchJobRequest.setValue(BatchJobRequest.ERROR_MESSAGE, errorMessage);
      batchJobRequest.setValue(BatchJobRequest.COMPLETED_IND, 1);
    }
    dataAccessObject.write(batchJobRequest);
    return status;
  }

  public void cancelBatchJob(long batchJobId) {
    // TODO remove from scheduler
  }
}
