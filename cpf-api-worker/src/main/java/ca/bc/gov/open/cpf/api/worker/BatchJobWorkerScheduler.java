package ca.bc.gov.open.cpf.api.worker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.api.worker.security.WebSecurityServiceFactory;
import ca.bc.gov.open.cpf.client.httpclient.OAuthHttpClient;
import ca.bc.gov.open.cpf.client.httpclient.OAuthHttpClientPool;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModule;
import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModuleLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEvent;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEventListener;

import com.revolsys.io.FileUtil;
import com.revolsys.parallel.NamedThreadFactory;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.parallel.process.Process;
import com.revolsys.parallel.process.ProcessNetwork;
import com.revolsys.spring.ClassLoaderFactoryBean;
import com.revolsys.util.UrlUtil;

public class BatchJobWorkerScheduler extends ThreadPoolExecutor implements
  Process, BeanNameAware, ModuleEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(BatchJobWorkerScheduler.class);

  private String beanName;

  private BusinessApplicationRegistry businessApplicationRegistry;

  private ConfigPropertyLoader configPropertyLoader;

  private String consumerKey = "cpf";

  private String consumerSecret = "cpf2009";

  private String environmentName = "default";

  private OAuthHttpClient httpClient;

  private OAuthHttpClientPool httpClientPool;

  private final String id = UUID.randomUUID().toString();

  /** The list of modules currently being loaded. */
  private Map<String, Long> loadingModules = new HashMap<String, Long>();

  private final int maxTimeout = 60 * 1000;

  private final int minTimeout = 0;

  private List<String> moduleNames;

  private final Object monitor = new Object();

  private ProcessNetwork processNetwork;

  private boolean running;

  private WebSecurityServiceFactory securityServiceFactory;

  private File tempDir;

  private Thread thread;

  private int timeout = 0;

  private final int timeoutStep = 10 * 1000;

  private String webServiceUrl = "http://localhost/cpf";

  private String messageUrl;

  private String nextIdUrl;

  private boolean abbortedRequest;

  public BatchJobWorkerScheduler() {
    super(0, 100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
      new NamedThreadFactory());
  }

  public void abortRequest() {
    OAuthHttpClient httpClient = this.httpClient;
    if (httpClient != null) {
      httpClientPool.releaseClient(httpClient);
      this.httpClient = null;
      abbortedRequest = true;
    }
  }

  public void addFailedGroup(final String groupId) {
    Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "failedGroupId");
    message.put("batchJobGroupId", groupId);
    addMessage(message);
  }

  @Override
  protected void afterExecute(final Runnable r, final Throwable t) {
    synchronized (monitor) {
      monitor.notifyAll();
    }
  }

  @SuppressWarnings("deprecation")
  @PreDestroy
  public void destroy() {
    running = false;
    getProcessNetwork().stop();
    OAuthHttpClient httpClient = this.httpClient;
    if (httpClient != null) {
      abortRequest();
      this.httpClient = null;
    } else if (thread != null) {
      thread.stop();
    }
    shutdownNow();
    if (httpClientPool != null) {
      httpClientPool.close();
      httpClientPool = null;
    }
    if (securityServiceFactory != null) {
      securityServiceFactory.close();
      securityServiceFactory = null;
    }
    if (tempDir != null) {
      if (!FileUtil.deleteDirectory(tempDir)) {
        LOG.error("Unable to delete jar cache " + tempDir);
      }
      tempDir = null;
    }
  }

  @Override
  public String getBeanName() {
    return beanName;
  }

  public BusinessApplication getBusinessApplication(final AppLog log,
    final String moduleName, final Long moduleTime,
    final String businessApplicationName) {
    final BusinessApplication businessApplication;
    if (StringUtils.hasText(moduleName)) {
      ClassLoaderModule module = (ClassLoaderModule)businessApplicationRegistry.getModule(moduleName);
      if (module == null) {
        return null;
      } else if (module.isStarted()) {
        businessApplication = module.getBusinessApplication(businessApplicationName);
      } else {
        businessApplication = null;
      }
    } else {
      businessApplication = businessApplicationRegistry.getBusinessApplication(businessApplicationName);
    }
    return businessApplication;
  }

  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return businessApplicationRegistry;
  }

  public ConfigPropertyLoader getConfigPropertyLoader() {
    return configPropertyLoader;
  }

  public String getConsumerKey() {
    return consumerKey;
  }

  public String getConsumerSecret() {
    return consumerSecret;
  }

  public String getEnvironmentName() {
    return environmentName;
  }

  public void loadModule(final String moduleName, final Long moduleTime) {
    AppLog log = new AppLog();
    final OAuthHttpClient httpClient = httpClientPool.getClient();

    ClassLoaderModule module = (ClassLoaderModule)businessApplicationRegistry.getModule(moduleName);
    if (module != null) {
      final long lastStartedTime = module.getStartedDate().getTime();
      if (lastStartedTime < moduleTime) {
        log.info("Unloading older module version " + moduleName + " "
          + lastStartedTime);
        unloadModule(module);
        module = null;
      } else if (lastStartedTime == moduleTime) {
        return;
      }
    }
    try {
      final String modulesUrl = httpClient.getUrl("/worker/modules/"
        + moduleName + "/" + moduleTime + "/urls.json");
      final Map<String, Object> response = httpClient.getJsonResource(modulesUrl);
      final File moduleDir = new File(tempDir, moduleName + "-" + moduleTime);
      moduleDir.mkdir();
      moduleDir.deleteOnExit();
      final List<URL> urls = new ArrayList<URL>();
      @SuppressWarnings("unchecked")
      final List<String> jarUrls = (List<String>)response.get("jarUrls");
      for (int i = 0; i < jarUrls.size(); i++) {
        final String jarUrl = jarUrls.get(i);
        try {
          final File jarFile = new File(moduleDir, i + ".jar");
          jarFile.deleteOnExit();
          final HttpResponse jarResponse = httpClient.getResource(jarUrl);
          final StatusLine statusLine = jarResponse.getStatusLine();
          final int httpStatusCode = statusLine.getStatusCode();
          final HttpEntity entity = jarResponse.getEntity();
          final InputStream in = entity.getContent();
          try {
            if (httpStatusCode == HttpStatus.SC_OK) {
              FileUtil.copy(in, jarFile);
            } else {
              log.error("Unable to download jar file " + jarUrl);
            }
          } finally {
            FileUtil.closeSilent(in);
          }

          urls.add(FileUtil.toUrl(jarFile));
        } catch (final Throwable e) {
          log.error("Unable to download jar file " + jarUrl, e);
        }
      }
      final ClassLoader parentClassLoader = getClass().getClassLoader();
      final ClassLoader classLoader = ClassLoaderFactoryBean.createClassLoader(
        parentClassLoader, urls);
      final List<URL> configUrls = ClassLoaderModuleLoader.getConfigUrls(
        classLoader, false);
      module = new ClassLoaderModule(businessApplicationRegistry, moduleName,
        classLoader, configPropertyLoader, configUrls.get(0));
      businessApplicationRegistry.addModule(module);
      module.setStartedDate(new Date(moduleTime));
      module.enable();
    } catch (Throwable t) {
      try {
        log.error("Unable to load module " + moduleName, t);
        LOG.error(log.getLogContent());
      } finally {
        setModuleExcluded(moduleName, moduleTime, log.getLogRecords());
        if (module != null) {
          businessApplicationRegistry.unloadModule(module);
        }
      }
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  private void setModuleLoaded(final String moduleName, final Long moduleTime) {
    Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "moduleLoaded");
    message.put("moduleName", moduleName);
    message.put("moduleTime", moduleTime);
    addMessage(message);
  }

  private void setModuleExcluded(final String moduleName,
    final Long moduleTime, List<Map<String, String>> logRecords) {
    Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "moduleExcluded");
    message.put("moduleName", moduleName);
    message.put("moduleTime", moduleTime);
    message.put("logRecords", logRecords);
    addMessage(message);
  }

  public void unloadModule(ClassLoaderModule module) {
    long time = module.getStartedDate().getTime();
    businessApplicationRegistry.unloadModule(module);
    final File lastModuleDir = new File(tempDir, module.getName() + "-" + time);
    FileUtil.deleteDirectory(lastModuleDir, true);
  }

  public List<String> getModuleNames() {
    return moduleNames;
  }

  public int getPriority() {
    return ((NamedThreadFactory)getThreadFactory()).getPriority();
  }

  /**
   * @return the processNetwork
   */
  @Override
  public ProcessNetwork getProcessNetwork() {
    return processNetwork;
  }

  public String getWebServiceUrl() {
    return webServiceUrl;
  }

  @PostConstruct
  public void init() {
    httpClientPool = new OAuthHttpClientPool(webServiceUrl, consumerKey,
      consumerSecret, getMaximumPoolSize() + 1);
    securityServiceFactory = new WebSecurityServiceFactory(httpClientPool);
    businessApplicationRegistry.addModuleEventListener(securityServiceFactory);
    tempDir = FileUtil.createTempDirectory("cpf", "jars");
    configPropertyLoader = new InternalWebServiceConfigPropertyLoader(
      httpClientPool, environmentName);

    messageUrl = webServiceUrl + "/worker/workers/" + id + "/message";
    nextIdUrl = webServiceUrl + "/worker/workers/" + id + "/jobs/groups/nextId";
  }

  @Override
  public void moduleChanged(ModuleEvent event) {
    String action = event.getAction();
    Module module = event.getModule();
    String moduleName = module.getName();
    if (action.equals(ModuleEvent.START)) {
      long moduleTime = module.getStartedDate().getTime();
      setModuleLoaded(moduleName, moduleTime);
    } else if (action.equals(ModuleEvent.START_FAILED)) {
      businessApplicationRegistry.unloadModule(module);

      String moduleError = module.getModuleError();
      AppLog log = new AppLog();
      LOG.error(moduleError);
      log.error(moduleError);
      long moduleTime = module.getStartedDate().getTime();
      setModuleExcluded(moduleName, moduleTime, log.getLogRecords());
    }
  }

  private void addMessage(Map<String, Object> message) {
    synchronized (messages) {
      messages.add(message);
    }
  }

  public Map<String, Object> postRequest(String url)
    throws ClientProtocolException, IOException {
    try {
      abbortedRequest = false;
      httpClient = httpClientPool.getClient();
      return httpClient.postJsonResource(url);
    } finally {
      try {
        httpClientPool.releaseClient(httpClient);
      } finally {
        httpClient = null;
      }
    }
  }

  public Map<String, Object> postRequest(String url,
    Map<String, ? extends Object> message) throws ClientProtocolException,
    IOException {
    try {
      abbortedRequest = false;
      httpClient = httpClientPool.getClient();
      return httpClient.postJsonResource(url, message);
    } finally {
      try {
        httpClientPool.releaseClient(httpClient);
      } finally {
        httpClient = null;
      }
    }
  }

  public void postRun() {
  }

  protected void preRun() {

  }

  public boolean processNextTask() {
    while (!messages.isEmpty()) {
      synchronized (messages) {
        Map<String, Object> message = messages.removeFirst();
        while (!sendMessage(message)) {
          if (running) {
            try {
              messages.wait(10000);
            } catch (InterruptedException e) {
            }
          } else {
            return false;
          }
        }
      }
    }
    if (!running) {
      return false;
    }
    if (getActiveCount() + 1 >= getMaximumPoolSize()) {
      sendMessage(Collections.singletonMap("action", "ping"));
      return false;
    } else {
      try {
        final Map<String, Object> parameters = new HashMap<String, Object>();

        Map<String, Object> response = null;
        if (moduleNames != null && !moduleNames.isEmpty()) {
          parameters.put("moduleName", moduleNames);
        }

        String url = UrlUtil.getUrl(nextIdUrl, parameters);
        if (running) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(url);
          }
          response = postRequest(url);
        }
        if (!running) {
          return false;
        } else {

          if (response != null && !response.isEmpty()) {
            if ("loadModule".equals(response.get("action"))) {
              String moduleName = (String)response.get("moduleName");
              final Long moduleTime = ((Number)response.get("moduleTime")).longValue();
              synchronized (loadingModules) {
                Long loadingTime = loadingModules.get(moduleName);
                if (loadingTime == null || loadingTime < moduleTime) {
                  loadingModules.put(moduleName, moduleTime);

                  execute(new InvokeMethodRunnable(this, "loadModule",
                    moduleName, moduleTime));
                }
              }
            } else if (response.get("batchJobId") != null) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Scheduling group " + response);
              }
              final Runnable runnable = new BatchJobRequestExecutionGroupRunnable(
                this, businessApplicationRegistry, httpClientPool,
                securityServiceFactory, id, response);
              execute(runnable);
            }
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("No group available");
            }
          }
        }
        return true;
      } catch (SocketException e) {
        if (abbortedRequest) {
          return true;
        } else {
          LOG.error("Unable to get group", e);
          return false;
        }
      } catch (final Throwable t) {
        if (running) {
          LOG.error("Unable to get group", t);
        }
      }
    }
    return false;
  }

  public boolean sendMessage(final Map<String, ? extends Object> message) {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug(messageUrl + "\n" + message);
      }
      final Map<String, Object> response = postRequest(messageUrl, message);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Message processed\n" + response);
      }
      if (running) {
        return true;
      } else {
        return false;
      }
    } catch (final Throwable t) {
      if (running) {
        LOG.error("Unable to process message\n" + message, t);
      }
      return false;
    }
  }

  private Deque<Map<String, Object>> messages = new LinkedList<Map<String, Object>>();

  @Override
  public void run() {
    thread = Thread.currentThread();
    LOG.info("Started");
    preRun();
    try {
      running = true;
      while (running) {
        try {
          if (processNextTask()) {
            timeout = minTimeout;
          } else {
            if (timeout < maxTimeout) {
              timeout += timeoutStep;
            }
          }
          if (running && timeout != 0) {
            synchronized (monitor) {
              LOG.info("Waiting " + timeout);
              monitor.wait(timeout);
            }
          }
        } catch (final InterruptedException e) {
          if (!running) {
            return;
          }
        }
      }

    } catch (final Throwable e) {
      LOG.error(e.getMessage(), e);
      getProcessNetwork().stop();
    } finally {
      thread = null;
      postRun();
      LOG.info("Stopped");
    }
  }

  @Override
  public void setBeanName(final String beanName) {
    this.beanName = beanName;
    final ThreadFactory threadFactory = getThreadFactory();
    if (threadFactory instanceof NamedThreadFactory) {
      final NamedThreadFactory namedThreadFactory = (NamedThreadFactory)threadFactory;
      namedThreadFactory.setNamePrefix(beanName + "-pool");
    }
  }

  public void setBusinessApplicationRegistry(
    final BusinessApplicationRegistry businessApplicationRegistry) {
    this.businessApplicationRegistry = businessApplicationRegistry;
    businessApplicationRegistry.addModuleEventListener(this);
  }

  public void setConsumerKey(final String consumerKey) {
    this.consumerKey = consumerKey;
  }

  public void setConsumerSecret(final String consumerSecret) {
    this.consumerSecret = consumerSecret;
  }

  public void setEnvironmentName(final String environmentName) {
    this.environmentName = environmentName;
  }

  @Override
  public void setMaximumPoolSize(final int maximumPoolSize) {
    super.setMaximumPoolSize(maximumPoolSize);
    if (httpClientPool != null) {
      httpClientPool.setMaxConnections(maximumPoolSize + 1);
    }
  }

  public void setModuleNames(final List<String> moduleNames) {
    this.moduleNames = moduleNames;
  }

  public void setPriority(final int priority) {
    ((NamedThreadFactory)getThreadFactory()).setPriority(priority);
  }

  /**
   * @param processNetwork the processNetwork to set
   */
  @Override
  public void setProcessNetwork(final ProcessNetwork processNetwork) {
    this.processNetwork = processNetwork;
    if (processNetwork != null) {
      processNetwork.addProcess(this);
      final ThreadFactory threadFactory = getThreadFactory();
      if (threadFactory instanceof NamedThreadFactory) {
        final NamedThreadFactory namedThreadFactory = (NamedThreadFactory)threadFactory;
        namedThreadFactory.setParentGroup(processNetwork.getThreadGroup());
      }
    }
  }

  public void setWebServiceUrl(final String webServiceUrl) {
    this.webServiceUrl = webServiceUrl;
  }

  @Override
  public String toString() {
    return beanName;
  }
}
