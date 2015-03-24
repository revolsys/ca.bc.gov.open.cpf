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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobPostProcess;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobPreProcess;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobScheduler;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;

import com.revolsys.data.query.And;
import com.revolsys.data.query.Q;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordStore;
import com.revolsys.jdbc.io.DataSourceImpl;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.ui.html.decorator.CollapsibleBox;
import com.revolsys.ui.html.fields.Field;
import com.revolsys.ui.html.fields.NumberField;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.form.UiBuilderObjectForm;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.MenuElement;
import com.revolsys.ui.model.Menu;
import com.revolsys.ui.web.config.Page;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.Property;

@Controller
public class TuningUiBuilder extends CpfUiBuilder {

  @Resource
  private CpfConfig cpfConfig;

  @Resource
  private BatchJobPreProcess cpfJobPreProcess;

  @Resource
  private BatchJobPostProcess cpfJobPostProcess;

  @Resource
  private BatchJobScheduler cpfJobScheduler;

  @Resource
  private DataSourceImpl cpfDataSource;

  public TuningUiBuilder() {
    setTypeName("tuning");
    setTableName(ConfigProperty.CONFIG_PROPERTY);
  }

  private void addCounts(final List<Object> rows, final String name, final int active,
    final int size, final int largestPoolSize, final int maxSize) {
    final Map<String, Object> row = new LinkedHashMap<>();
    row.put("name", name);
    row.put("activeCount", active);
    row.put("currentSize", size);
    row.put("largestSize", largestPoolSize);
    row.put("maxSize", maxSize);

    rows.add(row);
  }

  private void addCounts(final List<Object> rows, final String title,
    final ThreadPoolExecutor pool, final int maxSize) {
    final int activeCount = pool.getActiveCount();
    final int poolSize = pool.getPoolSize();
    final int largestPoolSize = pool.getLargestPoolSize();
    addCounts(rows, title, activeCount, poolSize, largestPoolSize, maxSize);
  }

  @Override
  protected void addListMenuItems(final Menu menu, final String prefix) {
    addMenuItem(menu, null, "config", "Config");
  }

