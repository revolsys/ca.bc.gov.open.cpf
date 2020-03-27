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
package ca.bc.gov.open.cpf.api.web;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import ca.bc.gov.open.cpf.plugin.impl.module.ClassLoaderModule;

import com.revolsys.log.LogAppender;
import com.revolsys.util.Property;

public class CpfLog4jContextListener implements ServletContextListener {

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
  }

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    LogAppender.removeAllAppenders();
    final ServletContext context = event.getServletContext();
    String cpfLogDirectory = context.getInitParameter("cpfLogDirectory");
    if (!Property.hasValue(cpfLogDirectory)) {
      cpfLogDirectory = System.getProperty("cpfLogDirectory", "logs/cpf");
    }
    File rootDirectory = new File(cpfLogDirectory);
    if (rootDirectory.exists()) {
      if (!rootDirectory.isDirectory()) {
        rootDirectory = null;
      }
    } else {
      if (!rootDirectory.mkdirs()) {
        rootDirectory = null;
      }
    }
    if (rootDirectory == null || !(rootDirectory.exists() || rootDirectory.mkdirs())) {
      LogAppender.addRootAppender("%d\t%p\t%c\t%m%n");
    } else {
      final Logger logger = (Logger)LogManager.getRootLogger();
      ClassLoaderModule.addAppender(logger, rootDirectory + "/master", "cpf-master-all");
      ClassLoaderModule.addAppender(logger, rootDirectory + "/master-app", "cpf-app");
    }
  }
}
