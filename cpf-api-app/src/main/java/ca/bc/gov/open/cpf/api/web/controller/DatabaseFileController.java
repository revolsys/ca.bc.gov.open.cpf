package ca.bc.gov.open.cpf.api.web.controller;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Map;

import ca.bc.gov.open.cpf.api.domain.BatchJobExecutionGroup;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.io.json.JsonParser;
import com.revolsys.util.WrappedException;

public class DatabaseFileController implements CpfFileController {
  private final CpfDataAccessObject dataAccessObject;

  public DatabaseFileController(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

  @Override
  public InputStream getJobResultData(final long batchJobId,
    final long batchJobResultId, final DataObject batchJobResult) {
    try {
      final Blob resultData = batchJobResult.getValue(BatchJobResult.RESULT_DATA);
      return resultData.getBinaryStream();
    } catch (final SQLException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public long getJobResultSize(final long batchJobId,
    final long batchJobResultId, final DataObject batchJobResult) {
    try {
      final Blob resultData = batchJobResult.getValue(BatchJobResult.RESULT_DATA);
      return resultData.length();
    } catch (final SQLException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public String getStructuredInputData(final long batchJobId,
    final long groupSequenceNumber) {
    final DataObject executionGroup = dataAccessObject.getBatchJobExecutionGroup(
      batchJobId, groupSequenceNumber);
    if (executionGroup == null) {
      return "";
    } else {
      final String inputData = executionGroup.getString(BatchJobExecutionGroup.STRUCTURED_INPUT_DATA);
      return inputData;
    }

  }

  @Override
  public Map<String, Object> getStructuredResultData(final long batchJobId,
    final long sequenceNumber, final DataObject batchJobExecutionGroup) {
    final Object resultData = batchJobExecutionGroup.getString(BatchJobExecutionGroup.STRUCTURED_RESULT_DATA);
    if (resultData == null) {
      return null;
    } else {
      return JsonParser.read(resultData);
    }
  }

  @Override
  public void setJobResultData(final long batchJobId,
    final DataObject batchJobResult, final Object resultData) {
    batchJobResult.setValue(BatchJobResult.RESULT_DATA, resultData);
  }

  @Override
  public void setStructuredInputData(final long batchJobId,
    final long sequenceNumber, final DataObject executionGroup,
    final String structuredInputData) {
    executionGroup.setValue(BatchJobExecutionGroup.STRUCTURED_INPUT_DATA,
      structuredInputData);
  }

  @Override
  public void setStructuredResultData(final long batchJobId,
    final long sequenceNumber, final DataObject executionGroup,
    final String structuredResultData) {
    executionGroup.setValue(BatchJobExecutionGroup.STRUCTURED_RESULT_DATA,
      structuredResultData);
  }
}
