package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;

import com.revolsys.beans.InvokeMethodCallable;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class BatchJobRequestExecutionGroupUiBuilder extends CpfUiBuilder
  implements CpfMethodSecurityExpressions {

  private final Callable<Collection<? extends Object>> workerGroupsCallable = new InvokeMethodCallable<Collection<? extends Object>>(
    this, "getWorkerExecutionGroups");

  public BatchJobRequestExecutionGroupUiBuilder() {
    super("executionGroup", "Execution Group", "Execution Groups");
  }

  public List<BatchJobRequestExecutionGroup> getWorkerExecutionGroups() {
    final String workerId = HttpServletUtils.getPathVariable("workerId");
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException("The worker " + workerId
        + " could not be found. It may no longer be connected.");
    } else {

      final List<BatchJobRequestExecutionGroup> executingGroups = worker.getExecutingGroups();
      return executingGroups;
    }
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/executingGroups"
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
      return createDataTableHandler(getRequest(), "workerList",
        workerGroupsCallable);
    }
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/executingGroups/{executionGroupId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN)
  public ElementContainer pageWorkerView(@PathVariable final String workerId,
    @PathVariable final String executionGroupId) throws ServletException {
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker == null) {
      throw new PageNotFoundException("The worker " + workerId
        + " could not be found. It may no longer be connected.");
    } else {
      final BatchJobRequestExecutionGroup group = worker.getExecutingGroup(executionGroupId);
      if (group == null) {
        throw new PageNotFoundException("The group " + workerId
          + " could not be found. It may no longer be executing.");
      } else {
        final TabElementContainer tabs = new TabElementContainer();
        addObjectViewPage(tabs, worker, null);
        return tabs;
      }
    }
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}/executingGroups/{executionGroupId}/restart"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postWorkerRestart(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String workerId,
    @PathVariable final String executionGroupId) {
    final BatchJobService batchJobService = getBatchJobService();
    final Worker worker = batchJobService.getWorker(workerId);
    if (worker != null) {
      batchJobService.cancelGroup(worker, executionGroupId);
    }
    referrerRedirect(request);
  }
}
