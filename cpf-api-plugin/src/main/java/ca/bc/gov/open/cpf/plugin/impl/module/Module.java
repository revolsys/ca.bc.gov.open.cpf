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

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;

public interface Module {

  Set<String> RESERVED_MODULE_NAMES = new LinkedHashSet<>(Arrays.asList("CPF", "VIEW", "EDIT",
    "ADD", "DELETE", "APP", "ADMIN", "DEFAULT", "COPY", "CLONE", "MODULE", "GROUP"));

  void addModuleError(String error);

  void clearModuleError();

  void destroy();

  void disable();

  void enable();

  BusinessApplication getBusinessApplication(String businessApplicationName);

  List<String> getBusinessApplicationNames();

  Object getBusinessApplicationPlugin(BusinessApplication application, String executionId,
    String logLevel);

  Object getBusinessApplicationPlugin(String businessApplicationName, String executionId,
    String logLevel);

  PluginAdaptor getBusinessApplicationPluginAdaptor(BusinessApplication application,
    String executionId, String logLevel);

  PluginAdaptor getBusinessApplicationPluginAdaptor(String businessApplicationName,
    String executionId, String logLevel);

  List<BusinessApplication> getBusinessApplications();

  ClassLoader getClassLoader();

  URL getConfigUrl();

  int getJarCount();

  URL getJarUrl(int urlIndex);

  List<URL> getJarUrls();

  long getLastStartTime();

  String getModuleDescriptor();

  String getModuleError();

  String getModuleType();

  String getName();

  Map<String, Set<ResourcePermission>> getPermissionsByGroupName();

  Date getStartedDate();

  long getStartedTime();

  boolean hasBusinessApplication(String businessApplicationName);

  boolean isApplicationsLoaded();

  boolean isEnabled();

  boolean isRelaodable();

  boolean isRemoteable();

  boolean isStarted();

  void loadApplications(boolean requireStarted);

  void restart();

  void start();

  void stop();
}
