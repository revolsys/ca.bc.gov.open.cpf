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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.api.scheduler.WorkerModuleState;

import com.revolsys.beans.InvokeMethodCallable;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class WorkerUiBuilder extends CpfUiBuilder {

  private final Callable<Collection<? extends Object>> workersCallable = new InvokeMethodCallable<Collection<? extends Object>>(
    this, "getWorkers");

  public WorkerUiBuilder() {
    super("worker", "Worker", "Workers");
  }

  public List<Worker> getWorkers() {
    final BatchJobService batchJobService = getBatchJobService();
    return batchJobService.getWorkers();
  }

  @RequestMapping(value = {
    "/admin/workers"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageList() {
    checkHasAnyRole(ADMIN);
    HttpServletUtils.setAttribute("title", "Workers");
    return createDataTableHandler(getRequest(), "list", workersCallable);
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public ElementContainer pageView(@PathVariable final String workerId) {
    checkHasAnyRole(ADMIN);
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException("The worker " + workerId
        + " could not be found. It may no longer be connected.");
    } else {
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, worker, null);

      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("serverSide", Boolean.FALSE);

      addTabDataTable(tabs, BatchJobRequestExecutionGroup.class.getName(),
        "workerList", parameters);

      addTabDataTable(tabs, WorkerModuleState.class.getName(), "workerList",
        parameters);

      return tabs;
    }
  }
}
