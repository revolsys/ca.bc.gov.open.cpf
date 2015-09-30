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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;

import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class BatchJobRequestExecutionGroupUiBuilder extends CpfUiBuilder {
  public BatchJobRequestExecutionGroupUiBuilder() {
    super("executionGroup", "Execution Group", "Execution Groups");
  }

  public List<BatchJobRequestExecutionGroup> getWorkerExecutionGroups() {
    final String workerId = HttpServletUtils.getPathVariable("workerId");
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException(
        "The worker " + workerId + " could not be found. It may no longer be connected.");
    } else {

      final List<BatchJobRequestExecutionGroup> executingGroups = worker.getExecutingGroups();
      return executingGroups;
    }
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/executingGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageWorkerList(@PathVariable("workerId") final String workerId) {
    checkHasAnyRole(ADMIN);
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException(
        "The worker " + workerId + " could not be found. It may no longer be connected.");
    } else {
      return createDataTableHandler(getRequest(), "workerList", this::getWorkerExecutionGroups);
    }
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/executingGroups/{executionGroupId}/restart"
  }, method = RequestMethod.POST)
  public void postWorkerRestart(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("workerId") final String workerId,
    @PathVariable("executionGroupId") final String executionGroupId) {
    checkHasAnyRole(ADMIN);
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker != null) {
      batchJobService.cancelGroup(worker, executionGroupId);
    }
    referrerRedirect(request);
  }
}
