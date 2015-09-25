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
package ca.bc.gov.open.cpf.api.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.log4j.Logger;
import org.springframework.util.StopWatch;

import ca.bc.gov.open.cpf.client.httpclient.DigestHttpClient;
import ca.bc.gov.open.cpf.plugin.api.RecoverableException;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.SecurityServiceFactory;

import com.revolsys.collection.map.Maps;
import com.revolsys.collection.range.RangeSet;
import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.datatype.DataType;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.io.FileBackedCache;
import com.revolsys.io.FileUtil;
import com.revolsys.io.LazyHttpPostOutputStream;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.record.io.format.csv.Csv;
import com.revolsys.record.io.format.csv.CsvWriter;
import com.revolsys.record.io.format.json.Json;
import com.revolsys.record.property.FieldProperties;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.util.MathUtil;
import com.revolsys.util.Property;

public class WorkerGroupRunnable implements Runnable {
  private final Map<String, Object> groupIdMap;

  private final DigestHttpClient httpClient;

  private final SecurityServiceFactory securityServiceFactory;

  private final String workerId;

  private final WorkerScheduler executor;

  private final String groupId;

  private final String moduleName;

  private final String logLevel;

  private final String businessApplicationName;

  private final Number batchJobId;

  private final String userId;

  private final AppLog log;

  private final RangeSet errorRequests = new RangeSet();

  private final RangeSet successRequests = new RangeSet();

  private long applicationExecutionTime = 0;

  private BusinessApplication businessApplication;

  private Module module;

  private SecurityService securityService;

  private CsvWriter errorWriter;

  private File errorFile;

  public WorkerGroupRunnable(final WorkerScheduler executor,
    final BusinessApplicationRegistry businessApplicationRegistry,
    final DigestHttpClient httpClient, final SecurityServiceFactory securityServiceFactory,
    final String workerId, final Map<String, Object> groupIdMap) {
    this.executor = executor;
    this.httpClient = httpClient;
    this.securityServiceFactory = securityServiceFactory;
    this.workerId = workerId;
    this.groupIdMap = groupIdMap;
    this.groupId = (String)groupIdMap.get("groupId");
    this.moduleName = (String)groupIdMap.get("moduleName");
    this.businessApplicationName = (String)groupIdMap.get("businessApplicationName");
    this.batchJobId = (Number)groupIdMap.get("batchJobId");
    this.userId = (String)groupIdMap.get("consumerKey");
    this.logLevel = (String)groupIdMap.get("logLevel");
    this.log = new AppLog(this.businessApplicationName, this.groupId, this.logLevel);
  }

  public void addError(final Integer sequenceNumber, final String logPrefix,
    final String errorCode, final Throwable e) {
    this.log.error(logPrefix + errorCode, e);
    if (this.errorWriter == null) {
      this.errorFile = FileUtil.createTempFile(this.groupId, "csv");
      this.errorWriter = new CsvWriter(FileUtil.createUtf8Writer(this.errorFile));
      this.errorWriter.write("sequenceNumber", "errorCode", "message", "trace");
    }
    String message;
    String trace = null;
    if (e == null) {
      message = logPrefix;
    } else {
      message = e.getMessage();
      final StringWriter errorOut = new StringWriter();
      e.printStackTrace(new PrintWriter(errorOut));
      trace = errorOut.toString();
    }
    this.errorWriter.write(sequenceNumber, errorCode, message, trace);
  }

