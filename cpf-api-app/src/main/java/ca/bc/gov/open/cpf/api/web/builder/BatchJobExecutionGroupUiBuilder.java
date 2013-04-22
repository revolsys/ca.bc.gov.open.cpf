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
import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;

@Controller
public class BatchJobExecutionGroupUiBuilder extends CpfUiBuilder implements
  CpfMethodSecurityExpressions {

  public BatchJobExecutionGroupUiBuilder() {
    super("batchJobExecutionGroup", BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP,
      BatchJobExecutionGroup.BATCH_JOB_EXECUTION_GROUP_ID, "Batch Job Request",
      "Batch Job Requests");
  }

  public DataObject getBatchJobExecutionGroup(final Long batchJobId,
    final Long batchJobExecutionGroupId) throws NoSuchRequestHandlingMethodException {
    final DataObject batchJobExecutionGroup = loadObject(batchJobExecutionGroupId);
    if (batchJobExecutionGroup != null) {
      if (EqualsRegistry.INSTANCE.equals(
        batchJobExecutionGroup.getValue(BatchJobExecutionGroup.BATCH_JOB_ID), batchJobId)) {
        return batchJobExecutionGroup;
      }
    }
    throw new NoSuchRequestHandlingMethodException(getRequest());
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{batchJobExecutionGroupId}/inputData"
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
    @PathVariable final Long batchJobExecutionGroupId)
    throws NoSuchRequestHandlingMethodException, IOException {
    final BusinessApplication businessApplication = getModuleBusinessApplication(
      moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobExecutionGroup = getBatchJobExecutionGroup(batchJobId,
      batchJobExecutionGroupId);
    final String baseName = "job-" + batchJobId + "-request-"
      + batchJobExecutionGroupId + "-input";
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

  protected void writeJson(final HttpServletResponse response,
    final DataObject batchJobExecutionGroup, String field) throws IOException {
    final String dataString = batchJobExecutionGroup.getString(field);
    response.setContentType("application/json");
    final java.io.Writer out = response.getWriter();
    try {
      out.write(dataString);
    } finally {
      FileUtil.closeSilent(out);
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{batchJobExecutionGroupId}/resultData"
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
    @PathVariable final Long batchJobExecutionGroupId)
    throws NoSuchRequestHandlingMethodException, IOException {
    final BusinessApplication businessApplication = getModuleBusinessApplication(
      moduleName, businessApplicationName);
    final DataObject batchJob = getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobExecutionGroup = getBatchJobExecutionGroup(batchJobId,
      batchJobExecutionGroupId);
    final String contentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
    final String baseName = "job-" + batchJobId + "-request-"
      + batchJobExecutionGroupId + "-result";
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
    filter.put(BatchJobExecutionGroup.BATCH_JOB_ID, batchJobId);
    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response,
      "moduleAppJobList", BatchJob.BATCH_JOB, "moduleAppView", parameters);
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{batchJobExecutionGroupId}"
      }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public ElementContainer pageModuleAppJobView(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String businessApplicationName,
    @PathVariable final Long batchJobId,
    @PathVariable final Long batchJobExecutionGroupId) throws IOException,
    ServletException {
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);
    final DataObject batchJobExecutionGroup = getBatchJobExecutionGroup(batchJobId,
      batchJobExecutionGroupId);

    final TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, batchJobExecutionGroup, "moduleAppJob");
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
}
