package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;
import ca.bc.gov.open.cpf.api.web.rest.ClientWebService;

import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.model.PageInfo;

@Controller
public class BatchJobResultUiBuilder extends CpfUiBuilder implements
  CpfMethodSecurityExpressions {

  private final PageInfo userJobResultPage = new PageInfo(
    "Result file {batchJobResultId} for job {batchJobId}");

  private final PageInfo userJobResultsPage = new PageInfo(
    "Result files for job {batchJobId}");

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
      },
      method = {
        RequestMethod.GET, RequestMethod.POST
      })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  public void getModuleAppJobDownload(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable
    final String moduleName, @PathVariable
    final String businessApplicationName, @PathVariable
    final Long batchJobId, @PathVariable
    final Long batchJobResultId) throws NoSuchRequestHandlingMethodException,
    IOException {
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

  @RequestMapping(value = {
    "/ws/users/{userId}/jobs/{batchJobId}/results/{resultId}"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void getUserJobResult(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable
    final String userId, @PathVariable
    final long batchJobId, @PathVariable
    final long resultId) throws NoSuchRequestHandlingMethodException,
    IOException {
    CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
    final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);

    if (batchJob != null) {
      if (userId.equals(batchJob.getValue(BatchJob.USER_ID))) {
        final DataObject batchJobResult = dataAccessObject.getBatchJobResult(resultId);
        if (EqualsRegistry.INSTANCE.equals(batchJobId,
          batchJobResult.getValue(BatchJobResult.BATCH_JOB_ID))) {
          if (!BatchJob.MARKED_FOR_DELETION.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
            dataAccessObject.setBatchJobStatus(batchJob,
              BatchJob.DOWNLOAD_INITIATED);
            if (batchJobResult.getValue(BatchJobResult.DOWNLOAD_TIMESTAMP) == null) {
              final Timestamp timestamp = new Timestamp(
                System.currentTimeMillis());
              batchJobResult.setValue(BatchJobResult.DOWNLOAD_TIMESTAMP,
                timestamp);
            }
          }
        }
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

            long size = resultData.length();
            String jsonCallback = null;
            if (resultDataContentType.equals(MediaType.APPLICATION_JSON.toString())) {
              jsonCallback = request.getParameter("callback");
              if (StringUtils.hasText(jsonCallback)) {
                size += 3 + jsonCallback.length();
              }
            }
            final DataObjectWriterFactory writerFactory = IoFactoryRegistry.getInstance()
              .getFactoryByMediaType(DataObjectWriterFactory.class,
                resultDataContentType);
            if (writerFactory != null) {
              final String fileExtension = writerFactory.getFileExtension(resultDataContentType);
              final String fileName = "job-" + batchJobId + "-result-"
                + resultId + "." + fileExtension;
              response.setHeader("Content-Disposition", "attachment; filename="
                + fileName + ";size=" + size);
            }
            final ServletOutputStream out = response.getOutputStream();
            if (StringUtils.hasText(jsonCallback)) {
              out.write(jsonCallback.getBytes());
              out.write("(".getBytes());
            }
            FileUtil.copy(in, out);
            if (StringUtils.hasText(jsonCallback)) {
              out.write(");".getBytes());
            }
            return;
          } catch (final SQLException e) {
            LoggerFactory.getLogger(getClass()).error(
              "Unable to get result data", e);
            throw new HttpMessageNotWritableException(
              "Unable to get result data", e);
          }
        }
      }
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/ws/users/{userId}/jobs/{batchJobId}/results"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Object getUserJobResults(final HttpServletRequest request,
    @PathVariable("userId")
    final String userId, @PathVariable("batchJobId")
    final long batchJobId) throws NoSuchRequestHandlingMethodException {
    CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
    final DataObject batchJob = dataAccessObject.getBatchJob(userId, batchJobId);
    if (batchJob == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      if (isDataTableCallback(request)) {
        final Map<String, Object> parameters = new HashMap<String, Object>();

        final Map<String, Object> filter = new HashMap<String, Object>();
        filter.put(BatchJobResult.BATCH_JOB_ID, batchJobId);
        parameters.put("filter", filter);

        return createDataTableMap(request, "clientList", parameters);
      } else if (isHtmlPage(request)) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("serverSide", false);
        TabElementContainer tabs = new TabElementContainer();
        addTabDataTable(tabs, BatchJob.BATCH_JOB, "clientList", parameters);
        return tabs;
      } else {

        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("userId", userId);
        parameters.put("batchJobId", batchJobId);
        final PageInfo userJobResultsPage = ClientWebService.getPageInfo(
          this.userJobResultsPage, parameters);
        final List<DataObject> results = dataAccessObject.getBatchJobResults(batchJobId);
        if (batchJob.getValue(BatchJob.COMPLETED_TIMESTAMP) != null
          && !results.isEmpty()) {
          for (final DataObject batchJobResult : results) {
            final Number batchJobResultId = batchJobResult.getIdValue();
            parameters.put("batchJobResultId", batchJobResultId);
            final PageInfo resultPage = ClientWebService.getPageInfo(
              userJobResultPage, parameters);
            final String batchJobResultType = batchJobResult.getValue(BatchJobResult.BATCH_JOB_RESULT_TYPE);
            resultPage.setAttribute("batchJobResultType", batchJobResultType);
            resultPage.setAttribute("batchJobResultContentType",
              batchJobResult.getValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE));
            if (batchJobResultType.equals(BatchJobResult.OPAQUE_RESULT_DATA)) {
              resultPage.setAttribute("batchJobRequestSequenceNumber",
                batchJobResult.getValue(BatchJobResult.REQUEST_SEQUENCE_NUMBER));
            }
            userJobResultsPage.addPage(batchJobResultId + "/", resultPage);
          }
        }
      }
      request.setAttribute("title", userJobResultsPage.getTitle());
      return userJobResultsPage;
    }
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/results"
      },
      method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  public Object pageModuleAppJobList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable
    final String moduleName, @PathVariable
    final String businessApplicationName, @PathVariable
    final Long batchJobId) throws IOException,
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
      },
      method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  public ElementContainer pageModuleAppJobView(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable
    final String moduleName, @PathVariable
    final String businessApplicationName, @PathVariable
    final Long batchJobId, @PathVariable
    final Long batchJobResultId) throws IOException, ServletException {
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final DataObject batchJobResult = getBatchJobResult(batchJobId,
      batchJobResultId);

    TabElementContainer tabs = new TabElementContainer();
    addObjectViewPage(tabs, batchJobResult, "moduleAppJob");
    return tabs;
  }
}
