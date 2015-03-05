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
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.servlet.ServletContext;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.FixedWindowRollingPolicy;
import org.apache.log4j.rolling.RollingFileAppender;
import org.apache.log4j.rolling.SizeBasedTriggeringPolicy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.web.context.ServletContextAware;

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
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.store.Buffer;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.parallel.process.Process;
import com.revolsys.parallel.process.ProcessNetwork;
import com.revolsys.spring.ClassLoaderFactoryBean;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.Maps;
import com.revolsys.util.Property;
import com.revolsys.util.UrlUtil;

public class CpfWorkerScheduler extends ThreadPoolExecutor implements Process,
  BeanNameAware, ModuleEventListener, ServletContextAware {

  private static Integer getPortNumber() {
    try {
      final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      for (final String protocol : Arrays.asList("HTTP/1.1", "AJP/1.3")) {

        final Set<ObjectName> objs = mbs.queryNames(new ObjectName(
          "*:type=Connector,*"), Query.match(Query.attr("protocol"),
          Query.value(protocol)));
        int protocolPort = Integer.MAX_VALUE;
        for (final ObjectName obj : objs) {
          final int port = Integer.parseInt(obj.getKeyProperty("port"));
          if (port < protocolPort) {
            protocolPort = port;
          }
        }
        if (protocolPort < Integer.MAX_VALUE) {
          return protocolPort;
        }
      }

    } catch (final Throwable e) {
      e.printStackTrace();
    }
    return 80;
  }

  private boolean abbortedRequest;

  private String beanName;

  private BusinessApplicationRegistry businessApplicationRegistry;

  private ConfigPropertyLoader configPropertyLoader;

  private String environmentName = "default";

  private final Set<String> executingGroupIds = new LinkedHashSet<String>();

  private DigestHttpClient httpClient;

  private String id;

  private long lastPingTime;

  private final long maxTimeBetweenPings = 5 * 60;

  private final int maxTimeout = 60;

  private final Deque<Map<String, Object>> messages = new LinkedList<Map<String, Object>>();

  private int maxInMessageId = 0;

  private final Channel<Map<String, Object>> inMessageChannel = new Channel<>(
    new Buffer<Map<String, Object>>(1000));

  private String messageUrl;

  private final List<String> includedModuleNames = new ArrayList<>();

  private final List<String> excludedModuleNames = new ArrayList<>();

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

  private final Set<String> loadedModuleNames = new HashSet<>();

  private final long startTime = System.currentTimeMillis();

  private final Map<String, Future<?>> futureTaskByGroupId = new HashMap<>();

  private final Map<Future<?>, String> groupIdByFutureTask = new HashMap<>();

  public CpfWorkerScheduler() {
    super(0, 100, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
      new NamedThreadFactory());
    String hostName;
    try {
      hostName = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (final UnknownHostException e) {
      hostName = "localhost";
    }

    this.id = this.environmentName + ":" + hostName + ":" + getPortNumber();

  }

  protected void addExecutingGroupId(final String groupId) {
    synchronized (this.executingGroupIds) {
      this.executingGroupIds.add(groupId);
    }
  }

  public void addExecutingGroupsMessage() {
    final Map<String, Object> message = createExecutingGroupsMessage();
    addMessage(message);
  }

  public void addFailedGroup(final String groupId) {
    final Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "failedGroupId");
    message.put("groupId", groupId);
    addMessage(message);
  }

  private void addMessage(final Map<String, Object> message) {
    synchronized (this.messages) {
      this.messages.add(message);
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected void afterExecute(final Runnable runnable, final Throwable e) {
    if (runnable instanceof FutureTask) {
      final FutureTask futureTask = (FutureTask)runnable;
      final String groupId = this.groupIdByFutureTask.remove(futureTask);
      this.futureTaskByGroupId.remove(groupId);
    }
    this.taskCount.decrementAndGet();
    synchronized (this.monitor) {
      this.monitor.notifyAll();
    }
  }

  public void cancelGroup(final Map<String, Object> message) {
    final String groupId = (String)message.get("groupId");
    removeExecutingGroupId(groupId);
    final Future<?> future = this.futureTaskByGroupId.get(groupId);
    if (future != null) {
      future.cancel(true);
    }
  }

  protected Map<String, Object> createExecutingGroupsMessage() {
    final Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", "executingGroupIds");
    message.put("workerId", this.id);
    synchronized (this.executingGroupIds) {
      message.put("executingGroupIds", new ArrayList<String>(
        this.executingGroupIds));
    }
    this.lastPingTime = System.currentTimeMillis();
    return message;
  }

  protected Map<String, Object> createModuleMessage(final Module module,
    final String action) {
    final String moduleName = module.getName();
    final long moduleTime = module.getStartedTime();
    return createModuleMessage(moduleName, moduleTime, action);
  }

  protected Map<String, Object> createModuleMessage(final String moduleName,
    final long moduleTime, final String action) {
    final Map<String, Object> message = new LinkedHashMap<String, Object>();
    message.put("action", action);
    message.put("moduleName", moduleName);
    message.put("moduleTime", moduleTime);
    return message;
  }

  @PreDestroy
  public void destroy() {
    this.running = false;
    if (this.processNetwork != null) {
      this.processNetwork.stop();
    }

    this.businessApplicationRegistry = null;
    this.configPropertyLoader = null;
    this.futureTaskByGroupId.clear();
    this.groupIdByFutureTask.clear();
    this.messages.clear();
    this.processNetwork = null;

    this.httpClient = null;
    shutdownNow();
    if (this.securityServiceFactory != null) {
      this.securityServiceFactory.close();
      this.securityServiceFactory = null;
    }
    if (this.tempDir != null) {
      if (!FileUtil.deleteDirectory(this.tempDir)) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to delete jar cache " + this.tempDir);
      }
      this.tempDir = null;
    }
    this.inMessageChannel.readDisconnect();
  }

  @Override
  public void execute(final Runnable command) {
    if (command != null) {
      while (!isShutdown()) {
        this.taskCount.incrementAndGet();
        try {
          super.execute(command);
          return;
        } catch (final RejectedExecutionException e) {
          this.taskCount.decrementAndGet();
        } catch (final RuntimeException e) {
          this.taskCount.decrementAndGet();
          throw e;
        } catch (final Error e) {
          this.taskCount.decrementAndGet();
          throw e;
        }
      }
    }
  }

  @Override
  public String getBeanName() {
    return this.beanName;
  }

  public BusinessApplication getBusinessApplication(final AppLog log,
    final String moduleName, final Long moduleTime,
    final String businessApplicationName) {
    final BusinessApplication businessApplication;
    if (Property.hasValue(moduleName)) {
      final ClassLoaderModule module = (ClassLoaderModule)this.businessApplicationRegistry.getModule(moduleName);
      if (module == null) {
        return null;
      } else if (module.isStarted()) {
        businessApplication = module.getBusinessApplication(businessApplicationName);
      } else {
        businessApplication = null;
      }
    } else {
      businessApplication = this.businessApplicationRegistry.getBusinessApplication(businessApplicationName);
    }
    return businessApplication;
  }

  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return this.businessApplicationRegistry;
  }

  public ConfigPropertyLoader getConfigPropertyLoader() {
    return this.configPropertyLoader;
  }

  public String getEnvironmentName() {
    return this.environmentName;
  }

  public Channel<Map<String, Object>> getInMessageChannel() {
    return this.inMessageChannel;
  }

  public String getPassword() {
    return this.password;
  }

  public int getPriority() {
    return ((NamedThreadFactory)getThreadFactory()).getPriority();
  }

  /**
   * @return the processNetwork
   */
  @Override
  public ProcessNetwork getProcessNetwork() {
    return this.processNetwork;
  }

  public String getUsername() {
    return this.username;
  }

  public String getWebServiceUrl() {
    return this.webServiceUrl;
  }

  @PostConstruct
  public void init() {
    initLogging();

    this.httpClient = new DigestHttpClient(this.webServiceUrl, this.username,
      this.password, getMaximumPoolSize() + 1);

    this.securityServiceFactory = new WebSecurityServiceFactory(this.httpClient);
    this.businessApplicationRegistry.addModuleEventListener(this.securityServiceFactory);
    this.tempDir = FileUtil.createTempDirectory("cpf", "jars");
    this.configPropertyLoader = new InternalWebServiceConfigPropertyLoader(
      this.httpClient, this.environmentName);

    this.messageUrl = this.webServiceUrl + "/worker/workers/" + this.id
      + "/message?workerStartTime=" + this.startTime;
    this.nextIdUrl = this.webServiceUrl + "/worker/workers/" + this.id
      + "/jobs/groups/nextId?workerStartTime=" + this.startTime;
    this.inMessageChannel.readConnect();
  }

  @SuppressWarnings("deprecation")
  protected void initLogging() {
    final Logger logger = Logger.getRootLogger();
    logger.removeAllAppenders();
    final File rootDirectory = this.businessApplicationRegistry.getAppLogDirectory();
    if (rootDirectory == null
      || !(rootDirectory.exists() || rootDirectory.mkdirs())) {
      new ConsoleAppender().activateOptions();
      final ConsoleAppender appender = new ConsoleAppender();
      appender.activateOptions();
      appender.setLayout(new PatternLayout("%d\t%p\t%c\t%m%n"));
      logger.addAppender(appender);
    } else {
      final String baseFileName = rootDirectory + "/" + "worker_"
        + this.id.replaceAll(":", "_");
      final String activeFileName = baseFileName + ".log";
      final FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
      rollingPolicy.setActiveFileName(activeFileName);
      final String fileNamePattern = baseFileName + ".%i.log";
      rollingPolicy.setFileNamePattern(fileNamePattern);

      final RollingFileAppender appender = new RollingFileAppender();

      appender.setFile(activeFileName);
      appender.setRollingPolicy(rollingPolicy);
      appender.setTriggeringPolicy(new SizeBasedTriggeringPolicy(
        1024 * 1024 * 10));
      appender.activateOptions();
      appender.setLayout(new PatternLayout("%d\t%p\t%c\t%m%n"));
      appender.rollover();
      logger.addAppender(appender);
    }
  }

  @Override
  public void moduleChanged(final ModuleEvent event) {
    final String action = event.getAction();
    final Module module = event.getModule();
    final String moduleName = module.getName();
    Map<String, Object> message = null;
    if (action.equals(ModuleEvent.START)) {

    } else if (action.equals(ModuleEvent.START_FAILED)) {
      this.businessApplicationRegistry.unloadModule(module);

      final String moduleError = module.getModuleError();
      message = createModuleMessage(module, "moduleExcluded");
      message.put("moduleError", moduleError);
    } else if (action.equals(ModuleEvent.STOP)) {
      message = createModuleMessage(module, "moduleStopped");
      this.loadedModuleNames.add(moduleName);
    }
    if (message != null) {
      sendMessageWithRetry(message);
    }
  }

  @SuppressWarnings("unchecked")
  public boolean processNextTask() {
    if (System.currentTimeMillis() > this.lastPingTime
      + this.maxTimeBetweenPings * 1000) {
      addExecutingGroupsMessage();
    }
    while (!this.messages.isEmpty()) {
      synchronized (this.messages) {
        final Map<String, Object> message = this.messages.removeFirst();
        if (!sendMessageWithRetry(message)) {
          return false;
        }
      }
    }
    if (!this.running) {
      return false;
    }
    final long time = System.currentTimeMillis();
    if (this.taskCount.get() >= getMaximumPoolSize()) {
      addExecutingGroupsMessage();
      this.lastPingTime = time;
      return false;
    } else {
      try {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("maxMessageId", this.maxInMessageId);
        Map<String, Object> response = null;
        parameters.put("moduleName", this.loadedModuleNames);

        final String url = UrlUtil.getUrl(this.nextIdUrl, parameters);
        if (this.running) {
          if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
            LoggerFactory.getLogger(getClass()).debug(url);
          }
          response = this.httpClient.postJsonResource(url);
        }
        if (!this.running) {
          return false;
        } else {

          if (response != null && !response.isEmpty()) {
            final Map<String, Map<String, Object>> messages = (Map<String, Map<String, Object>>)response.get("messages");
            if (messages != null) {
              for (final Entry<String, Map<String, Object>> entry : messages.entrySet()) {
                final int messageId = Integer.parseInt(entry.getKey());
                final Map<String, Object> message = entry.getValue();
                if (messageId > this.maxInMessageId) {
                  this.inMessageChannel.write(message);
                  this.maxInMessageId = messageId;
                }
              }
            }
            if (response.get("batchJobId") != null) {
              if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
                LoggerFactory.getLogger(getClass()).debug(
                  "Scheduling group " + response);
              }
              final String groupId = (String)response.get("groupId");
              addExecutingGroupId(groupId);
              try {
                final Runnable runnable = new BatchJobRequestExecutionGroupRunnable(
                  this, this.businessApplicationRegistry, this.httpClient,
                  this.securityServiceFactory, this.id, response);
                final Future<?> future = submit(runnable);
                this.futureTaskByGroupId.put(groupId, future);
                this.groupIdByFutureTask.put(future, groupId);
              } catch (final Throwable e) {
                if (this.running) {
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
          if (this.running) {
            LoggerFactory.getLogger(getClass()).error("Unable to get group", t);
          }
        }
      } catch (final Throwable t) {
        if (t.getCause() instanceof SocketException) {
          addExecutingGroupsMessage();
          if (this.abbortedRequest) {
            return true;
          } else {
            LoggerFactory.getLogger(getClass()).error("Unable to get group",
              t.getCause());
            return false;
          }
        } else {
          addExecutingGroupsMessage();
          if (this.running) {
            LoggerFactory.getLogger(getClass()).error("Unable to get group", t);
          }
        }
      }
    }
    return false;
  }

  protected void removeExecutingGroupId(final String groupId) {
    synchronized (this.executingGroupIds) {
      this.executingGroupIds.remove(groupId);
    }
  }

  @Override
  public void run() {
    LoggerFactory.getLogger(getClass()).info("Started");
    try {
      this.running = true;
      while (this.running) {
        try {
          if (processNextTask()) {
            this.timeout = 0;
          } else {
            if (this.timeout < this.maxTimeout) {
              this.timeout += this.timeoutStep;
            }
          }
          if (this.running && this.timeout != 0) {
            synchronized (this.monitor) {
              LoggerFactory.getLogger(getClass())
                .debug(
                  "Waiting " + this.timeout
                    + " seconds before getting next task");
              this.monitor.wait(this.timeout * 1000);
            }
          }
        } catch (final InterruptedException e) {
          if (!this.running) {
            return;
          }
        }
      }

    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
      getProcessNetwork().stop();
    } finally {
      LoggerFactory.getLogger(getClass()).info("Stopped");
    }
  }

  public boolean sendMessage(final Map<String, ? extends Object> message) {
    try {
      if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
        LoggerFactory.getLogger(getClass()).debug(
          this.messageUrl + "\n" + message);
      }
      final Map<String, Object> response = this.httpClient.postJsonResource(
        this.messageUrl, message);
      if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
        LoggerFactory.getLogger(getClass()).debug("Message sent\n" + response);
      }
      if (this.running) {
        return true;
      } else {
        return false;
      }
    } catch (final HttpStatusCodeException t) {
      if (this.running) {
        if (t.getStatusCode() == 404) {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to send message to " + this.messageUrl
              + " 404 returned from server");
        } else {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to send message to " + this.messageUrl + "\n" + message, t);
        }
      }
      return false;
    } catch (final Throwable t) {
      if (this.running) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to send message to " + this.messageUrl + "\n" + message, t);
      }
      return false;
    }
  }

  protected boolean sendMessageWithRetry(final Map<String, Object> message) {
    while (!sendMessage(message)) {
      long timeout = this.timeoutStep;
      if (this.running) {
        try {
          LoggerFactory.getLogger(getClass()).debug(
            "Waiting " + timeout + " seconds before sending message");
          this.messages.wait(timeout * 1000);
        } catch (final InterruptedException e) {
        }
        if (timeout < this.maxTimeout) {
          timeout += this.timeoutStep;
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

  public void setModuleNames(final List<String> moduleNames) {
    this.includedModuleNames.clear();
    this.excludedModuleNames.clear();
    if (moduleNames != null) {
      for (final String moduleName : moduleNames) {
        if (moduleName.startsWith("-")) {
          this.excludedModuleNames.add(moduleName.substring(1));
        } else {
          this.includedModuleNames.add(moduleName);
        }
      }
    }
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

  @Override
  public void setServletContext(final ServletContext servletContext) {
    this.id += ":"
      + servletContext.getContextPath()
        .replaceFirst("^/", "")
        .replaceAll("/", "-");
    this.businessApplicationRegistry.setEnvironmentId("worker_"
      + this.id.replaceAll(":", "_"));
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public void setWebServiceUrl(final String webServiceUrl) {
    this.webServiceUrl = webServiceUrl;
  }

  public void startApplications(final Module module) {
    final String moduleName = module.getName();
    Map<String, Object> message;
    try {
      module.loadApplications();
      final String moduleError = module.getModuleError();
      if (Property.hasValue(moduleError)) {
        message = createModuleMessage(module, "moduleStartFailed");
        message.put("moduleError", moduleError);
        this.businessApplicationRegistry.unloadModule(module);
      } else {
        this.loadedModuleNames.add(moduleName);
        message = createModuleMessage(module, "moduleStarted");
      }
    } catch (final Throwable e) {
      final AppLog log = new AppLog(moduleName);
      log.error("Unable to load module " + moduleName, e);
      message = createModuleMessage(module, "moduleStartFailed");
      message.put("moduleError", ExceptionUtil.toString(e));
      this.businessApplicationRegistry.unloadModule(module);
    }
    if (message != null) {
      sendMessageWithRetry(message);
    }
  }

  protected void startModule(final Map<String, Object> action) {
    final String moduleName = (String)action.get("moduleName");
    final Long moduleTime = Maps.getLong(action, "moduleTime");
    if ((this.includedModuleNames.isEmpty() || this.includedModuleNames.contains(moduleName))
      && !this.excludedModuleNames.contains(moduleName)) {
      final AppLog log = new AppLog(moduleName);

      ClassLoaderModule module = (ClassLoaderModule)this.businessApplicationRegistry.getModule(moduleName);
      if (module != null) {
        final long lastStartedTime = module.getStartedTime();
        if (lastStartedTime < moduleTime) {
          log.info("Unloading older module version\tmoduleName=" + moduleName
            + "\tmoduleTime=" + lastStartedTime);
          unloadModule(module);
          module = null;
        } else if (lastStartedTime == moduleTime) {
          return;
        }
      }
      try {
        final String modulesUrl = this.httpClient.getUrl("/worker/modules/"
          + moduleName + "/" + moduleTime + "/urls.json");
        final Map<String, Object> response = this.httpClient.getJsonResource(modulesUrl);
        final File moduleDir = new File(this.tempDir, moduleName + "-"
          + moduleTime);
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
            this.httpClient.getResource(jarUrl, jarFile);

            urls.add(FileUtil.toUrl(jarFile));
          } catch (final Throwable e) {
            throw new RuntimeException("Unable to download jar file " + jarUrl,
              e);
          }
        }
        final ClassLoader parentClassLoader = getClass().getClassLoader();
        final ClassLoader classLoader = ClassLoaderFactoryBean.createClassLoader(
          parentClassLoader, urls);
        final List<URL> configUrls = ClassLoaderModuleLoader.getConfigUrls(
          classLoader, false);
        module = new ClassLoaderModule(this.businessApplicationRegistry,
          moduleName, classLoader, this.configPropertyLoader, configUrls.get(0));
        this.businessApplicationRegistry.addModule(module);
        module.setStartedDate(new Date(moduleTime));
        module.enable();
        execute(new InvokeMethodRunnable(this, "startApplications", module));
      } catch (final Throwable e) {
        log.error("Unable to load module " + moduleName, e);
        final Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("action", "moduleStartFailed");
        message.put("moduleName", moduleName);
        message.put("moduleTime", moduleTime);
        message.put("moduleError", ExceptionUtil.toString(e));
        addMessage(message);
        if (module != null) {
          this.businessApplicationRegistry.unloadModule(module);
        }
      }
    } else {
      final Map<String, Object> message = createModuleMessage(moduleName,
        moduleTime, "moduleDisabled");
      addMessage(message);
    }
  }

  protected void stopModule(final Map<String, Object> action) {
    final String moduleName = (String)action.get("moduleName");
    final ClassLoaderModule module = (ClassLoaderModule)this.businessApplicationRegistry.getModule(moduleName);
    if (module != null) {
      unloadModule(module);
    }
  }

  @Override
  public String toString() {
    return this.beanName;
  }

  public void unloadModule(final ClassLoaderModule module) {
    final long time = module.getStartedTime();
    this.businessApplicationRegistry.unloadModule(module);
    final File lastModuleDir = new File(this.tempDir, module.getName() + "-"
      + time);
    FileUtil.deleteDirectory(lastModuleDir, true);
  }
}
