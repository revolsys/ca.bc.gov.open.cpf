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
package ca.bc.gov.open.cpf.api.web.controller;

import java.io.InputStream;
import java.util.Map;

import com.revolsys.data.record.Record;

public interface JobController {
  String GROUP_RESULTS = "groupResults";

  String GROUP_INPUTS = "groupInputs";

  String JOB_RESULTS = "jobResults";

  String JOB_INPUTS = "jobInputs";

  boolean cancelJob(long jobId);

  void createJobFile(long jobId, String path, long sequenceNumber,
    String contentType, Object data);

  void createJobInputFile(long jobId, String contentType, Object data);

  void deleteJob(long jobId);

  InputStream getJobResultData(long jobId, long sequenceNumber,
    Record batchJobResult);

  long getJobResultSize(long jobId, long sequenceNumber,
    Record batchJobResult);

  String getKey();

  Long getNonExecutingGroupSequenceNumber(Long jobId);

  String getStructuredInputData(long jobId, long executionGroupId);

  Map<String, Object> getStructuredResultData(long jobId,
    long executionGroupId, Record batchJobExecutionGroup);

  void setJobResultData(final long jobId, final Record batchJobResult,
    final Object resultData);

  void setStructuredInputData(long jobId, long executionGroupId,
    Record executionGroup, String structuredInputData);

  void setStructuredResultData(long jobId, long executionGroupId,
    Record executionGroup, String structuredInputData);
}
