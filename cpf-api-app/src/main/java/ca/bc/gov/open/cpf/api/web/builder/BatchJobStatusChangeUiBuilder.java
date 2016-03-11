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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobStatusChange;

import com.revolsys.identifier.Identifier;
import com.revolsys.record.Record;
import com.revolsys.ui.web.annotation.PageMapping;
import com.revolsys.ui.web.exception.PageNotFoundException;

@Controller
public class BatchJobStatusChangeUiBuilder extends CpfUiBuilder {

  public BatchJobStatusChangeUiBuilder() {
    super("batchJobStatusChange", BatchJobStatusChange.BATCH_JOB_STATUS_CHANGE,
      BatchJobStatusChange.BATCH_JOB_STATUS_CHANGE_ID, "Batch Job Status Change",
      "Batch Job Status Changes");
  }

  @PageMapping(title = "Status Changes", fieldNames = {
    BatchJobStatusChange.JOB_STATUS, //
    BatchJobStatusChange.WHEN_CREATED
  })
  @RequestMapping(value = {
    "/ws/jobs/{batchJobId}/statusChanges"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object clientList(final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable("batchJobId") final Long batchJobId)
    throws IOException, NoSuchRequestHandlingMethodException {
    final String consumerKey = getConsumerKey();
    final Identifier batchJobIdentifier = Identifier.newIdentifier(batchJobId);
    final Record batchJob = getBatchJobService().getBatchJob(batchJobIdentifier, consumerKey);
    if (batchJob == null) {
      throw new PageNotFoundException("Batch Job " + batchJobIdentifier + " does not exist.");
    } else {

      final Map<String, Object> parameters = new HashMap<>();

      final Map<String, Object> filter = new HashMap<>();
      filter.put("BATCH_JOB_ID", batchJobId);
      parameters.put("filter", filter);

      return newDataTableHandlerOrRedirect(request, response, "clientList", BatchJob.BATCH_JOB,
        "clientList", parameters);
    }
  }

  @PageMapping(title = "Status Changes", fieldNames = {
    BatchJobStatusChange.JOB_STATUS, //
    BatchJobStatusChange.WHEN_CREATED, //
    BatchJobStatusChange.WHO_CREATED
  })
  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/jobs/{batchJobId}/statusChanges"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object moduleAppJobList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("batchJobId") final Long batchJobId)
    throws IOException, NoSuchRequestHandlingMethodException {
    checkAdminOrModuleAdmin(moduleName);
    getModuleBusinessApplication(moduleName, businessApplicationName);
    getBatchJob(businessApplicationName, batchJobId);

    final Map<String, Object> parameters = new HashMap<>();

    final Map<String, Object> filter = new HashMap<>();
    filter.put("BATCH_JOB_ID", batchJobId);
    parameters.put("filter", filter);

    return newDataTableHandlerOrRedirect(request, response, "moduleAppJobList", BatchJob.BATCH_JOB,
      "moduleAppView", parameters);
  }
}
