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

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.api.scheduler.WorkerModuleState;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class WorkerModuleStateUiBuilder extends CpfUiBuilder {

  public WorkerModuleStateUiBuilder() {
    super("workerModule", "Worker Module", "Worker Modules");
    setIdParameterName("moduleName");
    setIdPropertyName("name");
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/modules/{moduleName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Element createModulePageView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws ServletException {
    checkAdminOrModuleAdmin(moduleName);
    final Module module = getModule(request, moduleName);
    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, module, "worker");

    return tabs;
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/modules"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageWorkerList(@PathVariable final String workerId) {
    checkHasAnyRole(ADMIN);
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException("The worker " + workerId
        + " could not be found. It may no longer be connected.");
    } else {
      final List<WorkerModuleState> modules = worker.getModules();
      return createDataTableHandlerOrRedirect(getRequest(),
        HttpServletUtils.getResponse(), "workerList", modules, Worker.class,
        "view");
    }
  }

}
