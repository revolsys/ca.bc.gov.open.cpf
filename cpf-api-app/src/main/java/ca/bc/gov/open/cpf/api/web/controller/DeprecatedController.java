package ca.bc.gov.open.cpf.api.web.controller;

import java.util.Collections;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ca.bc.gov.open.cpf.api.web.builder.BatchJobResultUiBuilder;
import ca.bc.gov.open.cpf.api.web.builder.BatchJobUiBuilder;
import ca.bc.gov.open.cpf.api.web.builder.BusinessApplicationUiBuilder;

import com.revolsys.ui.web.rest.interceptor.MediaTypeUtil;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.UrlUtil;

@Controller
@Deprecated
public class DeprecatedController {

  @Resource(
      name = "ca.bc.gov.open.cpf.plugin.impl.BusinessApplication-htmlbuilder")
  private BusinessApplicationUiBuilder appBuilder;

  @Resource(name = "/CPF/CPF_BATCH_JOBS-htmlbuilder")
  private BatchJobUiBuilder jobBuilder;

  @Resource(name = "/CPF/CPF_BATCH_JOB_RESULTS-htmlbuilder")
  private BatchJobResultUiBuilder resultBuilder;

  @RequestMapping("/ws/apps/{businessApplicationName}/{businessApplicationVersion}/instant")
  public Object getBusinessApplicationsInstant(
    @PathVariable final String businessApplicationName) {
    return appBuilder.redirectPage("clientInstant");
  }

  @RequestMapping("/ws/apps/{businessApplicationName}/{businessApplicationVersion}/multiple")
  public Object getBusinessApplicationsMultiple(
    @PathVariable final String businessApplicationName) {
    return appBuilder.redirectPage("clientMultiple");
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}",
    "/ws/users/{consumerKey}/apps/{businessApplicationName}"
  })
  public Object getBusinessApplicationsResources(
    @PathVariable final String businessApplicationName) {
    return appBuilder.redirectPage("clientView");
  }

  @RequestMapping("/ws/apps/{businessApplicationName}/{businessApplicationVersion}/single")
  public Object getBusinessApplicationsSingle(
    @PathVariable final String businessApplicationName) {
    return appBuilder.redirectPage("clientSingle");
  }

  @RequestMapping(value = {
    "/ws/users", "/ws/users/{userId}"
  })
  public void getUsers() {
    if (MediaTypeUtil.isHtmlPage()) {
      final String url = HttpServletUtils.getFullUrl("/ws/");
      HttpServletUtils.sendRedirect(url);
    } else {
      sendRedirectWithExtension("/ws/");
    }
  }

  @RequestMapping("/ws/users/{consumerKey}/apps")
  public Object getUsersBusinessApplications() {
    return appBuilder.redirectPage("clientList");
  }

  @RequestMapping(
      value = "/ws/users/{consumerKey}/apps/{businessApplicationName}/jobs/{jobId}")
  public Object getUsersBusinessApplicationsJobs() {
    return jobBuilder.redirectPage("clientAppList");
  }

  @RequestMapping("/ws/users/{consumerKey}/apps/{businessApplicationName}")
  public Object getUsersBusinessApplicationsView() {
    return appBuilder.redirectPage("clientView");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs/{jobId}")
  public Object getUsersJob() {
    return jobBuilder.redirectPage("clientView");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs")
  public Object getUsersJobs() {
    return jobBuilder.redirectPage("clientList");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs/{jobId}/cancel")
  public Object getUsersJobsCancel() {
    return jobBuilder.redirectPage("clientCancel");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs/{batchJobId}/results")
  public Object getUsersJobsResults() {
    return resultBuilder.redirectPage("clientList");
  }

  @RequestMapping(
      value = "/ws/users/{consumerKey}/jobs/{batchJobId}/results/{resultId}")
  public Object getUsersJobsResultsView() {
    return resultBuilder.redirectPage("clientView");
  }

  @RequestMapping(value = {
    "/ws/users/{consumerKey}/jobs/{batchJobId}/cancel"
  }, method = RequestMethod.POST)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postClientCancel(@PathVariable final long batchJobId) {
    jobBuilder.postClientCancel(batchJobId);
  }

  private Void sendRedirectWithExtension(final String path) {
    String url = MediaTypeUtil.getUrlWithExtension(path);
    final String callback = HttpServletUtils.getParameter("callback");
    if (StringUtils.hasText(callback)) {
      url = UrlUtil.getUrl(url, Collections.singletonMap("callback", callback));
    }
    HttpServletUtils.sendRedirect(url);
    return null;
  }
}
