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

import java.io.File;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

import ca.bc.gov.open.cpf.api.domain.BatchJobFile;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

import com.revolsys.identifier.Identifier;
import com.revolsys.io.FileUtil;
import com.revolsys.record.Record;
import com.revolsys.record.io.format.csv.CsvRecordWriter;
import com.revolsys.record.query.Q;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.WrappedException;

public class DatabaseJobController extends AbstractJobController {
  private final CpfDataAccessObject dataAccessObject;

  private final RecordStore recordStore;

  public DatabaseJobController(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
    this.recordStore = dataAccessObject.getRecordStore();
  }

  @Override
  public void deleteJob(final Identifier jobId) {
    this.dataAccessObject.deleteBatchJob(jobId);
  }

  @Override
  protected String getFileContentType(final Identifier jobId, final String path,
    final int sequenceNumber) {
    final Query query = new Query(BatchJobFile.BATCH_JOB_FILE);
    query.setFieldNames(BatchJobFile.CONTENT_TYPE);
    query.and(Q.equal(BatchJobFile.BATCH_JOB_ID, jobId));
    query.and(Q.equal(BatchJobFile.PATH, path));
    query.and(Q.equal(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber));
    final Record file = this.recordStore.getRecords(query).getFirst();
    if (file != null) {
      return file.getValue(BatchJobFile.CONTENT_TYPE);
    }
    return null;
  }

  @Override
  protected long getFileSize(final Identifier jobId, final String path, final int sequenceNumber) {
    final Query query = new Query(BatchJobFile.BATCH_JOB_FILE);
    query.setFieldNames(BatchJobFile.DATA);
    query.and(Q.equal(BatchJobFile.BATCH_JOB_ID, jobId));
    query.and(Q.equal(BatchJobFile.PATH, path));
    query.and(Q.equal(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber));
    final Record file = this.recordStore.getRecords(query).getFirst();
    if (file != null) {
      try {
        final Blob resultData = file.getValue(BatchJobFile.DATA);
        return resultData.length();
      } catch (final SQLException e) {
        throw new WrappedException(e);
      }
    }
    return 0;
  }

  @Override
  protected InputStream getFileStream(final Identifier jobId, final String path,
    final int sequenceNumber) {
    final Query query = new Query(BatchJobFile.BATCH_JOB_FILE);
    query.setFieldNames(BatchJobFile.DATA);
    query.and(Q.equal(BatchJobFile.BATCH_JOB_ID, jobId));
    query.and(Q.equal(BatchJobFile.PATH, path));
    query.and(Q.equal(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber));
    final Record file = this.recordStore.getRecords(query).getFirst();
    if (file != null) {
      try {
        final Blob resultData = file.getValue(BatchJobFile.DATA);
        return resultData.getBinaryStream();
      } catch (final SQLException e) {
        throw new WrappedException(e);
      }
    }
    return null;
  }

  @Override
  protected InputStream getFileStream(final Identifier jobId, final String path,
    final int sequenceNumber, final long fromIndex, final long toIndex) {
    final Query query = new Query(BatchJobFile.BATCH_JOB_FILE);
    query.setFieldNames(BatchJobFile.DATA);
    query.and(Q.equal(BatchJobFile.BATCH_JOB_ID, jobId));
    query.and(Q.equal(BatchJobFile.PATH, path));
    query.and(Q.equal(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber));
    final Record file = this.recordStore.getRecords(query).getFirst();
    if (file != null) {
      try {
        final Blob resultData = file.getValue(BatchJobFile.DATA);
        return resultData.getBinaryStream(fromIndex, toIndex - fromIndex);
      } catch (final SQLException e) {
        throw new WrappedException(e);
      }
    }
    return null;
  }

  @Override
  public String getGroupInputString(final Identifier jobId, final int sequenceNumber) {
    try (
      Transaction transaction = this.recordStore.newTransaction(Propagation.REQUIRED)) {
      return super.getGroupInputString(jobId, sequenceNumber);
    }
  }

  @Override
  public String getKey() {
    return "database";
  }

  @Override
  public void newJobFile(final Identifier jobId, final String path, final long sequenceNumber,
    final String contentType, final Object data) {
    try (
      Transaction transaction = this.dataAccessObject.newTransaction(Propagation.REQUIRED)) {
      final Record result = this.dataAccessObject.newRecord(BatchJobFile.BATCH_JOB_FILE);
      result.setValue(BatchJobFile.BATCH_JOB_ID, jobId);
      result.setValue(BatchJobFile.PATH, path);
      result.setValue(BatchJobFile.CONTENT_TYPE, contentType);
      result.setValue(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber);
      result.setValue(BatchJobFile.DATA, data);
      this.dataAccessObject.write(result);
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to create file", e);
    }
  }

  @Override
  public void setGroupInput(final Identifier jobId, final int sequenceNumber,
    final RecordDefinition recordDefinition, final List<Record> requests) {
    if (!requests.isEmpty()) {
      final File file = FileUtil.newTempFile("job", ".csv");
      try {
        try (
          CsvRecordWriter writer = new CsvRecordWriter(recordDefinition,
            FileUtil.newUtf8Writer(file), ',', true, false)) {
          for (final Record record : requests) {
            writer.write(record);
          }
        }
        newJobFile(jobId, GROUP_INPUTS, sequenceNumber, "text/csv", file);
      } finally {
        FileUtil.delete(file);
      }
    }
  }

}
