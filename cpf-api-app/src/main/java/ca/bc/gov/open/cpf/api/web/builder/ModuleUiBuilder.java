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
package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.controller.ConfigPropertyModule;
import ca.bc.gov.open.cpf.api.controller.ConfigPropertyModuleLoader;
import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.UserGroup;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.maven.MavenRepository;
import com.revolsys.ui.html.decorator.FormGroupDecorator;
import com.revolsys.ui.html.decorator.TableBody;
import com.revolsys.ui.html.fields.CheckBoxField;
import com.revolsys.ui.html.fields.TextField;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.ButtonsToolbarElement;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.model.Menu;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class ModuleUiBuilder extends CpfUiBuilder {
  private MavenRepository mavenRepository;

  private ConfigPropertyModuleLoader moduleLoader;

  private final Object MODULE_ADD_SYNC = new Object();

  public ModuleUiBuilder() {
    super("module", "Business Application Module", "Business Application Modules");
    setIdParameterName("moduleName");
    setIdPropertyName("name");
  }

  @Override
  @PreDestroy
  public void close() {
    super.close();
    this.mavenRepository = null;
    this.moduleLoader = null;
  }

  public MavenRepository getMavenRepository() {
    return this.mavenRepository;
  }

  public ConfigPropertyModuleLoader getModuleLoader() {
    return this.moduleLoader;
  }

  @RequestMapping(value = {
    "/admin/modules/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element newModulePageAdd(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    final Map<String, Object> parameters = new HashMap<>();

    final Form form = new Form(this.typeName);

    final TextField moduleNameField = new TextField("moduleName", 30, true);
    FormGroupDecorator.decorate(form, moduleNameField, "Module Name", null);

    final TextField mavenModuleIdField = new TextField("mavenModuleId", 70, true);
    FormGroupDecorator.decorate(form, mavenModuleIdField, "Maven Module ID", null);

    final CheckBoxField enabledField = new CheckBoxField("enabled");
    enabledField.setInitialValue(true);
    FormGroupDecorator.decorate(form, enabledField, "enabled", null);

    form.initialize(request);

    if (form.isPosted() && form.isMainFormTask()) {
      if (form.isValid()) {
        synchronized (this.MODULE_ADD_SYNC) {
          final String moduleName = ((String)moduleNameField.getValue()).toUpperCase();
          final String mavenModuleId = mavenModuleIdField.getValue();

          synchronized (getBusinessApplicationRegistry()) {
            boolean valid = true;
            for (final Module module : getBusinessApplicationRegistry().getModules()) {
              if (moduleName.equalsIgnoreCase(module.getName())) {
                moduleNameField.addValidationError("Module Name is already used");
                valid = false;
              } else if (Module.RESERVED_MODULE_NAMES.contains(moduleName)) {
                moduleNameField.addValidationError("Module Name is a reserved word");
                valid = false;
              } else if (!moduleName.matches("[A-Z0-9_]+")) {
                moduleNameField
                  .addValidationError("Can only contain the characters A-Z, 0-9, and _.");
                valid = false;
              }

              if (module instanceof ConfigPropertyModule) {
                final ConfigPropertyModule mavenModule = (ConfigPropertyModule)module;
                if (mavenModule.getModuleDescriptor().equals(mavenModuleId)) {
                  mavenModuleIdField.addValidationError("Maven Module ID is already registered");
                  valid = false;
                }
              }
            }
            if (valid) {
              final boolean enabled = enabledField.isSelected();
              this.moduleLoader.setMavenModuleConfigProperties(moduleName, mavenModuleId, enabled);
              this.moduleLoader.refreshModules();

              parameters.put("moduleName", moduleName);
              final String url = getPageUrl("view", parameters);
              response.sendRedirect(url);
              return null;
            }
          }

        }
      }
    }

    final String title = "Add " + getTitle();
    request.setAttribute("title", title);

    final Menu actionMenu = new Menu();
    actionMenu.addMenuItem(new Menu("Cancel", getPageUrl("list", parameters)));
    actionMenu.addMenuItem(new Menu("Refresh", getPageUrl("add", parameters)));
    final String name = form.getName();
    actionMenu.addMenuItem(new Menu("Save", "javascript:$('#" + name + "').submit()"));

    final ButtonsToolbarElement buttonsToolbar = new ButtonsToolbarElement(actionMenu);
    final ElementContainer view = new ElementContainer(form, buttonsToolbar);
    final TabElementContainer tabs = new TabElementContainer();
    tabs.add(title, view);
    return tabs;
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element newModulePageEdit(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName)
      throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    final Module module = getModule(request, moduleName);
    if (module instanceof ConfigPropertyModule) {
      final Form form = new Form(this.typeName);

      final ElementContainer fields = new ElementContainer(new TableBody());

      final TextField mavenModuleIdField = new TextField("mavenModuleId", 70, true);
      mavenModuleIdField.setInitialValue(module.getModuleDescriptor());
      FormGroupDecorator.decorate(fields, mavenModuleIdField, "Maven Module ID", null);

      final CheckBoxField enabledField = new CheckBoxField("enabled");
      enabledField.setInitialValue(module.isEnabled());
      FormGroupDecorator.decorate(fields, enabledField, "Enabled", null);

      form.add(fields);
      form.initialize(request);

      if (form.isPosted() && form.isMainFormTask()) {
        if (form.isValid()) {
          synchronized (this.MODULE_ADD_SYNC) {

            final String mavenModuleId = mavenModuleIdField.getValue();
            final boolean enabled = enabledField.isSelected();
            this.moduleLoader.setMavenModuleConfigProperties(moduleName, mavenModuleId, enabled);
            this.moduleLoader.refreshModules();

            HttpServletUtils.setPathVariable("moduleName", moduleName);

            final String url = getPageUrl("view", Collections.<String, Object> emptyMap());
            response.sendRedirect(url);
            return null;
          }
        }
      }

      final String title = "Edit " + getTitle();
      request.setAttribute("title", title);

      final Menu actionMenu = new Menu();
      addMenuItem(actionMenu, null, "view", "Cancel", "_top");
      addMenuItem(actionMenu, null, "edit", "Revert to Saved", "_top");
      final String name = form.getName();
      actionMenu.addMenuItem(new Menu("Save", "javascript:$('#" + name + "').submit()"));

      final ButtonsToolbarElement buttonsToolbar = new ButtonsToolbarElement(actionMenu);
      final ElementContainer view = new ElementContainer(form, buttonsToolbar);
      final TabElementContainer tabs = new TabElementContainer();
      tabs.add(title, view);
      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);

  }

  @RequestMapping(value = {
    "/admin/modules"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object newModulePageList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    HttpServletUtils.setAttribute("title", "Modules");
    checkAdminOrAnyModuleAdmin();
    return newDataTableHandler(request, "list", this::getModules);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Element newModulePageView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName)
      throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    final Module module = getModule(request, moduleName);
    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, module, null);

    final Map<String, Object> parameters = new HashMap<>();
    final Map<String, Object> filter = new HashMap<>();
    filter.put("MODULE_NAME", moduleName);
    parameters.put("filter", filter);

    parameters.put("serverSide", Boolean.FALSE);

    addTabDataTable(tabs, BusinessApplication.class.getName(), "moduleList", parameters);

    parameters.put("serverSide", Boolean.TRUE);

    addTabDataTable(tabs, ConfigProperty.CONFIG_PROPERTY, "moduleList", parameters);

    addTabDataTable(tabs, UserGroup.USER_GROUP, "moduleList", parameters);

    addTabDataTable(tabs, UserGroup.USER_GROUP, "moduleAdminList", parameters);

    return tabs;
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/delete"
  }, method = RequestMethod.POST)
  public void postModuleDelete(final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable("moduleName") final String moduleName) throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    final Module module = getModule(request, moduleName);
    this.moduleLoader.deleteModule(module);
    redirectPage("list");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/restart"
  }, method = RequestMethod.POST)
  public void postModuleRestart(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName)
      throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    final Module module = getModule(request, moduleName);
    module.restart();
    referrerRedirect(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/start"
  }, method = RequestMethod.POST)
  public void postModuleStart(final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable("moduleName") final String moduleName) throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    final Module module = getModule(request, moduleName);
    module.start();
    referrerRedirect(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/stop"
  }, method = RequestMethod.POST)
  public void postModuleStop(final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable("moduleName") final String moduleName) throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    final Module module = getModule(request, moduleName);
    module.stop();
    referrerRedirect(request);
  }

  public void setMavenRepository(final MavenRepository mavenRepository) {
    this.mavenRepository = mavenRepository;
  }

  public void setModuleLoader(final ConfigPropertyModuleLoader moduleLoader) {
    this.moduleLoader = moduleLoader;
  }
}
