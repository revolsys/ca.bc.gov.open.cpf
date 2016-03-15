/*
 * Copyright © 2008-2016, Province of British Columbia
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
import java.util.UUID;

import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;

import com.revolsys.io.FileUtil;
import com.revolsys.io.PathUtil;
import com.revolsys.spring.ClassLoaderFactoryBean;
import com.revolsys.util.Property;

public class ClassLoaderModuleLoader implements ModuleLoader {

  public static List<URL> getConfigUrls(final ClassLoader classLoader,
    final boolean useParentClassloader) {
    final List<URL> configUrls = new ArrayList<URL>();
    try {
      final Enumeration<URL> urls = classLoader
        .getResources("META-INF/ca.bc.gov.open.cpf.plugin.sf.xml");
      while (urls.hasMoreElements()) {
        final URL configUrl = urls.nextElement();
        if (isDefinedInClassLoader(classLoader, useParentClassloader, configUrl)) {
          configUrls.add(configUrl);
        }
      }
    } catch (final IOException e) {
      LoggerFactory.getLogger(ClassLoaderModuleLoader.class)
        .error("Unable to get spring config URLs", e);
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
    return this.businessApplicationRegistry;
  }

  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  @Override
  public void refreshModules() {
    if (this.modulesByName == null) {
      this.modulesByName = new HashMap<String, Module>();
      try {
        final List<URL> configUrls = getConfigUrls(this.classLoader, this.useParentClassLoader);
        for (final URL configUrl : configUrls) {
          try {
            String moduleName = configUrl.toString();
            moduleName = moduleName.replaceAll("META-INF/ca.bc.gov.open.cpf.plugin.sf.xml", "");
            moduleName = moduleName.replaceAll("!", "");
            moduleName = moduleName.replaceAll("/target/classes", "");
            moduleName = moduleName.replaceAll("/+$", "");
            moduleName = moduleName.replaceAll(".*/", "");
            moduleName = moduleName.replaceAll(".jar", "");
            if (moduleName.indexOf('.') == -1) {
              final int dashIndex = moduleName.lastIndexOf('-');
              if (dashIndex != -1) {
                moduleName = moduleName.substring(dashIndex + 1);
              }
            } else {
              moduleName = FileUtil.getFileNameExtension(PathUtil.getName(moduleName));
            }
            if (!Property.hasValue(moduleName)) {
              moduleName = UUID.randomUUID().toString();
            }
            final ConfigPropertyLoader configPropertyLoader = this.businessApplicationRegistry
              .getConfigPropertyLoader();
            final ClassLoaderModule module = new ClassLoaderModule(this.businessApplicationRegistry,
              moduleName, this.classLoader, configPropertyLoader, configUrl);
            this.businessApplicationRegistry.addModule(module);
            this.modulesByName.put(moduleName, module);
            module.enable();
          } catch (final Throwable e) {
            LoggerFactory.getLogger(ClassLoaderModuleLoader.class)
              .error("Unable to register module for " + configUrl, e);
          }
        }
      } catch (final Throwable e) {
        LoggerFactory.getLogger(ClassLoaderModuleLoader.class).error("Unable to register modules",
          e);
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
    final URLClassLoader classLoader = ClassLoaderFactoryBean.newClassLoader(parentClassLoader,
      file);
    setClassLoader(classLoader);
  }

  public void setUrls(final Collection<URL> urls) {
    final ClassLoader parentClassLoader = getClass().getClassLoader();
    final URLClassLoader classLoader = ClassLoaderFactoryBean.newClassLoader(parentClassLoader,
      urls);
    setClassLoader(classLoader);
  }

  public void setUrls(final URL... urls) {
    final ClassLoader parentClassLoader = getClass().getClassLoader();
    final URLClassLoader classLoader = new URLClassLoader(urls, parentClassLoader);
    setClassLoader(classLoader);
  }

}
