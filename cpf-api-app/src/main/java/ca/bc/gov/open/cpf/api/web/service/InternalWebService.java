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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.io.FileUtil;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.io.StringPrinter;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.UrlUtil;

@Controller
public class InternalWebService {
  private BatchJobService batchJobService;

  private ConfigPropertyLoader configPropertyLoader;

  private CpfDataAccessObject dataAccessObject;

  private String webServiceUrl = "http://localhost/cpf";

  private JobController jobController;

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

  @PreDestroy
  public void close() {
    batchJobService = null;
    configPropertyLoader = null;
    dataAccessObject = null;
    jobController = null;
  }

  @RequestMapping("/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/requests/{sequenceNumber}/inputData")
  public void getBatchJobExecutionGroupOpaqueInputData(
    @PathVariable("workerId") final String workerId,
    final HttpServletResponse response, @PathVariable final long batchJobId,
    @PathVariable final long sequenceNumber)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final DataObject batchJobExecutionGroup = dataAccessObject.getBatchJobExecutionGroup(
      batchJobId, sequenceNumber);
    if (batchJobExecutionGroup == null) {
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
        final String inputDataContentType = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE);
        final String inputDataUrl = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_URL);
        if (inputDataUrl != null) {
          response.setStatus(HttpServletResponse.SC_SEE_OTHER);
          response.setHeader("Location", inputDataUrl);
          return;
        } else {
          final Blob inputData = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA);
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
              LoggerFactory.getLogger(InternalWebService.class).error(
                "Unable to load data from database", e);
              throw new HttpMessageNotWritableException(
                "Unable to load data from database", e);
            } catch (final IOException e) {
              LoggerFactory.getLogger(InternalWebService.class).error(
                "Unable to write blob to request", e);
              throw new HttpMessageNotWritableException(
                "Unable to write blob to request", e);
            }
          }
        }
      }
    }
  }

  @RequestMapping(
      value = "/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}")
  @ResponseBody
  public Map<String, Object> getBatchJobRequestExecutionGroup(
    final HttpServletRequest request,
    @PathVariable("workerId") final String workerId,
    @PathVariable("groupId") final String groupId)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final BatchJobRequestExecutionGroup group = batchJobService.getBatchJobRequestExecutionGroup(
      workerId, groupId);
    if (group == null || group.isCancelled()) {
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
      if (module == null || !module.isStarted()) {
        batchJobService.schedule(group);
      } else {
        final String moduleName = module.getName();
        groupSpecification.put("moduleName", moduleName);
        if (module.isRemoteable()) {
          groupSpecification.put("moduleTime", module.getStartedTime());
        }
        groupSpecification.put("businessApplicationName",
          businessApplication.getName());
        groupSpecification.put("applicationParameters",
          group.getBusinessApplicationParameterMap());
        if (businessApplication.isPerRequestResultData()) {
          groupSpecification.put("resultDataContentType",
            group.getResultDataContentType());
        }

        final long groupSequenceNumber = group.getSequenceNumber();
        if (businessApplication.isPerRequestInputData()) {
          final DataObject executionGroup = dataAccessObject.getBatchJobExecutionGroup(
            batchJobId, groupSequenceNumber);
          final List<Map<String, Object>> requestParameterList = new ArrayList<Map<String, Object>>();
          groupSpecification.put("requests", requestParameterList);
          final Map<String, Object> requestParameters = new HashMap<String, Object>();
          requestParameters.put("sequenceNumber", groupSequenceNumber);
          requestParameters.put(
            "inputDataContentType",
            executionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE));
          requestParameterList.add(requestParameters);
        } else {
          final String structuredInputData = jobController.getStructuredInputData(
            batchJobId, groupSequenceNumber);
          if (structuredInputData.charAt(0) == '{') {
            groupSpecification.put("requests", new StringPrinter(
              structuredInputData));
          } else {
            groupSpecification.put("requests", structuredInputData);
          }
        }
      }
      return groupSpecification;
    }
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

  @RequestMapping(
      value = "/worker/modules/{moduleName}/config/{environmentName}/{componentName}")
  @ResponseBody
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
  public void getModuleUrl(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final Long moduleTime, @PathVariable final int urlId)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkRunning();
    final Module module = batchJobService.getModule(moduleName);
    if (module == null || !module.isStarted()
      || module.getStartedTime() != moduleTime) {
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
  public Map<String, Object> getModuleUrls(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final Long moduleTime)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkRunning();
    final BusinessApplicationRegistry businessApplicationRegistry = batchJobService.getBusinessApplicationRegistry();
    final Module module = businessApplicationRegistry.getModule(moduleName);
    if (module == null || !module.isStarted()
      || module.getStartedTime() != moduleTime) {
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

  public String getWebServiceUrl() {
    return webServiceUrl;
  }

  @RequestMapping("/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/requests/{requestSequenceNumber}/resultData")
  @ResponseBody
  public Map<String, ? extends Object> postBatchJobExecutionGroupOpaqueOutputData(
    @PathVariable("workerId") final String workerId,
    @PathVariable final Long batchJobId, @PathVariable final String groupId,
    @PathVariable final long requestSequenceNumber, final InputStream in)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final BatchJobRequestExecutionGroup group = batchJobService.getBatchJobRequestExecutionGroup(
      workerId, groupId);

    if (group != null) {
      synchronized (group) {
        if (!group.isCancelled()) {
          final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);
          final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
          final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
          if (businessApplication != null
            && businessApplication.isPerRequestResultData()) {
            synchronized (group) {
              final String resultDataContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
              final File file = FileUtil.createTempFile("result", ".bin");

              try {
                FileUtil.copy(in, file);
                if (!group.isCancelled()) {
                  batchJobService.createBatchJobResultOpaque(batchJobId,
                    requestSequenceNumber, resultDataContentType, file);
                }
              } finally {
                FileUtil.closeSilent(in);
                FileUtil.deleteDirectory(file);
              }
            }
          }
        }
      }
    }
    final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
      "OpaqueOutputDataResults");
    map.put("workerId", workerId);
    map.put("batchJobId", batchJobId);
    map.put("groupId", groupId);
    map.put("requestSequenceNumber", requestSequenceNumber);
    return map;

  }

  @RequestMapping(
      value = "/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/results",
      method = RequestMethod.POST)
  @ResponseBody
  public Map<String, ? extends Object> postBatchJobRequestExecutionGroupResults(
    @PathVariable("workerId") final String workerId,
    @PathVariable("batchJobId") final String batchJobId,
    @PathVariable("groupId") final String groupId,
    @RequestBody final Map<String, Object> results)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
      "ExecutionGroupResultsConfirmation");
    batchJobService.setBatchJobExecutionGroupResults(workerId, groupId, results);
    map.put("workerId", workerId);
    map.put("batchJobId", batchJobId);
    map.put("groupId", groupId);
    return map;
  }

  @RequestMapping(value = "/worker/workers/{workerId}/jobs/groups/nextId",
      method = RequestMethod.POST)
  @ResponseBody
  public Map<String, Object> postNextBatchJobExecutionGroupId(
    @PathVariable("workerId") final String workerId,
    @RequestParam final long workerStartTime,
    @RequestParam(value = "moduleName", required = false) final List<String> moduleNames,
    @RequestParam(value = "maxMessageId", required = false, defaultValue = "0") final int maxMessageId) {
    Map<String, Object> response = Collections.emptyMap();
    final BatchJobService batchJobService = this.batchJobService;
    if (batchJobService != null) {
      checkRunning();
      try {
        batchJobService.setWorkerConnected(workerId, workerStartTime, true);
        response = batchJobService.getNextBatchJobRequestExecutionGroup(
          workerId, maxMessageId, moduleNames);
      } catch (final Throwable e) {
        LoggerFactory.getLogger(InternalWebService.class).error(e.getMessage(),
          e);
        throw new HttpMessageNotWritableException(
          "Unable to get execution group id", e);
      } finally {
        batchJobService.setWorkerConnected(workerId, workerStartTime, false);
      }
    }
    return response;
  }

  @RequestMapping(value = "/worker/workers/{workerId}/message",
      method = RequestMethod.POST)
  @ResponseBody
  public Map<String, Object> postWorkerMessage(
    @PathVariable("workerId") final String workerId,
    @RequestParam final long workerStartTime,
    @RequestBody final Map<String, Object> message) {
    checkRunning();
    try {
      return batchJobService.processWorkerMessage(workerId, workerStartTime,
        message);
    } catch (final Throwable e) {
      LoggerFactory.getLogger(InternalWebService.class)
        .error(e.getMessage(), e);
      throw new HttpMessageNotWritableException("Unable to process message", e);
    }
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/users/{consumerKey}/resourcePermission")
  @ResponseBody
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
    this.dataAccessObject = batchJobService.getDataAccessObject();
    this.jobController = batchJobService.getjobController();
  }

  public void setConfigPropertyLoader(
    final ConfigPropertyLoader configPropertyLoader) {
    this.configPropertyLoader = configPropertyLoader;
  }

  public void setWebServiceUrl(final String webServiceUrl) {
    this.webServiceUrl = webServiceUrl;
  }

}
