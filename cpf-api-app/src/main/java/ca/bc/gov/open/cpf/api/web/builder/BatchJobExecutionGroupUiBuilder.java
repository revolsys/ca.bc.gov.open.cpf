package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.data.io.RecordWriterFactory;
import com.revolsys.data.record.Record;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.io.xml.XmlWriter;
import com.revolsys.ui.html.serializer.key.ActionFormKeySerializer;
import com.revolsys.ui.html.serializer.key.BooleanImageKeySerializer;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.util.Property;

@Controller
public class BatchJobExecutionGroupUiBuilder extends CpfUiBuilder {

  public BatchJobExecutionGroupUiBuilder() {
    super("batchJobExecutionGroup",
      BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP,
      BatchJobExecutionGroup.SEQUENCE_NUMBER, "Batch Job Request",
        "Batch Job Requests");
    setIdParameterName("sequenceNumber");
  }

  public void completed(final XmlWriter out, final Object object) {
    final Record executionGroup = (Record)object;
    final boolean completed = Boolean.TRUE.equals(JavaBeanUtil.getBooleanValue(
      executionGroup, BatchJobExecutionGroup.COMPLETED_IND));
    if (completed) {
      final List<String> parameterNames = Collections.emptyList();
      final Map<String, String> map = Collections.emptyMap();
      ActionFormKeySerializer.serialize(out, executionGroup, this,
        parameterNames, map, "_top", "Result", null, "resultData",
          "ui-auto-button-disk");
    } else {
      BooleanImageKeySerializer.serialize(out, executionGroup,
        BatchJobExecutionGroup.COMPLETED_IND);
    }

  }

  public Record getBatchJobExecutionGroup(final Long batchJobId,
    final Long sequenceNumber) throws NoSuchRequestHandlingMethodException {
    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    final Record batchJobExecutionGroup = dataAccessObject.getBatchJobExecutionGroup(
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
    final Record batchJobExecutionGroup = getBatchJobExecutionGroup(batchJobId,
      sequenceNumber);
    final String baseName = "job-" + batchJobId + "-group-" + sequenceNumber
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
      final String inputData = getBatchJobService().getJobController(batchJobId)
          .getStructuredInputData(batchJobId, sequenceNumber);
      writeJson(response, baseName, inputData);
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
    final Record batchJob = getBatchJob(businessApplicationName, batchJobId);
    final Record batchJobExecutionGroup = getBatchJobExecutionGroup(batchJobId,
      sequenceNumber);
    final String contentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
    final String baseName = "job-" + batchJobId + "-group-" + sequenceNumber
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
      final Map<String, Object> resultData = getBatchJobService().getJobController(
        batchJobId)
        .getStructuredResultData(batchJobId, sequenceNumber,
          batchJobExecutionGroup);
      writeJson(response, baseName, resultData);
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

  private void writeJson(final HttpServletResponse response,
    final String filename, final Map<String, Object> map) throws IOException {
    response.setContentType("application/json");
    response.setHeader("Content-disposition", "attachment; filename="
        + filename + ".json");
    try (
        java.io.Writer out = response.getWriter()) {
      if (map != null) {
        out.write(JsonMapIoFactory.toString(map));
      } else {
        out.write("{}");
      }
    }
  }

  private void writeJson(final HttpServletResponse response,
    final String filename, final String content) throws IOException {
    response.setContentType("application/json");
    response.setHeader("Content-disposition", "attachment; filename="
        + filename + ".json");
    try (
        java.io.Writer out = response.getWriter()) {
      if (Property.hasValue(content)) {
        out.write(content);
      } else {
        out.write("{}");
      }
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

        final RecordWriterFactory writerFactory = IoFactoryRegistry.getInstance()
            .getFactoryByMediaType(RecordWriterFactory.class, contentType);
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
