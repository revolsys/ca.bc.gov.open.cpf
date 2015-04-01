package ca.bc.gov.open.cpf.api.web.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.api.scheduler.WorkerModuleState;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEvent;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEventListener;

import com.revolsys.collection.map.Maps;
import com.revolsys.data.record.Record;
import com.revolsys.util.Property;
import com.revolsys.websocket.json.JsonDecoder;
import com.revolsys.websocket.json.JsonEncoder;

@ServerEndpoint(value = "/worker/workers/{workerId}/{startTime}/message",
    encoders = JsonEncoder.class, decoders = JsonDecoder.class)
public class WorkerMessageHandler implements ModuleEventListener {

  private BusinessApplicationRegistry businessApplicationRegistry;

  private BatchJobService batchJobService;

  public WorkerMessageHandler() {
  }

  private void addConfigProperties(final Map<String, Map<String, Object>> configProperties,
    final String environmentName, final String moduleName, final String componentName) {
    final CpfDataAccessObject dataAccessObject = this.batchJobService.getDataAccessObject();
    final List<Record> properties = dataAccessObject.getConfigPropertiesForModule(environmentName,
      moduleName, componentName);
    for (final Record configProperty : properties) {
      final String propertyName = configProperty.getValue(ConfigProperty.PROPERTY_NAME);
      configProperties.put(propertyName, configProperty);
    }
  }

  private Map<String, Object> createResultMessage(final Map<String, Object> message) {
    final String messageId = Maps.getString(message, "messageId");
    final Map<String, Object> resultMessage = Maps.create("messageId", messageId);
    return resultMessage;
  }

  public void executingGroupIds(final Map<String, Object> message, final Worker worker) {
    @SuppressWarnings("unchecked")
    final List<String> executingGroupIds = (List<String>)message.get("executingGroupIds");
    this.batchJobService.updateWorkerExecutingGroups(worker, executingGroupIds);
  }

  public void failedGroupId(final Map<String, Object> message, final Worker worker) {
    final String groupId = (String)message.get("groupId");
    this.batchJobService.cancelGroup(worker, groupId);
  }

  private Collection<Map<String, Object>> getConfigProperties(final String environmentName,
    final String moduleName, final String componentName) {
    final Map<String, Map<String, Object>> configProperties = new HashMap<String, Map<String, Object>>();
    addConfigProperties(configProperties, ConfigProperty.DEFAULT, moduleName, componentName);
    addConfigProperties(configProperties, environmentName, moduleName, componentName);
    return configProperties.values();
  }

  public boolean isModuleEnabled(final String moduleName) {
    final Module module = this.batchJobService.getModule(moduleName);
    final boolean enabled = module != null && module.isEnabled();
    return enabled;
  }

  @Override
  public void moduleChanged(final ModuleEvent event) {
    final String type = event.getAction();
    final Map<String, Object> message = Maps.create("type", type);

    final String moduleName = event.getModuleName();
    message.put("moduleName", moduleName);

    final long moduleTime = event.getModuleTime();
    message.put("moduleTime", moduleTime);

    for (final Worker worker : this.batchJobService.getWorkers()) {
      worker.sendMessage(message);
    }
  }

  public void moduleConfigLoad(final Map<String, Object> message, final Worker worker) {
    final String moduleName = Maps.getString(message, "moduleName");
    final String environmentName = Maps.getString(message, "environmentName");
    final String componentName = Maps.getString(message, "componentName");
    final Collection<Map<String, Object>> applicationConfigProperties = new ArrayList<Map<String, Object>>();
    final Collection<Map<String, Object>> configProperties = getConfigProperties(environmentName,
      moduleName, componentName);
    if (configProperties != null) {
      applicationConfigProperties.addAll(configProperties);
    }
    final Map<String, Object> resultMessage = createResultMessage(message);
    resultMessage.put("properties", applicationConfigProperties);
    worker.sendMessage(resultMessage);
  }

  public void moduleDisabled(final Map<String, Object> message, final Worker worker) {
    final String moduleName = (String)message.get("moduleName");
    final boolean enabled = isModuleEnabled(moduleName);
    final WorkerModuleState moduleState = worker.getModuleState(moduleName);
    moduleState.setEnabled(enabled);
    moduleState.setStatus("Disabled");
    moduleState.setStartedTime(0);
  }

  public void moduleStarted(final Map<String, Object> message, final Worker worker) {
    final String moduleName = Maps.getString(message, "moduleName");
    final boolean enabled = isModuleEnabled(moduleName);
    final WorkerModuleState moduleState = worker.getModuleState(moduleName);
    moduleState.setEnabled(enabled);
    moduleState.setStatus("Started");
    final long moduleTime = Maps.getLong(message, "moduleTime");
    moduleState.setStartedTime(moduleTime);
  }

  public void moduleStartFailed(final Map<String, Object> message, final Worker worker) {
    final String moduleName = (String)message.get("moduleName");
    final boolean enabled = isModuleEnabled(moduleName);
    final WorkerModuleState moduleState = worker.getModuleState(moduleName);
    moduleState.setEnabled(enabled);
    moduleState.setStatus("Start Failed");
    final String moduleError = (String)message.get("moduleError");
    moduleState.setModuleError(moduleError);
    moduleState.setStartedTime(0);
  }

