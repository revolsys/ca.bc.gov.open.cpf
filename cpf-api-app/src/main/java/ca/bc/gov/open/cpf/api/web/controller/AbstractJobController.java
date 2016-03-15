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
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.revolsys.identifier.Identifier;
import com.revolsys.io.FileUtil;
import com.revolsys.io.Reader;
import com.revolsys.record.io.format.csv.Csv;
import com.revolsys.util.Exceptions;

public abstract class AbstractJobController implements JobController {
  protected abstract long getFileSize(Identifier jobId, String path, int sequenceNumber);

  protected abstract InputStream getFileStream(final Identifier jobId, String path,
    int sequenceNumber);

  protected abstract InputStream getFileStream(final Identifier jobId, String path,
    int sequenceNumber, long fromIndex, long toIndex);

  @Override
  public String getGroupInputString(final Identifier jobId, final int sequenceNumber) {
    final InputStream inputStream = getFileStream(jobId, GROUP_INPUTS, sequenceNumber);
    if (inputStream == null) {
      return null;
    } else {
      final String inputData = FileUtil.getString(inputStream);
      return inputData;
    }
  }

  @Override
  public Reader<Map<String, Object>> getGroupResultReader(final Identifier jobId,
    final int sequenceNumber) {
    final InputStream in = getGroupResultStream(jobId, sequenceNumber);
    if (in == null) {
      return null;
    } else {
      return Csv.mapReader(in);
    }
  }

  @Override
  public InputStream getGroupResultStream(final Identifier jobId, final int sequenceNumber) {
    return getFileStream(jobId, GROUP_RESULTS, sequenceNumber);
  }

  @Override
  public InputStream getJobInputStream(final Identifier jobId) {
    return getFileStream(jobId, JOB_INPUTS, 1);
  }

  @Override
  public long getJobResultSize(final Identifier jobId, final int sequenceNumber) {
    return getFileSize(jobId, JOB_RESULTS, sequenceNumber);
  }

  @Override
  public InputStream getJobResultStream(final Identifier jobId, final int sequenceNumber) {
    return getFileStream(jobId, JOB_RESULTS, sequenceNumber);
  }

  @Override
  public InputStream getJobResultStream(final Identifier jobId, final int sequenceNumber,
    final long fromIndex, long toIndex) {
    return getFileStream(jobId, JOB_RESULTS, sequenceNumber, fromIndex, toIndex);
  }

  @Override
  public void newJobInputFile(final Identifier jobId, final String contentType, final Object data) {
    newJobFile(jobId, JOB_INPUTS, 1, contentType, data);
  }

  @Override
  public void setGroupError(final Identifier jobId, final int sequenceNumber, final Object data) {
    newJobFile(jobId, GROUP_ERRORS, sequenceNumber, "text/csv", data);
  }

  @Override
  public void setGroupInput(final Identifier jobId, final int sequenceNumber,
    final String contentType, final Object data) {
    newJobFile(jobId, GROUP_INPUTS, sequenceNumber, contentType, data);
  }

  @Override
  public void setGroupResult(final Identifier jobId, final int sequenceNumber,
    final InputStream in) {
    newJobFile(jobId, GROUP_RESULTS, sequenceNumber, "text/csv", in);
  }

  @Override
  public void setJobResult(final Identifier jobId, final int sequenceNumber,
    final String contentType, final Object data) {
    newJobFile(jobId, JOB_RESULTS, sequenceNumber, contentType, data);
  }

  protected void writeFile(final HttpServletResponse response, final Identifier jobId,
    final String path, final int sequenceNumber) throws IOException {
    final String baseName = "job-" + jobId + "-" + sequenceNumber + "-" + path;

    response.setContentType("application/csv");
    response.setHeader("Content-disposition", "attachment; filename=" + baseName + ".csv");
    try (
      OutputStream out = response.getOutputStream()) {
      try (
        final InputStream in = getFileStream(jobId, path, sequenceNumber)) {
        if (in != null) {
          FileUtil.copy(in, out);
        }
      } catch (final Throwable e) {
        Exceptions.throwUncheckedException(e);
      }
    }
  }

  @Override
  public void writeGroupInput(final HttpServletResponse response, final Identifier jobId,
    final int sequenceNumber) throws IOException {
    writeFile(response, jobId, GROUP_INPUTS, sequenceNumber);
  }

  @Override
  public void writeGroupResult(final HttpServletResponse response, final Identifier jobId,
    final int sequenceNumber) throws IOException {
    writeFile(response, jobId, GROUP_RESULTS, sequenceNumber);
  }
}
