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

public interface JobController {
  String GROUP_RESULTS = "groupResults";

  String GROUP_INPUTS = "groupInputs";

  String JOB_RESULTS = "jobResults";

  String JOB_INPUTS = "jobInputs";

  boolean cancelJob(long jobId);

  void createJobFile(long jobId, String path, long sequenceNumber, String contentType, Object data);

  void createJobInputFile(long jobId, String contentType, Object data);

  void deleteJob(long jobId);

  String getGroupInputString(long jobId, int sequenceNumber);

  Map<String, Object> getGroupResultMap(long jobId, int sequenceNumber);

  InputStream getJobInputStream(long jobId);

  long getJobResultSize(long jobId, int sequenceNumber);

  InputStream getJobResultStream(long jobId, int sequenceNumber);

  String getKey();

  void setGroupInput(long jobId, int sequenceNumber, String contentType, Object data);

  void setGroupResult(long jobId, int sequenceNumber, String contentType, Object data);

  void setJobResult(long jobId, int sequenceNumber, String contentType, Object data);
}
