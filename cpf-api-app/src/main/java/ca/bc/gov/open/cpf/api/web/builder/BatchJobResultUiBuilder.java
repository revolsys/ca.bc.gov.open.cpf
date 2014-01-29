package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;

import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.model.data.equals.EqualsInstance;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;

@Controller
public class BatchJobResultUiBuilder extends CpfUiBuilder {

  public BatchJobResultUiBuilder() {
    super("batchJobResult", BatchJobResult.BATCH_JOB_RESULT,
      BatchJobResult.BATCH_JOB_RESULT_ID, "Batch Job Result",
      "Batch Job Results");
  }

  public DataObject getBatchJobResult(final Long batchJobId,
    final Long batchJobResultId) throws NoSuchRequestHandlingMethodException {
    final DataObject batchJobResult = loadObject(batchJobResultId);
    if (batchJobResult != null) {
      if (EqualsInstance.INSTANCE.equals(
        batchJobResult.getValue(BatchJobResult.BATCH_JOB_ID), batchJobId)) {
        return batchJobResult;
      }
    }
    throw new NoSuchRequestHandlingMethodException(getRequest());
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/results/{batchJobResultId}/download"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  public void getModuleAppJobDownload(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobResultId)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkAdminOrModuleAdmin(moduleName);
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final DataObject batchJobResult = getBatchJobResult(batchJobId,
      batchJobResultId);

    final String resultDataUrl = batchJobResult.getValue(BatchJobResult.RESULT_DATA_URL);
    if (resultDataUrl != null) {
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", resultDataUrl);
    } else {
      final BatchJobService batchJobService = getBatchJobService();
      final InputStream in = batchJobService.getBatchJobResultData(batchJobId,
        batchJobResultId, batchJobResult);
      final String resultDataContentType = batchJobResult.getValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE);
      response.setContentType(resultDataContentType);
      final long size = batchJobService.getBatchJobResultSize(batchJobId,
        batchJobResultId, batchJobResult);

      final DataObjectWriterFactory writerFactory = IoFactoryRegistry.getInstance()
        .getFactoryByMediaType(DataObjectWriterFactory.class,
          resultDataContentType);
      if (writerFactory != null) {
        final String fileExtension = writerFactory.getFileExtension(resultDataContentType);
        final String fileName = "job-" + batchJobId + "-result-"
          + batchJobResultId + "." + fileExtension;
        response.setHeader("Content-Disposition", "attachment; filename="
          + fileName + ";size=" + size);
      }
      final ServletOutputStream out = response.getOutputStream();

      FileUtil.copy(in, out);
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/results"
      }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleAppJobList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId) throws IOException,
    NoSuchRequestHandlingMethodException {
    checkAdminOrModuleAdmin(moduleName);
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final Map<String, Object> parameters = new HashMap<String, Object>();

    final Map<String, Object> filter = new HashMap<String, Object>();
    filter.put("BATCH_JOB_ID", batchJobId);
    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response,
      "moduleAppJobList", BatchJob.BATCH_JOB, "moduleAppView", parameters);
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/results/{batchJobResultId}"
      }, method = RequestMethod.GET)
  @ResponseBody
  public ElementContainer pageModuleAppJobView(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobResultId) throws IOException,
    ServletException {
    checkAdminOrModuleAdmin(moduleName);
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final DataObject batchJobResult = getBatchJobResult(batchJobId,
      batchJobResultId);

    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, batchJobResult, "moduleAppJob");
    return tabs;
  }
}
