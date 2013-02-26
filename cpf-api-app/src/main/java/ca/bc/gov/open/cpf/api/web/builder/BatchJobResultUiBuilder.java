package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;

import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;

@Controller
public class BatchJobResultUiBuilder extends CpfUiBuilder implements
  CpfMethodSecurityExpressions {

  public BatchJobResultUiBuilder() {
    super("batchJobResult", BatchJobResult.BATCH_JOB_RESULT,
      BatchJobResult.BATCH_JOB_RESULT_ID, "Batch Job Result",
      "Batch Job Results");
  }

  public DataObject getBatchJobResult(final Long batchJobId,
    final Long batchJobResultId) throws NoSuchRequestHandlingMethodException {
    final DataObject batchJobResult = loadObject(batchJobResultId);
    if (batchJobResult != null) {
      if (EqualsRegistry.INSTANCE.equals(
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
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void getModuleAppJobDownload(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobResultId)
    throws NoSuchRequestHandlingMethodException, IOException {
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final DataObject batchJobResult = getBatchJobResult(batchJobId,
      batchJobResultId);

    final String resultDataUrl = batchJobResult.getValue(BatchJobResult.RESULT_DATA_URL);
    if (resultDataUrl != null) {
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", resultDataUrl);
    } else {
      try {
        final Blob resultData = batchJobResult.getValue(BatchJobResult.RESULT_DATA);
        final InputStream in = resultData.getBinaryStream();
        final String resultDataContentType = batchJobResult.getValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE);
        response.setContentType(resultDataContentType);
        final long size = resultData.length();

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
      } catch (final SQLException e) {
        Logger.getLogger(getClass()).error("Unable to get result data", e);
        throw new HttpMessageNotWritableException("Unable to get result data",
          e);
      }
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/results"
      }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageModuleAppJobList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId) throws IOException,
    NoSuchRequestHandlingMethodException {
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
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public ElementContainer pageModuleAppJobView(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobResultId) throws IOException,
    ServletException {
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final DataObject batchJobResult = getBatchJobResult(batchJobId,
      batchJobResultId);

    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, batchJobResult, "moduleAppJob");
    return tabs;
  }
}
