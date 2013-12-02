package ca.bc.gov.open.cpf.api.worker;

import java.io.File;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.api.worker.security.WebSecurityServiceFactory;
import ca.bc.gov.open.cpf.client.httpclient.DigestHttpClient;
import ca.bc.gov.open.cpf.client.httpclient.HttpStatusCodeException;
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

  private boolean abbortedRequest;

  private String beanName;

  private BusinessApplicationRegistry businessApplicationRegistry;

  private ConfigPropertyLoader configPropertyLoader;

  private String environmentName = "default";

  private final Set<String> executingGroupIds = new LinkedHashSet<String>();

  private DigestHttpClient httpClient;

  private final String id = UUID.randomUUID().toString();

  private long lastPingTime;

  /** The list of modules currently being loaded. */
  private final Map<String, Long> loadingModules = new HashMap<String, Long>();

  private final long maxTimeBetweenPings = 5 * 60;

  private final int maxTimeout = 60;

  private final Deque<Map<String, Object>> messages = new LinkedList<Map<String, Object>>();

  private String messageUrl;

  private List<String> moduleNames;

  private final Object monitor = new Object();

  private String nextIdUrl;

  private final AtomicInteger taskCount = new AtomicInteger();

  private String password = "cpf2009";

  private ProcessNetwork processNetwork;

  private boolean running;

  private WebSecurityServiceFactory securityServiceFactory;

  private File tempDir;

  private int timeout = 0;

  private final int timeoutStep = 10;

  private String username = "cpf";

  private String webServiceUrl = "http://localhost/cpf";

  public BatchJobWorkerScheduler() {
    super(0, 100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1),
      new NamedThreadFactory());
    setKeepAliveTime(60, TimeUnit.SECONDS);
  }

  protected void addExecutingGroupId(final String groupId) {
    synchronized (executingGroupIds) {
      executingGroupIds.add(groupId);
    }
  }

  public void addExecutingGroupsMessage() {
    final Map<String, Object> message = createExecutingGroupsMessage();
    addMessage(message);
  }

  public void addFailedGroup(final String groupId) {
    final Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "failedGroupId");
    message.put("batchJobGroupId", groupId);
    addMessage(message);
  }

  private void addMessage(final Map<String, Object> message) {
    synchronized (messages) {
      messages.add(message);
    }
  }

  @Override
  protected void afterExecute(final Runnable r, final Throwable t) {
    taskCount.decrementAndGet();
    synchronized (monitor) {
      monitor.notifyAll();
    }
  }

  protected Map<String, Object> createExecutingGroupsMessage() {
    final Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "executingGroupIds");
    message.put("workerId", id);
    synchronized (executingGroupIds) {
      message.put("executingGroupIds", new ArrayList<String>(executingGroupIds));
    }
    lastPingTime = System.currentTimeMillis();
    return message;
  }

  @PreDestroy
  public void destroy() {
    running = false;
    getProcessNetwork().stop();
    this.httpClient = null;
    shutdownNow();
    if (securityServiceFactory != null) {
      securityServiceFactory.close();
      securityServiceFactory = null;
    }
    if (tempDir != null) {
      if (!FileUtil.deleteDirectory(tempDir)) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to delete jar cache " + tempDir);
      }
      tempDir = null;
    }

  }

  @Override
  public void execute(final Runnable command) {
    if (command != null) {
      while (!isShutdown()) {
        taskCount.incrementAndGet();
        try {
          super.execute(command);
          return;
        } catch (final RejectedExecutionException e) {
          taskCount.decrementAndGet();
        } catch (final RuntimeException e) {
          taskCount.decrementAndGet();
          throw e;
        } catch (final Error e) {
          taskCount.decrementAndGet();
          throw e;
        }
      }
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
      final ClassLoaderModule module = (ClassLoaderModule)businessApplicationRegistry.getModule(moduleName);
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

  public String getEnvironmentName() {
    return environmentName;
  }

  public List<String> getModuleNames() {
    return moduleNames;
  }

  public String getPassword() {
    return password;
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

  public String getUsername() {
    return username;
  }

  public String getWebServiceUrl() {
    return webServiceUrl;
  }

  @PostConstruct
  public void init() {
    httpClient = new DigestHttpClient(webServiceUrl, username, password,
      getMaximumPoolSize() + 1);

    securityServiceFactory = new WebSecurityServiceFactory(httpClient);
    businessApplicationRegistry.addModuleEventListener(securityServiceFactory);
    tempDir = FileUtil.createTempDirectory("cpf", "jars");
    configPropertyLoader = new InternalWebServiceConfigPropertyLoader(
      httpClient, environmentName);

    messageUrl = webServiceUrl + "/worker/workers/" + id + "/message";
    nextIdUrl = webServiceUrl + "/worker/workers/" + id + "/jobs/groups/nextId";
  }

  protected void loadModule(final Map<String, Object> action) {
    final String moduleName = (String)action.get("moduleName");
    final Long moduleTime = ((Number)action.get("moduleTime")).longValue();
    synchronized (loadingModules) {
      final Long loadingTime = loadingModules.get(moduleName);
      if (loadingTime == null || loadingTime < moduleTime) {
        loadingModules.put(moduleName, moduleTime);
        LoggerFactory.getLogger(getClass()).info(
          "Loading module: " + moduleName);

        try {
          execute(new InvokeMethodRunnable(this, "loadModule", moduleName,
            moduleTime));
          final Map<String, Object> message = new LinkedHashMap<String, Object>();
          message.put("action", "moduleLoading");
          message.put("moduleName", moduleName);
          message.put("moduleTime", moduleTime);
          addMessage(message);
        } catch (final Throwable t) {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to load module " + moduleName, t);
          setModuleExcluded(moduleName, moduleTime);
        }
      }
    }
  }

  public void loadModule(final String moduleName, final Long moduleTime) {
    final AppLog log = new AppLog(moduleName);

    ClassLoaderModule module = (ClassLoaderModule)businessApplicationRegistry.getModule(moduleName);
    if (module != null) {
      final long lastStartedTime = module.getStartedTime();
      if (lastStartedTime < moduleTime) {
        LoggerFactory.getLogger(getClass()).info(
          "Unloading older module version " + moduleName + " "
            + lastStartedTime);
        log.info("Unloading older module version\tmoduleName=" + moduleName
          + "\tmoduleTime=" + lastStartedTime);
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
          httpClient.getResource(jarUrl, jarFile);

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
    } catch (final Throwable t) {
      try {
        log.error("Unable to load module " + moduleName, t);
      } finally {
        setModuleExcluded(moduleName, moduleTime);
        if (module != null) {
          businessApplicationRegistry.unloadModule(module);
        }
      }
    }
  }

  @Override
  public void moduleChanged(final ModuleEvent event) {
    final String action = event.getAction();
    final Module module = event.getModule();
    final String moduleName = module.getName();
    if (action.equals(ModuleEvent.START)) {
      final long moduleTime = module.getStartedTime();
      setModuleLoaded(moduleName, moduleTime);
    } else if (action.equals(ModuleEvent.START_FAILED)) {
      businessApplicationRegistry.unloadModule(module);

      final String moduleError = module.getModuleError();
      final AppLog log = new AppLog(moduleName);
      LoggerFactory.getLogger(getClass()).error(moduleError);
      log.error(moduleError);
      final long moduleTime = module.getStartedTime();
      setModuleExcluded(moduleName, moduleTime);
    }
  }

  public void postRun() {
  }

  protected void preRun() {

  }

  public boolean processNextTask() {
    if (System.currentTimeMillis() > lastPingTime + maxTimeBetweenPings * 1000) {
      addExecutingGroupsMessage();
    }
    while (!messages.isEmpty()) {
      synchronized (messages) {
        final Map<String, Object> message = messages.removeFirst();
        if (!sendMessageWithRetry(message)) {
          return false;
        }
      }
    }
    if (!running) {
      return false;
    }
    final long time = System.currentTimeMillis();
    if (taskCount.get() >= getMaximumPoolSize()) {
      addExecutingGroupsMessage();
      lastPingTime = time;
      return false;
    } else {
      try {
        final Map<String, Object> parameters = new HashMap<String, Object>();

        Map<String, Object> response = null;
        if (moduleNames != null && !moduleNames.isEmpty()) {
          parameters.put("moduleName", moduleNames);
        }

        final String url = UrlUtil.getUrl(nextIdUrl, parameters);
        if (running) {
          if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
            LoggerFactory.getLogger(getClass()).debug(url);
          }
          response = httpClient.postJsonResource(url);
        }
        if (!running) {
          return false;
        } else {

          if (response != null && !response.isEmpty()) {
            if ("loadModule".equals(response.get("action"))) {
              loadModule(response);
            } else if (response.get("batchJobId") != null) {
              if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
                LoggerFactory.getLogger(getClass()).debug(
                  "Scheduling group " + response);
              }
              final String groupId = (String)response.get("groupId");
              addExecutingGroupId(groupId);
              try {
                final Runnable runnable = new BatchJobRequestExecutionGroupRunnable(
                  this, businessApplicationRegistry, httpClient,
                  securityServiceFactory, id, response);
                execute(runnable);
              } catch (final Throwable e) {
                if (running) {
                  LoggerFactory.getLogger(getClass()).error(
                    "Unable to get execute group " + groupId, e);
                }
                removeExecutingGroupId(groupId);
                addExecutingGroupsMessage();
              }
            }
          } else {
            if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
              LoggerFactory.getLogger(getClass()).debug("No group available");
            }
          }
        }
        return true;
      } catch (final HttpStatusCodeException t) {
        addExecutingGroupsMessage();
        if (t.getStatusCode() == 404) {

        } else {
          if (running) {
            LoggerFactory.getLogger(getClass()).error("Unable to get group", t);
          }
        }
      } catch (final Throwable t) {
        if (t.getCause() instanceof SocketException) {
          addExecutingGroupsMessage();
          if (abbortedRequest) {
            return true;
          } else {
            LoggerFactory.getLogger(getClass()).error("Unable to get group",
              t.getCause());
            return false;
          }
        } else {
          addExecutingGroupsMessage();
          if (running) {
            LoggerFactory.getLogger(getClass()).error("Unable to get group", t);
          }
        }
      }
    }
    return false;
  }

  protected void removeExecutingGroupId(final String groupId) {
    synchronized (executingGroupIds) {
      executingGroupIds.remove(groupId);
    }
  }

  @Override
  public void run() {
    LoggerFactory.getLogger(getClass()).info("Started");
    preRun();
    try {
      running = true;
      while (running) {
        try {
          if (processNextTask()) {
            timeout = 0;
          } else {
            if (timeout < maxTimeout) {
              timeout += timeoutStep;
            }
          }
          if (running && timeout != 0) {
            synchronized (monitor) {
              LoggerFactory.getLogger(getClass()).debug(
                "Waiting " + timeout + " seconds before getting next task");
              monitor.wait(timeout * 1000);
            }
          }
        } catch (final InterruptedException e) {
          if (!running) {
            return;
          }
        }
      }

    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
      getProcessNetwork().stop();
    } finally {
      postRun();
      LoggerFactory.getLogger(getClass()).info("Stopped");
    }
  }

  public boolean sendMessage(final Map<String, ? extends Object> message) {
    try {
      if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
        LoggerFactory.getLogger(getClass()).debug(messageUrl + "\n" + message);
      }
      final Map<String, Object> response = httpClient.postJsonResource(
        messageUrl, message);
      if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
        LoggerFactory.getLogger(getClass()).debug("Message sent\n" + response);
      }
      if (running) {
        return true;
      } else {
        return false;
      }
    } catch (final HttpStatusCodeException t) {
      if (running) {
        if (t.getStatusCode() == 404) {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to send message to " + messageUrl
              + " 404 returned from server");
        } else {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to send message to " + messageUrl + "\n" + message, t);
        }
      }
      return false;
    } catch (final Throwable t) {
      if (running) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to send message to " + messageUrl + "\n" + message, t);
      }
      return false;
    }
  }

  protected boolean sendMessageWithRetry(final Map<String, Object> message) {
    while (!sendMessage(message)) {
      long timeout = timeoutStep;
      if (running) {
        try {
          LoggerFactory.getLogger(getClass()).debug(
            "Waiting " + timeout + " seconds before sending message");
          messages.wait(timeout * 1000);
        } catch (final InterruptedException e) {
        }
        if (timeout < maxTimeout) {
          timeout += timeoutStep;
        }
      } else {
        return false;
      }
    }
    return true;
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

  public void setEnvironmentName(final String environmentName) {
    this.environmentName = environmentName;
  }

  @Override
  public void setMaximumPoolSize(final int maximumPoolSize) {
    super.setMaximumPoolSize(maximumPoolSize);
  }

  private void setModuleExcluded(final String moduleName, final Long moduleTime) {
    LoggerFactory.getLogger(getClass()).info("Excluding module: " + moduleName);
    final Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "moduleExcluded");
    message.put("moduleName", moduleName);
    message.put("moduleTime", moduleTime);
    addMessage(message);
  }

  private void setModuleLoaded(final String moduleName, final Long moduleTime) {
    final Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "moduleLoaded");
    message.put("moduleName", moduleName);
    message.put("moduleTime", moduleTime);
    sendMessageWithRetry(message);
  }

  public void setModuleNames(final List<String> moduleNames) {
    this.moduleNames = moduleNames;
  }

  public void setPassword(final String password) {
    this.password = password;
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

  public void setUsername(final String username) {
    this.username = username;
  }

  public void setWebServiceUrl(final String webServiceUrl) {
    this.webServiceUrl = webServiceUrl;
  }

  @Override
  public String toString() {
    return beanName;
  }

  public void unloadModule(final ClassLoaderModule module) {
    final long time = module.getStartedTime();
    businessApplicationRegistry.unloadModule(module);
    final File lastModuleDir = new File(tempDir, module.getName() + "-" + time);
    FileUtil.deleteDirectory(lastModuleDir, true);
  }
}