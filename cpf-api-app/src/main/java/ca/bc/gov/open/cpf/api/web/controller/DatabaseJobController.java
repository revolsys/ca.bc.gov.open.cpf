package ca.bc.gov.open.cpf.api.web.controller;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Map;

import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.domain.BatchJobFile;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.io.json.JsonParser;
import com.revolsys.util.WrappedException;

public class DatabaseJobController extends AbstractJobController {
  private final CpfDataAccessObject dataAccessObject;

  public DatabaseJobController(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

  @Override
  public boolean cancelJob(final long jobId) {
    return dataAccessObject.cancelBatchJob(jobId);
  }

  @Override
  public void createJobFile(final long jobId, final String path,
    final long sequenceNumber, final String contentType, final Object data) {
    try {
      final DataObject result = dataAccessObject.create(BatchJobFile.BATCH_JOB_FILE);
      result.setValue(BatchJobFile.BATCH_JOB_ID, jobId);
      result.setValue(BatchJobFile.PATH, path);
      result.setValue(BatchJobFile.CONTENT_TYPE, contentType);
      result.setValue(BatchJobFile.SEQUENCE_NUMBER, sequenceNumber);
      result.setValue(BatchJobFile.DATA, data);
      dataAccessObject.write(result);
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to create file", e);
    }
  }

  @Override
  public void deleteJob(final long jobId) {
    dataAccessObject.deleteBatchJob(jobId);
  }

  @Override
  public InputStream getJobResultData(final long jobId,
    final long batchJobResultId, final DataObject batchJobResult) {
    try {
      final Blob resultData = batchJobResult.getValue(BatchJobResult.RESULT_DATA);
      return resultData.getBinaryStream();
    } catch (final SQLException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public long getJobResultSize(final long jobId, final long batchJobResultId,
    final DataObject batchJobResult) {
    try {
      final Blob resultData = batchJobResult.getValue(BatchJobResult.RESULT_DATA);
      return resultData.length();
    } catch (final SQLException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public String getKey() {
    return "database";
  }

  @Override
  public Long getNonExecutingGroupSequenceNumber(final Long jobId) {
    return dataAccessObject.getNonExecutingGroupSequenceNumber(jobId);
  }

  @Override
  public String getStructuredInputData(final long jobId,
    final long groupSequenceNumber) {
    final DataObject executionGroup = dataAccessObject.getBatchJobExecutionGroup(
      jobId, groupSequenceNumber);
    if (executionGroup == null) {
      return "";
    } else {
      final String inputData = executionGroup.getString(BatchJobExecutionGroup.STRUCTURED_INPUT_DATA);
      return inputData;
    }
  }

  @Override
  public Map<String, Object> getStructuredResultData(final long jobId,
    final long sequenceNumber, final DataObject batchJobExecutionGroup) {
    final Object resultData = batchJobExecutionGroup.getString(BatchJobExecutionGroup.STRUCTURED_RESULT_DATA);
    if (resultData == null) {
      return null;
    } else {
      return JsonParser.read(resultData);
    }
  }

  @Override
  public void setJobResultData(final long jobId,
    final DataObject batchJobResult, final Object resultData) {
    batchJobResult.setValue(BatchJobResult.RESULT_DATA, resultData);
  }

  @Override
  public void setStructuredInputData(final long jobId,
    final long sequenceNumber, final DataObject executionGroup,
    final String structuredInputData) {
    executionGroup.setValue(BatchJobExecutionGroup.STRUCTURED_INPUT_DATA,
      structuredInputData);
  }

  @Override
  public void setStructuredResultData(final long jobId,
    final long sequenceNumber, final DataObject executionGroup,
    final String structuredResultData) {
    executionGroup.setValue(BatchJobExecutionGroup.STRUCTURED_RESULT_DATA,
      structuredResultData);
  }
}
