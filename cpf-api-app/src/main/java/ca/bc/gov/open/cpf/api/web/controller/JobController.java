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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.revolsys.identifier.Identifier;
import com.revolsys.io.Reader;
import com.revolsys.record.Record;
import com.revolsys.record.schema.RecordDefinition;

public interface JobController {
  String GROUP_RESULTS = "groupResults";

  String GROUP_ERRORS = "groupErrors";

  String GROUP_INPUTS = "groupInputs";

  String JOB_RESULTS = "jobResults";

  String JOB_INPUTS = "jobInputs";

  boolean cancelJob(Identifier batchJobId);

  void deleteJob(Identifier batchJobId);

  String getGroupInputString(Identifier batchJobId, int sequenceNumber);

  Reader<Map<String, Object>> getGroupResultReader(Identifier batchJobId, int sequenceNumber);

  InputStream getGroupResultStream(Identifier batchJobId, int sequenceNumber);

  InputStream getJobInputStream(Identifier batchJobId);

  long getJobResultSize(Identifier batchJobId, int sequenceNumber);

  InputStream getJobResultStream(Identifier batchJobId, int sequenceNumber);

  String getKey();

  void newJobFile(Identifier batchJobId, String path, long sequenceNumber, String contentType,
    Object data);

  void newJobInputFile(Identifier batchJobId, String contentType, Object data);

  void setGroupError(Identifier batchJobId, int sequenceNumber, Object data);

  void setGroupInput(Identifier batchJobId, int sequenceNumber, RecordDefinition recordDefinition,
    List<Record> requests);

  void setGroupInput(Identifier batchJobId, int sequenceNumber, String contentType, Object data);

  void setGroupResult(Identifier batchJobId, int sequenceNumber, InputStream in);

  void setJobResult(Identifier batchJobId, int sequenceNumber, String contentType, Object data);

  void writeGroupInput(HttpServletResponse response, Identifier batchJobId, int sequenceNumber)
    throws IOException;

  void writeGroupResult(HttpServletResponse response, Identifier batchJobId, int sequenceNumber)
    throws IOException;
}
