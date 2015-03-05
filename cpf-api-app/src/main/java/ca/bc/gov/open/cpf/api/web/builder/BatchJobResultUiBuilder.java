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
import java.util.Date;
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

import com.revolsys.data.io.RecordWriterFactory;
import com.revolsys.data.query.And;
import com.revolsys.data.query.Q;
import com.revolsys.data.query.Query;
import com.revolsys.data.record.Record;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.xml.XmlWriter;
import com.revolsys.util.DateUtil;

@Controller
public class BatchJobResultUiBuilder extends CpfUiBuilder {

  public BatchJobResultUiBuilder() {
    super("batchJobResult", BatchJobResult.BATCH_JOB_RESULT,
      BatchJobResult.SEQUENCE_NUMBER, "Batch Job Result", "Batch Job Results");
    setIdParameterName("sequenceNumber");
  }

  public void expiryDate(final XmlWriter out, final Object object)
    throws IOException {
    final Record batchJobResult = (Record)object;
    final Date completionTimestamp = batchJobResult.getValue(BatchJob.WHEN_CREATED);
    if (completionTimestamp == null) {
      out.append('-');
    } else {
      final java.sql.Date expiryDate = getBatchJobService().getExpiryDate(
        completionTimestamp);
      out.append(DateUtil.format("yyyy-MM-dd", expiryDate));
    }
  }

  public Record getBatchJobResult(final Long batchJobId,
    final Long sequenceNumber) throws NoSuchRequestHandlingMethodException {
    final And where = Q.and(Q.equal(BatchJobResult.BATCH_JOB_ID, batchJobId),
      Q.equal(BatchJobResult.SEQUENCE_NUMBER, sequenceNumber));
    final Query query = new Query(BatchJobResult.BATCH_JOB_RESULT, where);
    final Record batchJobResult = getRecordStore().queryFirst(query);

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

    final Record batchJobResult = getBatchJobResult(batchJobId, sequenceNumber);

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

      final RecordWriterFactory writerFactory = IoFactoryRegistry.getInstance()
          .getFactoryByMediaType(RecordWriterFactory.class, resultDataContentType);
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