  @SuppressWarnings("unchecked")
  private void execute(final CsvWriter resultWriter, final AppLog appLog,
    final Integer requestSequenceNumber, final Object plugin, final Map<String, Object> parameters) {
    final String resultListProperty = this.businessApplication.getResultListProperty();

    final boolean testMode = this.businessApplication.isTestModeEnabled();
    final Map<String, Object> testParameters = null;
    if (testMode && Maps.getBool(parameters, "cpfPluginTest")) {
      double testMinTime = Maps.getDouble(parameters, "cpfMinExecutionTime", -1.0);
      double testMaxTime = Maps.getDouble(parameters, "cpfMaxExecutionTime", -1.0);
      final double testMeanTime = Maps.getDouble(parameters, "cpfMeanExecutionTime", -1.0);
      final double testStandardDeviation = Maps.getDouble(parameters, "cpfStandardDeviation", -1.0);
      double executionTime;
      if (testStandardDeviation <= 0) {
        if (testMinTime < 0) {
          testMinTime = 0.0;
        }
        if (testMaxTime < testMinTime) {
          testMaxTime = testMinTime + 10;
        }
        executionTime = MathUtil.randomRange(testMinTime, testMaxTime);
      } else {
        executionTime = MathUtil.randomGaussian(testMeanTime, testStandardDeviation);
      }
      if (testMinTime >= 0 && executionTime < testMinTime) {
        executionTime = testMinTime;
      }
      if (testMaxTime > 0 && testMaxTime > testMinTime && executionTime > testMaxTime) {
        executionTime = testMaxTime;
      }
      final long milliSeconds = (long)(executionTime * 1000);
      if (milliSeconds > 1) {
        ThreadUtil.pause(milliSeconds);
      }
      this.businessApplication.pluginTestExecute(plugin);
    } else {
      this.businessApplication.pluginExecute(plugin);
    }
    Map<String, Object> customizationProperties = null;
    if (this.businessApplication.isHasCustomizationProperties()) {
      try {
        customizationProperties = (Map<String, Object>)Property.get(plugin,
          "customizationProperties");
      } catch (final Throwable e) {
        appLog.error("Unable to get customization properties", e);
      }
    }
    if (resultListProperty == null) {
      writeResult(resultWriter, plugin, parameters, customizationProperties, requestSequenceNumber,
        0, testMode);
    } else {
      final List<Object> resultObjects = JavaBeanUtil.getProperty(plugin, resultListProperty);
      if (resultObjects == null || resultObjects.isEmpty()) {
        if (testMode) {
          final double meanNumResults = Maps.getDouble(testParameters, "cpfMeanNumResults", 3.0);
          final int numResults = (int)Math.round(MathUtil.randomGaussian(meanNumResults,
            meanNumResults / 5));
          for (int i = 0; i < numResults; i++) {
            writeResult(resultWriter, plugin, parameters, customizationProperties,
              requestSequenceNumber, i, testMode);

          }
        }
      } else {
        int i = 1;
        for (final Object resultObject : resultObjects) {
          writeResult(resultWriter, resultObject, parameters, customizationProperties,
            requestSequenceNumber, i++, false);
        }
      }
    }
  }

