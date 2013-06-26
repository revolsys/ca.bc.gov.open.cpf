package ca.bc.gov.open.cpf.plugin.impl;

import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.log.ModuleLog;
import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModuleLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.MockSecurityService;
import ca.bc.gov.open.cpf.plugin.impl.security.MockSecurityServiceFactory;
import ca.bc.gov.open.cpf.plugin.impl.security.SecurityServiceFactory;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.io.json.JsonDataObjectIoFactory;

/**
 * The BusinessApplicationPluginExecutor can be used to simulate running a
 * {@link BusinessApplicationPlugin} with the CPF. This can be used to test the
 * business application without deploying it to the CPF.
 */
public class BusinessApplicationPluginExecutor {
  private BusinessApplicationRegistry businessApplicationRegistry;

  private String consumerKey = "cpftest";

  private final SecurityServiceFactory securityServiceFactory = new MockSecurityServiceFactory();

  public BusinessApplicationPluginExecutor() {
    final ClassLoader classLoader = Thread.currentThread()
      .getContextClassLoader();
    final ClassLoaderModuleLoader moduleLoader = new ClassLoaderModuleLoader(
      classLoader);
    businessApplicationRegistry = new BusinessApplicationRegistry(false,
      moduleLoader);
  }

  public BusinessApplicationPluginExecutor(
    final BusinessApplicationRegistry businessApplicationRegistry) {
    this.businessApplicationRegistry = businessApplicationRegistry;
  }

  public void close() {
    if (businessApplicationRegistry != null) {
      businessApplicationRegistry.close();
      businessApplicationRegistry = null;
    }
  }

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts and returns
   * structured data.
   * 
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param inputParameters The input parameters to the business application.
   * @return The result parameters from the business application.
   */
  public Map<String, Object> execute(final String businessApplicationName,
    final Map<String, ? extends Object> inputParameters) {
    final PluginAdaptor plugin = getPlugin(businessApplicationName);
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final BusinessApplication businessApplication = plugin.getApplication();
    final Module module = businessApplication.getModule();
    final String moduleName = module.getName();
    ModuleLog.info(moduleName, businessApplicationName, "Start Execution", null);

    final DataObject requestDataObject = getRequestDataObject(
      businessApplicationName, inputParameters);

    if (businessApplication.getResultListProperty() != null) {
      throw new IllegalArgumentException(
        "Business Application return a list of results, use executeList method");
    }

    if (businessApplication.isPerRequestInputData()) {
      throw new IllegalArgumentException(
        "Business Application requires an input data URL");
    }

    if (businessApplication.isPerRequestResultData()) {
      throw new IllegalArgumentException(
        "Business Application requires an result data and content type");
    }

    plugin.setParameters(requestDataObject);
    plugin.execute();

    final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
    final Map<String, Object> response = plugin.getResponseFields();
    final DataObject result = getResultDataObject(resultMetaData, response);
    ModuleLog.info(moduleName, businessApplicationName, "End Execution",
      stopWatch, null);
    log(plugin);
    return result;
  }

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts structured data
   * and returns opaque data.
   * 
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param inputParameters The input parameters to the business application.
   * @param resultDataContentType The content type of the result data.
   * @param resultData The output stream that the plug-in can use to write the
   *          result data to.
   */
  public void execute(final String businessApplicationName,
    final Map<String, ? extends Object> inputParameters,
    final String resultDataContentType, final OutputStream resultData) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final PluginAdaptor plugin = getPlugin(businessApplicationName);
    final BusinessApplication businessApplication = plugin.getApplication();
    final Module module = businessApplication.getModule();
    final String moduleName = module.getName();
    ModuleLog.info(moduleName, businessApplicationName, "Start Execution", null);

    final DataObject requestDataObject = getRequestDataObject(
      businessApplicationName, inputParameters);
    final Map<String, Object> parameters = new HashMap<String, Object>(
      requestDataObject);

    if (businessApplication.isPerRequestResultData()) {
      parameters.put("resultData", resultData);
      parameters.put("resultDataContentType", resultDataContentType);
    } else {
      throw new IllegalArgumentException(
        "Business Application does not support opaque result data");
    }

