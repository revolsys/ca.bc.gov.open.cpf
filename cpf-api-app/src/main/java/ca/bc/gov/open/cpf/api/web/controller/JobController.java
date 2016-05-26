/*
 * Copyright © 2008-2016, Province of British Columbia
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
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.PreProcessGroup;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.identifier.Identifier;
import com.revolsys.io.map.MapReader;

public interface JobController {
  String GROUP_RESULTS = "groupResults";

  String GROUP_ERRORS = "groupErrors";

  String GROUP_INPUTS = "groupInputs";

  String JOB_RESULTS = "jobResults";

  String JOB_INPUTS = "jobInputs";

  default void cancelJob(final Identifier batchJobId) {
  }

  default void deleteJob(final Identifier jobId) {
    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    dataAccessObject.deleteBatchJob(jobId);
  }

  CpfDataAccessObject getDataAccessObject();

  String getGroupInputContentType(Identifier batchJobId, int sequenceNumber);

  InputStream getGroupInputStream(Identifier batchJobId, int sequenceNumber);

  String getGroupInputString(Identifier batchJobId, int sequenceNumber);

  MapReader getGroupResultReader(Identifier batchJobId, int sequenceNumber);

  InputStream getGroupResultStream(Identifier batchJobId, int sequenceNumber);

  InputStream getJobInputStream(Identifier batchJobId);

  long getJobResultSize(Identifier batchJobId, int sequenceNumber);

  InputStream getJobResultStream(Identifier batchJobId, int sequenceNumber);

  InputStream getJobResultStream(Identifier batchJobId, int sequenceNumber, long fromIndex,
    long toIndex);

  String getKey();

  void newJobFile(Identifier batchJobId, String path, long sequenceNumber, String contentType,
    Object data);

  void newJobInputFile(Identifier batchJobId, String contentType, Object data);

  PreProcessGroup newPreProcessGroup(BusinessApplication businessApplication, BatchJob batchJob,
    Map<String, String> jobParameters, int groupSequenceNumber);

  void setGroupError(Identifier batchJobId, int sequenceNumber, Object data);

  void setGroupInput(Identifier batchJobId, int sequenceNumber, String contentType, Object data);

  void setGroupResult(Identifier batchJobId, int sequenceNumber, InputStream in);

  void setJobResult(Identifier batchJobId, int sequenceNumber, String contentType, Object data);

  void writeGroupInput(HttpServletResponse response, Identifier batchJobId, int sequenceNumber)
    throws IOException;

  void writeGroupResult(HttpServletResponse response, Identifier batchJobId, int sequenceNumber)
    throws IOException;
}
