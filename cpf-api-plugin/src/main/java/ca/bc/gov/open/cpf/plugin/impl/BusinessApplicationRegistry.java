package ca.bc.gov.open.cpf.plugin.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModule;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleControlProcess;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEvent;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEventListener;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleLoader;

import com.revolsys.comparator.IgnoreCaseStringComparator;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.store.Buffer;

public final class BusinessApplicationRegistry implements
  ApplicationListener<ContextRefreshedEvent> {

  private final Map<String, Module> modulesByName = new TreeMap<String, Module>();

  private Map<String, String> moduleNamesByBusinessApplicationName;

  private List<ModuleLoader> moduleLoaders = new ArrayList<ModuleLoader>();

  private boolean lazyLoad = false;

  private ConfigPropertyLoader configPropertyLoader;

  private final Set<ModuleEventListener> listeners = new LinkedHashSet<ModuleEventListener>();

  private File logDirectory;

  private File appLogDirectory;

  private Channel<Map<String, Object>> moduleControlChannel = new Channel<Map<String, Object>>(
    "moduleControlChannel", new Buffer<Map<String, Object>>(10000));

  private Thread moduleControlThread;

  private boolean useModuleControlThread = true;

  public BusinessApplicationRegistry() {
    this(true);
  }

  public BusinessApplicationRegistry(final boolean useModuleControlThread,
    final ModuleLoader... moduleLoader) {
    this.useModuleControlThread = useModuleControlThread;
    if (useModuleControlThread) {
      moduleControlChannel.writeConnect();
      final ModuleControlProcess moduleControlProcess = new ModuleControlProcess(
        this, moduleControlChannel);
      moduleControlThread = new Thread(moduleControlProcess, "ModuleControl");
      moduleControlThread.setDaemon(true);
      moduleControlThread.start();
    }
    setModuleLoaders(Arrays.asList(moduleLoader));
    refreshModules();
  }

  public synchronized void addModule(final Module module) {
    clearModuleToAppCache();
    final String name = module.getName();
    final Module currentModule = modulesByName.get(name);
    if (currentModule != module) {
      if (currentModule == null) {
        modulesByName.put(name, module);
      } else {
        module.clearModuleError();
        module.addModuleError("Module with the same name is already loaded");
      }
    }
  }

  public void addModuleEventListener(final ModuleEventListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  public void clearModuleToAppCache() {
    moduleNamesByBusinessApplicationName = null;
  }

  @PreDestroy
  public void close() {
    useModuleControlThread = false;
    final List<Module> modules = getModules();
    for (final Module module : modules) {
      final ClassLoaderModule classModule = (ClassLoaderModule)module;
      classModule.stop();
    }

    if (moduleControlChannel != null) {
      moduleControlChannel.writeDisconnect();
      moduleControlChannel = null;
    }
    if (moduleControlThread != null) {
      moduleControlThread.stop();
      moduleControlThread = null;
    }
    configPropertyLoader = null;

    listeners.clear();
    moduleLoaders.clear();
    modulesByName.clear();
  }

  public File getAppLogDirectory() {
    return appLogDirectory;
  }

  public BusinessApplication getBusinessApplication(
    String businessApplicationName) {
    if (businessApplicationName == null) {
      return null;
    } else {
      String businessApplicationVersion = "CURRENT";
      final int colonIndex = businessApplicationName.lastIndexOf(':');
      if (colonIndex != -1) {
        businessApplicationName = businessApplicationName.substring(0,
          colonIndex - 1);
        businessApplicationVersion = businessApplicationName.substring(colonIndex + 1);
      }
      return getBusinessApplication(businessApplicationName,
        businessApplicationVersion);
    }
  }

  public BusinessApplication getBusinessApplication(
    final String businessApplicationName,
    final String businessApplicationVersion) {
    final Module module = getModule(businessApplicationName,
      businessApplicationVersion);
    if (module == null) {
      return null;
    } else {
      return module.getBusinessApplication(businessApplicationName);
    }
  }

  public synchronized List<String> getBusinessApplicationNames() {
    final List<String> names = new ArrayList<String>();
    for (final Module module : modulesByName.values()) {
      if (module.isEnabled()) {
        final List<String> businessApplicationNames = module.getBusinessApplicationNames();
        if (businessApplicationNames != null) {
          names.addAll(businessApplicationNames);
        }
      }
    }
    Collections.sort(names, new IgnoreCaseStringComparator());
    return names;
  }

  public PluginAdaptor getBusinessApplicationPlugin(
    final BusinessApplication businessApplication) {
    if (businessApplication == null) {
      return null;
    } else {
      final Module module = businessApplication.getModule();
      if (module == null) {
        return null;
      } else {
        return module.getBusinessApplicationPlugin(businessApplication, null,
          null);
      }
    }
  }

  public PluginAdaptor getBusinessApplicationPlugin(
    String businessApplicationName) {
    String businessApplicationVersion = "CURRENT";
    final int colonIndex = businessApplicationName.lastIndexOf(':');
    if (colonIndex != -1) {
      businessApplicationName = businessApplicationName.substring(0,
        colonIndex - 1);
      businessApplicationVersion = businessApplicationName.substring(colonIndex + 1);
    }
    return getBusinessApplicationPlugin(businessApplicationName,
      businessApplicationVersion);
  }

  public PluginAdaptor getBusinessApplicationPlugin(
    final String businessApplicationName,
    final String businessApplicationVersion) {
    final Module module = getModule(businessApplicationName,
      businessApplicationVersion);
    if (module == null) {
      return null;
    } else {
      return module.getBusinessApplicationPlugin(businessApplicationName, null,
        null);
    }
  }

  public List<BusinessApplication> getBusinessApplications() {
    final List<BusinessApplication> businessApplications = new ArrayList<BusinessApplication>();
    final List<String> businessApplicationNames = getBusinessApplicationNames();
    for (final String businessApplicationName : businessApplicationNames) {
      final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
      if (businessApplication != null) {
        businessApplications.add(businessApplication);
      }
    }
    return businessApplications;
  }

  public ConfigPropertyLoader getConfigPropertyLoader() {
    return configPropertyLoader;
  }

  public File getLogDirectory() {
    return logDirectory;
  }

  public synchronized Module getModule(final String moduleName) {
    final Module module = modulesByName.get(moduleName);
    return module;
  }

  private synchronized Module getModule(final String businessApplicationName,
    final String businessApplicationVersion) {
    if (moduleNamesByBusinessApplicationName == null) {
      moduleNamesByBusinessApplicationName = new HashMap<String, String>();
      final Map<BusinessApplication, Module> businessApplicationModuleMap = new TreeMap<BusinessApplication, Module>();
      for (final Module module : modulesByName.values()) {
        for (final BusinessApplication application : module.getBusinessApplications()) {
          businessApplicationModuleMap.put(application, module);
        }
      }

      for (final Entry<BusinessApplication, Module> entry : businessApplicationModuleMap.entrySet()) {
        final BusinessApplication businessApplication = entry.getKey();
        final Module module = entry.getValue();
        final String name = businessApplication.getName();
        final String moduleName = module.getName();

        for (final String version : businessApplication.getCompatibleVersions()) {
          final String key = name + ":" + version;
          moduleNamesByBusinessApplicationName.put(key, moduleName);
        }
      }
      for (final Entry<BusinessApplication, Module> entry : businessApplicationModuleMap.entrySet()) {
        final BusinessApplication businessApplication = entry.getKey();
        final Module module = entry.getValue();
        final String name = businessApplication.getName();
        final String moduleName = module.getName();

        final String version = businessApplication.getVersion();
        final String key = name + ":" + version;
        moduleNamesByBusinessApplicationName.put(key, moduleName);
        moduleNamesByBusinessApplicationName.put(name + ":CURRENT", moduleName);

      }
    }
    final String moduleName = moduleNamesByBusinessApplicationName.get(businessApplicationName
      + ":" + businessApplicationVersion);
    if (moduleName == null) {
      if (businessApplicationVersion.equals("CURRENT")) {
        return null;
      } else {
        return null;
      }
    } else {
      return getModule(moduleName);
    }
  }

  public BusinessApplication getModuleBusinessApplication(
    final String moduleName, final String businessApplicationName) {
    final Module module = getModule(moduleName);
    if (module != null) {
      return module.getBusinessApplication(businessApplicationName);
    }
    return null;
  }

  public List<ModuleLoader> getModuleLoaders() {
    return moduleLoaders;
  }

  public List<String> getModuleNames() {
    return new ArrayList<String>(modulesByName.keySet());
  }

  public synchronized List<Module> getModules() {
    final Collection<Module> modules = modulesByName.values();
    return new ArrayList<Module>(modules);
  }

  public boolean hasBusinessApplication(final String moduleName,
    final String businessApplicationName) {
    final Module module = getModule(moduleName);
    if (module != null) {
      return module.hasBusinessApplication(businessApplicationName);
    }
    return false;
  }

  public boolean hasModule(final String moduleName) {
    return getModule(moduleName) != null;
  }

  public boolean isLazyLoad() {
    return lazyLoad;
  }

  public void moduleEvent(final Module module, final String action) {
    final ModuleEvent event = new ModuleEvent(module, action);
    for (final ModuleEventListener listener : listeners) {
      try {
        listener.moduleChanged(event);
      } catch (final Throwable t) {
        LoggerFactory.getLogger(BusinessApplicationRegistry.class).error(
          "Error invoking listener", t);
      }
    }
  }

  @Override
  public void onApplicationEvent(final ContextRefreshedEvent event) {
    if (!lazyLoad) {
      refreshModules();
    }
  }

  public void refreshModules() {
    for (final ModuleLoader moduleLoader : moduleLoaders) {
      moduleLoader.setBusinessApplicationRegistry(this);
      moduleLoader.refreshModules();
    }
  }

  public void removeModuleEventListener(final ModuleEventListener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  public void restartModule(final String moduleName) {
    if (useModuleControlThread) {
      final HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("moduleName", moduleName);
      parameters.put("action", "restart");
      moduleControlChannel.write(parameters);
    } else {
      final ClassLoaderModule module = (ClassLoaderModule)getModule(moduleName);
      module.doRestart();
    }
  }

  public void setAppLogDirectory(final File appLogDirectory) {
    this.appLogDirectory = appLogDirectory;
  }

  public void setConfigPropertyLoader(
    final ConfigPropertyLoader configPropertyLoader) {
    this.configPropertyLoader = configPropertyLoader;
  }

  public void setLazyLoad(final boolean lazyLoad) {
    this.lazyLoad = lazyLoad;
  }

  public void setModuleLoaders(final List<ModuleLoader> moduleLoaders) {
    this.moduleLoaders = new ArrayList<ModuleLoader>(moduleLoaders);
  }

  public void startModule(final String moduleName) {
    if (useModuleControlThread) {
      final HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("moduleName", moduleName);
      parameters.put("action", "start");
      moduleControlChannel.write(parameters);
    } else {
      final ClassLoaderModule module = (ClassLoaderModule)getModule(moduleName);
      module.doStart();
    }
  }

  public void stopModule(final String moduleName) {
    if (useModuleControlThread) {
      final HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("moduleName", moduleName);
      parameters.put("action", "stop");
      moduleControlChannel.write(parameters);
    } else {
      final ClassLoaderModule module = (ClassLoaderModule)getModule(moduleName);
      module.doStop();
    }
  }

  public synchronized void unloadModule(final Module module) {
    LoggerFactory.getLogger(BusinessApplicationRegistry.class).debug(
      "Unloading module " + module.toString());
    clearModuleToAppCache();
    final String moduleName = module.getName();
    if (module == modulesByName.get(moduleName)) {
      modulesByName.remove(moduleName);
    }
    module.destroy();
  }

}