  public void moduleStopped(final Map<String, Object> message, final Worker worker) {
    final String moduleName = (String)message.get("moduleName");
    final boolean enabled = isModuleEnabled(moduleName);
    final WorkerModuleState moduleState = worker.getModuleState(moduleName);
    moduleState.setEnabled(enabled);
    if (enabled) {
      moduleState.setStatus("Stopped");
    } else {
      moduleState.setStatus("Disabled");
    }
    moduleState.setStartedTime(0);
  }

  @OnClose
  public void onClose(@PathParam("workerId") final String workerId,
    @PathParam("startTime") final long workerStartTime, final Session session) {
    this.batchJobService.setWorkerDisconnected(workerId, workerStartTime, session);
  }

  @OnMessage
  public void onMessage(@PathParam("workerId") final String workerId,
    final Map<String, Object> message) {
    final Worker worker = this.batchJobService.getWorker(workerId);
    if (worker != null) {
      final String type = Maps.getString(message, "type");
      try {
        Property.invoke(this, type, message, worker);
      } catch (final Throwable e) {
        LoggerFactory.getLogger(getClass()).error("Unable to handle message: " + message, e);
      }
    }
  }

  @OnOpen
  public void onOpen(@PathParam("workerId") final String workerId,
    @PathParam("startTime") final long workerStartTime, final Session session) {
    if (this.businessApplicationRegistry == null) {
      final WebApplicationContext wac = (WebApplicationContext)ContextLoader.getCurrentWebApplicationContext()
        .getServletContext()
        .getAttribute("org.springframework.web.servlet.FrameworkServlet.CONTEXT.cpf");
      this.businessApplicationRegistry = wac.getBean(BusinessApplicationRegistry.class);
      this.businessApplicationRegistry.addModuleEventListener(this);
      this.batchJobService = wac.getBean(BatchJobService.class);
    }
    this.batchJobService.setWorkerConnected(workerId, workerStartTime, session);
  }

  @RequestMapping(value = "/worker/modules/{moduleName}/users/{consumerKey}/resourcePermission")
  @ResponseBody
  public void securityCanAccessResource(final Map<String, Object> message, final Worker worker) {
    final String moduleName = Maps.getString(message, "moduleName");
    final String consumerKey = Maps.getString(message, "consumerKey");
    final String resourceClass = Maps.getString(message, "resourceClass");
    final String resourceId = Maps.getString(message, "resourceId");
    final String actionName = Maps.getString(message, "actionName");

    final Map<String, Object> resultMessage = createResultMessage(message);
    final Module module = this.batchJobService.getModule(moduleName);
    if (module != null) {
      final SecurityService securityService = this.batchJobService.getSecurityService(module,
        consumerKey);
      final boolean hasAccess = securityService.canAccessResource(resourceClass, resourceId,
        actionName);
      resultMessage.put("hasAccess", hasAccess);
    }
    worker.sendMessage(resultMessage);
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/users/{consumerKey}/actions/{actionName}/hasAccess")
  @ResponseBody
  public void securityCanPerformAction(final Map<String, Object> message, final Worker worker) {
    final String moduleName = Maps.getString(message, "moduleName");
    final String consumerKey = Maps.getString(message, "consumerKey");
    final String actionName = Maps.getString(message, "actionName");

    final Map<String, Object> resultMessage = createResultMessage(message);
    final Module module = this.batchJobService.getModule(moduleName);
    if (module != null) {
      final SecurityService securityService = this.batchJobService.getSecurityService(module,
        consumerKey);
      final boolean hasAccess = securityService.canPerformAction(actionName);
      resultMessage.put("hasAccess", hasAccess);
    }
    worker.sendMessage(resultMessage);
  }

  @RequestMapping(
      value = "/worker/modules/{moduleName}/users/{consumerKey}/groups/{groupName}/memberOf")
  @ResponseBody
  public void securityIsMemberOfGroup(final Map<String, Object> message, final Worker worker) {
    final String moduleName = Maps.getString(message, "moduleName");
    final String consumerKey = Maps.getString(message, "consumerKey");
    final String groupName = Maps.getString(message, "groupName");

    final Map<String, Object> resultMessage = createResultMessage(message);
    final Module module = this.batchJobService.getModule(moduleName);
    if (module != null) {
      final SecurityService securityService = this.batchJobService.getSecurityService(module,
        consumerKey);
      final boolean inGroup = securityService.isInGroup(groupName);
      resultMessage.put("memberOfGroup", inGroup);
    }
    worker.sendMessage(resultMessage);
  }

  public void securityUserAttributes(final Map<String, Object> message, final Worker worker) {
    final String moduleName = Maps.getString(message, "moduleName");
    final String consumerKey = Maps.getString(message, "consumerKey");

    final Map<String, Object> resultMessage = createResultMessage(message);
    final Module module = this.batchJobService.getModule(moduleName);
    if (module != null) {
      final SecurityService securityService = this.batchJobService.getSecurityService(module,
        consumerKey);
      resultMessage.put("attributes", securityService.getUserAttributes());
    }
    worker.sendMessage(resultMessage);
  }
}