  /**
   * <h2>Fields</h2>
   *
   * groupId long
   * sequenceNumber long
   * perRequestResultData boolean
   *
   * errorCode String
   * errorMessage String
   * errorDebugMessage String
   *
   * pluginExecutionTime long
   * results List<Map<String, Object>>
   * logRecords List<Map<String,Object>>
   * @param resultWriter
   *
   * @param requestRecordDefinition
   * @param applicationParameters
   * @param requestParameters
   * @return The request map
   */
  protected void executeRequest(final CsvWriter resultWriter,
    final RecordDefinition requestRecordDefinition,
    final Map<String, Object> applicationParameters, final Map<String, Object> requestParameters) {
    final StopWatch requestStopWatch = new StopWatch("Request");
    requestStopWatch.start();

    final Integer requestSequenceNumber = Maps.getInteger(requestParameters, "i");

    boolean hasError = true;
    try {
      final Map<String, Object> parameters = getParameters(this.businessApplication,
        requestRecordDefinition, applicationParameters, requestParameters);
      final Object plugin = this.module.getBusinessApplicationPlugin(this.businessApplicationName,
        this.groupId, this.logLevel);
      if (plugin == null) {
        addError(requestSequenceNumber, "Unable to create plugin " + this.businessApplicationName
          + " ", "ERROR_PROCESSING_REQUEST", null);
      } else {
        final AppLog appLog = new AppLog(this.businessApplicationName, this.groupId, this.logLevel);
        File resultFile = null;
        OutputStream resultData = null;
        if (this.businessApplication.isPerRequestInputData()) {
          // TODO urls for per request input data
          // final String inputDataUrl = httpClient.getOAuthUrl("GET",
          // "/worker/workers/" + workerId + "/jobs/" + batchJobId
          // + "/groups/" + groupId + "/requests/" + sequenceNumber
          // + "/inputData");
          // parameters.put("inputDataUrl", inputDataUrl);
        }
        if (this.businessApplication.isPerRequestResultData()) {
          resultFile = FileUtil.createTempFile(this.businessApplicationName, ".bin");
          resultData = new FileOutputStream(resultFile);
          parameters.put("resultData", resultData);
        }
        try {
          setParameters(plugin, parameters);

          if (appLog.isDebugEnabled()) {
            appLog.debug("Request Execution Start " + this.groupId + " " + requestSequenceNumber);
          }
          try {
            execute(resultWriter, appLog, requestSequenceNumber, plugin, parameters);
          } finally {
            if (appLog.isDebugEnabled()) {
              appLog.debug("Request Execution End " + this.groupId + " " + requestSequenceNumber);
            }
          }
          // TODO sendResultData(requestSequenceNumber, requestResult,
          // parameters, resultFile, resultData);

        } catch (final IllegalArgumentException e) {
          addError(requestSequenceNumber, "Error processing request ", "BAD_INPUT_DATA_VALUE", e);
        } catch (final RecoverableException e) {
          this.log.error("Error processing request " + requestSequenceNumber, e);
          addError(requestSequenceNumber, "Error processing request ", "RECOVERABLE_EXCEPTION",
            null);
        } finally {
          if (resultFile != null) {
            FileUtil.closeSilent(resultData);
            FileUtil.deleteDirectory(resultFile);
          }
        }
        try {
          if (requestStopWatch.isRunning()) {
            requestStopWatch.stop();
          }
        } catch (final IllegalStateException e) {
        }
      }
      hasError = false;
    } catch (final Throwable e) {
      this.log.error("Error processing request " + requestSequenceNumber, e);
      addError(requestSequenceNumber, "Error processing request ", "ERROR_PROCESSING_REQUEST", null);
    }

    if (hasError) {
      this.errorRequests.add(requestSequenceNumber);
    } else {
      this.successRequests.add(requestSequenceNumber);
    }
    this.applicationExecutionTime = requestStopWatch.getTotalTimeMillis();
  }

