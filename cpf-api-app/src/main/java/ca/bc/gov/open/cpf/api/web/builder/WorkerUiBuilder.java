package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.parallel.process.InvokeMethodCallable;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.exception.PageNotFoundException;

@Controller
public class WorkerUiBuilder extends CpfUiBuilder implements
  CpfMethodSecurityExpressions {

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
  @PreAuthorize(ADMIN)
  public Object pageList() throws IOException,
    NoSuchRequestHandlingMethodException {
    return createDataTableHandler(getRequest(), "list", workersCallable);
  }

  @RequestMapping(value = {
    "/admin/workers/{workerId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  public ElementContainer pageView(@PathVariable final String workerId)
    throws ServletException {
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

      addTabDataTable(tabs, Module.class.getName(), "workerList", parameters);

      return tabs;
    }
  }
}
