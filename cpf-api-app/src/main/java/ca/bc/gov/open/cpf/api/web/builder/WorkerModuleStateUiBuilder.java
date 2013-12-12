package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.List;

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
  @PreAuthorize(ADMIN_OR_ANY_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Element createModulePageView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    final Module module = getModule(request, moduleName);
    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, module, "worker");

    return tabs;
  }

  @PostFilter(FILTER_ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  public List<Module> getPermittedModules() {
    return getBusinessApplicationRegistry().getModules();
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
      final List<WorkerModuleState> modules = worker.getModules();
      return createDataTableHandlerOrRedirect(getRequest(),
        HttpServletUtils.getResponse(), "workerList", modules, Worker.class,
        "view");
    }
  }

}