    plugin.setParameters(parameters);
    plugin.execute();
    if (!plugin.getResponseFields().isEmpty()) {
      throw new RuntimeException(
        "Business Application does not support response fields");
    }
    ModuleLog.info(moduleName, businessApplicationName, "End Execution",
      stopWatch, null);
    log(plugin);
  }

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts opaque data and
   * returns structured data.
   * 
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param jobParameters The global job parameters to the business application.
   * @param inputDataUrl The URL to the opaque input data.
   * @return The result parameters from the business application.
   */
  public Map<String, Object> execute(final String businessApplicationName,
    final Map<String, ? extends Object> jobParameters, final URL inputDataUrl) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final PluginAdaptor plugin = getPlugin(businessApplicationName);
    final BusinessApplication businessApplication = plugin.getApplication();
    final Module module = businessApplication.getModule();
    final String moduleName = module.getName();
    ModuleLog.info(moduleName, businessApplicationName, "Start Execution", null);

    final DataObject requestDataObject = getRequestDataObject(
      businessApplicationName, jobParameters);

    final Map<String, Object> parameters = new HashMap<String, Object>(
      requestDataObject);

    if (businessApplication.getResultListProperty() == null) {
      throw new IllegalArgumentException(
        "Business Application return a list of results");
    }
    if (businessApplication.isPerRequestInputData()) {
      parameters.put("inputDataUrl", inputDataUrl);
    } else {
      throw new IllegalArgumentException(
        "Business Application does not support an input data URL");
    }
    if (businessApplication.isPerRequestResultData()) {
      throw new IllegalArgumentException(
        "Business Application requires an result data and content type");
    }

    plugin.setParameters(parameters);
    plugin.execute();
    final Map<String, Object> response = plugin.getResponseFields();
    final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
    final Map<String, Object> results = getResultDataObject(resultMetaData,
      response);
    ModuleLog.info(moduleName, businessApplicationName, "End Execution",
      stopWatch, null);
    log(plugin);
    return results;
  }

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts and returns
   * opaque data.
   * 
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param jobParameters The global job parameters to the business application.
   * @param inputDataUrl The URL to the opaque input data.
   * @param resultDataContentType The content type of the result data.
   * @param resultData The output stream that the plug-in can use to write the
   *          result data to.
   */
  public void execute(final String businessApplicationName,
    final Map<String, ? extends Object> jobParameters, final URL inputDataUrl,
    final String resultDataContentType, final OutputStream resultData) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final PluginAdaptor plugin = getPlugin(businessApplicationName);
    final BusinessApplication businessApplication = plugin.getApplication();
    final Module module = businessApplication.getModule();
    final String moduleName = module.getName();
    ModuleLog.info(moduleName, businessApplicationName, "Start Execution", null);

    final DataObject requestDataObject = getRequestDataObject(
      businessApplicationName, jobParameters);

    final Map<String, Object> parameters = new HashMap<String, Object>(
      requestDataObject);

    if (businessApplication.isPerRequestInputData()) {
      parameters.put("inputDataUrl", inputDataUrl);
    } else {
      throw new IllegalArgumentException(
        "Business Application does not support an input data URL");
    }

    if (businessApplication.isPerRequestResultData()) {
      parameters.put("resultData", resultData);
      parameters.put("resultDataContentType", resultDataContentType);
    } else {
      throw new IllegalArgumentException(
        "Business Application does not support opaque result data");
    }

    plugin.setParameters(parameters);
    plugin.execute();
    if (!plugin.getResponseFields().isEmpty()) {
      throw new RuntimeException(
        "Business Application does not support response fields");
    }
    ModuleLog.info(moduleName, businessApplicationName, "End Execution",
      stopWatch, null);
    log(plugin);
  }

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts and returns
   * structured data.
   * 
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param inputParameters The input parameters to the business application.
   * @return The result parameters from the business application.
   */
  public List<Map<String, Object>> executeList(
    final String businessApplicationName,
    final Map<String, Object> inputParameters) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final PluginAdaptor plugin = getPlugin(businessApplicationName);
    final BusinessApplication businessApplication = plugin.getApplication();
    final Module module = businessApplication.getModule();
    final String moduleName = module.getName();
    ModuleLog.info(moduleName, businessApplicationName, "Start Execution", null);

    final DataObject requestDataObject = getRequestDataObject(
      businessApplicationName, inputParameters);

    if (businessApplication.getResultListProperty() == null) {
      throw new IllegalArgumentException(
        "Business Application does not return a list of results");
    }

    if (businessApplication.isPerRequestInputData()) {
      throw new IllegalArgumentException(
        "Business Application requires an input data URL");
    }

    if (businessApplication.isPerRequestResultData()) {
      throw new IllegalArgumentException(
        "Business Application requires an result data and content type");
    }

    plugin.setParameters(requestDataObject);
    plugin.execute();
    final List<Map<String, Object>> results = plugin.getResults();
    final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
    final List<Map<String, Object>> resultsList = getResultList(resultMetaData,
      results);
    ModuleLog.info(moduleName, businessApplicationName, "End Execution",
      stopWatch, null);
    log(plugin);
    return resultsList;
  }

  protected BusinessApplication getBusinessApplication(
    final String businessApplicationName) {
    final BusinessApplication businessApplication = businessApplicationRegistry.getBusinessApplication(businessApplicationName);
    return businessApplication;
  }

  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return businessApplicationRegistry;
  }

  public String getConsumerKey() {
    return consumerKey;
  }

  protected PluginAdaptor getPlugin(final String businessApplicationName) {
    final PluginAdaptor plugin = businessApplicationRegistry.getBusinessApplicationPlugin(businessApplicationName);
    if (plugin == null) {
      throw new IllegalArgumentException("Cannot find business application "
        + businessApplicationName);
    } else {
      final SecurityService securityService = getSecurityService(businessApplicationName);
      plugin.setSecurityService(securityService);
      return plugin;
    }
  }

  protected DataObject getRequestDataObject(
    final String businessApplicationName,
    final Map<String, ? extends Object> parameters) {
    final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
    final DataObjectMetaData metaData = businessApplication.getRequestMetaData();
    final String jsonString = JsonDataObjectIoFactory.toString(metaData,
      parameters);
    return JsonDataObjectIoFactory.toDataObject(metaData, jsonString);
  }

  protected DataObject getResultDataObject(final DataObjectMetaData metaData,
    final Map<String, Object> object) {
    final String jsonString = JsonDataObjectIoFactory.toString(metaData, object);
    return JsonDataObjectIoFactory.toDataObject(metaData, jsonString);
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  protected List<Map<String, Object>> getResultList(
    final DataObjectMetaData metaData, final List<Map<String, Object>> list) {
    if (list.isEmpty()) {
      final List results = list;
      return results;
    } else {
      final String jsonString = JsonDataObjectIoFactory.toString(metaData, list);
      final List results = JsonDataObjectIoFactory.toDataObjectList(metaData,
        jsonString);
      return results;
    }
  }

  public MockSecurityService getSecurityService(
    final String businessApplicationName) {
    final BusinessApplication application = getBusinessApplication(businessApplicationName);
    if (application != null) {
      final Module module = application.getModule();
      return (MockSecurityService)securityServiceFactory.getSecurityService(
        module, consumerKey);
    }
    return null;
  }

  public boolean hasResultsList(final String businessApplicationName) {
    final PluginAdaptor plugin = getPlugin(businessApplicationName);
    final BusinessApplication businessApplication = plugin.getApplication();
    return businessApplication.getResultListProperty() != null;
  }

  public void log(final PluginAdaptor plugin) {
    final AppLog appLog = plugin.getAppLog();
    if (appLog != null) {
      LoggerFactory.getLogger(getClass()).info(appLog.getLogContent());
    }
  }

  public void setConsumerKey(final String consumerKey) {
    this.consumerKey = consumerKey;
  }

}
