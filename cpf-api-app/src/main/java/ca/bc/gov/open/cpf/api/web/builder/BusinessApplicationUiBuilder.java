package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BusinessApplicationStatistics;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.beans.InvokeMethodCallable;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.io.DataObjectReader;
import com.revolsys.gis.data.io.ListDataObjectReader;
import com.revolsys.gis.data.model.ArrayDataObject;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.ui.html.decorator.CollapsibleBox;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.MenuElement;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.model.Menu;
import com.revolsys.ui.web.config.Page;
import com.revolsys.util.JavaBeanUtil;

@Controller
public class BusinessApplicationUiBuilder extends CpfUiBuilder {

  public BusinessApplicationUiBuilder() {
    super("businessApplication", "Business Application",
      "Business Applications");
    setIdParameterName("businessApplicationName");
    setIdPropertyName("name");
  }

  @RequestMapping("/ws/sample/input")
  @ResponseBody
  public DataObjectReader getSampleInputData() {
    final DataObjectMetaDataImpl metaData = new DataObjectMetaDataImpl(
      "/Buffer");
    final GeometryFactory factory = GeometryFactory.getFactory(3005, 2, 1000,
      1000);
    metaData.setGeometryFactory(factory);
    metaData.addAttribute("title", DataTypes.STRING);
    metaData.addAttribute("buffer", DataTypes.DOUBLE);
    metaData.addAttribute("geometry", DataTypes.GEOMETRY);

    final List<DataObject> objects = new ArrayList<DataObject>();

    final DataObject object1 = new ArrayDataObject(metaData);
    object1.setValue("title", "Buffered centroid of BC");
    object1.setValue("buffer", 10000);
    object1.setGeometryValue(factory.createPoint(921100.281, 1076394.357));
    objects.add(object1);

    final DataObject object2 = new ArrayDataObject(metaData);
    object2.setValue("title", "Stanley Park");
    object2.setValue("buffer", 1000);
    object2.setGeometryValue(factory.createPoint(1207714.288, 480508.637));
    objects.add(object2);

    final ListDataObjectReader reader = new ListDataObjectReader(metaData,
      objects);
    return reader;
  }

  @RequestMapping("/ws/sample/result")
  @ResponseBody
  public DataObjectReader getSampleResultData() {
    final DataObjectMetaDataImpl metaData = new DataObjectMetaDataImpl(
      "/Buffer");
    final GeometryFactory factory = GeometryFactory.getFactory(3005, 2, 1000,
      1000);
    metaData.setGeometryFactory(factory);
    metaData.addAttribute("sequenceNumber", DataTypes.INTEGER);
    metaData.addAttribute("resultNumber", DataTypes.INTEGER);
    metaData.addAttribute("title", DataTypes.STRING);
    metaData.addAttribute("buffer", DataTypes.DOUBLE);
    metaData.addAttribute("geometry", DataTypes.GEOMETRY);

    final List<DataObject> objects = new ArrayList<DataObject>();

    final DataObject object1 = new ArrayDataObject(metaData);
    object1.setValue("sequenceNumber", 1);
    object1.setValue("resultNumber", 1);
    object1.setValue("title", "Buffered centroid of BC");
    object1.setValue("buffer", 10000);
    object1.setGeometryValue(factory.createPoint(921100.281, 1076394.357)
      .buffer(10000));
    objects.add(object1);

    final DataObject object2 = new ArrayDataObject(metaData);
    object2.setValue("sequenceNumber", 2);
    object2.setValue("resultNumber", 1);
    object2.setValue("title", "Stanley Park");
    object2.setValue("buffer", 1000);
    object2.setGeometryValue(factory.createPoint(1207714.288, 480508.637)
      .buffer(1000));
    objects.add(object2);

    final ListDataObjectReader reader = new ListDataObjectReader(metaData,
      objects);
    return reader;
  }

  @RequestMapping(value = {
    "/admin/apps"
  }, method = RequestMethod.GET)
  @PreAuthorize(ADMIN_OR_MODULE_ANY_ADMIN_EXCEPT_SECURITY)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    final List<BusinessApplication> businessApplications = getBusinessApplications();
    final Map<String, Object> parameters = Collections.emptyMap();
    final ElementContainer table = createDataTable(request, "list", parameters,
      businessApplications);

