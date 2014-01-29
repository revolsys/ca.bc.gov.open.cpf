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
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.util.Compress;

@Controller
public class BatchJobExecutionGroupUiBuilder extends CpfUiBuilder {

  public BatchJobExecutionGroupUiBuilder() {
    super("batchJobExecutionGroup",
      BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP,
      BatchJobExecutionGroup.SEQUENCE_NUMBER, "Batch Job Request",
      "Batch Job Requests");
  }

  public DataObject getBatchJobExecutionGroup(final Long batchJobId,
    final Long sequenceNumber) throws NoSuchRequestHandlingMethodException {
    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    final DataObject batchJobExecutionGroup = dataAccessObject.getBatchJobExecutionGroup(
      batchJobId, sequenceNumber);
    if (batchJobExecutionGroup == null) {
      throw new NoSuchRequestHandlingMethodException(getRequest());
    }
    return batchJobExecutionGroup;
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{sequenceNumber}/inputData"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  public void getModuleAppJobRequestInputDataDownload(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId, @PathVariable final Long sequenceNumber)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkAdminOrModuleAdmin(moduleName);
    final BusinessApplication businessApplication = getModuleBusinessApplication(
      moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobExecutionGroup = getBatchJobExecutionGroup(
      batchJobId, sequenceNumber);
    final String baseName = "job-" + batchJobId + "-request-" + sequenceNumber
      + "-input";
    if (businessApplication.isPerRequestInputData()) {
      final String dataUrl = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_URL);
      if (dataUrl != null) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", dataUrl);
      } else {
        try {
          final String contentType = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE);
          final Blob inputData = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA);
          writeOpaqueData(response, contentType, baseName, inputData);
        } catch (final SQLException e) {
          final String message = "Unable to get data for " + baseName;
          Logger.getLogger(getClass()).error(message, e);
          throw new HttpMessageNotWritableException(message, e);
        }
      }
    } else {
      writeJson(response, batchJobExecutionGroup,
        BatchJobExecutionGroup.STRUCTURED_INPUT_DATA);
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{sequenceNumber}/resultData"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  public void getModuleAppJobRequestResultDataDownload(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId, @PathVariable final Long sequenceNumber)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkAdminOrModuleAdmin(moduleName);
    final BusinessApplication businessApplication = getModuleBusinessApplication(
      moduleName, businessApplicationName);
    final DataObject batchJob = getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobExecutionGroup = getBatchJobExecutionGroup(
      batchJobId, sequenceNumber);
    final String contentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
    final String baseName = "job-" + batchJobId + "-request-" + sequenceNumber
      + "-result";
    if (businessApplication.isPerRequestResultData()) {
      final String resultDataUrl = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.RESULT_DATA_URL);
      if (resultDataUrl != null) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", resultDataUrl);
      } else {
        try {
          final Blob data = batchJobExecutionGroup.getValue(BatchJobExecutionGroup.RESULT_DATA);
          writeOpaqueData(response, contentType, baseName, data);
        } catch (final SQLException e) {
          final String message = "Unable to get data for " + baseName;
          Logger.getLogger(getClass()).error(message, e);
          throw new HttpMessageNotWritableException(message, e);
        }
      }
    } else {
      writeJson(response, batchJobExecutionGroup,
        BatchJobExecutionGroup.STRUCTURED_RESULT_DATA);
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups"
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
    filter.put(BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response,
      "moduleAppJobList", BatchJob.BATCH_JOB, "moduleAppView", parameters);
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{sequenceNumber}"
      }, method = RequestMethod.GET)
  @ResponseBody
  public ElementContainer pageModuleAppJobView(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId, @PathVariable final Long sequenceNumber)
    throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobExecutionGroup = getBatchJobExecutionGroup(
      batchJobId, sequenceNumber);

    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, batchJobExecutionGroup, "moduleAppJob");
    return tabs;
  }

  protected void writeJson(final HttpServletResponse response,
    final DataObject batchJobExecutionGroup, final String field)
    throws IOException {
    String dataString = batchJobExecutionGroup.getString(field);
    response.setContentType("application/json");
    final java.io.Writer out = response.getWriter();
    if (StringUtils.hasText(dataString)) {
      if (dataString.charAt(0) != '{') {
        dataString = Compress.inflateBase64(dataString);
      }
      try {
        out.write(dataString);
      } finally {
        FileUtil.closeSilent(out);
      }
    } else {
      out.write("{}");
    }
  }

  private void writeOpaqueData(final HttpServletResponse response,
    final String contentType, final String baseName, final Blob data)
    throws SQLException, IOException {
    response.setContentType(contentType);
    if (data != null) {
      final InputStream in = data.getBinaryStream();
      try {
        final long size = data.length();

        final DataObjectWriterFactory writerFactory = IoFactoryRegistry.getInstance()
          .getFactoryByMediaType(DataObjectWriterFactory.class, contentType);
        if (writerFactory != null) {
          final String fileExtension = writerFactory.getFileExtension(contentType);
          final String fileName = baseName + "." + fileExtension;
          response.setHeader("Content-Disposition", "attachment; filename="
            + fileName + ";size=" + size);
        }
        final ServletOutputStream out = response.getOutputStream();
        try {
          FileUtil.copy(in, out);
        } finally {
          FileUtil.closeSilent(out);
        }
      } finally {
        FileUtil.closeSilent(in);
      }
    }
  }
}