  @RequestMapping(value = {
    "/admin/tuning/config"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Object createPageConfig(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    checkHasAnyRole(ADMIN);
    final List<String> fieldNames = getKeyList("config");

    final Map<String, Object> updatedConfig = new HashMap<>();
    for (final String fieldName : fieldNames) {
      updatedConfig.put(fieldName, Property.get(this.cpfConfig, fieldName));
    }
    final Form form = createTableForm(updatedConfig, "config");
    form.initialize(request);
    for (final Field field : form.getFields().values()) {
      if (field instanceof NumberField) {
        final NumberField numberField = (NumberField)field;
        numberField.setMinimumValue(1);
      }
    }
    if (form.isPosted() && form.isMainFormTask()) {
      if (form.isValid()) {
        final RecordStore recordStore = getRecordStore();
        try (
          Transaction transaction = recordStore.createTransaction(Propagation.REQUIRES_NEW)) {
          for (final String fieldName : fieldNames) {
            final Field field = form.getField(fieldName);
            final Object value = field.getValue();
            Property.set(this.cpfConfig, fieldName, value);
            final Query query = new Query(ConfigProperty.CONFIG_PROPERTY);
            final And condition = new And();
            condition.add(Q.equal(ConfigProperty.ENVIRONMENT_NAME, ConfigProperty.DEFAULT));
            condition.add(Q.equal(ConfigProperty.MODULE_NAME, ConfigProperty.CPF_TUNING));
            condition.add(Q.equal(ConfigProperty.COMPONENT_NAME, ConfigProperty.GLOBAL));
            condition.add(Q.equal(ConfigProperty.PROPERTY_NAME, fieldName));
            query.setWhereCondition(condition);
            Record configProperty = recordStore.queryFirst(query);
            final boolean exists = configProperty != null;
            if (!exists) {
              configProperty = createObject();
              configProperty.setValue(ConfigProperty.ENVIRONMENT_NAME, ConfigProperty.DEFAULT);
              configProperty.setValue(ConfigProperty.MODULE_NAME, ConfigProperty.CPF_TUNING);
              configProperty.setValue(ConfigProperty.COMPONENT_NAME, ConfigProperty.GLOBAL);
              configProperty.setValue(ConfigProperty.PROPERTY_NAME, fieldName);
            }
            configProperty.setValue(ConfigProperty.PROPERTY_VALUE, value);
            configProperty.setValue(ConfigProperty.PROPERTY_VALUE_TYPE, "int");
            if (exists) {
              updateObject(configProperty);
            } else {
              insertObject(configProperty);
            }
          }
        }
        final String url = getPageUrl("list");
        redirectAfterCommit(url);
        return new ElementContainer();
      }
    }

    final Page page = getPage("config");

    final String title = page.getExpandedTitle();
    request.setAttribute("title", title);

    final Menu actionMenu = new Menu();
    addMenuItem(actionMenu, null, "list", "Cancel", "_top");
    addMenuItem(actionMenu, null, "config", "Revert to Saved", "_top");
    final String name = form.getName();
    actionMenu.addMenuItem(new Menu("Save", "javascript:$('#" + name + "').submit()"));

    final MenuElement actionMenuElement = new MenuElement(actionMenu, "actionMenu");
    final ElementContainer view = new ElementContainer(form, actionMenuElement);
    view.setDecorator(new CollapsibleBox(title, true));
    return view;
  }

  @RequestMapping(value = {
    "/admin/tuning"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object createPageList(final HttpServletRequest request, final HttpServletResponse response)
    throws IOException {
    checkAdminOrAnyModuleAdmin();
    HttpServletUtils.setAttribute("title", "Tuning");
    final List<Object> rows = new ArrayList<>();

    final int preProcessPoolSize = this.cpfConfig.getPreProcessPoolSize();
    addCounts(rows, "Pre Process Thread Pool Size", this.cpfJobPreProcess, preProcessPoolSize);

    final int schedulerPoolSize = this.cpfConfig.getSchedulerPoolSize();
    addCounts(rows, "Scheduler Thread Pool Size", this.cpfJobScheduler, schedulerPoolSize);

    final BatchJobService batchJobService = getBatchJobService();
    final int groupResultCount = batchJobService.getGroupResultCount();
    final int largestGroupResultCount = batchJobService.getLargestGroupResultCount();
    final int groupResultPoolSize = this.cpfConfig.getGroupResultPoolSize();
    addCounts(rows, "Group Result Thread Pool Size", groupResultCount, groupResultCount,
      largestGroupResultCount, groupResultPoolSize);

    final int postProcessPoolSize = this.cpfConfig.getPostProcessPoolSize();
    addCounts(rows, "Post Process Thread Pool Size", this.cpfJobPostProcess, postProcessPoolSize);

    addCounts(rows, "Database Connection Pool Size", this.cpfDataSource.getNumActive(),
      this.cpfDataSource.getNumActive() + this.cpfDataSource.getNumIdle(),
      this.cpfDataSource.getMaxTotal(), this.cpfDataSource.getMaxTotal());

    return createDataTableHandler(request, "list", rows);
  }

  @Override
  public boolean validateForm(final UiBuilderObjectForm form) {
    final int preProcessPoolSize = form.getField("preProcessPoolSize").getValue(Integer.class);
    final int postProcessPoolSize = form.getField("postProcessPoolSize").getValue(Integer.class);
    final int schedulerPoolSize = form.getField("schedulerPoolSize").getValue(Integer.class);
    final int groupResultPoolSize = form.getField("groupResultPoolSize").getValue(Integer.class);
    final Field databaseConnectionField = form.getField("databaseConnectionPoolSize");
    final int databaseConnectionPoolSize = databaseConnectionField.getValue(Integer.class);
    if (preProcessPoolSize + postProcessPoolSize + schedulerPoolSize + groupResultPoolSize > 0.9 * databaseConnectionPoolSize) {
      databaseConnectionField.addValidationError("Not enough database connections, at least 10% must be available for handling web service requests. preProcessPoolSize + schedulerPoolSize + groupResultPoolSize + postProcessPoolSize > 90% * databaseConnectionPoolSize");
      return false;
    }

    return true;
  }
}
