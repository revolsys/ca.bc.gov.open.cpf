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
import java.sql.BatchUpdateException;
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
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StopWatch;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.security.service.AuthorizationService;
import ca.bc.gov.open.cpf.api.security.service.AuthorizationServiceUserSecurityServiceFactory;
import ca.bc.gov.open.cpf.api.web.controller.DatabaseJobController;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
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

import com.revolsys.data.io.MapReaderRecordReader;
import com.revolsys.data.io.RecordWriterFactory;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.ArrayRecord;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordUtil;
import com.revolsys.data.record.property.FieldProperties;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.data.types.DataType;
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
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.io.json.JsonRecordIoFactory;
import com.revolsys.io.kml.Kml22Constants;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.operation.valid.IsValidOp;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.channel.NamedChannelBundle;
import com.revolsys.spring.InputStreamResource;
import com.revolsys.spring.InvokeMethodAfterCommit;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.SendToChannelAfterCommit;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.Compress;
import com.revolsys.util.DateUtil;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.Maps;
import com.revolsys.util.Property;
import com.revolsys.util.UrlUtil;

public class BatchJobService implements ModuleEventListener {
  protected static Map<String, String> getBusinessApplicationParameters(
    final Record batchJob) {
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

  public static boolean isDatabaseResourcesException(final Throwable e) {
    if (e instanceof BatchUpdateException) {
      final BatchUpdateException batchException = (BatchUpdateException)e;
      for (SQLException sqlException = batchException.getNextException(); sqlException != null; sqlException = batchException.getNextException()) {
        if (isDatabaseResourcesException(sqlException)) {
          return true;
        }
      }
    } else if (e instanceof SQLException) {
      final SQLException sqlException = (SQLException)e;
      final int errorCode = sqlException.getErrorCode();
      boolean isCapacityError = false;
      if (errorCode == 1653) {
        isCapacityError = true;
      } else if (errorCode == 1688) {
        isCapacityError = true;
      } else if (errorCode == 1691) {
        isCapacityError = true;
      } else {
        final String sqlState = ((SQLException)e).getSQLState();
        if (sqlState != null && sqlState.startsWith("53")) {
          isCapacityError = true;
        }
      }
      if (isCapacityError) {
        capacityErrorTime = System.currentTimeMillis();
      }
      return isCapacityError;
    } else {
      final Throwable cause = e.getCause();
      if (cause != null) {
        return isDatabaseResourcesException(cause);
      }
    }
    return false;
  }

  private int largestGroupResultCount = 0;

  @Resource(name = "cpfConfig")
  private CpfConfig config;

  private final AtomicInteger groupResultCount = new AtomicInteger(0);

  private int daysToKeepOldJobs = 7;

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private boolean jsonpEnabled = true;

  private JobController jobController;

  private boolean compressData = false;

  private StatisticsProcess statisticsProcess;

  private AuthorizationService authorizationService;

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

  private RecordStore recordStore;

  private final Map<String, Worker> workersById = new TreeMap<String, Worker>();

  private final Map<Long, Set<BatchJobRequestExecutionGroup>> groupsByJobId = new HashMap<Long, Set<BatchJobRequestExecutionGroup>>();

  private final Set<Long> preprocesedJobIds = new HashSet<Long>();

  private static long capacityErrorTime;

  private long timeoutForCapacityErrors = 5 * 60 * 1000;

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
    if (this.dataAccessObject != null) {

      this.dataAccessObject.deleteBatchJobExecutionGroups(batchJobId);

      final String errorFormat = "text/csv";
      final StringWriter errorWriter = new StringWriter();

      String newErrorMessage = validationErrorMessage;
      if (validationErrorMessage.equals("")) {
        newErrorMessage = validationErrorCode.getDescription();
      }
      try (
        final MapWriter errorMapWriter = new CsvMapWriter(errorWriter)) {
        final Map<String, String> errorResultMap = new HashMap<String, String>();
        errorResultMap.put("Code", validationErrorCode.name());
        errorResultMap.put("Message", newErrorMessage);
        errorMapWriter.write(errorResultMap);
        try {
          final byte[] errorBytes = errorWriter.toString().getBytes("UTF-8");
          createBatchJobResult(batchJobId, BatchJobResult.ERROR_RESULT_DATA,
            errorFormat, errorBytes, 0);
        } catch (final UnsupportedEncodingException e) {
        }
      }
      this.dataAccessObject.setBatchJobFailed(batchJobId);
      LoggerFactory.getLogger(BatchJobService.class).debug(
        validationErrorDebugMessage);
    }
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

  public void addWorkerMessage(final Map<String, Object> message) {
    synchronized (this.workersById) {
      for (final Worker worker : this.workersById.values()) {
        worker.addMessage(message);
      }
      final NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = this.groupsToSchedule;
      if (groupsToSchedule != null) {
        groupsToSchedule.notifyReaders();
      }
    }
  }

  public boolean cancelBatchJob(final long batchJobId) {
    synchronized (this.preprocesedJobIds) {
      this.preprocesedJobIds.remove(batchJobId);
    }
    Set<BatchJobRequestExecutionGroup> groups;
    synchronized (this.groupsByJobId) {
      groups = this.groupsByJobId.remove(batchJobId);
    }
    if (groups != null) {
      for (final BatchJobRequestExecutionGroup group : groups) {
        group.cancel();
      }
    }
    final JobController jobController = getJobController(batchJobId);
    final boolean cancelled = jobController.cancelJob(batchJobId);
    synchronized (this.workersById) {
      for (final Worker worker : this.workersById.values()) {
        worker.cancelBatchJob(batchJobId);
      }
      final NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = this.groupsToSchedule;
      if (groupsToSchedule != null) {
        groupsToSchedule.notifyReaders();
      }
    }
    return cancelled;
  }

  public void cancelGroup(final Worker worker, final String groupId) {
    if (groupId != null) {
      final BatchJobRequestExecutionGroup group = worker.removeExecutingGroup(groupId);
      if (group != null) {
        LoggerFactory.getLogger(BatchJobService.class).info(
          "Reschedule Group\tgroupId" + groupId);
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

  protected void collateAllStatistics() {
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId = new HashMap<String, Map<String, BusinessApplicationStatistics>>();

        collateInMemoryStatistics(statisticsByAppAndId);

        collateDatabaseStatistics(statisticsByAppAndId);

        saveStatistics(statisticsByAppAndId);

        setStatisticsByAppAndId(statisticsByAppAndId);
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error("Unable to collate statistics",
        e);
    }
  }

  private void collateDatabaseStatistics(
    final Map<String, Map<String, BusinessApplicationStatistics>> statisticsByAppAndId) {
    final Query query = new Query(
      BusinessApplicationStatistics.APPLICATION_STATISTICS);
    query.addOrderBy(BusinessApplicationStatistics.START_TIMESTAMP, true);
    final Reader<Record> reader = this.recordStore.query(query);
    try {
      for (final Record dbStatistics : reader) {
        boolean delete = false;
        final Date startTime = dbStatistics.getValue(BusinessApplicationStatistics.START_TIMESTAMP);
        final String durationType = dbStatistics.getValue(BusinessApplicationStatistics.DURATION_TYPE);
        final String businessApplicationName = dbStatistics.getValue(BusinessApplicationStatistics.BUSINESS_APPLICATION_NAME);
        final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
        if (businessApplication != null) {
          final String statisticsId = BusinessApplicationStatistics.getId(
            durationType, startTime);
          final String valuesString = dbStatistics.getValue(BusinessApplicationStatistics.STATISTIC_VALUES);
          if (Property.hasValue(valuesString)) {
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
            this.recordStore.delete(dbStatistics);
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
    final int sequenceNumber, final RecordDefinition requestRecordDefinition,
    final List<Map<String, Object>> group) {
    String structuredInputDataString = JsonRecordIoFactory.toString(
      requestRecordDefinition, group);
    if (this.compressData) {
      structuredInputDataString = Compress.deflateBase64(structuredInputDataString);
    }
    synchronized (this.preprocesedJobIds) {
      if (this.preprocesedJobIds.contains(batchJobId)) {
        this.dataAccessObject.createBatchJobExecutionGroup(this.jobController,
          batchJobId, sequenceNumber, structuredInputDataString, group.size());
        return true;
      } else {
        return false;
      }
    }
  }

  public void createBatchJobResult(final long batchJobId,
    final String resultDataType, final String contentType, final Object data,
    final int sequenceNumber) {
    if (data != null) {
      if (data instanceof File) {
        final File file = (File)data;
        if (file.length() == 0) {
          return;
        }
      }
      try {
        final Record result = this.dataAccessObject.create(BatchJobResult.BATCH_JOB_RESULT);
        result.setValue(BatchJobResult.SEQUENCE_NUMBER, sequenceNumber);
        result.setValue(BatchJobResult.BATCH_JOB_ID, batchJobId);
        result.setValue(BatchJobResult.BATCH_JOB_RESULT_TYPE, resultDataType);
        result.setValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE, contentType);
        this.jobController.setJobResultData(batchJobId, result, data);
        this.dataAccessObject.write(result);
      } catch (final Throwable e) {
        throw new RuntimeException("Unable to save result data", e);
      }
    }
  }

  public void createBatchJobResultOpaque(final long batchJobId,
    final long sequenceNumber, final String contentType, final Object data) {
    final Record result = this.dataAccessObject.create(BatchJobResult.BATCH_JOB_RESULT);
    result.setValue(BatchJobResult.SEQUENCE_NUMBER, sequenceNumber);
    result.setValue(BatchJobResult.BATCH_JOB_ID, batchJobId);
    result.setValue(BatchJobResult.BATCH_JOB_RESULT_TYPE,
      BatchJobResult.OPAQUE_RESULT_DATA);
    result.setValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE, contentType);
    this.jobController.setJobResultData(batchJobId, result, data);
    this.dataAccessObject.write(result);
  }

  /**
   * Create the error BatchJobResult for a BatchJob. This will only be created
   * if there were any errors.
   *
   * @param batchJobId The Record identifier.
   */
  public void createResults(final AppLog log, final long batchJobId) {
    final Record batchJob = this.dataAccessObject.getBatchJobLocked(batchJobId);
    final String resultFormat = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

    final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
    final BusinessApplication application = getBusinessApplication(businessApplicationName);
    final RecordDefinition resultRecordDefinition = application.getResultRecordDefinition();

    final String fileExtension = IoFactoryRegistry.getFileExtension(resultFormat);
    File structuredResultFile = null;

    File errorFile = null;
    Writer errorWriter = null;
    MapWriter errorResultWriter = null;
    com.revolsys.io.Writer<Record> structuredResultWriter = null;
    try {
      errorFile = FileUtil.createTempFile("errors", ".csv");
      errorWriter = new FileWriter(errorFile);
      errorResultWriter = new CsvMapWriter(errorWriter);

      structuredResultFile = FileUtil.createTempFile("result", "."
        + fileExtension);
      structuredResultWriter = createStructuredResultWriter(batchJob,
        batchJobId, application, structuredResultFile, resultRecordDefinition,
        resultFormat);
      structuredResultWriter.open();
      final Map<String, Object> defaultProperties = new HashMap<String, Object>(
        structuredResultWriter.getProperties());

      boolean hasErrors = false;
      boolean hasResults = false;
      try {
        final Integer numSubmittedGroups = batchJob.getInteger(BatchJob.NUM_SUBMITTED_GROUPS);
        if (numSubmittedGroups != null && numSubmittedGroups > 0) {
          final int groupBatchSize = 100;
          for (int groupSeqeunceNumber = 0; groupSeqeunceNumber < numSubmittedGroups; groupSeqeunceNumber += groupBatchSize) {
            final int startIndex = groupSeqeunceNumber + 1;
            final int endIndex = groupSeqeunceNumber + groupBatchSize;
            final int mask = this.dataAccessObject.writeGroupResults(
              this.jobController, batchJobId, startIndex, endIndex,
              application, resultRecordDefinition, errorResultWriter,
              structuredResultWriter, defaultProperties);
            if ((mask & 4) > 0) {
              hasErrors = true;
            }
            if ((mask & 2) > 0) {
              hasResults = true;
            }
          }
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
          resultFormat, structuredResultFile, 1);
      }
      if (hasErrors) {
        createBatchJobResult(batchJobId, BatchJobResult.ERROR_RESULT_DATA,
          "text/csv", errorFile, 0);
      }
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to save results", e);
    } finally {
      InvokeMethodAfterCommit.invoke(errorFile, "delete");
      InvokeMethodAfterCommit.invoke(structuredResultFile, "delete");
    }
  }

  public com.revolsys.io.Writer<Record> createStructuredResultWriter(
    final org.springframework.core.io.Resource resource,
    final BusinessApplication application,
    final RecordDefinition resultRecordDefinition, final String resultFormat,
    final String title, final GeometryFactory geometryFactory) {
    final IoFactoryRegistry ioFactory = IoFactoryRegistry.getInstance();
    final RecordWriterFactory writerFactory = ioFactory.getFactoryByMediaType(
      RecordWriterFactory.class, resultFormat.trim());
    if (writerFactory == null) {
      throw new IllegalArgumentException("Unsupported result content type: "
        + resultFormat);
    } else {
      final com.revolsys.io.Writer<Record> recordWriter = writerFactory.createRecordWriter(
        resultRecordDefinition, resource);
      recordWriter.setProperty(Kml22Constants.STYLE_URL_PROPERTY,
        this.getBaseUrl() + "/kml/defaultStyle.kml#default");
      recordWriter.setProperty(IoConstants.TITLE_PROPERTY, title);
      recordWriter.setProperty("htmlCssStyleUrl", this.getBaseUrl()
        + "/css/default.css");

      recordWriter.setProperty(IoConstants.GEOMETRY_FACTORY, geometryFactory);
      recordWriter.setProperties(application.getProperties());
      return recordWriter;
    }
  }

  protected com.revolsys.io.Writer<Record> createStructuredResultWriter(
    final Record batchJob, final long batchJobId,
    final BusinessApplication application, final File structuredResultFile,
    final RecordDefinition resultRecordDefinition, final String resultFormat) {
    final Map<String, ? extends Object> businessApplicationParameters = getBusinessApplicationParameters(batchJob);
    final GeometryFactory geometryFactory = getGeometryFactory(
      resultRecordDefinition.getGeometryFactory(),
      businessApplicationParameters);
    final String title = "Job " + batchJobId + " Result";

    final FileSystemResource resource = new FileSystemResource(
      structuredResultFile);
    return createStructuredResultWriter(resource, application,
      resultRecordDefinition, resultFormat, title, geometryFactory);
  }

  public void deleteJob(final long batchJobId) {
    cancelBatchJob(batchJobId);
    final JobController jobController = getJobController(batchJobId);
    jobController.deleteJob(batchJobId);
  }

  @PreDestroy
  public void destory() {
    this.running = false;
    this.authorizationService = null;
    this.businessApplicationRegistry = null;
    this.connectedWorkerCounts.clear();
    this.dataAccessObject = null;
    this.recordStore = null;
    this.groupsByJobId.clear();
    if (this.groupsToSchedule != null) {
      this.groupsToSchedule.close();
      this.groupsToSchedule = null;
    }
    this.mailSender = null;
    if (this.postProcess != null) {
      this.postProcess.getIn().writeDisconnect();
      this.postProcess = null;
    }
    this.preprocesedJobIds.clear();
    if (this.preProcess != null) {
      this.preProcess.getIn().writeDisconnect();
      this.preProcess = null;
    }
    if (this.scheduler != null) {
      this.scheduler.getIn().writeDisconnect();
      this.scheduler = null;
    }
    this.securityServiceFactory = null;
    this.statisticsByAppAndId.clear();
    if (this.statisticsProcess != null) {
      this.statisticsProcess.getIn().writeDisconnect();
      this.statisticsProcess = null;
    }
    this.userClassBaseUrls.clear();
    this.workersById.clear();
    this.jobController = null;
  }

  private void error(final AppLog log, final String message, final Throwable e) {
    if (log == null) {
      LoggerFactory.getLogger(getClass()).error(message, e);
    } else {
      log.error(message, e);
    }
  }

  public AppLog getAppLog(final String businessApplicationName) {
    final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
    if (businessApplication == null) {
      return new AppLog(businessApplicationName);
    } else {
      return businessApplication.getLog();
    }
  }

  public AuthorizationService getAuthorizationService() {
    return this.authorizationService;
  }

  public String getBaseUrl() {
    return this.config.getBaseUrl();
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

  public InputStream getBatchJobResultData(final long batchJobId,
    final long sequenceNumber, final Record batchJobResult) {
    return this.jobController.getJobResultData(batchJobId, sequenceNumber,
      batchJobResult);
  }

  public long getBatchJobResultSize(final long batchJobId,
    final long sequenceNumber, final Record batchJobResult) {
    return this.jobController.getJobResultSize(batchJobId, sequenceNumber,
      batchJobResult);
  }

  public BusinessApplication getBusinessApplication(final String name) {
    return this.businessApplicationRegistry.getBusinessApplication(name);
  }

  public BusinessApplication getBusinessApplication(final String name,
    final String version) {
    return this.businessApplicationRegistry.getBusinessApplication(name);
  }

  public List<String> getBusinessApplicationNames() {
    return this.businessApplicationRegistry.getBusinessApplicationNames();
  }

  public PluginAdaptor getBusinessApplicationPlugin(
    final BusinessApplication businessApplication) {
    return this.businessApplicationRegistry.getBusinessApplicationPlugin(businessApplication);
  }

  public PluginAdaptor getBusinessApplicationPlugin(
    final String businessApplicationName,
    final String businessApplicationVersion) {
    return this.businessApplicationRegistry.getBusinessApplicationPlugin(
      businessApplicationName, businessApplicationVersion);
  }

  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return this.businessApplicationRegistry;
  }

  public List<BusinessApplication> getBusinessApplications() {
    return this.businessApplicationRegistry.getBusinessApplications();
  }

  public CpfDataAccessObject getDataAccessObject() {
    return this.dataAccessObject;
  }

  public int getDaysToKeepOldJobs() {
    return this.daysToKeepOldJobs;
  }

  public java.sql.Date getExpiryDate(final Date completionTimestamp) {
    if (completionTimestamp == null) {
      return null;
    } else {
      final int dayInMilliseconds = 1000 * 60 * 60 * 24;
      final long timeXDaysAgo = completionTimestamp.getTime()
        + this.daysToKeepOldJobs * dayInMilliseconds;
      return new java.sql.Date(timeXDaysAgo);
    }
  }

  /**
   * Get the email address messages are sent from.
   *
   * @return The email address messages are sent from.
   */
  public String getFromEmail() {
    return this.fromEmail;
  }

  public GeometryFactory getGeometryFactory(
    final GeometryFactory geometryFactory,
    final Map<String, ? extends Object> parameters) {
    if (geometryFactory == null) {
      return null;
    } else {
      final int srid = Maps.getInteger(parameters, "resultSrid",
        geometryFactory.getSrid());
      final int axisCount = Maps.getInteger(parameters,
        "resultNumAxis", geometryFactory.getAxisCount());
      final double scaleXY = Maps.getDouble(parameters,
        "resultScaleFactorXy", geometryFactory.getScaleXY());
      final double scaleZ = Maps.getDouble(parameters,
        "resultScaleFactorZ", geometryFactory.getScaleZ());
      return GeometryFactory.fixed(srid, axisCount, scaleXY, scaleZ);
    }
  }

  public int getGroupResultCount() {
    return this.groupResultCount.get();
  }

  public JobController getjobController() {
    return this.jobController;
  }

  public JobController getJobController(final long batchJobId) {
    return this.jobController;
  }

  /**
   * Get a buffered reader for the job's input data. The input Data may be a
   * remote URL or a CLOB field.
   *
   * @return BufferedReader or null if unable to connect to data
   */
  private InputStream getJobInputDataStream(final Record batchJob) {
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

  public int getLargestGroupResultCount() {
    return this.largestGroupResultCount;
  }

  /**
   * Get the class used to send email.
   *
   * @return The class used to send email.
   */
  public JavaMailSender getMailSender() {
    return this.mailSender;
  }

  public Module getModule(final String moduleName) {
    return this.businessApplicationRegistry.getModule(moduleName);
  }

  public Map<String, Object> getNextBatchJobRequestExecutionGroup(
    final String workerId, final int maxMessageId,
    final List<String> moduleNames) {
    final long startTime = System.currentTimeMillis();
    final long endTime = startTime + this.maxWorkerWaitTime;
    final Map<String, Object> response = new HashMap<String, Object>();
    if (this.running) {
      BatchJobRequestExecutionGroup group = null;
      try {
        final long waitTime = Math.min(10000, endTime - startTime);
        if (waitTime > 0) {
          final NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = this.groupsToSchedule;
          if (groupsToSchedule == null) {
            return Collections.emptyMap();
          } else {
            group = groupsToSchedule.read(waitTime, moduleNames);
          }
        }
      } catch (final ClosedException e) {
      }

      if (this.running && group != null && !group.isCancelled()) {
        final BusinessApplication businessApplication = group.getBusinessApplication();
        if (businessApplication == null) {
          schedule(group);
        } else {
          final String businessApplicationName = businessApplication.getName();
          final Module module = businessApplication.getModule();
          if (module == null || !module.isStarted()) {
            schedule(group);
          } else {
            final String moduleName = group.getModuleName();
            final long moduleStartTime = module.getStartedTime();
            final Worker worker = getWorker(workerId);
            if (worker == null || moduleStartTime == -1 || !module.isStarted()) {
              schedule(group);
            } else {
              response.put("workerId", workerId);
              response.put("moduleName", moduleName);
              response.put("moduleTime", moduleStartTime);
              response.put("businessApplicationName", businessApplicationName);
              response.put("logLevel", businessApplication.getLogLevel());

              group.setExecutionStartTime(System.currentTimeMillis());
              final String groupId = group.getId();
              final Long batchJobId = group.getBatchJobId();
              final String baseId = group.getBaseId();

              response.put("batchJobId", batchJobId);
              response.put("baseId", baseId);
              response.put("groupId", groupId);
              final AppLog log = businessApplication.getLog();
              log.info("Start\tGroup execution\tgroupId=" + groupId
                + "\tworkerId=" + workerId);
              response.put("consumerKey", group.getconsumerKey());
              worker.addExecutingGroup(moduleName, moduleStartTime, group);
            }
          }
        }
      }
    }
    final Worker worker = getWorker(workerId);
    if (worker != null) {
      final Map<String, Map<String, Object>> messages = worker.getMessages(maxMessageId);
      if (!messages.isEmpty()) {
        response.put("messages", messages);
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
      if (Property.hasValue(result)) {
        return value;
      } else {
        return null;
      }
    }
  }

  public BatchJobPostProcess getPostProcess() {
    return this.postProcess;
  }

  public BatchJobPreProcess getPreProcess() {
    return this.preProcess;
  }

  public RecordStore getRecordStore() {
    return this.recordStore;
  }

  public SecurityService getSecurityService(final Module module,
    final String consumerKey) {
    return this.securityServiceFactory.getSecurityService(module, consumerKey);
  }

  public BusinessApplicationStatistics getStatistics(
    final String businessApplicationName, final String statisticsId) {
    synchronized (this.statisticsByAppAndId) {
      return getStatistics(this.statisticsByAppAndId, businessApplicationName,
        statisticsId);
    }
  }

  public Map<String, Map<String, BusinessApplicationStatistics>> getStatisticsByAppAndId() {
    return this.statisticsByAppAndId;
  }

  public List<BusinessApplicationStatistics> getStatisticsList(
    final String businessApplicationName) {
    synchronized (this.statisticsByAppAndId) {
      final Map<String, BusinessApplicationStatistics> statisticsById = getStatistics(
        this.statisticsByAppAndId, businessApplicationName);
      return new ArrayList<BusinessApplicationStatistics>(
        statisticsById.values());
    }
  }

  public long getTimeoutForCapacityErrors() {
    return this.timeoutForCapacityErrors;
  }

  public Map<String, String> getUserClassBaseUrls() {
    return this.userClassBaseUrls;
  }

  public Worker getWorker(final String workerId) {
    synchronized (this.workersById) {
      final Worker worker = this.workersById.get(workerId);
      return worker;
    }
  }

  public List<Worker> getWorkers() {
    return new ArrayList<Worker>(this.workersById.values());
  }

  @PostConstruct
  public void init() {
    this.running = true;
    this.securityServiceFactory = new AuthorizationServiceUserSecurityServiceFactory(
      this.authorizationService);
    this.businessApplicationRegistry.addModuleEventListener(this.securityServiceFactory);
    this.recordStore = this.dataAccessObject.getRecordStore();
    LoggerFactory.getLogger(getClass()).info("Started");
  }

  public boolean isCompressData() {
    return this.compressData;
  }

  public boolean isHasTablespaceError() {
    if (System.currentTimeMillis() < capacityErrorTime
      + this.timeoutForCapacityErrors) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isJsonpEnabled() {
    return this.jsonpEnabled;
  }

  public boolean isRunning() {
    return this.running;
  }

  @Override
  public void moduleChanged(final ModuleEvent event) {
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final String action = event.getAction();
        final Module module = event.getModule();
        final String moduleName = module.getName();
        synchronized (module) {
          if (action.equals(ModuleEvent.STOP)) {
            final LinkedHashMap<String, Object> message = new LinkedHashMap<String, Object>();
            message.put("action", "moduleStop");
            message.put("moduleName", module.getName());
            message.put("moduleTime", module.getLastStartTime());
            addWorkerMessage(message);

            final NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = this.groupsToSchedule;
            if (groupsToSchedule != null) {
              groupsToSchedule.remove(moduleName);
            }
            for (final Worker worker : getWorkers()) {
              final String moduleNameAndTime = moduleName + ":"
                + module.getStartedTime();
              worker.cancelExecutingGroups(moduleNameAndTime);
            }
            for (final String businessApplicationName : event.getBusinessApplicationNames()) {
              this.scheduler.clearBusinessApplication(businessApplicationName);
            }
          } else if (action.equals(ModuleEvent.START)) {
            final LinkedHashMap<String, Object> message = new LinkedHashMap<String, Object>();
            message.put("action", "moduleStart");
            message.put("moduleName", module.getName());
            message.put("moduleTime", module.getStartedTime());
            addWorkerMessage(message);
          }
          final List<String> businessApplicationNames = module.getBusinessApplicationNames();
          for (final String businessApplicationName : businessApplicationNames) {
            synchronized (businessApplicationName.intern()) {
              if (action.equals(ModuleEvent.STOP)) {
                final NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = this.groupsToSchedule;
                if (groupsToSchedule != null) {
                  groupsToSchedule.remove(businessApplicationName);
                }
              } else if (action.equals(ModuleEvent.START)) {
                resetProcessingBatchJobs(moduleName, businessApplicationName);
                resetCreatingRequestsBatchJobs(moduleName,
                  businessApplicationName);
                resetCreatingResultsBatchJobs(moduleName,
                  businessApplicationName);
                scheduleFromDatabase(moduleName, businessApplicationName);
                if (this.preProcess != null) {
                  this.preProcess.scheduleFromDatabase(moduleName,
                    businessApplicationName);
                }
                if (this.postProcess != null) {
                  this.postProcess.scheduleFromDatabase(moduleName,
                    businessApplicationName);
                }
              }
            }
          }
        }
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }

  }

  public void postProcess(final long batchJobId) {
    if (this.postProcess != null) {
      SendToChannelAfterCommit.send(this.postProcess.getIn(), batchJobId);
    }
  }

  public boolean postProcessBatchJob(final long batchJobId, final long time,
    final long lastChangedTime) {
    AppLog log = null;
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try (
        com.revolsys.io.Writer<Record> writer = this.recordStore.getWriter();) {
        final Record batchJob = this.dataAccessObject.getBatchJob(batchJobId);
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final long numRequests = RecordUtil.getInteger(batchJob,
          BatchJob.NUM_SUBMITTED_REQUESTS);
        final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
        final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
        if (businessApplication == null) {
          log = getAppLog(businessApplicationName);
        } else {
          log = businessApplication.getLog();
        }
        if (log.isInfoEnabled()) {
          log.info("Start\tJob post-process\tbatchJobId=" + batchJobId);
        }
        final Map<String, Object> postProcessScheduledStatistics = new HashMap<String, Object>();
        postProcessScheduledStatistics.put("postProcessScheduledJobsTime", time
          - lastChangedTime);
        postProcessScheduledStatistics.put("postProcessScheduledJobsCount", 1);
        addStatistics(businessApplication, postProcessScheduledStatistics);

        createResults(log, batchJobId);

        if (this.dataAccessObject.setBatchJobCompleted(batchJobId)) {
          sendNotification(batchJobId, batchJob);
        }
        final long numCompletedRequests = RecordUtil.getInteger(batchJob,
          BatchJob.NUM_COMPLETED_REQUESTS);
        final long numFailedRequests = RecordUtil.getInteger(batchJob,
          BatchJob.NUM_FAILED_REQUESTS);
        if (log.isInfoEnabled()) {
          AppLogUtil.infoAfterCommit(log, "End\tJob post-process\tbatchJobId="
            + batchJobId, stopWatch);
          AppLogUtil.infoAfterCommit(log, "End\tJob execution\tbatchJobId="
            + batchJobId);
        }
        final Timestamp whenCreated = batchJob.getValue(BatchJob.WHEN_CREATED);

        final Map<String, Object> postProcessStatistics = new HashMap<String, Object>();

        postProcessStatistics.put("postProcessedTime", stopWatch);
        postProcessStatistics.put("postProcessedJobsCount", 1);
        postProcessStatistics.put("postProcessedRequestsCount", numRequests);

        postProcessStatistics.put("completedJobsCount", 1);
        postProcessStatistics.put("completedRequestsCount",
          numCompletedRequests + numFailedRequests);
        postProcessStatistics.put("completedFailedRequestsCount",
          numFailedRequests);
        postProcessStatistics.put("completedTime", System.currentTimeMillis()
          - whenCreated.getTime());

        InvokeMethodAfterCommit.invoke(this, "addStatistics",
          businessApplication, postProcessStatistics);
      } catch (final Throwable e) {
        boolean result = true;
        if (isDatabaseResourcesException(e)) {
          error(log, "Tablespace error post-processing job " + batchJobId, e);
          result = false;
        } else {
          error(log, "Error post-processing job " + batchJobId, e);
        }
        transaction.setRollbackOnly();
        return result;
      }
    }
    return true;
  }

  public void preProcess(final long batchJobId) {
    if (this.preProcess != null) {
      SendToChannelAfterCommit.send(this.preProcess.getIn(), batchJobId);
    }
  }

  public boolean preProcessBatchJob(final Long batchJobId, final long time,
    final long lastChangedTime) {
    synchronized (this.preprocesedJobIds) {
      this.preprocesedJobIds.add(batchJobId);
    }
    AppLog log = null;
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        this.dataAccessObject.deleteBatchJobExecutionGroups(batchJobId);
        int numSubmittedRequests = 0;
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final Record batchJob = this.dataAccessObject.getBatchJob(batchJobId);
        final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
        final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
        if (businessApplication == null) {
          throw new IllegalArgumentException(
            "Cannot find business application: " + businessApplicationName);
        }
        log = businessApplication.getLog();
        if (log.isInfoEnabled()) {
          log.info("Start\tJob pre-process\tbatchJobId=" + batchJobId);
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
            if (!businessApplication.isInputContentTypeSupported(inputContentType)) {
              valid = addJobValidationError(batchJobId,
                ErrorCode.BAD_INPUT_DATA_TYPE, "", "");
            } else if (!businessApplication.isResultContentTypeSupported(resultContentType)) {
              valid = addJobValidationError(batchJobId,
                ErrorCode.BAD_RESULT_DATA_TYPE, "", "");
            } else if (inputDataStream == null) {
              valid = addJobValidationError(batchJobId,
                ErrorCode.INPUT_DATA_UNREADABLE, "", "");
            } else {
              final RecordDefinition requestRecordDefinition = businessApplication.getRequestRecordDefinition();
              try {
                final MapReaderFactory factory = IoFactoryRegistry.getInstance()
                  .getFactoryByMediaType(MapReaderFactory.class,
                    inputContentType);
                if (factory == null) {
                  valid = addJobValidationError(batchJobId,
                    ErrorCode.INPUT_DATA_UNREADABLE, inputContentType,
                    "Media type not supported");
                } else {
                  final InputStreamResource resource = new InputStreamResource(
                    "in", inputDataStream);
                  try (
                    final Reader<Map<String, Object>> mapReader = factory.createMapReader(resource)) {
                    if (mapReader == null) {
                      valid = addJobValidationError(batchJobId,
                        ErrorCode.INPUT_DATA_UNREADABLE, inputContentType,
                        "Media type not supported");
                    } else {
                      try (
                        final Reader<Record> inputDataReader = new MapReaderRecordReader(
                          requestRecordDefinition, mapReader)) {

                        final int commitInterval = 100;
                        final PlatformTransactionManager transactionManager = this.recordStore.getTransactionManager();
                        final TransactionDefinition transactionDefinition = new DefaultTransactionDefinition(
                          TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
                        try {
                          List<Map<String, Object>> group = new ArrayList<Map<String, Object>>();
                          for (final Map<String, Object> inputDataRecord : inputDataReader) {
                            if (group.size() == maxGroupSize) {
                              numGroups++;
                              if (createBatchJobExecutionGroup(batchJobId,
                                numGroups, requestRecordDefinition, group)) {
                                group = new ArrayList<Map<String, Object>>();
                                if (numGroups % commitInterval == 0) {
                                  transactionManager.commit(status);
                                  status = transactionManager.getTransaction(transactionDefinition);
                                }
                              } else {
                                transactionManager.rollback(status);
                                return true;
                              }
                            }
                            numSubmittedRequests++;
                            final Map<String, Object> requestParemeters = processParameters(
                              batchJob, businessApplication,
                              numSubmittedRequests, jobParameters,
                              inputDataRecord);
                            if (requestParemeters == null) {
                              numFailedRequests++;
                            } else {
                              requestParemeters.put("requestSequenceNumber",
                                numSubmittedRequests);
                              group.add(requestParemeters);
                            }

                          }
                          numGroups++;
                          if (createBatchJobExecutionGroup(batchJobId,
                            numGroups, requestRecordDefinition, group)) {
                          } else {
                            transactionManager.rollback(status);
                            return true;
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
                          ErrorCode.INPUT_DATA_UNREADABLE,
                          "No records specified",
                          String.valueOf(numSubmittedRequests));
                      } else if (numSubmittedRequests > maxRequests) {
                        valid = addJobValidationError(batchJobId,
                          ErrorCode.TOO_MANY_REQUESTS, null,
                          String.valueOf(numSubmittedRequests));
                      }
                    }
                  }
                }
              } catch (final Throwable e) {
                if (isDatabaseResourcesException(e)) {
                  LoggerFactory.getLogger(getClass()).error(
                    "Tablespace error pre-processing job " + batchJobId, e);
                  return false;
                } else {
                  LoggerFactory.getLogger(getClass()).error(
                    "Error pre-processing job " + batchJobId, e);
                  final StringWriter errorDebugMessage = new StringWriter();
                  e.printStackTrace(new PrintWriter(errorDebugMessage));
                  valid = addJobValidationError(batchJobId,
                    ErrorCode.ERROR_PROCESSING_REQUEST,
                    errorDebugMessage.toString(), e.getMessage());
                }
              }
            }
          } finally {
            FileUtil.closeSilent(inputDataStream);
          }

          if (!valid || numSubmittedRequests == numFailedRequests) {
            valid = false;
            if (this.dataAccessObject.setBatchJobRequestsFailed(batchJobId,
              numSubmittedRequests, numFailedRequests, maxGroupSize, numGroups)) {
              postProcess(batchJobId);
            } else {
              this.dataAccessObject.setBatchJobStatus(batchJobId,
                BatchJob.CREATING_REQUESTS, BatchJob.SUBMITTED);
            }
          } else if (this.dataAccessObject.setBatchJobRequestsCreated(
            batchJobId, numSubmittedRequests, numFailedRequests, maxGroupSize,
            numGroups)) {
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
          }
        } finally {
          if (log.isInfoEnabled()) {
            AppLogUtil.infoAfterCommit(log, "End\tJob pre-process\tbatchJobId="
              + batchJobId);
          }
        }
        synchronized (this.preprocesedJobIds) {
          this.preprocesedJobIds.remove(batchJobId);
        }

      } catch (final Throwable e) {
        synchronized (this.preprocesedJobIds) {
          this.preprocesedJobIds.remove(batchJobId);
        }
        this.dataAccessObject.setBatchJobStatus(batchJobId,
          BatchJob.CREATING_REQUESTS, BatchJob.SUBMITTED);
        error(log, "Error\tJob pre-process\tbatchJobId=" + batchJobId, e);
        throw transaction.setRollbackOnly(e);
      }
    }
    return true;
  }

  private Map<String, Object> processParameters(final Record batchJob,
    final BusinessApplication businessApplication,
    final int requestSequenceNumber, final Map<String, String> jobParameters,
    final Map<String, Object> inputDataRecord) {
    final long batchJobId = RecordUtil.getInteger(batchJob,
      BatchJob.BATCH_JOB_ID);
    final RecordDefinitionImpl requestRecordDefinition = businessApplication.getRequestRecordDefinition();
    final Record requestParameters = new ArrayRecord(requestRecordDefinition);
    for (final FieldDefinition attribute : requestRecordDefinition.getFields()) {
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
          this.dataAccessObject.createBatchJobExecutionGroup(
            this.jobController, batchJobId, requestSequenceNumber,
            ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription(),
            ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription() + " "
              + parameterName, null);
          return null;
        }
      } else {
        try {
          attribute.validate(parameterValue);
        } catch (final IllegalArgumentException e) {
          this.dataAccessObject.createBatchJobExecutionGroup(
            this.jobController, batchJobId, requestSequenceNumber,
            ErrorCode.INVALID_PARAMETER_VALUE.getDescription(), e.getMessage(),
            null);
          return null;
        }
        try {
          final String sridString = jobParameters.get("srid");
          setStructuredInputDataValue(sridString, requestParameters, attribute,
            parameterValue, !jobParameter);
        } catch (final IllegalArgumentException e) {
          final StringWriter errorOut = new StringWriter();
          e.printStackTrace(new PrintWriter(errorOut));
          this.dataAccessObject.createBatchJobExecutionGroup(
            this.jobController, batchJobId, requestSequenceNumber,
            ErrorCode.INVALID_PARAMETER_VALUE.getDescription(),
            ErrorCode.INVALID_PARAMETER_VALUE.getDescription() + " "
              + parameterName + " " + e.getMessage(), errorOut.toString());
          return null;
        }
      }
    }
    final Map<String, Object> params = new LinkedHashMap<>(requestParameters);
    for (final Entry<String, String> entry : jobParameters.entrySet()) {
      final String name = entry.getKey();
      if (name.startsWith("cpf")) {
        final String value = entry.getValue();
        params.put(name, value);
      }
    }
    return requestParameters;
  }

  public Map<String, Object> processWorkerMessage(final String workerId,
    final long workerStartTime, final Map<String, Object> message) {
    try {
      setWorkerConnected(workerId, workerStartTime, true);
      final Map<String, Object> response = new NamedLinkedHashMap<String, Object>(
        "MessageResponse");
      response.put("workerId", "workerId");
      final Worker worker = getWorker(workerId);
      if (worker != null) {
        final String action = (String)message.get("action");
        if (action != null) {
          final String moduleName = (String)message.get("moduleName");
          final Module module = getModule(moduleName);
          final boolean enabled = module != null && module.isEnabled();
          final WorkerModuleState moduleState = worker.getModuleState(moduleName);
          if ("executingGroupIds".equals(action)) {
            @SuppressWarnings("unchecked")
            final List<String> executingGroupIds = (List<String>)message.get("executingGroupIds");
            updateWorkerExecutingGroups(worker, executingGroupIds);
          } else if ("failedGroupId".equals(action)) {
            final String groupId = (String)message.get("groupId");
            cancelGroup(worker, groupId);
          } else if ("moduleStarted".equals(action)) {
            moduleState.setEnabled(enabled);
            moduleState.setStatus("Started");
            final long moduleTime = Maps.getLong(message,
              "moduleTime");
            moduleState.setStartedTime(moduleTime);
          } else if ("moduleStartFailed".equals(action)) {
            moduleState.setEnabled(enabled);
            moduleState.setStatus("Start Failed");
            final String moduleError = (String)message.get("moduleError");
            moduleState.setModuleError(moduleError);
            moduleState.setStartedTime(0);
          } else if ("moduleStopped".equals(action)) {
            moduleState.setEnabled(enabled);
            if (enabled) {
              moduleState.setStatus("Stopped");
            } else {
              moduleState.setStatus("Disabled");
            }
            moduleState.setStartedTime(0);
          } else if ("moduleDisabled".equals(action)) {
            moduleState.setEnabled(enabled);
            moduleState.setStatus("Disabled");
            moduleState.setStartedTime(0);
          }
        }
        response.put("errorMessage", "Unknown message");
        response.put("message", message);
      }
      return response;
    } finally {
      setWorkerConnected(workerId, workerStartTime, false);
    }
  }

  protected void removeGroup(final BatchJobRequestExecutionGroup group) {
    final long batchJobId = group.getBatchJobId();
    synchronized (this.groupsByJobId) {
      final Set<BatchJobRequestExecutionGroup> groups = this.groupsByJobId.get(batchJobId);
      if (groups != null) {
        groups.remove(group);
        if (groups.isEmpty()) {
          this.groupsByJobId.remove(batchJobId);
        }
      }
    }
  }

  public void resetCreatingRequestsBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final AppLog log = getAppLog(businessApplicationName);
    try {
      final int numCleanedJobs = this.dataAccessObject.updateBatchJobStatus(
        BatchJob.SUBMITTED, BatchJob.CREATING_REQUESTS, businessApplicationName);
      if (numCleanedJobs > 0) {
        log.info("Job status reset to submitted\tcount=" + numCleanedJobs);
      }
    } catch (final Throwable e) {
      log.error("Unable to reset job status to submitted", e);
    }
  }

  public void resetCreatingResultsBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final AppLog log = getAppLog(businessApplicationName);
    try {
      final int numCleanedJobs = this.dataAccessObject.updateBatchJobStatus(
        BatchJob.PROCESSED, BatchJob.CREATING_RESULTS, businessApplicationName);
      if (numCleanedJobs > 0) {
        log.info("Job status reset to processed\tcount=" + numCleanedJobs);
      }
    } catch (final Throwable e) {
      log.error("Unable to reset job status to processed", e);
    }
  }

  public void resetHungWorkers() {
    final Timestamp lastIdleTime = new Timestamp(System.currentTimeMillis()
      - this.maxWorkerWaitTime * 2);
    ArrayList<String> workerIds;
    synchronized (this.workersById) {
      workerIds = new ArrayList<String>(this.workersById.keySet());
    }
    for (final String workerId : workerIds) {
      if (!this.connectedWorkerCounts.containsKey(workerId)) {
        Worker worker = getWorker(workerId);
        if (worker != null) {
          final Timestamp workerTimestamp = worker.getLastConnectTime();
          if (workerTimestamp == null || workerTimestamp.before(lastIdleTime)) {
            synchronized (this.workersById) {
              worker = this.workersById.remove(workerId);
            }
            final Map<String, BatchJobRequestExecutionGroup> groupsById = worker.getExecutingGroupsById();
            if (groupsById != null) {
              synchronized (groupsById) {
                for (final BatchJobRequestExecutionGroup group : new ArrayList<>(
                  groupsById.values())) {
                  final String groupId = group.getId();
                  if (LoggerFactory.getLogger(BatchJobService.class)
                    .isDebugEnabled()) {
                    LoggerFactory.getLogger(BatchJobService.class).debug(
                      "Rescheduling group " + groupId + " from worker "
                        + workerId);
                  }
                  worker.removeExecutingGroup(groupId);
                  removeGroup(group);
                  group.resetId();
                  schedule(group);
                }
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
  public void resetProcessingBatchJobs(final String moduleName,
    final String businessApplicationName) {
    final AppLog log = getAppLog(businessApplicationName);
    try {
      final NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = this.groupsToSchedule;
      if (groupsToSchedule != null) {
        groupsToSchedule.remove(moduleName);
      }
      final int numCleanedRequests = this.dataAccessObject.updateResetGroupsForRestart(businessApplicationName);
      if (numCleanedRequests > 0) {
        log.info("Groups cleaned for restart\tcount=" + numCleanedRequests);
      }
      final int numCleanedJobs = this.dataAccessObject.updateResetBatchJobExecutingGroups(businessApplicationName);
      if (numCleanedJobs > 0) {
        log.info("Batch Jobs cleaned for restart\tcount=" + numCleanedJobs);
      }
      final int numCleanedStatus = this.dataAccessObject.updateBatchJobProcessedStatus(businessApplicationName);
      if (numCleanedStatus > 0) {
        log.info("Jobs status for restart\tcount=" + numCleanedStatus);
      }
    } catch (final Throwable e) {
      log.error("Unable to reset jobs and groups for restart", e);
    }
  }

  protected void saveAllStatistics() {
    List<String> businessApplicationNames;
    synchronized (this.statisticsByAppAndId) {
      businessApplicationNames = new ArrayList<String>(
        this.statisticsByAppAndId.keySet());
    }
    saveStatistics(businessApplicationNames);
  }

  protected void saveStatistics(
    final Collection<String> businessApplicationNames) {
    for (final String businessApplicationName : businessApplicationNames) {
      Map<String, BusinessApplicationStatistics> statisticsById;
      synchronized (this.statisticsByAppAndId) {
        statisticsById = this.statisticsByAppAndId.remove(businessApplicationName);
      }
      if (statisticsById != null) {
        for (final BusinessApplicationStatistics statistics : statisticsById.values()) {
          final String durationType = statistics.getDurationType();
          if (durationType.equals(BusinessApplicationStatistics.HOUR)) {
            this.dataAccessObject.saveStatistics(statistics);
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
          this.dataAccessObject.deleteBusinessApplicationStatistics(databaseId);
        }
      } else {
        final String durationType = statistics.getDurationType();
        final String currentId = BusinessApplicationStatistics.getId(
          durationType, currentTime);
        if (!currentId.equals(statistics.getId())) {
          this.dataAccessObject.saveStatistics(statistics);
        } else {
          final Integer databaseId = statistics.getDatabaseId();
          if (databaseId != null) {
            this.dataAccessObject.deleteBusinessApplicationStatistics(databaseId);
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
    if (this.running) {
      final long batchJobId = group.getBatchJobId();
      synchronized (this.groupsByJobId) {
        Set<BatchJobRequestExecutionGroup> groups = this.groupsByJobId.get(batchJobId);
        if (groups == null) {
          groups = new LinkedHashSet<BatchJobRequestExecutionGroup>();
          this.groupsByJobId.put(batchJobId, groups);
        }
        groups.add(group);
      }
      final BusinessApplication businessApplication = group.getBusinessApplication();
      final Module module = businessApplication.getModule();
      final String moduleName = module.getName();
      final NamedChannelBundle<BatchJobRequestExecutionGroup> groupsToSchedule = this.groupsToSchedule;
      if (groupsToSchedule != null) {
        groupsToSchedule.write(moduleName, group);
      }
    }
  }

  public void schedule(final String businessApplicationName,
    final long batchJobId) {
    if (this.scheduler != null) {
      final BatchJobScheduleInfo jobInfo = new BatchJobScheduleInfo(
        businessApplicationName, batchJobId, BatchJobScheduleInfo.SCHEDULE);
      SendToChannelAfterCommit.send(this.scheduler.getIn(), jobInfo);
    }
  }

  public BatchJobRequestExecutionGroup scheduleBatchJobExecutionGroups(
    final Long batchJobId) {
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final long start = System.currentTimeMillis();
        final Timestamp startTime = new Timestamp(start);
        final Record batchJob = this.dataAccessObject.getBatchJob(batchJobId);
        this.dataAccessObject.setBatchJobStatus(batchJobId,
          BatchJob.REQUESTS_CREATED, BatchJob.PROCESSING);
        if (batchJob == null) {
          return null;
        } else {
          final String consumerKey = batchJob.getValue(BatchJob.USER_ID);

          final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);

          final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
          if (businessApplication == null) {
            return null;
          } else {
            final AppLog log = businessApplication.getLog();
            if (log.isInfoEnabled()) {
              log.info("Start\tJob schedule\tbatchJobId=" + batchJobId);
            }
            try {
              final Map<String, String> businessApplicationParameterMap = getBusinessApplicationParameters(batchJob);
              final String resultDataContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

              final Long sequenceNumber = this.jobController.getNonExecutingGroupSequenceNumber(batchJobId);

              if (sequenceNumber == null) {
                return null;
              } else {
                this.dataAccessObject.setBatchJobExecutionGroupsStarted(
                  batchJobId, sequenceNumber);

                this.dataAccessObject.updateBatchJobAddScheduledGroupCount(
                  batchJobId, startTime);

                final BatchJobRequestExecutionGroup group = new BatchJobRequestExecutionGroup(
                  consumerKey, batchJobId, businessApplication,
                  businessApplicationParameterMap, resultDataContentType,
                  new Timestamp(System.currentTimeMillis()), sequenceNumber);

                InvokeMethodAfterCommit.invoke(this, "schedule", group);

                final Map<String, Object> statistics = new HashMap<String, Object>();
                statistics.put("executeScheduledTime", stopWatch);
                statistics.put("executeScheduledGroupsCount", 1);

                InvokeMethodAfterCommit.invoke(this, "addStatistics",
                  businessApplication, statistics);
                return group;
              }
            } finally {
              if (log.isInfoEnabled()) {
                AppLogUtil.infoAfterCommit(log,
                  "End\tJob schedule\tbatchJobId=" + batchJobId, stopWatch);
              }
            }
          }
        }
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void scheduleFromDatabase() {
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        for (final Module module : this.businessApplicationRegistry.getModules()) {
          if (module.isStarted()) {
            final String moduleName = module.getName();
            for (final String businessApplicationName : module.getBusinessApplicationNames()) {
              scheduleFromDatabase(moduleName, businessApplicationName);
            }
          }
        }
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void scheduleFromDatabase(final String jobStatus) {
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        for (final Module module : this.businessApplicationRegistry.getModules()) {
          if (module.isEnabled()) {
            final String moduleName = module.getName();
            for (final BusinessApplication businessApplication : module.getBusinessApplications()) {
              final String businessApplicationName = businessApplication.getName();
              scheduleFromDatabase(moduleName, businessApplicationName,
                jobStatus);
            }
          }
        }
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void scheduleFromDatabase(final String moduleName,
    final String businessApplicationName) {
    synchronized (businessApplicationName.intern()) {
      final List<Long> batchJobIds = this.dataAccessObject.getBatchJobIdsToSchedule(businessApplicationName);
      for (final Long batchJobId : batchJobIds) {
        getAppLog(businessApplicationName).info(
          "Schedule from database\tbatchJobId=" + batchJobId);
        schedule(businessApplicationName, batchJobId);
      }
    }
  }

  public void scheduleFromDatabase(final String moduleName,
    final String businessApplicationName, final String jobStatus) {
    final AppLog log = getAppLog(businessApplicationName);
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final List<Long> batchJobIds = this.dataAccessObject.getBatchJobIds(
          businessApplicationName, jobStatus);
        for (final Long batchJobId : batchJobIds) {
          if (jobStatus.equals(BatchJob.SUBMITTED)) {
            log.info("Pre-process from database\tbatchJobId=" + batchJobId);
            this.preProcess.schedule(batchJobId);
          } else if (jobStatus.equals(BatchJob.PROCESSED)) {
            log.info("Post-process from database\tbatchJobId=" + batchJobId);
            this.postProcess.schedule(batchJobId);
          }
        }
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void scheduleSaveStatistics(final List<String> businessApplicationNames) {
    final Map<String, Object> values = new HashMap<String, Object>();
    values.put(StatisticsProcess.SAVE, Boolean.TRUE);
    values.put("businessApplicationNames", businessApplicationNames);
    sendStatistics(values);
  }

  /**
   * @param batchJobId The Record identifier.
   * @param batchJob The BatchJob.
   */
  public void sendNotification(final Long batchJobId, final Record batchJob) {
    String notificationUrl = batchJob.getValue(BatchJob.NOTIFICATION_URL);
    if (Property.hasValue(notificationUrl)) {
      try {
        String baseUrl = this.getBaseUrl();
        if (this.userClassBaseUrls != null && !this.userClassBaseUrls.isEmpty()) {
          final String consumerKey = batchJob.getValue(BatchJob.USER_ID);
          final Record userAccount = this.dataAccessObject.getUserAccount(consumerKey);
          final String userClass = userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS);
          if (this.userClassBaseUrls.containsKey(userClass)) {
            baseUrl = this.userClassBaseUrls.get(userClass);
          }
        }
        final String propertiesText = batchJob.getValue(BatchJob.PROPERTIES);
        String batchJobUrl = baseUrl;
        if (Property.hasValue(propertiesText)) {
          final String webServicePrefix = JsonMapIoFactory.toMap(propertiesText)
            .get("webServicePrefix");
          if (Property.hasValue(webServicePrefix)) {
            batchJobUrl += webServicePrefix;
          }
        }
        batchJobUrl += "/ws/jobs/" + batchJobId;
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

            final MimeMessage message = this.mailSender.createMimeMessage();

            final MimeMessageHelper messageHelper = new MimeMessageHelper(
              message);
            messageHelper.setTo(notificationUri.getSchemeSpecificPart());
            messageHelper.setSubject(subject);
            messageHelper.setFrom(this.fromEmail);
            messageHelper.setText(bodyOut.toString(), true);
            this.mailSender.send(message);
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
                "Media type not supported for Record #" + batchJobId + " to "
                  + contentType);
            } else {
              final MapWriter writer = writerFactory.getMapWriter(bodyOut);
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
                    "Unable to send notification for Record #" + batchJobId
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
          "Unable to send notification for Record #" + batchJobId + " to "
            + notificationUrl, e);
      }
    }
  }

  private void sendStatistics(final Map<String, ?> values) {
    if (this.statisticsProcess != null) {
      final Channel<Map<String, ? extends Object>> in = this.statisticsProcess.getIn();
      if (!in.isClosed()) {
        in.write(values);
      }
    }
  }

  public void setAuthorizationService(
    final AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> setBatchJobExecutionGroupResults(
    final String workerId, final String groupId,
    final Map<String, Object> results) {
    synchronized (this.groupResultCount) {
      while (this.groupResultCount.get() >= this.config.getGroupResultPoolSize()) {
        ThreadUtil.pause(1000);
      }
      this.largestGroupResultCount = Math.max(this.largestGroupResultCount,
        +this.groupResultCount.incrementAndGet());
    }
    try {
      final Worker worker = getWorker(workerId);
      final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
        "ExecutionGroupResultsConfirmation");
      map.put("workerId", workerId);
      map.put("groupId", groupId);
      if (worker != null) {
        final BatchJobRequestExecutionGroup group = worker.removeExecutingGroup(groupId);
        if (group != null && !group.isCancelled()) {
          synchronized (group) {
            final long batchJobId = group.getBatchJobId();
            map.put("batchJobId", batchJobId);
            try (
              final Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW);
              com.revolsys.io.Writer<Record> writer = this.recordStore.createWriter()) {
              final BusinessApplication businessApplication = group.getBusinessApplication();
              final String moduleName = group.getModuleName();

              final List<Map<String, Object>> groupResults = (List<Map<String, Object>>)results.get("results");
              final long groupExecutedTime = Maps.getLong(results,
                "groupExecutedTime");
              final long applicationExecutedTime = Maps.getLong(
                results, "applicationExecutedTime");
              final int successCount = Maps.getInteger(results,
                "successCount");
              final int errorCount = Maps.getInteger(results,
                "errorCount");

              final Record batchJob = this.dataAccessObject.getBatchJob(batchJobId);
              if (batchJob != null) {
                if (groupResults != null && !groupResults.isEmpty()) {
                  try {
                    this.dataAccessObject.updateBatchJobExecutionGroupFromResponse(
                      this, batchJobId, group, groupResults, successCount,
                      errorCount);
                  } catch (final Throwable e) {
                    if (isDatabaseResourcesException(e)) {
                      LoggerFactory.getLogger(getClass()).error(
                        "Tablespace error saving group results: " + groupId);
                    } else {
                      LoggerFactory.getLogger(getClass()).error(
                        "Error saving group results: " + groupId, e);
                    }
                    transaction.setRollbackOnly();
                    cancelGroup(worker, groupId);
                  }
                }
              }
              final long executionTime = updateGroupStatistics(group,
                businessApplication, moduleName, applicationExecutedTime,
                groupExecutedTime, successCount, errorCount);
              removeGroup(group);
              final AppLog appLog = businessApplication.getLog();
              AppLogUtil.infoAfterCommit(appLog,
                "End\tGroup execution\tgroupId=" + groupId + "\tworkerId="
                  + workerId + "\ttime=" + executionTime / 1000.0);
              this.scheduler.groupFinished(group);
            }
          }
        }
      }
      return map;
    } finally {
      synchronized (this.groupResultCount) {
        this.groupResultCount.decrementAndGet();
        this.groupResultCount.notifyAll();
      }
    }
  }

  public void setBusinessApplicationRegistry(
    final BusinessApplicationRegistry businessApplicationRegistry) {
    this.businessApplicationRegistry = businessApplicationRegistry;
  }

  public void setCompressData(final boolean compressData) {
    this.compressData = compressData;
  }

  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
    this.jobController = new DatabaseJobController(dataAccessObject);
    // this.jobController = new FileJobController(this, new
    // File("/apps/data/cpf"));
  }

  public void setDaysToKeepOldJobs(final int daysToKeepOldJobs) {
    this.daysToKeepOldJobs = daysToKeepOldJobs;
  }

  /**
   * Set the email address messages are sent from.
   *
   * @param fromEmail The email address messages are sent from.
   */
  public void setFromEmail(final String fromEmail) {
    this.fromEmail = fromEmail;
  }

  public void setJsonpEnabled(final boolean jsonpEnabled) {
    this.jsonpEnabled = jsonpEnabled;
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
    final Map<String, Object> requestParemeters,
    final FieldDefinition attribute, Object parameterValue,
    final boolean setValue) {
    final DataType dataType = attribute.getType();
    final Class<?> dataClass = dataType.getJavaClass();
    if (Geometry.class.isAssignableFrom(dataClass)) {
      if (parameterValue != null) {
        final GeometryFactory geometryFactory = attribute.getProperty(FieldProperties.GEOMETRY_FACTORY);
        Geometry geometry;
        if (parameterValue instanceof Geometry) {

          geometry = (Geometry)parameterValue;
          if (geometry.getSrid() == 0 && Property.hasValue(sridString)) {
            final int srid = Integer.parseInt(sridString);
            final GeometryFactory sourceGeometryFactory = GeometryFactory.floating3(srid);
            geometry = sourceGeometryFactory.geometry(geometry);
          }
        } else {
          String wkt = parameterValue.toString();
          if (Property.hasValue(wkt)) {
            wkt = wkt.trim();
          }
          if (wkt.startsWith("http")) {
            wkt = UrlUtil.getContent(wkt + "/feature.wkt?srid=3005");
            if (!wkt.startsWith("SRID")) {
              wkt = "SRID=3005;" + wkt;
            }
          }
          try {
            if (Property.hasValue(sridString)) {
              final int srid = Integer.parseInt(sridString);
              final GeometryFactory sourceGeometryFactory = GeometryFactory.floating3(srid);
              geometry = sourceGeometryFactory.geometry(wkt, false);
            } else {
              geometry = geometryFactory.geometry(wkt, false);
            }
          } catch (final Throwable t) {
            throw new IllegalArgumentException("invalid WKT geometry", t);
          }
        }
        if (geometryFactory != GeometryFactory.floating3()) {
          geometry = geometryFactory.geometry(geometry);
        }
        final Boolean validateGeometry = attribute.getProperty(FieldProperties.VALIDATE_GEOMETRY);
        if (geometry.getSrid() == 0) {
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

  public void setTimeoutForCapacityErrors(final long timeoutForCapacityErrors) {
    this.timeoutForCapacityErrors = timeoutForCapacityErrors * 60 * 1000;
  }

  public void setUserClassBaseUrls(final Map<String, String> userClassBaseUrls) {
    this.userClassBaseUrls = userClassBaseUrls;
  }

  public void setWorkerConnected(final String workerId,
    final long workerStartTime, final boolean connected) {
    synchronized (this.connectedWorkerCounts) {
      Integer count = this.connectedWorkerCounts.get(workerId);
      if (count == null) {
        count = 0;
      }
      if (connected) {
        this.connectedWorkerCounts.put(workerId, count + 1);
      } else {
        count = count - 1;
        if (count <= 0) {
          this.connectedWorkerCounts.remove(workerId);
        } else {
          this.connectedWorkerCounts.put(workerId, count);
        }
      }

      synchronized (this.workersById) {
        Worker worker = this.workersById.get(workerId);
        if (worker == null || worker.getStartTime() != workerStartTime) {
          worker = new Worker(workerId, workerStartTime);
          if (connected) {
            this.workersById.put(workerId, worker);
            for (final Module module : this.businessApplicationRegistry.getModules()) {
              if (module.isStarted()) {
                final LinkedHashMap<String, Object> message = new LinkedHashMap<String, Object>();
                message.put("action", "moduleStart");
                message.put("moduleName", module.getName());
                message.put("moduleTime", module.getStartedTime());
                worker.addMessage(message);
              }
            }
          }
        }
        final long time = System.currentTimeMillis();
        final Timestamp lastConnectTime = new Timestamp(time);
        worker.setLastConnectTime(lastConnectTime);
      }
    }
  }

  public Map<String, Object> toMap(final Record batchJob, final String jobUrl,
    final long timeUntilNextCheck) {
    try {
      final Map<String, Object> jobMap = new NamedLinkedHashMap<String, Object>(
        "BatchJob");
      jobMap.put("id", new URI(jobUrl));
      jobMap.put("consumerKey", batchJob.getValue(BatchJob.USER_ID));
      jobMap.put("businessApplicationName",
        batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME));
      jobMap.put("inputDataContentType",
        batchJob.getValue(BatchJob.INPUT_DATA_CONTENT_TYPE));
      jobMap.put("structuredInputDataUrl",
        batchJob.getValue(BatchJob.STRUCTURED_INPUT_DATA_URL));
      jobMap.put("resultDataContentType",
        batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE));
      final Map<String, String> parameters = getBusinessApplicationParameters(batchJob);
      for (final Entry<String, String> param : parameters.entrySet()) {
        jobMap.put(param.getKey(), param.getValue());
      }
      jobMap.put("jobStatus", batchJob.getValue(BatchJob.JOB_STATUS));
      jobMap.put("jobStatusDate", DateUtil.format(DATE_TIME_FORMAT));
      jobMap.put(
        "startTime",
        DateUtil.format(DATE_TIME_FORMAT,
          (Date)batchJob.getValue(BatchJob.WHEN_CREATED)));
      jobMap.put(
        "modificationTime",
        DateUtil.format(DATE_TIME_FORMAT,
          (Date)batchJob.getValue(BatchJob.WHEN_UPDATED)));
      jobMap.put(
        "lastScheduledTime",
        DateUtil.format(DATE_TIME_FORMAT,
          (Date)batchJob.getValue(BatchJob.LAST_SCHEDULED_TIMESTAMP)));
      final Date completedDate = (Date)batchJob.getValue(BatchJob.COMPLETED_TIMESTAMP);
      jobMap.put("completionTime",
        DateUtil.format(DATE_TIME_FORMAT, completedDate));
      jobMap.put("expiryDate",
        DateUtil.format("yyyy-MM-dd", getExpiryDate(completedDate)));

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

  protected long updateGroupStatistics(
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
    final Map<String, Object> executedStatistics = new HashMap<String, Object>();
    executedStatistics.put("executedGroupsCount", 1);
    executedStatistics.put("executedRequestsCount", successCount + errorCount);
    executedStatistics.put("executedTime", durationInMillis);

    InvokeMethodAfterCommit.invoke(this, "addStatistics", businessApplication,
      executedStatistics);

    group.setNumCompletedRequests(successCount);
    group.setNumFailedRequests(errorCount);
    return durationInMillis;
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

  public void waitIfTablespaceError(final Class<?> logClass) {
    final long currentTime = System.currentTimeMillis();
    final long waitTime = capacityErrorTime + this.timeoutForCapacityErrors
      - currentTime;
    if (waitTime > 0) {
      LoggerFactory.getLogger(logClass).error(
        "Waiting " + waitTime / 1000 + " seconds for tablespace error");
      ThreadUtil.pause(waitTime);
    }
  }
}
