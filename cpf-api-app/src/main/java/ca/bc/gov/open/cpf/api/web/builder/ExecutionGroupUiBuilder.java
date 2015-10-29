/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.identifier.Identifier;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.PathName;
import com.revolsys.record.io.RecordWriterFactory;
import com.revolsys.ui.html.serializer.key.KeySerializer;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.Property;

@Controller
public class ExecutionGroupUiBuilder extends CpfUiBuilder {

  public ExecutionGroupUiBuilder() {
    super("executionGroup", PathName.newPathName("ExecutionGroup"), "sequenceNumber",
      "Execution Group", "Execution Groups");
    setIdParameterName("sequenceNumber");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{sequenceNumber}/inputData"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public void downloadInputData(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("batchJobId") final Long batchJobId,
    @PathVariable("sequenceNumber") final Integer sequenceNumber)
      throws NoSuchRequestHandlingMethodException, IOException {
    checkAdminOrModuleAdmin(moduleName);
    final BusinessApplication businessApplication = getModuleBusinessApplication(moduleName,
      businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);
    // final Record batchJobExecutionGroup =
    // getBatchJobExecutionGroup(batchJobId, sequenceNumber);
    if (businessApplication.isPerRequestInputData()) {
      // final String dataUrl =
      // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_URL);
      // if (dataUrl != null) {
      // response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      // response.setHeader("Location", dataUrl);
      // } else {
      // try {
      // final String contentType =
      // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE);
      // final Blob inputData =
      // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA);
      // writeOpaqueData(response, contentType, baseName, inputData);
      // } catch (final SQLException e) {
      // final String message = "Unable to get data for " + baseName;
      // LoggerFactory.getLogger(getClass()).error(message, e);
      // throw new HttpMessageNotWritableException(message, e);
      // }
      // }
    } else {
      final JobController jobController = getJobController();
      jobController.writeGroupInput(response, Identifier.create(batchJobId), sequenceNumber);
    }
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups/{sequenceNumber}/resultData"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public void downloadResultData(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("batchJobId") final Long batchJobId,
    @PathVariable("sequenceNumber") final Integer sequenceNumber)
      throws NoSuchRequestHandlingMethodException, IOException {
    checkAdminOrModuleAdmin(moduleName);
    final BusinessApplication businessApplication = getModuleBusinessApplication(moduleName,
      businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);
    // final Record batchJobExecutionGroup =
    // getBatchJobExecutionGroup(batchJobId, sequenceNumber);
    // final String contentType =
    // batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
    // final String baseName = "job-" + batchJobId + "-group-" + sequenceNumber
    // + "-result";
    if (businessApplication.isPerRequestResultData()) {
      // final String resultDataUrl =
      // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.RESULT_DATA_URL);
      // if (resultDataUrl != null) {
      // response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      // response.setHeader("Location", resultDataUrl);
      // } else {
      // try {
      // final Blob data =
      // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.RESULT_DATA);
      // writeOpaqueData(response, contentType, baseName, data);
      // } catch (final SQLException e) {
      // final String message = "Unable to get data for " + baseName;
      // Logger.getLogger(getClass()).error(message, e);
      // throw new HttpMessageNotWritableException(message, e);
      // }
      // }
    } else {
      getJobController().writeGroupResult(response, Identifier.create(batchJobId), sequenceNumber);
    }
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/groups"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleAppJobList(final HttpServletRequest request,
    @PathVariable("moduleName") final String moduleName,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("batchJobId") final Long batchJobId)
      throws IOException, NoSuchRequestHandlingMethodException {

    if (isDataTableCallback(request)) {
      checkAdminOrModuleAdmin(moduleName);

      final BusinessApplication businessApplication = getModuleBusinessApplication(moduleName,
        businessApplicationName);
      final BatchJob batchJob = getBatchJob(businessApplicationName, batchJobId);
      final int numRecords = batchJob.getNumSubmittedGroups();

      final Map<String, Object> response = new LinkedHashMap<>();
      response.put("draw", HttpServletUtils.getIntegerParameter(request, "draw"));
      response.put("recordsTotal", numRecords);
      response.put("recordsFiltered", numRecords);
      final List<List<String>> rows = new ArrayList<>();
      int recordCount = 50;
      final String lengthString = request.getParameter("length");
      if (Property.hasValue(lengthString)) {
        if (!"NAN".equalsIgnoreCase(lengthString)) {
          try {
            recordCount = Integer.valueOf(lengthString);
          } catch (final Throwable e) {
          }
        }
      }
      if (recordCount > 0) {
        int offset = HttpServletUtils.getIntegerParameter(request, "start");
        if (offset < 0) {
          offset = 0;
        }
        offset++;
        if (businessApplication.isPerRequestInputData()) {
        } else {
          int maxRow = offset + recordCount;
          if (maxRow > numRecords) {
            maxRow = numRecords;
          }

          final List<KeySerializer> serializers = getSerializers("moduleAppJobList", "list");
          final Map<String, Object> data = new HashMap<>();
          data.put("inputData", "");
          // <value>inputData</value>
          // <value>completedRequests</value>
          // <value>failedRequests</value>
          // <value>resultData</value>
          // <value>errorData</value>

          for (int sequenceNumber = offset; sequenceNumber < maxRow; sequenceNumber++) {
            data.put("sequenceNumber", sequenceNumber);
            final boolean completed = batchJob.isCompleted(sequenceNumber);
            data.put("completed", completed);
            if (completed) {
              data.put("resultData", "");
            }
            final List<String> row = new ArrayList<>();
            for (final KeySerializer serializer : serializers) {
              final String key = serializer.getKey();
              final Object value = data.get(key);
              if (value == null) {
                row.add("-");
              } else {
                final String html = serializer.toString(data);
                row.add(html);
              }
            }
            rows.add(row);
          }
        }
      }
      response.put("data", rows);
      return response;

    } else {
      return redirectToTab(BatchJob.BATCH_JOB, "moduleAppView", "moduleAppJobList");
    }
  }

  private void writeOpaqueData(final HttpServletResponse response, final String contentType,
    final String baseName, final Blob data) throws SQLException, IOException {
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
          response.setHeader("Content-Disposition",
            "attachment; filename=" + fileName + ";size=" + size);
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
