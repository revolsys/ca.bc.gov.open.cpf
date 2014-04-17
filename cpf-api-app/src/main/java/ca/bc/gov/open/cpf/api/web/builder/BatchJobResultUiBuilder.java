package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
import com.revolsys.gis.data.query.And;
import com.revolsys.gis.data.query.Q;
import com.revolsys.gis.data.query.Query;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;

@Controller
public class BatchJobResultUiBuilder extends CpfUiBuilder {

  public BatchJobResultUiBuilder() {
    super("batchJobResult", BatchJobResult.BATCH_JOB_RESULT,
      BatchJobResult.SEQUENCE_NUMBER, "Batch Job Result", "Batch Job Results");
    setIdParameterName("sequenceNumber");
  }

  public DataObject getBatchJobResult(final Long batchJobId,
    final Long sequenceNumber) throws NoSuchRequestHandlingMethodException {
    final And where = Q.and(Q.equal(BatchJobResult.BATCH_JOB_ID, batchJobId),
      Q.equal(BatchJobResult.SEQUENCE_NUMBER, sequenceNumber));
    final Query query = new Query(BatchJobResult.BATCH_JOB_RESULT, where);
    final DataObject batchJobResult = getDataStore().queryFirst(query);

    if (batchJobResult != null) {
      return batchJobResult;
    }
    throw new NoSuchRequestHandlingMethodException(getRequest());
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/results/{sequenceNumber}/download"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  public void getModuleAppJobDownload(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId, @PathVariable final Long sequenceNumber)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkAdminOrModuleAdmin(moduleName);
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final DataObject batchJobResult = getBatchJobResult(batchJobId,
      sequenceNumber);

    final String resultDataUrl = batchJobResult.getValue(BatchJobResult.RESULT_DATA_URL);
    if (resultDataUrl != null) {
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", resultDataUrl);
    } else {
      final BatchJobService batchJobService = getBatchJobService();
      final InputStream in = batchJobService.getBatchJobResultData(batchJobId,
        sequenceNumber, batchJobResult);
      final String resultDataContentType = batchJobResult.getValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE);
      response.setContentType(resultDataContentType);
      final long size = batchJobService.getBatchJobResultSize(batchJobId,
        sequenceNumber, batchJobResult);

      final DataObjectWriterFactory writerFactory = IoFactoryRegistry.getInstance()
        .getFactoryByMediaType(DataObjectWriterFactory.class,
          resultDataContentType);
      if (writerFactory != null) {
        final String fileExtension = writerFactory.getFileExtension(resultDataContentType);
        final String fileName = "job-" + batchJobId + "-result-"
          + sequenceNumber + "." + fileExtension;
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

}
