package ca.bc.gov.open.cpf.plugin.impl.module;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;

import com.revolsys.io.FileUtil;
import com.revolsys.io.PathUtil;
import com.revolsys.spring.ClassLoaderFactoryBean;

public class ClassLoaderModuleLoader implements ModuleLoader {

  public static List<URL> getConfigUrls(final ClassLoader classLoader,
    final boolean useParentClassloader) {
    final List<URL> configUrls = new ArrayList<URL>();
    try {
      final Enumeration<URL> urls = classLoader.getResources("META-INF/ca.bc.gov.open.cpf.plugin.sf.xml");
      while (urls.hasMoreElements()) {
        final URL configUrl = urls.nextElement();
        if (isDefinedInClassLoader(classLoader, useParentClassloader, configUrl)) {
          configUrls.add(configUrl);
        }
      }
    } catch (final IOException e) {
      LoggerFactory.getLogger(ClassLoaderModuleLoader.class).error("Unable to get spring config URLs", e);
    }
    return configUrls;
  }

  public static boolean isDefinedInClassLoader(final ClassLoader classLoader,
    final boolean useParentClassLoader, final URL resourceUrl) {
    if (useParentClassLoader) {
      return true;
    } else if (classLoader instanceof URLClassLoader) {
      final URLClassLoader urlClassLoader = (URLClassLoader)classLoader;
      for (final URL url : urlClassLoader.getURLs()) {
        if (resourceUrl.toString().contains(url.toString())) {
          return true;
        }
      }
      return false;
    } else {
      return true;
    }
  }

  private BusinessApplicationRegistry businessApplicationRegistry;

  private ClassLoader classLoader;

  private Map<String, Module> modulesByName;

  private final boolean useParentClassLoader = true;

  public ClassLoaderModuleLoader(final ClassLoader classLoader) {
    setClassLoader(classLoader);
  }

  @Override
  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return businessApplicationRegistry;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public void refreshModules() {
    if (modulesByName == null) {
      modulesByName = new HashMap<String, Module>();
      try {
        final List<URL> configUrls = getConfigUrls(classLoader,
          useParentClassLoader);
        for (final URL configUrl : configUrls) {
          try {
            String moduleName = configUrl.toString();
            moduleName = moduleName.replaceAll(
              "META-INF/ca.bc.gov.open.cpf.plugin.sf.xml", "");
            moduleName = moduleName.replaceAll("!", "");
            moduleName = moduleName.replaceAll("/target/classes", "");
            moduleName = moduleName.replaceAll("/+$", "");
            moduleName = moduleName.replaceAll(".jar", "");
            moduleName = FileUtil.getFileNameExtension(PathUtil.getName(moduleName));

            final ConfigPropertyLoader configPropertyLoader = businessApplicationRegistry.getConfigPropertyLoader();
            final ClassLoaderModule module = new ClassLoaderModule(
              businessApplicationRegistry, moduleName, classLoader,
              configPropertyLoader, configUrl);
            businessApplicationRegistry.addModule(module);
            modulesByName.put(moduleName, module);
            module.enable();
          } catch (final Throwable e) {
            LoggerFactory.getLogger(ClassLoaderModuleLoader.class).error("Unable to register module for " + configUrl, e);
          }
        }
      } catch (final Throwable e) {
        LoggerFactory.getLogger(ClassLoaderModuleLoader.class).error("Unable to register modules", e);
      }
    }
  }

  @Override
  public void setBusinessApplicationRegistry(
    final BusinessApplicationRegistry businessApplicationRegistry) {
    this.businessApplicationRegistry = businessApplicationRegistry;
  }

  public void setClassLoader(final ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public void setFile(final File file) {
    final ClassLoader parentClassLoader = getClass().getClassLoader();
    final URLClassLoader classLoader = ClassLoaderFactoryBean.createClassLoader(
      parentClassLoader, file);
    setClassLoader(classLoader);
  }

  public void setUrls(final Collection<URL> urls) {
    final ClassLoader parentClassLoader = getClass().getClassLoader();
    final URLClassLoader classLoader = ClassLoaderFactoryBean.createClassLoader(
      parentClassLoader, urls);
    setClassLoader(classLoader);
  }

  public void setUrls(final URL... urls) {
    final ClassLoader parentClassLoader = getClass().getClassLoader();
    final URLClassLoader classLoader = new URLClassLoader(urls,
      parentClassLoader);
    setClassLoader(classLoader);
  }

}
