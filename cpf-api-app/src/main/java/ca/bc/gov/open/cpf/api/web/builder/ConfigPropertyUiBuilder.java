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
package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.data.query.Condition;
import com.revolsys.data.query.Q;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.ui.html.fields.Field;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class ConfigPropertyUiBuilder extends CpfUiBuilder {

  private static final List<String> GLOBAL_MODULE_NAMES = Arrays.asList("CPF",
    "CPF_WORKER");

  private final List<String> INTERNAL_APP_PROPERTY_NAMES = Arrays.asList(
    "maxConcurrentRequests", "numRequestsPerWorker", "maxRequestsPerJob",
    "logLevel", "batchModePermission", "instantModePermission",
    "testModeEnabled");

  public ConfigPropertyUiBuilder() {
    super("configProperty", ConfigProperty.CONFIG_PROPERTY,
      "CONFIG_PROPERTY_ID", "Config Property", "Config Properties");
  }

  private String getAppComponentName(final String businessApplicationName) {
    final String componentName = "APP_" + businessApplicationName;
    return componentName;
  }

  @RequestMapping(value = {
    "/admin/configProperties/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageCpfAdd(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    final Map<String, Object> defaultValues = new HashMap<String, Object>();
    defaultValues.put(ConfigProperty.COMPONENT_NAME, ConfigProperty.GLOBAL);

    final Element result = super.createObjectAddPage(defaultValues, null,
      "preInsert");
    return result;
  }

  @RequestMapping(value = {
    "/admin/configProperties/{configPropertyId}/delete"
  }, method = RequestMethod.POST)
  public void pageCpfDelete(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkHasAnyRole(ADMIN);

    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null) {
      final String moduleName = configProperty.getValue(ConfigProperty.MODULE_NAME);
      if (GLOBAL_MODULE_NAMES.contains(moduleName)) {
        final String componentName = configProperty.getValue(ConfigProperty.COMPONENT_NAME);
        if (componentName.equals(ConfigProperty.GLOBAL)) {
          final RecordStore recordStore = getRecordStore();
          recordStore.delete(configProperty);
          redirectPage("list");
          return;
        }
      }
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/configProperties/{configPropertyId}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageCpfEdit(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkHasAnyRole(ADMIN);
    final Record configProperty = loadObject(configPropertyId);
    return super.createObjectEditPage(configProperty, null);
  }

  @RequestMapping(value = {
    "/admin/configProperties"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageCpfList(final HttpServletRequest request,
    final HttpServletResponse response) {
    checkHasAnyRole(ADMIN);
    HttpServletUtils.setAttribute("title", "Config Properties");
    final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.MODULE_NAME, GLOBAL_MODULE_NAMES);
    parameters.put("filter", filter);

    return createDataTableHandler(request, "list", parameters);
  }

  @RequestMapping(value = {
    "/admin/configProperties/{configPropertyId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public ElementContainer pageCpfView(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable final Integer configPropertyId) {
    checkHasAnyRole(ADMIN);
    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null) {
      final String moduleName = configProperty.getValue(ConfigProperty.MODULE_NAME);
      if (GLOBAL_MODULE_NAMES.contains(moduleName)) {
        final String componentName = configProperty.getValue(ConfigProperty.COMPONENT_NAME);
        if (componentName.equals(ConfigProperty.GLOBAL)) {
          final TabElementContainer tabs = new TabElementContainer();
          addObjectViewPage(tabs, configProperty, null);
          return tabs;
        }
      }
    }
    throw new PageNotFoundException();
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/configProperties/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageModuleAdd(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);
    final Map<String, Object> defaultValues = new HashMap<String, Object>();
    defaultValues.put(ConfigProperty.MODULE_NAME, moduleName);
    defaultValues.put(ConfigProperty.COMPONENT_NAME,
      ConfigProperty.MODULE_BEAN_PROPERTY);

    return createObjectAddPage(defaultValues, "module", "preInsertModule");
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/configProperties/add"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  public Element pageModuleAppAdd(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final String componentName = getAppComponentName(businessApplicationName);
    final Map<String, Object> defaultValues = new HashMap<String, Object>();
    defaultValues.put(ConfigProperty.MODULE_NAME, moduleName);
    defaultValues.put(ConfigProperty.COMPONENT_NAME, componentName);

    return createObjectAddPage(defaultValues, "moduleApp", "preInsertModule");
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/configProperties/{configPropertyId}/delete"
      }, method = RequestMethod.POST)
  public void pageModuleAppDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final String componentName = getAppComponentName(businessApplicationName);
    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null
      && configProperty.getValue(ConfigProperty.MODULE_NAME).equals(moduleName)
      && configProperty.getValue(ConfigProperty.COMPONENT_NAME).equals(
        componentName)) {
      getRecordStore().delete(configProperty);
      redirectPage("moduleAppList");
      return;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/configProperties/{configPropertyId}/edit"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  public Element pageModuleAppEdit(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final String componentName = getAppComponentName(businessApplicationName);
    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null
      && configProperty.getValue(ConfigProperty.MODULE_NAME).equals(moduleName)
      && configProperty.getValue(ConfigProperty.COMPONENT_NAME).equals(
        componentName)) {
      return createObjectEditPage(configProperty, "moduleApp");
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/configProperties"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleAppList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName) throws IOException,
    NoSuchRequestHandlingMethodException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final String componentName = getAppComponentName(businessApplicationName);
    final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

    final Condition where = Q.and(
      Q.equal(ConfigProperty.MODULE_NAME, moduleName),
      Q.equal(ConfigProperty.COMPONENT_NAME, componentName),
      Q.not(Q.in(ConfigProperty.PROPERTY_NAME, INTERNAL_APP_PROPERTY_NAMES)));

    final Query query = new Query(getTableName(), where);
    parameters.put("query", query);

    return createDataTableHandlerOrRedirect(request, response, "moduleAppList",
      Module.class, "view", parameters);
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/configProperties/{configPropertyId}"
      }, method = RequestMethod.GET)
  @ResponseBody
  public Element pageModuleAppView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final String componentName = getAppComponentName(businessApplicationName);
    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null
      && configProperty.getValue(ConfigProperty.MODULE_NAME).equals(moduleName)
      && configProperty.getValue(ConfigProperty.COMPONENT_NAME).equals(
        componentName)) {
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, configProperty, "moduleApp");
      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/configProperties/{configPropertyId}/delete"
  }, method = RequestMethod.POST)
  public void pageModuleDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null
      && configProperty.getValue(ConfigProperty.MODULE_NAME).equals(moduleName)
      && configProperty.getValue(ConfigProperty.COMPONENT_NAME).equals(
        ConfigProperty.MODULE_BEAN_PROPERTY)) {
      getRecordStore().delete(configProperty);
      redirectPage("moduleList");
      return;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/configProperties/{configPropertyId}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageModuleEdit(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null
      && configProperty.getValue(ConfigProperty.MODULE_NAME).equals(moduleName)
      && configProperty.getValue(ConfigProperty.COMPONENT_NAME).equals(
        ConfigProperty.MODULE_BEAN_PROPERTY)) {
      return createObjectEditPage(configProperty, "module");
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/configProperties"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, NoSuchRequestHandlingMethodException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

    final Map<String, Object> filter = new LinkedHashMap<String, Object>();
    filter.put(ConfigProperty.MODULE_NAME, moduleName);
    filter.put(ConfigProperty.COMPONENT_NAME,
      ConfigProperty.MODULE_BEAN_PROPERTY);
    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response, "moduleList",
      Module.class, "view", parameters);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/configProperties/{configPropertyId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Element pageModuleView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final Integer configPropertyId) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Record configProperty = loadObject(configPropertyId);
    if (configProperty != null
      && configProperty.getValue(ConfigProperty.MODULE_NAME).equals(moduleName)
      && configProperty.getValue(ConfigProperty.COMPONENT_NAME).equals(
        ConfigProperty.MODULE_BEAN_PROPERTY)) {
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, configProperty, "module");
      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @Override
  public boolean preInsert(final Form form, final Record configProperty) {
    final Field field = form.getField(ConfigProperty.MODULE_NAME);
    final String moduleName = field.getValue();
    if (GLOBAL_MODULE_NAMES.contains(moduleName)) {
      return true;
    } else {
      field.addValidationError("Module name must be either CPF or CPF_WORKER");
      return false;
    }
  }

  public boolean preInsertModule(final Form form, final Record object) {
    return true;
  }

}