  public String getGroupId() {
    return this.groupId;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Object> getParameters(final BusinessApplication businessApplication,
    final RecordDefinition requestRecordDefinition,
    final Map<String, Object> applicationParameters, final Map<String, Object> requestParameters) {
    final Map<String, Object> parameters = new LinkedHashMap<String, Object>(applicationParameters);
    parameters.putAll(requestParameters);
    if (!businessApplication.isPerRequestInputData()) {

      for (final Entry<String, Object> entry : parameters.entrySet()) {
        final String name = entry.getKey();
        final Object value = entry.getValue();
        if (value != null) {
          final FieldDefinition attribute = requestRecordDefinition.getField(name);
          if (attribute != null) {
            final DataType dataType = attribute.getType();
            final Class<Object> dataTypeClass = (Class<Object>)dataType.getJavaClass();
            if (!dataTypeClass.isAssignableFrom(value.getClass())) {
              entry.setValue(value);
              final StringConverter<Object> converter = StringConverterRegistry.getInstance()
                .getConverter(dataTypeClass);
              if (converter != null) {
                final Object convertedValue = converter.toObject(value);
                entry.setValue(convertedValue);
              }
            }
          }
        }
      }
    }
    return parameters;
  }

  /**
   * <h2>Fields</h2>
   * batchJobId long
   * groupId long

   * errorCode String
   * errorMessage String
   * errorDebugMessage String

   * results List<Map<String,Object>
   * logRecords List<Map<String,Object>
   * groupExecutionTime long
   * applicationExecutionTime long
   * errorCount long
   * successCount long
   */
  @Override
  @SuppressWarnings("unchecked")
  public void run() {
    long getTime = 0;
    long putTime = 0;
    long runTime = 0;
    long time = System.currentTimeMillis();
    this.log.info("Start\tGroup Execution\tgroupId=" + this.groupId);
    try (
      FileBackedCache resultCache = new FileBackedCache()) {
      final StopWatch groupStopWatch = new StopWatch("Group");
      groupStopWatch.start();

      final Map<String, Object> groupResponse = new NamedLinkedHashMap<String, Object>(
        "ExecutionGroupResults");

      final Long moduleTime = ((Number)this.groupIdMap.get("moduleTime")).longValue();
      this.businessApplication = this.executor.getBusinessApplication(this.log, this.moduleName,
        moduleTime, this.businessApplicationName);
      if (this.businessApplication == null) {
        this.executor.addFailedGroup(this.groupId);
        return;
      } else {
        try (
          final CsvWriter resultWriter = new CsvWriter(resultCache.getWriter())) {
          resultWriter.write(this.businessApplication.getResultFieldNames());
          this.businessApplication.setLogLevel(this.logLevel);
          this.module = this.businessApplication.getModule();
          if (this.businessApplication.isSecurityServiceRequired()) {
            this.securityService = this.securityServiceFactory.getSecurityService(this.module,
              this.userId);
          }

          final String groupUrl = this.httpClient.getUrl("/worker/workers/" + this.workerId
            + "/jobs/" + this.batchJobId + "/groups/" + this.groupId);
          final Map<String, Object> group = this.httpClient.getJsonResource(groupUrl);
          if (!group.isEmpty()) {
            getTime = System.currentTimeMillis() - time;
            time = System.currentTimeMillis();
            final Map<String, Object> globalError = new LinkedHashMap<String, Object>();

            final RecordDefinition requestRecordDefinition = this.businessApplication.getRequestRecordDefinition();
            final Map<String, Object> applicationParameters = new HashMap<String, Object>(
              (Map<String, Object>)group.get("applicationParameters"));
            for (final String name : requestRecordDefinition.getFieldNames()) {
              final Object value = applicationParameters.get(name);
              if (value != null) {
                try {
                  final DataType dataType = requestRecordDefinition.getFieldType(name);
                  final Object convertedValue = StringConverterRegistry.toObject(dataType, value);
                  applicationParameters.put(name, convertedValue);
                } catch (final Throwable e) {
                  this.log.error("Error processing group", e);
                  // TODO addError("Error processing group ",
                  // "BAD_INPUT_DATA_VALUE", null);
                }
              }
            }
            if (globalError.isEmpty()) {
              final String requestsCsv = Maps.getString(group, "requests");

              for (final Map<String, Object> requestParameters : Csv.mapReader(requestsCsv)) {
                if (ThreadUtil.isInterrupted() || !this.module.isStarted()) {
                  this.executor.addFailedGroup(this.groupId);
                  return;
                }
                executeRequest(resultWriter, requestRecordDefinition, applicationParameters,
                  requestParameters);
              }
            } else {
              groupResponse.putAll(globalError);
            }
          }
        }
        if (ThreadUtil.isInterrupted() || !this.module.isStarted()) {
          this.executor.addFailedGroup(this.groupId);
          return;
        }
        try {
          if (groupStopWatch.isRunning()) {
            groupStopWatch.stop();
          }
        } catch (final IllegalStateException e) {
        }
        final long groupExecutionTime = groupStopWatch.getTotalTimeMillis();
        runTime = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();

        final String path = "/worker/workers/" + this.workerId + "/jobs/" + this.batchJobId
          + "/groups/" + this.groupId + "/results?groupExecutedTime=" + groupExecutionTime
          + "&applicationExecutedTime=" + this.applicationExecutionTime + //
          "&completedRequestRange=" + this.successRequests + //
          "&failedRequestRange=" + this.errorRequests;
        final HttpResponse response = this.httpClient.postResource(this.httpClient.getUrl(path),
          "text/csv", resultCache.getInputStream());
        this.httpClient.closeResponse(response);
        putTime = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();
        if (this.errorWriter != null) {
          final String errorPath = "/worker/workers/" + this.workerId + "/jobs/" + this.batchJobId
            + "/groups/" + this.groupId + "/error";
          final HttpResponse errorResponse = this.httpClient.postResource(
            this.httpClient.getUrl(errorPath), "text/csv", this.errorFile);
          this.httpClient.closeResponse(errorResponse);
        }
      }
    } catch (final Throwable e) {
      this.log.error("Unable to process group " + this.groupId, e);
      this.executor.addFailedGroup(this.groupId);
    } finally {
      this.executor.removeExecutingGroupId(this.groupId);
      this.log.info("End\tGroup execution\tgroupId=" + this.groupId);
      FileUtil.delete(this.errorFile);
    }
    System.out.println(getTime + "\t" + runTime + "\t" + putTime + "\t"
      + (System.currentTimeMillis() - time));
  }

  protected void sendResultData(final Integer requestSequenceNumber,
    final Map<String, Object> requestResult, final Map<String, Object> parameters,
    final File resultFile, final OutputStream resultData) {
    if (resultData != null) {
      try {
        resultData.flush();
        FileUtil.closeSilent(resultData);
        final String resultDataContentType = (String)parameters.get("resultDataContentType");
        final String resultDataUrl = this.httpClient.getUrl("/worker/workers/" + this.workerId
          + "/jobs/" + this.batchJobId + "/groups/" + this.groupId + "/requests/"
          + requestSequenceNumber + "/resultData");

        final HttpResponse response = this.httpClient.postResource(resultDataUrl,
          resultDataContentType, resultFile);
        try {

          final StatusLine statusLine = response.getStatusLine();
          if (statusLine.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new RecoverableException("Result data not accepted by server "
              + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
          }
        } finally {
          FileUtil.closeSilent(response.getEntity().getContent());
        }
      } catch (final Throwable e) {
        this.log.error("Error sending result data", e);
        addError(requestSequenceNumber, "Error processing request", "RECOVERABLE_EXCEPTION", null);
      }
    }
  }

  @SuppressWarnings("resource")
  public void setParameters(final Object plugin, final Map<String, ? extends Object> parameters) {

    this.businessApplication.pluginSetParameters(plugin, parameters);
    if (this.businessApplication.isPerRequestInputData()) {
      for (final String parameterName : PluginAdaptor.INPUT_DATA_PARAMETER_NAMES) {
        final Object parameterValue = parameters.get(parameterName);
        setPluginProperty(plugin, parameterName, parameterValue);
      }
    }
    if (this.businessApplication.isPerRequestResultData()) {
      final String resultDataContentType = (String)parameters.get("resultDataContentType");
      final String resultDataUrl = (String)parameters.get("resultDataUrl");
      OutputStream resultData;
      if (resultDataUrl == null) {
        resultData = (OutputStream)parameters.get("resultData");
        if (resultData == null) {
          try {
            final File file = File.createTempFile("cpf", ".out");
            resultData = new FileOutputStream(file);
            Logger.getLogger(getClass()).info("Writing result to " + file);
          } catch (final IOException e) {
            resultData = System.out;
          }
        }
      } else {
        resultData = new LazyHttpPostOutputStream(resultDataUrl, resultDataContentType);
      }
      setPluginProperty(plugin, "resultData", resultData);
      setPluginProperty(plugin, "resultDataContentType", resultDataContentType);
    }
    if (this.businessApplication.isSecurityServiceRequired()) {
      if (this.securityService == null) {
        throw new IllegalArgumentException("Security service is required");
      } else {
        setPluginProperty(plugin, "securityService", this.securityService);
      }
    }
  }

  public void setPluginProperty(final Object plugin, final String parameterName,
    Object parameterValue) {
    try {
      final RecordDefinitionImpl requestRecordDefinition = this.businessApplication.getRequestRecordDefinition();
      final Class<?> attributeClass = requestRecordDefinition.getFieldClass(parameterName);
      if (attributeClass != null) {
        parameterValue = StringConverterRegistry.toObject(attributeClass, parameterValue);
      }
      if (parameterValue != null) {
        BeanUtils.setProperty(plugin, parameterName, parameterValue);
      }
    } catch (final Throwable t) {
      throw new IllegalArgumentException(this.businessApplication.getName() + "." + parameterName
        + " could not be set", t);
    }
  }

  private void writeResult(final CsvWriter resultWriter, final Object plugin,
    final Map<String, Object> parameters, Map<String, Object> customizationProperties,
    final Integer requestSequenceNumber, final int resultIndex, final boolean test) {
    final RecordDefinition resultRecordDefinition = this.businessApplication.getResultRecordDefinition();
    final List<Object> result = new ArrayList<>();
    result.add(requestSequenceNumber);
    if (resultIndex != 0) {
      result.add(resultIndex);
    }
    for (final FieldDefinition field : resultRecordDefinition.getFields()) {
      final String fieldName = field.getName();
      Object value = null;
      if (!PluginAdaptor.INTERNAL_PROPERTY_NAMES.contains(fieldName)) {
        value = this.businessApplication.pluginGetResultFieldValue(plugin, fieldName);
        if (value == null && test) {
          value = PluginAdaptor.getTestValue(field);
        } else {
          if (value instanceof Geometry) {
            Geometry geometry = (Geometry)value;
            GeometryFactory geometryFactory = field.getProperty(FieldProperties.GEOMETRY_FACTORY);
            if (geometryFactory == GeometryFactory.floating3()) {
              geometryFactory = geometry.getGeometryFactory();
            }
            final int srid = Maps.getInteger(parameters, "resultSrid", geometryFactory.getSrid());
            final int axisCount = Maps.getInteger(parameters, "resultNumAxis",
              geometryFactory.getAxisCount());
            final double scaleXY = Maps.getDouble(parameters, "resultScaleFactorXy",
              geometryFactory.getScaleXY());
            final double scaleZ = Maps.getDouble(parameters, "resultScaleFactorZ",
              geometryFactory.getScaleZ());

            geometryFactory = GeometryFactory.fixed(srid, axisCount, scaleXY, scaleZ);
            geometry = geometryFactory.geometry(geometry);
            if (geometry.getSrid() == 0) {
              throw new IllegalArgumentException(
                "Geometry does not have a coordinate system (SRID) specified");
            }
            final Boolean validateGeometry = field.getProperty(FieldProperties.VALIDATE_GEOMETRY);
            if (validateGeometry == true) {
              if (!geometry.isValid()) {
                throw new IllegalArgumentException("Geometry is not valid for"
                  + this.businessApplication.getName() + "." + fieldName);
              }
            }
            value = geometry;
          }
        }
        result.add(value);
      }
    }
    if (resultIndex != 0 && this.businessApplication.isHasResultListCustomizationProperties()) {
      final Map<String, Object> resultListProperties = Property.get(plugin,
        "customizationProperties");
      if (resultListProperties != null) {
        customizationProperties = Maps.createLinkedHashMap(customizationProperties);
        customizationProperties.putAll(resultListProperties);
      }
    }
    if (Property.hasValue(customizationProperties)) {
      result.add(Json.toString(customizationProperties));
    }
    resultWriter.write(result);
  }
}