    final TabElementContainer tabs = new TabElementContainer();
    tabs.add("Business Applications", table);
    return tabs;
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element pageModuleEdit(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String moduleName,
    final @PathVariable String businessApplicationName) throws IOException,
    ServletException {
    final BusinessApplicationRegistry businessApplicationRegistry = getBusinessApplicationRegistry();
    final BusinessApplication businessApplication = businessApplicationRegistry.getModuleBusinessApplication(
      moduleName, businessApplicationName);
    if (businessApplication != null) {
      final ConfigPropertyLoader configPropertyLoader = businessApplicationRegistry.getConfigPropertyLoader();
      final String componentName = "APP_" + businessApplicationName;
      final Map<String, Object> configProperties = configPropertyLoader.getConfigProperties(
        moduleName, componentName);
      final String pageName = "moduleEdit";
      final String viewName = "moduleView";

      final List<String> propertyNames = getKeyList(pageName);
      for (final String propertyName : propertyNames) {
        if (!configProperties.containsKey(propertyName)) {
          final Object value = JavaBeanUtil.getValue(businessApplication,
            propertyName);
          configProperties.put(propertyName, value);
        }
      }

      final Form form = createTableForm(configProperties, pageName);
      form.initialize(request);

      if (form.isPosted() && form.isMainFormTask()) {
        if (form.isValid()) {
          final CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
          final BusinessApplicationPlugin pluginMetaData = businessApplication.getPluginMetadata();
          for (final String propertyName : propertyNames) {
            final Object defaultValue = JavaBeanUtil.getValue(pluginMetaData,
              propertyName);
            final Object newValue = form.getValue(propertyName);
            final boolean equal = EqualsRegistry.INSTANCE.equals(defaultValue,
              newValue);

            DataObject configProperty = dataAccessObject.getConfigProperty(
              ConfigProperty.DEFAULT, moduleName, componentName, propertyName);
            if (configProperty == null) {
              if (!equal) {
                try {
                  final Class<?> propertyClass = PropertyUtils.getPropertyType(
                    businessApplication, propertyName);
                  final DataType valueType = DataTypes.getType(propertyClass);

                  configProperty = dataAccessObject.createConfigProperty(
                    ConfigProperty.DEFAULT, moduleName, componentName,
                    propertyName, newValue, valueType);
                } catch (final Throwable e) {
                  LoggerFactory.getLogger(getClass()).error(
                    "Unable to set property " + propertyName, e);
                }
              }
            } else {
              if (equal) {
                dataAccessObject.delete(configProperty);
              } else {
                dataAccessObject.setConfigPropertyValue(configProperty,
                  newValue);
                dataAccessObject.write(configProperty);
              }
            }
            JavaBeanUtil.setProperty(businessApplication, propertyName,
              newValue);
          }

          final Map<String, Object> parameters = new HashMap<String, Object>();

          final String url = getPageUrl(viewName, parameters);
          response.sendRedirect(url);
          return null;
        }
      }

      final Page page = getPage(pageName);

      final String title = page.getExpandedTitle();
      request.setAttribute("title", title);

      final Menu actionMenu = new Menu();
      addMenuItem(actionMenu, null, viewName, "Cancel", "_top");
      addMenuItem(actionMenu, null, pageName, "Revert to Saved", "_top");
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
    "/admin/modules/{moduleName}/apps"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageModuleList(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String moduleName)
    throws IOException, NoSuchRequestHandlingMethodException {
    final Module module = getModule(request, moduleName);
    final Callable<Collection<? extends Object>> rowsCallable = new InvokeMethodCallable<Collection<? extends Object>>(
      module, "getBusinessApplications");
    return createDataTableHandlerOrRedirect(request, response, "moduleList",
      rowsCallable, Module.class, "view");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Element pageModuleView(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String moduleName,
    final @PathVariable String businessApplicationName) throws IOException,
    ServletException {
    final BusinessApplication businessApplication = getBusinessApplicationRegistry().getModuleBusinessApplication(
      moduleName, businessApplicationName);
    if (businessApplication != null) {
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, businessApplication, "module");

      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("serverSide", Boolean.FALSE);

      addTabDataTable(tabs, BusinessApplicationStatistics.class.getName(),
        "moduleAppList", parameters);

      parameters.put("serverSide", Boolean.TRUE);
      addTabDataTable(tabs, BatchJob.BATCH_JOB, "moduleAppList", parameters);

      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }
}
