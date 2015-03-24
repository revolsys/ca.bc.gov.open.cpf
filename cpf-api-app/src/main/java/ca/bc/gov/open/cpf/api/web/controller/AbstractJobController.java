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

import com.revolsys.io.FileUtil;
import com.revolsys.io.json.JsonParser;

public abstract class AbstractJobController implements JobController {
  @Override
  public void createJobInputFile(final long jobId, final String contentType, final Object data) {
    createJobFile(jobId, JOB_INPUTS, 1, contentType, data);
  }

  protected abstract long getFileSize(long jobId, String path, int sequenceNumber);

  protected abstract InputStream getFileStream(final long jobId, String path, int sequenceNumber);

  @Override
  public String getGroupInputString(final long jobId, final int sequenceNumber) {
    final InputStream inputStream = getFileStream(jobId, GROUP_INPUTS, sequenceNumber);
    if (inputStream == null) {
      return null;
    } else {
      final String inputData = FileUtil.getString(inputStream);
      return inputData;
    }
  }

  @Override
  public Map<String, Object> getGroupResultMap(final long jobId, final int sequenceNumber) {
    final InputStream in = getFileStream(jobId, GROUP_RESULTS, sequenceNumber);
    if (in == null) {
      return null;
    } else {
      final Map<String, Object> resultData = JsonParser.read(in);
      return resultData;
    }
  }

  @Override
  public InputStream getJobInputStream(final long jobId) {
    return getFileStream(jobId, JOB_INPUTS, 1);
  }

  @Override
  public long getJobResultSize(final long jobId, final int sequenceNumber) {
    return getFileSize(jobId, JOB_RESULTS, sequenceNumber);
  }

  @Override
  public InputStream getJobResultStream(final long jobId, final int sequenceNumber) {
    return getFileStream(jobId, JOB_RESULTS, sequenceNumber);
  }

  @Override
  public void setGroupInput(final long jobId, final int sequenceNumber, final String contentType,
    final Object data) {
    createJobFile(jobId, GROUP_INPUTS, sequenceNumber, contentType, data);
  }

  @Override
  public void setGroupResult(final long jobId, final int sequenceNumber, final String contentType,
    final Object data) {
    createJobFile(jobId, GROUP_RESULTS, sequenceNumber, contentType, data);
  }

  @Override
  public void setJobResult(final long jobId, final int sequenceNumber, final String contentType,
    final Object data) {
    createJobFile(jobId, JOB_RESULTS, sequenceNumber, contentType, data);
  }
}
