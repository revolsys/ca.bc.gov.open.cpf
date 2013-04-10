package ca.bc.gov.open.cpf.api.web.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobRequest;
import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.log.ModuleLog;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.io.FileUtil;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.UrlUtil;

@Controller
public class InternalWebService {
  private static final Logger LOG = LoggerFactory.getLogger(InternalWebService.class);

  private BatchJobService batchJobService;

  private ConfigPropertyLoader configPropertyLoader;

  private CpfDataAccessObject dataAccessObject;

  private void addConfigProperties(
    final Map<String, Map<String, Object>> configProperties,
    final String environmentName, final String moduleName,
    final String componentName) {
    final List<DataObject> properties = dataAccessObject.getConfigPropertiesForModule(
      environmentName, moduleName, componentName);
    for (final DataObject configProperty : properties) {
      final String propertyName = configProperty.getValue(ConfigProperty.PROPERTY_NAME);
      configProperties.put(propertyName, configProperty);
    }
  }

  private void checkRunning() {
    if (!batchJobService.isRunning()) {
      throw new IllegalStateException("Application is not running");
    }
  }

  @RequestMapping(
      value = "/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}")
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> getBatchJobRequestExecutionGroup(
    final HttpServletRequest request,
    @PathVariable("workerId") final String workerId,
    @PathVariable("groupId") final String groupId)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final BatchJobRequestExecutionGroup group = batchJobService.getBatchJobRequestExecutionGroup(
      workerId, groupId);
    if (group == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final Long batchJobId = group.getBatchJobId();
      final Map<String, Object> groupSpecification = new LinkedHashMap<String, Object>();
      final BusinessApplication businessApplication = group.getBusinessApplication();
      groupSpecification.put("workerId", workerId);
      groupSpecification.put("groupId", groupId);
      groupSpecification.put("consumerKey", group.getconsumerKey());
      groupSpecification.put("batchJobId", batchJobId);
      final Module module = businessApplication.getModule();
      if (module == null) {
        batchJobService.schedule(group);
      } else {
        final String moduleName = module.getName();
        groupSpecification.put("moduleName", moduleName);
        if (module.isRemoteable()) {
          groupSpecification.put("moduleTime", module.getStartedDate()
            .getTime());
        }
        groupSpecification.put("businessApplicationName",
          businessApplication.getName());
        groupSpecification.put("applicationParameters",
          group.getBusinessApplicationParameterMap());

        final List<Long> requestIds = group.getBatchJobRequestIds();
        final List<Map<String, Object>> requestParameterList = new ArrayList<Map<String, Object>>();
        groupSpecification.put("requests", requestParameterList);
        for (final DataObject jobRequest : dataAccessObject.getBatchJobRequests(requestIds)) {
          final long requestId = DataObjectUtil.getLong(jobRequest,
            BatchJobRequest.BATCH_JOB_REQUEST_ID);
          final Map<String, Object> requestParameters = new HashMap<String, Object>();
          requestParameters.put("requestId", requestId);
          if (businessApplication.isPerRequestInputData()) {
            requestParameters.put("inputDataContentType",
              jobRequest.getValue(BatchJobRequest.INPUT_DATA_CONTENT_TYPE));
          } else {
            final String structuredInputData = jobRequest.getString(BatchJobRequest.STRUCTURED_INPUT_DATA);
            requestParameters.put("structuredInputData", structuredInputData);
          }
          if (businessApplication.isPerRequestResultData()) {
            requestParameters.put("resultDataContentType",
              group.getResultDataContentType());

          }
          requestParameterList.add(requestParameters);
        }
      }
      return groupSpecification;
    }
  }

  @RequestMapping("/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/requests/{requestId}/inputData")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void getBatchJobRequestOpaqueInputData(
    @PathVariable("workerId") final String workerId,
    final HttpServletResponse response, @PathVariable final long batchJobId,
    @PathVariable final long requestId)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final DataObject batchJobRequest = dataAccessObject.getBatchJobRequest(requestId);
    if (batchJobRequest == null) {
      throw new NoSuchRequestHandlingMethodException(
        HttpServletUtils.getRequest());
    } else {
      final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
      final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
      final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
      if (businessApplication == null
        || !businessApplication.isPerRequestInputData()) {
        throw new NoSuchRequestHandlingMethodException(
          HttpServletUtils.getRequest());
      } else {
        final String inputDataContentType = batchJobRequest.getValue(BatchJobRequest.INPUT_DATA_CONTENT_TYPE);
        final String inputDataUrl = batchJobRequest.getValue(BatchJobRequest.INPUT_DATA_URL);
        if (inputDataUrl != null) {
          response.setStatus(HttpServletResponse.SC_SEE_OTHER);
          response.setHeader("Location", inputDataUrl);
          return;
        } else {
          final Blob inputData = batchJobRequest.getValue(BatchJobRequest.INPUT_DATA);
          if (inputData == null) {
            throw new NoSuchRequestHandlingMethodException(
              HttpServletUtils.getRequest());
          } else {
            try {
              response.setContentType(inputDataContentType);
              final InputStream in = inputData.getBinaryStream();
              final OutputStream out = response.getOutputStream();
              FileUtil.copy(in, out);
              return;
            } catch (final SQLException e) {
              LOG.error("Unable to load data from database", e);
              throw new HttpMessageNotWritableException(
                "Unable to load data from database", e);
            } catch (final IOException e) {
              LOG.error("Unable to write blob to request", e);
              throw new HttpMessageNotWritableException(
                "Unable to write blob to request", e);
            }
          }
        }
      }
    }
  }

  public BatchJobService getBatchJobService() {
    return batchJobService;
  }

  public Collection<Map<String, Object>> getConfigProperties(
    final String environmentName, final String moduleName,
    final String componentName) {
    final Map<String, Map<String, Object>> configProperties = new HashMap<String, Map<String, Object>>();
    addConfigProperties(configProperties, ConfigProperty.DEFAULT, moduleName,
      componentName);
    addConfigProperties(configProperties, environmentName, moduleName,
      componentName);
    return configProperties.values();
  }

  public ConfigPropertyLoader getConfigPropertyLoader() {
    return configPropertyLoader;
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/config/{environmentName}/{componentName}")
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Map<String, ? extends Object> getModuleBeanConfigProperties(
    @PathVariable final String moduleName,
    @PathVariable final String environmentName,
    @PathVariable final String componentName) {
    checkRunning();

    final Collection<Map<String, Object>> applicationConfigProperties = new ArrayList<Map<String, Object>>();
    if (configPropertyLoader != null) {
      final Collection<Map<String, Object>> configProperties = getConfigProperties(
        environmentName, moduleName, componentName);
      if (configProperties != null) {
        applicationConfigProperties.addAll(configProperties);
      }
    }
    final NamedLinkedHashMap<String, Object> map = new NamedLinkedHashMap<String, Object>(
      "EnvironmentConfiguration");
    map.put("environmentName", environmentName);
    map.put("moduleName", moduleName);
    map.put("componentName", moduleName);
    map.put("properties", applicationConfigProperties);

    return map;
  }

  @RequestMapping(value = {
    "/worker/modules/{moduleName}/{moduleTime}/urls/{urlId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void getModuleUrl(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final Long moduleTime, @PathVariable final int urlId)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkRunning();
    final Module module = batchJobService.getModule(moduleName);
    if (module == null || module.getStartedDate().getTime() != moduleTime) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final List<URL> jarUrls = module.getJarUrls();
      final URL url = jarUrls.get(urlId);
      final InputStream in = UrlUtil.getInputStream(url);
      try {
        final OutputStream out = response.getOutputStream();
        try {
          FileUtil.copy(in, out);
        } finally {
          FileUtil.closeSilent(out);
        }
      } finally {
        FileUtil.closeSilent(in);
      }

    }
  }

  @RequestMapping(value = {
    "/worker/modules/{moduleName}/{moduleTime}/urls"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Map<String, Object> getModuleUrls(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final Long moduleTime)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkRunning();
    final BusinessApplicationRegistry businessApplicationRegistry = batchJobService.getBusinessApplicationRegistry();
    final Module module = businessApplicationRegistry.getModule(moduleName);
    if (module == null || module.getStartedDate().getTime() != moduleTime) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final List<URL> jarUrls = module.getJarUrls();
      final String url = webServiceUrl + "/worker/modules/" + moduleName + "/"
        + moduleTime + "/urls/";
      final List<String> webServiceJarUrls = new ArrayList<String>();
      for (int i = 0; i < jarUrls.size(); i++) {
        webServiceJarUrls.add(url + i);
      }
      final Map<String, Object> result = new NamedLinkedHashMap<String, Object>(
        "ModuleUrls");
      result.put("jarUrls", webServiceJarUrls);
      return result;
    }
  }

  private String webServiceUrl = "http://localhost/cpf";

  public void setWebServiceUrl(String webServiceUrl) {
    this.webServiceUrl = webServiceUrl;
  }

  public String getWebServiceUrl() {
    return webServiceUrl;
  }

  @RequestMapping(
      value = "/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/results",
      method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, ? extends Object> postBatchJobRequestExecutionGroupResults(
    @PathVariable("workerId") final String workerId,
    @PathVariable("batchJobId") final String batchJobId,
    @PathVariable("groupId") final String groupId,
    @RequestBody final Map<String, Object> results)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
      "ExecutionGroupResultsConfirmation");
    map.put("workerId", workerId);
    map.put("batchJobId", batchJobId);
    map.put("groupId", groupId);
    batchJobService.setBatchJobExecutionGroupResults(workerId, groupId, results);
    return map;
  }

  @RequestMapping("/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/requests/{requestId}/resultData")
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, ? extends Object> postBatchJobRequestOpaqueOutputData(
    @PathVariable("workerId") final String workerId,
    @PathVariable final Long batchJobId, @PathVariable final String groupId,
    @PathVariable final long requestId, final InputStream in)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final BatchJobRequestExecutionGroup group = batchJobService.getBatchJobRequestExecutionGroup(
      workerId, groupId);

    final DataObject batchJobRequest = dataAccessObject.getBatchJobRequestLocked(requestId);
    if (batchJobRequest == null || group == null) {
      throw new NoSuchRequestHandlingMethodException(
        HttpServletUtils.getRequest());
    } else {
      synchronized (group) {
        final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
        final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
        final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
        if (businessApplication == null
          || !businessApplication.isPerRequestResultData()) {
          throw new NoSuchRequestHandlingMethodException(
            HttpServletUtils.getRequest());
        } else {
          final String resultDataContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

          final File file = FileUtil.createTempFile("result", ".bin");

          try {
            FileUtil.copy(in, file);
            batchJobService.createBatchJobResultOpaque(batchJobId, requestId,
              resultDataContentType, file);
          } finally {
            FileUtil.closeSilent(in);
            file.delete();
          }
          final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
            "OpaqueOutputDataResults");
          map.put("workerId", workerId);
          map.put("batchJobId", batchJobId);
          map.put("groupId", groupId);
          map.put("requestId", requestId);
          return map;
        }
      }
    }
  }

  @RequestMapping(value = "/worker/workers/{workerId}/jobs/groups/nextId",
      method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> postNextBatchJobExecutionGroupId(
    @PathVariable("workerId") final String workerId, @RequestParam(
        value = "moduleName", required = false) final List<String> moduleNames) {
    checkRunning();
    try {
      batchJobService.setWorkerConnected(workerId, true);
      final Map<String, Object> response = batchJobService.getNextBatchJobRequestExecutionGroup(
        workerId, moduleNames);
      return response;
    } catch (final Throwable e) {
      LOG.error(e.getMessage(), e);
      throw new HttpMessageNotWritableException(
        "Unable to get execution group id", e);
    } finally {
      batchJobService.setWorkerConnected(workerId, false);
    }
  }

  @RequestMapping(value = "/worker/workers/{workerId}/message",
      method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> postWorkerMessage(
    @PathVariable("workerId") final String workerId,
    @RequestBody final Map<String, Object> message) {
    checkRunning();
    try {
      batchJobService.setWorkerConnected(workerId, true);
      try {
        final Map<String, Object> response = new NamedLinkedHashMap<String, Object>(
          "MessageResponse");
        response.put("workerId", "workerId");
        final Worker worker = batchJobService.getWorker(workerId);
        if (worker != null) {
          final String action = (String)message.get("action");
          if (action != null) {
            if ("executingGroupIds".equals(action)) {
              @SuppressWarnings("unchecked")
              final List<String> executingGroupIds = (List<String>)message.get("executingGroupIds");
              batchJobService.updateWorkerExecutingGroups(worker,
                executingGroupIds);
            } else if ("failedGroupId".equals(action)) {
              final String groupId = (String)message.get("groupId");
              batchJobService.cancelGroup(worker, groupId);
            } else if ("moduleExcluded".equals(action)) {
              setModuleExcluded(worker, message);
            } else if ("moduleLoaded".equals(action)) {
              setModuleLoaded(worker, message);
            }  else if ("moduleLoading".equals(action)) {
              worker.addLoadingModule(message);
            }
          }
          response.put("errorMessage", "Unknown message");
          response.put("message", message);
        }
        return response;
      } catch (final Throwable e) {
        LOG.error(e.getMessage(), e);
        throw new HttpMessageNotWritableException("Unable to process message",
          e);
      }
    } finally {
      batchJobService.setWorkerConnected(workerId, false);
    }
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/users/{consumerKey}/resourcePermission")
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Map<String, ? extends Object> securityCanAccessResource(
    final HttpServletRequest request, @PathVariable final String moduleName,
    @PathVariable final String consumerKey,
    @RequestParam final String resourceClass,
    @RequestParam final String resourceId, @RequestParam final String actionName)
    throws ServletException {
    checkRunning();
    final Module module = batchJobService.getModule(moduleName);
    if (module == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final SecurityService securityService = batchJobService.getSecurityService(
        module, consumerKey);
      final boolean hasAccess = securityService.canAccessResource(
        resourceClass, resourceId, actionName);
      final NamedLinkedHashMap<String, Object> map = new NamedLinkedHashMap<String, Object>(
        "ResourcePermission");
      map.put("moduleName", moduleName);
      map.put("consumerKey", consumerKey);
      map.put("resourceClass", resourceClass);
      map.put("resourceId", resourceId);
      map.put("actionName", actionName);
      map.put("hasAccess", hasAccess);
      return map;
    }
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/users/{consumerKey}/actions/{actionName}/hasAccess")
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Map<String, ? extends Object> securityCanPerformAction(
    final HttpServletRequest request,
    @PathVariable("moduleName") final String moduleName,
    @PathVariable("consumerKey") final String consumerKey,
    @PathVariable("actionName") final String actionName)
    throws ServletException {
    checkRunning();
    final Module module = batchJobService.getModule(moduleName);
    if (module == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final SecurityService securityService = batchJobService.getSecurityService(
        module, consumerKey);
      final boolean hasAccess = securityService.canPerformAction(actionName);
      final NamedLinkedHashMap<String, Object> map = new NamedLinkedHashMap<String, Object>(
        "ActionPermission");
      map.put("moduleName", moduleName);
      map.put("consumerKey", consumerKey);
      map.put("actionName", actionName);
      map.put("hasAccess", hasAccess);
      return map;
    }
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/users/{consumerKey}/groups/{groupName}/memberOf")
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Map<String, ? extends Object> securityIsMemberOfGroup(
    final HttpServletRequest request,
    @PathVariable("moduleName") final String moduleName,
    @PathVariable("consumerKey") final String consumerKey,
    @PathVariable("groupName") final String groupName) throws ServletException {
    checkRunning();
    final Module module = batchJobService.getModule(moduleName);
    if (module == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final SecurityService securityService = batchJobService.getSecurityService(
        module, consumerKey);
      final boolean inGroup = securityService.isInGroup(groupName);
      final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
        "GroupMembership");
      map.put("moduleName", moduleName);
      map.put("consumerKey", consumerKey);
      map.put("groupName", groupName);
      map.put("memberOfGroup", inGroup);
      return map;
    }
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/users/{consumerKey}/attributes")
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Map<String, Object> securityUserAttributes(
    final HttpServletRequest request,
    @PathVariable("moduleName") final String moduleName,
    @PathVariable("consumerKey") final String consumerKey)
    throws ServletException {
    checkRunning();
    final Module module = batchJobService.getModule(moduleName);
    if (module == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final SecurityService securityService = batchJobService.getSecurityService(
        module, consumerKey);
      final Map<String, Object> userAttributes = new NamedLinkedHashMap<String, Object>(
        "UserAttributes", securityService.getUserAttributes());
      return userAttributes;
    }
  }

  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }

  public void setConfigPropertyLoader(
    final ConfigPropertyLoader configPropertyLoader) {
    this.configPropertyLoader = configPropertyLoader;
  }

  @Resource(name = "cpfDataAccessObject")
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

  public void setModuleExcluded(final Worker worker,
    final Map<String, Object> message) {
    final String moduleName = (String)message.get("moduleName");
    final Number moduleTime = (Number)message.get("moduleTime");
    final String moduleNameTime = moduleName + ":" + moduleTime;
    worker.addExcludedModule(moduleNameTime);
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> logRecords = (List<Map<String, Object>>)message.get("logRecords");
    if (logRecords != null && !logRecords.isEmpty()) {
      final Map<String, Object> appLogData = new LinkedHashMap<String, Object>();
      appLogData.put("moduleName", moduleName);
      appLogData.put("moduleTime", moduleTime);
      appLogData.put("workerId", worker);
      appLogData.put("logRecords", logRecords);
      ModuleLog.info(moduleName, "Module failed to load ", "Application Log",
        appLogData);
    }
  }

  private void setModuleLoaded(final Worker worker,
    final Map<String, Object> message) {
    final String moduleName = (String)message.get("moduleName");
    final Number moduleTime = (Number)message.get("moduleTime");
    final String moduleNameTime = moduleName + ":" + moduleTime;
    worker.addLoadedModule(moduleNameTime);
  }
 
}
