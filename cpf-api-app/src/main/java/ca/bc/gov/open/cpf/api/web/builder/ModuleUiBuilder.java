package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.controller.ConfigPropertyModule;
import ca.bc.gov.open.cpf.api.controller.ConfigPropertyModuleLoader;
import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.UserGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.maven.MavenRepository;
import com.revolsys.parallel.process.InvokeMethodCallable;
import com.revolsys.ui.html.decorator.CollapsibleBox;
import com.revolsys.ui.html.decorator.TableBody;
import com.revolsys.ui.html.decorator.TableHeadingDecorator;
import com.revolsys.ui.html.fields.CheckBoxField;
import com.revolsys.ui.html.fields.TextField;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.MenuElement;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.model.Menu;
import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class ModuleUiBuilder extends CpfUiBuilder {

  private MavenRepository mavenRepository;

  private ConfigPropertyModuleLoader moduleLoader;

  private final Object MODULE_ADD_SYNC = new Object();

  private final Callable<Collection<? extends Object>> modulesCallable = new InvokeMethodCallable<Collection<? extends Object>>(
    this, "getModules");

  private final Callable<Collection<? extends Object>> workerModulesCallable = new InvokeMethodCallable<Collection<? extends Object>>(
    this, "getWorkerModules");

  public ModuleUiBuilder() {
    super("module", "Business Application Module",
      "Business Application Modules");
    setIdParameterName("moduleName");
    setIdPropertyName("name");
  }

  @RequestMapping(value = {
    "/admin/modules/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @PreAuthorize(ADMIN)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element createModulePageAdd(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException, ServletException {
    final Map<String, Object> parameters = new HashMap<String, Object>();

    final Form form = new Form(typeName);

    final ElementContainer fields = new ElementContainer(new TableBody());

    final TextField moduleNameField = new TextField("moduleName", 30, true);
    TableHeadingDecorator.addRow(fields, moduleNameField, "Module Name", null);

    final TextField mavenModuleIdField = new TextField("mavenModuleId", 70,
      true);
    TableHeadingDecorator.addRow(fields, mavenModuleIdField, "Maven Module ID",
      null);

    form.add(fields);
    form.initialize(request);

    if (form.isPosted() && form.isMainFormTask()) {
      if (form.isValid()) {
        synchronized (MODULE_ADD_SYNC) {
          final String moduleName = ((String)moduleNameField.getValue()).toUpperCase();
          final String mavenModuleId = mavenModuleIdField.getValue();

          synchronized (getBusinessApplicationRegistry()) {
            boolean valid = true;
            for (final Module module : getBusinessApplicationRegistry().getModules()) {
              if (moduleName.equalsIgnoreCase(module.getName())) {
                moduleNameField.addValidationError("Module Name is already used");
                valid = false;
              } else if (Module.RESERVED_MODULE_NAMES.contains(moduleName)) {
                mavenModuleIdField.addValidationError("Module Name is a reserved word");
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
              final boolean enabled = Boolean.TRUE;
              moduleLoader.setMavenModuleConfigProperties(moduleName,
                mavenModuleId, enabled);
              moduleLoader.refreshModules();

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
    actionMenu.addMenuItem(new Menu("Save", "javascript:$('#" + name
      + "').submit()"));

    final MenuElement actionMenuElement = new MenuElement(actionMenu,
      "actionMenu");
    final ElementContainer view = new ElementContainer(form, actionMenuElement);
    view.setDecorator(new CollapsibleBox(title, true));
    return view;
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element createModulePageEdit(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    final Module module = getModule(request, moduleName);
    if (module instanceof ConfigPropertyModule) {
      final Form form = new Form(typeName);

      final ElementContainer fields = new ElementContainer(new TableBody());

      final TextField mavenModuleIdField = new TextField("mavenModuleId", 70,
        true);
      mavenModuleIdField.setInitialValue(module.getModuleDescriptor());
      TableHeadingDecorator.addRow(fields, mavenModuleIdField,
        "Maven Module ID", null);

      final CheckBoxField enabledField = new CheckBoxField("enabled");
      enabledField.setInitialValue(module.isEnabled());
      TableHeadingDecorator.addRow(fields, enabledField, "Enabled", null);

      form.add(fields);
      form.initialize(request);

      if (form.isPosted() && form.isMainFormTask()) {
        if (form.isValid()) {
          synchronized (MODULE_ADD_SYNC) {

            final String mavenModuleId = mavenModuleIdField.getValue();
            final boolean enabled = enabledField.isSelected();
            moduleLoader.setMavenModuleConfigProperties(moduleName,
              mavenModuleId, enabled);
            moduleLoader.refreshModules();

            HttpServletUtils.setPathVariable("moduleName", moduleName);

            final String url = getPageUrl("view",
              Collections.<String, Object> emptyMap());
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
      actionMenu.addMenuItem(new Menu("Save", "javascript:$('#" + name
        + "').submit()"));

      final MenuElement actionMenuElement = new MenuElement(actionMenu,
        "actionMenu");
      final ElementContainer view = new ElementContainer(form,
        actionMenuElement);
      view.setDecorator(new CollapsibleBox(title, true));
      return view;
    }
    throw new NoSuchRequestHandlingMethodException(request);

  }

  @RequestMapping(value = {
    "/admin/modules"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY_OR_ANY_MODULE_ADMIN)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object createModulePageList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    return createDataTableHandler(request, "list", modulesCallable);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ANY_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Element createModulePageView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    final Module module = getModule(request, moduleName);
    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, module, null);

    final Map<String, Object> parameters = new HashMap<String, Object>();
    final Map<String, Object> filter = new HashMap<String, Object>();
    filter.put("MODULE_NAME", moduleName);
    parameters.put("filter", filter);

    parameters.put("serverSide", Boolean.FALSE);

    addTabDataTable(tabs, BusinessApplication.class.getName(), "moduleList",
      parameters);

    parameters.put("serverSide", Boolean.TRUE);

    addTabDataTable(tabs, ConfigProperty.CONFIG_PROPERTY, "moduleList",
      parameters);

    addTabDataTable(tabs, UserGroup.USER_GROUP, "moduleList", parameters);

    addTabDataTable(tabs, UserGroup.USER_GROUP, "moduleAdminList", parameters);

    parameters.put("serverSide", Boolean.FALSE);
    addTabDataTable(tabs, ModuleLogFile.class.getName(), "moduleList",
      parameters);

    return tabs;
  }

  public MavenRepository getMavenRepository() {
    return mavenRepository;
  }

  public ConfigPropertyModuleLoader getModuleLoader() {
    return moduleLoader;
  }

  @PostFilter(FILTER_ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  public List<Module> getPermittedModules() {
    return getBusinessApplicationRegistry().getModules();
  }

  public List<Module> getWorkerModules() {
    final String workerId = HttpServletUtils.getPathVariable("workerId");
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException("The worker " + workerId
        + " could not be found. It may no longer be connected.");
    } else {
      return worker.getLoadedModules();
    }
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/modules"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN)
  public Object pageWorkerList(@PathVariable final String workerId)
    throws IOException, NoSuchRequestHandlingMethodException {
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException("The worker " + workerId
        + " could not be found. It may no longer be connected.");
    } else {
      return createDataTableHandlerOrRedirect(getRequest(),
        HttpServletUtils.getResponse(), "workerList", workerModulesCallable,
        Worker.class, "view");
    }
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/delete"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postModuleDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    final Module module = getModule(request, moduleName);
    moduleLoader.deleteModule(module);
    referrerRedirect(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/restart"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postModuleRestart(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    final Module module = getModule(request, moduleName);
    module.restart();
    referrerRedirect(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/start"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postModuleStart(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    final Module module = getModule(request, moduleName);
    module.start();
    referrerRedirect(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/stop"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postModuleStop(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
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
