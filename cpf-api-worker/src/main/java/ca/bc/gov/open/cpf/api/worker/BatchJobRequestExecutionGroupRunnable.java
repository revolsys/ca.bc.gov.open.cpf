package ca.bc.gov.open.cpf.api.worker;

import java.io.File;
import java.io.FileOutputStream;
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

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.io.FileUtil;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.io.json.JsonDataObjectIoFactory;
import com.revolsys.io.json.JsonMapIoFactory;

public class BatchJobRequestExecutionGroupRunnable implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BatchJobRequestExecutionGroupRunnable.class);

  private final Map<String, Object> groupIdMap;

  private final DigestHttpClient httpClient;

  private final SecurityServiceFactory securityServiceFactory;

  private final String workerId;

  private final BatchJobWorkerScheduler executor;

  public BatchJobRequestExecutionGroupRunnable(
    final BatchJobWorkerScheduler executor,
    final BusinessApplicationRegistry businessApplicationRegistry,
    final DigestHttpClient httpClient,
    final SecurityServiceFactory securityServiceFactory, final String workerId,
    final Map<String, Object> groupIdMap) {
    this.executor = executor;
    this.httpClient = httpClient;
    this.securityServiceFactory = securityServiceFactory;
    this.workerId = workerId;
    this.groupIdMap = groupIdMap;
  }

  public void addError(final Map<String, Object> groupResult,
    final String errorCode, final Throwable e) {
    if (groupResult.get("errorCode") == null) {
      final StringWriter errorOut = new StringWriter();
      e.printStackTrace(new PrintWriter(errorOut));
      groupResult.put("errorCode", errorCode);
      groupResult.put("errorMessage", e.getMessage());
      groupResult.put("errorTrace", errorOut);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void run() {
    final String groupId = (String)groupIdMap.get("groupId");
    try {
      final String moduleName = (String)groupIdMap.get("moduleName");
      final String logLevel = (String)groupIdMap.get("logLevel");
      final String businessApplicationName = (String)groupIdMap.get("businessApplicationName");
      final Number batchJobId = (Number)groupIdMap.get("batchJobId");
      final AppLog log = new AppLog(logLevel);
      log.info("Group Execution Start " + batchJobId + "\t" + groupId);
      try {
        final StopWatch groupStopWatch = new StopWatch("Group");
        groupStopWatch.start();

        final Map<String, Object> groupResponse = new NamedLinkedHashMap<String, Object>(
          "ExecutionGroupResults");
        groupResponse.put("batchJobId", batchJobId);
        groupResponse.put("groupId", groupId);

        final Long moduleTime = ((Number)groupIdMap.get("moduleTime")).longValue();
        final BusinessApplication businessApplication = executor.getBusinessApplication(
          log, moduleName, moduleTime, businessApplicationName);
        if (businessApplication == null) {
          executor.addFailedGroup(groupId);
        } else {
          final String userId = (String)groupIdMap.get("userId");
          final String groupUrl = httpClient.getUrl("/worker/workers/"
            + workerId + "/jobs/" + batchJobId + "/groups/" + groupId);
          final Map<String, Object> group = httpClient.getJsonResource(groupUrl);
          final Module module = businessApplication.getModule();

          final Map<String, Object> globalErrors = new LinkedHashMap<String, Object>();

          final DataObjectMetaData requestMetaData = businessApplication.getRequestMetaData();
          final Map<String, Object> applicationParameters = new HashMap<String, Object>(
            (Map<String, Object>)group.get("applicationParameters"));
          for (final String name : requestMetaData.getAttributeNames()) {
            final Object value = applicationParameters.get(name);
            if (value != null) {
              try {
                final DataType dataType = requestMetaData.getAttributeType(name);
                final Object convertedValue = StringConverterRegistry.toObject(
                  dataType, value);
                applicationParameters.put(name, convertedValue);
              } catch (final Throwable e) {
                addError(globalErrors, "BAD_INPUT_DATA_VALUE", e);
              }
            }
          }
          final List<Map<String, Object>> requests = (List<Map<String, Object>>)group.get("requests");

          final List<Map<String, Object>> groupResults = new ArrayList<Map<String, Object>>();
          groupResponse.put("results", groupResults);

          for (final Map<String, Object> requestParameters : requests) {
            final StopWatch requestStopWatch = new StopWatch("Request");
            requestStopWatch.start();
            final Map<String, Object> parameters = new LinkedHashMap<String, Object>(
              applicationParameters);
            parameters.putAll(requestParameters);
            final String structuredInputDataString = (String)parameters.remove("structuredInputData");
            if (structuredInputDataString != null) {
              final DataObject structuredInputData = JsonDataObjectIoFactory.toDataObject(
                requestMetaData, structuredInputDataString);
              for (final Entry<String, Object> entry : structuredInputData.entrySet()) {
                final String name = entry.getKey();
                final Object value = entry.getValue();
                if (value != null) {
                  parameters.put(name, value);
                }
              }
            }
            final Number requestId = (Number)parameters.remove("requestId");
            final boolean perRequestResultData = businessApplication.isPerRequestResultData();

            final Map<String, Object> requestResult = new LinkedHashMap<String, Object>(
              globalErrors);
            requestResult.put("requestId", requestId);
            requestResult.put("perRequestResultData", perRequestResultData);
            groupResults.add(requestResult);

            PluginAdaptor plugin = null;
            try {
              plugin = module.getBusinessApplicationPlugin(
                businessApplicationName, logLevel);
            } catch (final Throwable e) {
              addError(requestResult, "ERROR_PROCESSING_REQUEST", e);
              log.error("Unable to create plugin " + businessApplicationName, e);
            }

            if (plugin != null && globalErrors.isEmpty()) {
              final AppLog appLog = plugin.getAppLog();
              File resultFile = null;
              OutputStream resultData = null;
              if (businessApplication.isPerRequestInputData()) {
                // final String inputDataUrl = httpClient.getOAuthUrl("GET",
                // "/worker/workers/" + workerId + "/jobs/" + batchJobId
                // + "/groups/" + groupId + "/requests/" + requestId
                // + "/inputData");
                // parameters.put("inputDataUrl", inputDataUrl);
              }
              if (businessApplication.isPerRequestResultData()) {
                resultFile = FileUtil.createTempFile(businessApplicationName,
                  ".bin");
                resultData = new FileOutputStream(resultFile);
                parameters.put("resultData", resultData);
              }
              try {

                if (businessApplication.isSecurityServiceRequired()) {
                  final SecurityService securityService = securityServiceFactory.getSecurityService(
                    module, userId);
                  plugin.setSecurityService(securityService);
                }
                plugin.setParameters(parameters);

                final StopWatch pluginStopWatch = new StopWatch(
                  "Plugin execute");
                pluginStopWatch.start();
                if (appLog.isDebugEnabled()) {
                  appLog.debug("Request Execution Start " + groupId + " "
                    + requestId);
                }
                try {
                  plugin.execute();
                } finally {
                  try {
                    if (pluginStopWatch.isRunning()) {
                      pluginStopWatch.stop();
                    }
                  } catch (IllegalStateException e) {
                  }
                  final long pluginTime = pluginStopWatch.getTotalTimeMillis();
                  requestResult.put("pluginExecutionTime", pluginTime);
                  if (appLog.isDebugEnabled()) {
                    appLog.debug("Request Execution End " + groupId + " "
                      + requestId);
                  }
                }
                final List<Map<String, Object>> results = plugin.getResults();
                final String resultString = JsonMapIoFactory.toString(results);
                requestResult.put("results", resultString);
                sendResultData(batchJobId, groupId, requestId, requestResult,
                  parameters, resultFile, resultData);
              } catch (final IllegalArgumentException e) {
                appLog.error("Request Execution Failed BAD_INPUT_DATA_VALUE"
                  + groupId + " " + requestId, e);
                addError(requestResult, "BAD_INPUT_DATA_VALUE", e);
              } catch (final RecoverableException e) {
                appLog.error("Request Execution Failed RECOVERABLE_EXCEPTION"
                  + groupId + " " + requestId, e);
                addError(requestResult, "RECOVERABLE_EXCEPTION", e);
              } catch (final Throwable e) {
                appLog.error(
                  "Request Execution Failed ERROR_PROCESSING_REQUEST" + groupId
                    + " " + requestId, e);
                addError(requestResult, "ERROR_PROCESSING_REQUEST", e);
              } finally {
                if (resultFile != null) {
                  FileUtil.closeSilent(resultData);
                  resultFile.delete();
                }
              }
              try {
                if (requestStopWatch.isRunning()) {
                  requestStopWatch.stop();
                }
              } catch (IllegalStateException e) {
              }
              requestResult.put("requestExecutionTime",
                requestStopWatch.getTotalTimeMillis());
              requestResult.put("logRecords", appLog.getLogRecords());
            }

          }
        }
        try {
          if (groupStopWatch.isRunning()) {
            groupStopWatch.stop();
          }
        } catch (IllegalStateException e) {
        }
        groupResponse.put("groupExecutionTime",
          groupStopWatch.getTotalTimeMillis());
        groupResponse.put("logRecords", log.getLogRecords());
        final String path = "/worker/workers/" + workerId + "/jobs/"
          + batchJobId + "/groups/" + groupId + "/results";
        @SuppressWarnings("unused")
        final Map<String, Object> submitResponse = httpClient.postJsonResource(
          httpClient.getUrl(path), groupResponse);

      } catch (final Throwable e) {
        LOG.error("Unable to process group " + groupId, e);
        executor.addFailedGroup(groupId);
      } finally {
        log.info("Group execution end " + batchJobId + "\t" + groupId);
      }
    } finally {
      executor.removeExecutingGroupId(groupId);
    }
  }

  public void sendResultData(final Number batchJobId, final String groupId,
    final Number requestId, final Map<String, Object> groupResult,
    final Map<String, Object> parameters, final File resultFile,
    final OutputStream resultData) {
    if (resultData != null) {

      try {
        resultData.flush();
        FileUtil.closeSilent(resultData);
        final String resultDataContentType = (String)parameters.get("resultDataContentType");
        final String resultDataUrl = httpClient.getUrl("/worker/workers/"
          + workerId + "/jobs/" + batchJobId + "/groups/" + groupId
          + "/requests/" + requestId + "/resultData");

        final HttpResponse response = httpClient.postResource(resultDataUrl,
          resultDataContentType, resultFile);
        try {

          final StatusLine statusLine = response.getStatusLine();
          if (statusLine.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new RecoverableException(
              "Result data not accepted by server "
                + statusLine.getStatusCode() + " "
                + statusLine.getReasonPhrase());
          }
        } finally {
          FileUtil.closeSilent(response.getEntity().getContent());
        }
      } catch (final Throwable e) {
        addError(groupResult, "RECOVERABLE_EXCEPTION", e);
      }
    }
  }
}
