package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobRequest;
import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplication;

import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.Writer;
import com.revolsys.io.json.JsonDataObjectIoFactory;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;

@Controller
public class BatchJobRequestUiBuilder extends CpfUiBuilder implements
  CpfMethodSecurityExpressions {

  public BatchJobRequestUiBuilder() {
    super("batchJobRequest", BatchJobRequest.BATCH_JOB_REQUEST,
      BatchJobRequest.BATCH_JOB_REQUEST_ID, "Batch Job Request",
      "Batch Job Requests");
  }

  public DataObject getBatchJobRequest(final Long batchJobId,
    final Long batchJobRequestId) throws NoSuchRequestHandlingMethodException {
    final DataObject batchJobRequest = loadObject(batchJobRequestId);
    if (batchJobRequest != null) {
      if (EqualsRegistry.INSTANCE.equals(
        batchJobRequest.getValue(BatchJobRequest.BATCH_JOB_ID), batchJobId)) {
        return batchJobRequest;
      }
    }
    throw new NoSuchRequestHandlingMethodException(getRequest());
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/requests/{batchJobRequestId}/inputData"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void getModuleAppJobRequestInputDataDownload(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobRequestId)
    throws NoSuchRequestHandlingMethodException, IOException {
    final BusinessApplication businessApplication = getModuleBusinessApplication(
      moduleName, businessApplicationName);
    final DataObject batchJob = getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobRequest = getBatchJobRequest(batchJobId,
      batchJobRequestId);
    final String baseName = "job-" + batchJobId + "-request-"
      + batchJobRequestId + "-input";
    if (businessApplication.isPerRequestInputData()) {
      final String dataUrl = batchJobRequest.getValue(BatchJobRequest.INPUT_DATA_URL);
      if (dataUrl != null) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", dataUrl);
      } else {
        try {
          final String contentType = batchJobRequest.getValue(BatchJobRequest.INPUT_DATA_CONTENT_TYPE);
          final Blob inputData = batchJobRequest.getValue(BatchJobRequest.INPUT_DATA);
          writeOpaqueData(response, contentType, baseName, inputData);
        } catch (final SQLException e) {
          final String message = "Unable to get data for " + baseName;
          Logger.getLogger(getClass()).error(message, e);
          throw new HttpMessageNotWritableException(message, e);
        }
      }
    } else {
      final String contentType = batchJob.getValue(BatchJob.INPUT_DATA_CONTENT_TYPE);
      final DataObjectMetaData metaData = businessApplication.getRequestMetaData();
      final String dataString = batchJobRequest.getString(BatchJobRequest.STRUCTURED_INPUT_DATA);
      final DataObject dataObject = JsonDataObjectIoFactory.toDataObject(
        metaData, dataString);
      final List<DataObject> data = Collections.singletonList(dataObject);
      writeStructuredData(response, contentType, baseName, metaData, data);
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/requests/{batchJobRequestId}/resultData"
      }, method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void getModuleAppJobRequestResultDataDownload(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobRequestId)
    throws NoSuchRequestHandlingMethodException, IOException {
    final BusinessApplication businessApplication = getModuleBusinessApplication(
      moduleName, businessApplicationName);
    final DataObject batchJob = getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobRequest = getBatchJobRequest(batchJobId,
      batchJobRequestId);
    final String contentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
    final String baseName = "job-" + batchJobId + "-request-"
      + batchJobRequestId + "-result";
    if (businessApplication.isPerRequestResultData()) {
      final String resultDataUrl = batchJobRequest.getValue(BatchJobRequest.RESULT_DATA_URL);
      if (resultDataUrl != null) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", resultDataUrl);
      } else {
        try {
          final Blob data = batchJobRequest.getValue(BatchJobRequest.RESULT_DATA);
          writeOpaqueData(response, contentType, baseName, data);
        } catch (final SQLException e) {
          final String message = "Unable to get data for " + baseName;
          Logger.getLogger(getClass()).error(message, e);
          throw new HttpMessageNotWritableException(message, e);
        }
      }
    } else {
      final DataObjectMetaData metaData = businessApplication.getResultMetaData();
      final String dataString = batchJobRequest.getString(BatchJobRequest.STRUCTURED_RESULT_DATA);
      final List<DataObject> data = JsonDataObjectIoFactory.toDataObjectList(
        metaData, dataString);
      writeStructuredData(response, contentType, baseName, metaData, data);
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/requests"
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
    filter.put(BatchJobRequest.BATCH_JOB_ID, batchJobId);
    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response,
      "moduleAppJobList", BatchJob.BATCH_JOB, "moduleAppView", parameters);
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/requests/{batchJobRequestId}"
      }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public ElementContainer pageModuleAppJobView(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobRequestId) throws IOException,
    ServletException {
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobRequest = getBatchJobRequest(batchJobId,
      batchJobRequestId);

    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, batchJobRequest, "moduleAppJob");
    return tabs;
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

  private void writeStructuredData(final HttpServletResponse response,
    String contentType, final String baseName,
    final DataObjectMetaData metaData, final List<DataObject> data)
    throws IOException {
    if (!StringUtils.hasText(contentType)) {
      contentType = "application/json";
    }
    DataObjectWriterFactory writerFactory = IoFactoryRegistry.getInstance()
      .getFactoryByMediaType(DataObjectWriterFactory.class, contentType);
    String fileExtension;
    if (writerFactory == null) {
      fileExtension = "json";
      writerFactory = new JsonDataObjectIoFactory();
    } else {
      fileExtension = writerFactory.getFileExtension(contentType);

    }
    final String fileName = baseName + "." + fileExtension;
    response.setHeader("Content-Disposition", "attachment; filename="
      + fileName);

    final ServletOutputStream out = response.getOutputStream();
    try {
      final Writer<DataObject> writer = writerFactory.createDataObjectWriter(
        baseName, metaData, out);
      try {
        for (final DataObject object : data) {
          writer.write(object);
        }
      } finally {
        writer.close();
        writer.close();
      }
    } finally {
      out.close();
    }
  }
}
