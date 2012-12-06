package ca.bc.gov.open.cpf.api.domain;

import java.util.Arrays;
import java.util.List;

/**
 * The BatchJobResult is a result file generated after the execution of a
 * BatchJob. For structured output data one file will be generated containing
 * all the results. For opaque output data one file will be created for each
 * BatchJobRequest. A BatchJob may also have one file containing any errors
 * generated.
 */
public class BatchJobResult {

  public static final String BATCH_JOB_RESULT = "/CPF/CPF_BATCH_JOB_RESULTS";

  public static final String BATCH_JOB_RESULT_ID = "BATCH_JOB_RESULT_ID";

  public static final String BATCH_JOB_ID = "BATCH_JOB_ID";

  public static final String RESULT_DATA = "RESULT_DATA";

  public static final String RESULT_DATA_URL = "RESULT_DATA_URL";

  public static final String RESULT_DATA_CONTENT_TYPE = "RESULT_DATA_CONTENT_TYPE";

  /**
   * The structuredResultData BatchJobResultType represents a file which
   * contains the structured result data from all BusinessApplicationRequests
   * for a BatchJob is a single file.
   */
  public static final String STRUCTURED_RESULT_DATA = "structuredResultData";

  /**
   * The opaqueResultData BatchJobResultType represents files containing opaque
   * result data for a single BatchJobRequest.
   */
  public static final String OPAQUE_RESULT_DATA = "opaqueResultData";

  /**
   * The errorResultData represents a file containing all of the errors from all
   * BusinessApplicationRequests or the BatchJob in a single file. The file will
   * be a text/csv file contain the requestSequenceNumber, errorCode, and
   * errorMessage.
   */
  public static final String ERROR_RESULT_DATA = "errorResultData";

  public static final String BATCH_JOB_RESULT_TYPE = "BATCH_JOB_RESULT_TYPE";

  public static final String DOWNLOAD_TIMESTAMP = "DOWNLOAD_TIMESTAMP";

  public static final String REQUEST_SEQUENCE_NUMBER = "REQUEST_SEQUENCE_NUMBER";

  public static final String WHO_CREATED = "WHO_CREATED";

  public static final String WHEN_CREATED = "WHEN_CREATED";

  public static final String WHO_UPDATED = "WHO_UPDATED";

  public static final String WHEN_UPDATED = "WHEN_UPDATED";

  public static final String BATCH_JOB_REQUEST_ID = "BATCH_JOB_REQUEST_ID";

  public static final List<String> ALL_EXCEPT_BLOB = Arrays.asList(
    BATCH_JOB_RESULT_ID, BATCH_JOB_RESULT_TYPE, DOWNLOAD_TIMESTAMP,
    REQUEST_SEQUENCE_NUMBER, RESULT_DATA_CONTENT_TYPE, RESULT_DATA_URL,
    WHO_CREATED, WHEN_CREATED, WHO_UPDATED, WHEN_UPDATED, BATCH_JOB_ID,
    BATCH_JOB_REQUEST_ID);

}
