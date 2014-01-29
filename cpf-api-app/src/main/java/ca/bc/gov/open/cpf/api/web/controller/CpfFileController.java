package ca.bc.gov.open.cpf.api.web.controller;

import java.io.InputStream;
import java.util.Map;

import com.revolsys.gis.data.model.DataObject;

public interface CpfFileController {

  InputStream getJobResultData(long batchJobId, long batchJobResultId,
    DataObject batchJobResult);

  long getJobResultSize(long batchJobId, long batchJobResultId,
    DataObject batchJobResult);

  String getStructuredInputData(long batchJobId, long executionGroupId);

  Map<String, Object> getStructuredResultData(long batchJobId,
    long executionGroupId, DataObject batchJobExecutionGroup);

  void setJobResultData(final long batchJobId, final DataObject batchJobResult,
    final Object resultData);

  void setStructuredInputData(long batchJobId, long executionGroupId,
    DataObject executionGroup, String structuredInputData);

  void setStructuredResultData(long batchJobId, long executionGroupId,
    DataObject executionGroup, String structuredInputData);
}
