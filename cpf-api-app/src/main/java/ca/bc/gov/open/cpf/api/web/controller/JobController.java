package ca.bc.gov.open.cpf.api.web.controller;

import java.io.InputStream;
import java.util.Map;

import com.revolsys.data.record.Record;

public interface JobController {
  String GROUP_RESULTS = "groupResults";

  String GROUP_INPUTS = "groupInputs";

  String JOB_RESULTS = "jobResults";

  String JOB_INPUTS = "jobInputs";

  boolean cancelJob(long jobId);

  void createJobFile(long jobId, String path, long sequenceNumber,
    String contentType, Object data);

  void createJobInputFile(long jobId, String contentType, Object data);

  void deleteJob(long jobId);

  InputStream getJobResultData(long jobId, long sequenceNumber,
    Record batchJobResult);

  long getJobResultSize(long jobId, long sequenceNumber,
    Record batchJobResult);

  String getKey();

  Long getNonExecutingGroupSequenceNumber(Long jobId);

  String getStructuredInputData(long jobId, long executionGroupId);

  Map<String, Object> getStructuredResultData(long jobId,
    long executionGroupId, Record batchJobExecutionGroup);

  void setJobResultData(final long jobId, final Record batchJobResult,
    final Object resultData);

  void setStructuredInputData(long jobId, long executionGroupId,
    Record executionGroup, String structuredInputData);

  void setStructuredResultData(long jobId, long executionGroupId,
    Record executionGroup, String structuredInputData);
}
