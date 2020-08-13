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
package ca.bc.gov.open.cpf.api.scheduler;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.jeometry.common.data.identifier.Identifier;
import org.jeometry.common.data.type.DataType;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.client.api.ErrorCode;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.io.FileUtil;
import com.revolsys.record.Record;
import com.revolsys.record.io.RecordWriter;
import com.revolsys.record.io.format.csv.Csv;
import com.revolsys.record.io.format.csv.CsvRecordWriter;
import com.revolsys.record.io.format.tsv.Tsv;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.util.Property;

public abstract class PreProcessGroup {
  private final JobController jobController;

  private final CpfDataAccessObject dataAccessObject;

  private final int groupSequenceNumber;

  private RecordWriter writer;

  private final RecordDefinition recordDefinition;

  private final BatchJob batchJob;

  private int groupSize = 0;

  private final BusinessApplication businessApplication;

  private final Map<String, String> jobParameters;

  private File groupFile;

  private final JobPreProcessTask preProcess;

  public PreProcessGroup(final JobPreProcessTask preProcess, final JobController jobController,
    final BusinessApplication businessApplication, final BatchJob batchJob,
    final Map<String, String> jobParameters, final int groupSequenceNumber) {
    this.preProcess = preProcess;
    this.jobController = jobController;
    this.dataAccessObject = jobController.getDataAccessObject();
    this.businessApplication = businessApplication;
    this.batchJob = batchJob;
    this.jobParameters = jobParameters;
    this.recordDefinition = businessApplication.getInternalRequestRecordDefinition();
    this.groupSequenceNumber = groupSequenceNumber;
  }

  public boolean addRequest(final Record inputDataRecord, final int requestSequenceNumber) {
    final Record requestParameters = preProcessParameters(this.batchJob, this.businessApplication,
      requestSequenceNumber, this.jobParameters, inputDataRecord);
    if (requestParameters == null) {
      this.batchJob.addFailedRequests(Integer.toString(requestSequenceNumber));
      return false;
    } else {
      final RecordWriter writer = getWriter();
      writer.write(requestParameters);
      this.groupSize++;
      return true;
    }

  }

  public void cancel() {
    closeWriter();
    deleteFile();
  }

  private void closeWriter() {
    final RecordWriter writer = this.writer;
    if (writer != null) {
      this.writer = null;
      writer.close();
    }
  }

  public void commit() {
    closeWriter();
    final File groupFile = this.groupFile;
    if (groupFile != null) {
      final Identifier batchJobId = getBatchJobId();
      this.jobController.setGroupInput(batchJobId, this.groupSequenceNumber, Csv.MIME_TYPE,
        this.groupFile);
    }
    deleteFile();
  }

  private void deleteFile() {
    final File groupFile = this.groupFile;
    if (groupFile != null) {
      this.groupFile = null;
      FileUtil.delete(groupFile);
    }
  }

  public BatchJob getBatchJob() {
    return this.batchJob;
  }

  public Identifier getBatchJobId() {
    return this.batchJob.getIdentifier();
  }

  public CpfDataAccessObject getDataAccessObject() {
    return this.dataAccessObject;
  }

  public int getGroupSequenceNumber() {
    return this.groupSequenceNumber;
  }

  public int getGroupSize() {
    return this.groupSize;
  }

  public JobController getJobController() {
    return this.jobController;
  }

  private Object getNonEmptyValue(final Map<String, ? extends Object> map, final String key) {
    final Object value = map.get(key);
    if (value == null) {
      return null;
    } else {
      final String result = value.toString().trim();
      if (Property.hasValue(result)) {
        return value;
      } else {
        return null;
      }
    }
  }

  protected RecordWriter getWriter() {
    if (this.writer == null) {
      this.groupFile = newGroupFile();
      this.writer = new CsvRecordWriter(this.recordDefinition,
        FileUtil.newUtf8Writer(this.groupFile), Tsv.FIELD_SEPARATOR, true, true);
    }
    return this.writer;
  }

  protected File newGroupFile() {
    return this.groupFile;
  }

  private boolean preProcessParameter(final BusinessApplication businessApplication,
    final Identifier batchJobId, final int sequenceNumber, final Map<String, String> jobParameters,
    final Record requestParameters, final FieldDefinition field) {
    boolean jobParameter = false;
    final String parameterName = field.getName();
    Object parameterValue = getNonEmptyValue(requestParameters, parameterName);
    if (businessApplication.isJobParameter(parameterName)) {
      jobParameter = true;
      final Object jobValue = getNonEmptyValue(jobParameters, parameterName);
      if (jobValue != null) {
        if (parameterValue == null) {
          parameterValue = jobValue;
        } else if (DataType.equal(parameterValue, jobValue)) {
          requestParameters.setValue(parameterName, null);
          return true;
        }
      }
    }
    if (parameterValue == null) {
      if (field.isRequired()) {
        this.preProcess.addRequestError(sequenceNumber,
          ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription(),
          ErrorCode.MISSING_REQUIRED_PARAMETER.getDescription() + " " + parameterName, null);
        return false;
      }
    } else if (!jobParameter) {
      try {
        field.validate(parameterValue);
      } catch (final IllegalArgumentException e) {
        this.preProcess.addRequestError(sequenceNumber,
          ErrorCode.INVALID_PARAMETER_VALUE.getDescription(), e.getMessage(), null);
        return false;
      }
      try {
        final String sridString = jobParameters.get("srid");
        BatchJobService.setStructuredInputDataValue(sridString, requestParameters, field,
          parameterValue, true);
      } catch (final IllegalArgumentException e) {
        final StringWriter errorOut = new StringWriter();
        e.printStackTrace(new PrintWriter(errorOut));
        this.preProcess.addRequestError(sequenceNumber,
          ErrorCode.INVALID_PARAMETER_VALUE.getDescription(),
          ErrorCode.INVALID_PARAMETER_VALUE.getDescription() + " " + parameterName + " "
            + e.getMessage(),
          errorOut.toString());
        return false;
      }
    }
    return true;
  }

  private Record preProcessParameters(final Record batchJob,
    final BusinessApplication businessApplication, final int requestSequenceNumber,
    final Map<String, String> jobParameters, final Record requestRecord) {
    final Identifier batchJobId = batchJob.getIdentifier(BatchJob.BATCH_JOB_ID);
    requestRecord.put(BusinessApplication.SEQUENCE_NUMBER, requestSequenceNumber);
    final RecordDefinition recordDefinition = requestRecord.getRecordDefinition();
    for (final FieldDefinition field : recordDefinition.getFields()) {
      if (!preProcessParameter(businessApplication, batchJobId, requestSequenceNumber,
        jobParameters, requestRecord, field)) {
        return null;
      }
    }
    return requestRecord;
  }
}
