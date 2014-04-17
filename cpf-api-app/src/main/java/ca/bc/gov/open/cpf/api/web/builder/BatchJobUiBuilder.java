package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.io.xml.XmlWriter;
import com.revolsys.ui.html.builder.HtmlUiBuilder;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.DateUtil;
import com.revolsys.util.JavaBeanUtil;

@Controller
public class BatchJobUiBuilder extends CpfUiBuilder {

  public BatchJobUiBuilder() {
    super("batchJob", BatchJob.BATCH_JOB, BatchJob.BATCH_JOB_ID, "Batch Job",
      "Batch Jobs");
  }

  public void businessApplication(final XmlWriter out, final Object object) {
    final DataObject batchJob = (DataObject)object;
    final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
    final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
    final BusinessApplicationUiBuilder appBuilder = getBuilder(BusinessApplication.class);
    final Map<String, String> parameterKeys = new HashMap<String, String>();
    parameterKeys.put("moduleName", "moduleName");
    parameterKeys.put("businessApplicationName", "name");
    appBuilder.serializeLink(out, businessApplication, "name", "moduleView",
      parameterKeys);
  }

  @Override
  public Object getProperty(final Object object, final String keyName) {
    if (keyName.startsWith("BUSINESS_APPLICATION_NAME")
      && object instanceof DataObject) {
      final DataObject batchJob = (DataObject)object;
      final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
      BusinessApplication businessApplication = getBusinessApplicationRegistry().getBusinessApplication(
        businessApplicationName);
      if (businessApplication == null) {
        businessApplication = new BusinessApplication(businessApplicationName);
      }
      final String subKey = JavaBeanUtil.getSubName(keyName);
      if (StringUtils.hasText(subKey)) {
        final HtmlUiBuilder<?> uiBuilder = getBuilder(businessApplication);
        return uiBuilder.getProperty(businessApplication, subKey);
      } else {
        return businessApplication;
      }
    } else if (keyName.equals("jobStatusDate")) {
      return DateUtil.format("yyyy-MM-dd HH:mm:ss");
    } else {
      return super.getProperty(object, keyName);
    }
  }

  public void module(final XmlWriter out, final Object object) {
    final DataObject batchJob = (DataObject)object;
    final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);

    final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
    final Module module = businessApplication.getModule();
    final CpfUiBuilder appBuilder = getBuilder(Module.class);

    final Map<String, String> parameterKeys = new HashMap<String, String>();
    parameterKeys.put("moduleName", "name");
    appBuilder.serializeLink(out, module, "name", "view", parameterKeys);
  }

  @RequestMapping(value = {
    "/admin/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    checkAdminOrAnyModuleAdminExceptSecurity();
    HttpServletUtils.setAttribute("title", "Batch Jobs");
    return createDataTableHandler(request, "list");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleAppList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    getModuleBusinessApplication(moduleName, businessApplicationName);

    final Map<String, Object> parameters = new HashMap<String, Object>();

    final Map<String, Object> filter = new HashMap<String, Object>();
    filter.put("BUSINESS_APPLICATION_NAME", businessApplicationName);
    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response, "moduleAppList",
      BusinessApplication.class, "moduleView", parameters);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public ElementContainer pageModuleAppView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Integer batchJobId) throws IOException,
    ServletException {
    checkAdminOrAnyModuleAdminExceptSecurity();
    getModuleBusinessApplication(moduleName, businessApplicationName);
    final DataObject batchJob = getBatchJob(businessApplicationName, batchJobId);

    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, batchJob, "moduleApp");

    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("serverSide", Boolean.TRUE);

    addTabDataTable(tabs, BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP,
      "moduleAppJobList", parameters);

    addTabDataTable(tabs, BatchJobResult.BATCH_JOB_RESULT, "moduleAppJobList",
      parameters);

    return tabs;
  }

  @RequestMapping(value = {
    "/ws/jobs/{batchJobId}/cancel"
  }, method = RequestMethod.POST)
  public void postClientCancel(@PathVariable final long batchJobId) {
    final String consumerKey = getConsumerKey();
    final DataObject batchJob = getDataAccessObject().getBatchJob(consumerKey,
      batchJobId);
    if (batchJob == null) {
      throw new PageNotFoundException("The cloud job " + batchJobId
        + " does not exist");
    } else {
      final BatchJobService batchJobService = getBatchJobService();
      batchJobService.cancelBatchJob(batchJobId);
      redirectPage("clientList");
    }
  }

  @RequestMapping(value = {
    "/ws/jobs/{batchJobId}/delete"
  }, method = RequestMethod.POST)
  public void postClientDelete(@PathVariable final long batchJobId) {
    final String consumerKey = getConsumerKey();
    final DataObject batchJob = getDataAccessObject().getBatchJob(consumerKey,
      batchJobId);
    if (batchJob == null) {
      throw new PageNotFoundException("The cloud job " + batchJobId
        + " does not exist");
    } else {
      final BatchJobService batchJobService = getBatchJobService();
      batchJobService.deleteJob(batchJobId);
      redirectPage("clientList");
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/cancel"
      }, method = RequestMethod.POST)
  public void postModuleAppCancel(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId) throws IOException, ServletException {
    checkAdminOrAnyModuleAdminExceptSecurity();
    getModuleBusinessApplication(moduleName, businessApplicationName);
    final BatchJobService batchJobService = getBatchJobService();
    batchJobService.cancelBatchJob(batchJobId);
    final String url = request.getHeader("Referer");
    if (StringUtils.hasText(url) && url.indexOf("/apps") != -1) {
      redirectPage("moduleAppList");
    } else {
      redirectPage("list");
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/delete"
      }, method = RequestMethod.POST)
  public void postModuleAppDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId) throws IOException, ServletException {
    checkAdminOrAnyModuleAdminExceptSecurity();
    getModuleBusinessApplication(moduleName, businessApplicationName);
    final BatchJobService batchJobService = getBatchJobService();
    batchJobService.deleteJob(batchJobId);
    final String url = request.getHeader("Referer");
    if (StringUtils.hasText(url) && url.indexOf("/apps") != -1) {
      redirectPage("moduleAppList");
    } else {
      redirectPage("list");
    }
  }
}
