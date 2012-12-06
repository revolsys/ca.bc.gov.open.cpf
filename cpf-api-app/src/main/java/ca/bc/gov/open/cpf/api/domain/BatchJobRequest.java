package ca.bc.gov.open.cpf.api.domain;

/**
 * The BatchJobRequest represents a single request to execute a
 * BusinessApplication within a BatchJob. It contains the input data, result
 * data and any permanent errors.
 * 
 * @author Paul Austin
 */
public class BatchJobRequest {

  public static final String BATCH_JOB_ID = "BATCH_JOB_ID";

  public static final String BATCH_JOB_REQUEST = "/CPF/CPF_BATCH_JOB_REQUESTS";

  public static final String BATCH_JOB_REQUEST_ID = "BATCH_JOB_REQUEST_ID";

  public static final String BATCH_JOB_RESULT_ID = "BATCH_JOB_RESULT_ID";

  public static final String COMPLETED_IND = "COMPLETED_IND";

  public static final String ERROR_CODE = "ERROR_CODE";

  public static final String ERROR_DEBUG_MESSAGE = "ERROR_DEBUG_MESSAGE";

  public static final String ERROR_MESSAGE = "ERROR_MESSAGE";

  public static final String INPUT_DATA = "INPUT_DATA";

  public static final String INPUT_DATA_CONTENT_TYPE = "INPUT_DATA_CONTENT_TYPE";

  public static final String INPUT_DATA_URL = "INPUT_DATA_URL";

  public static final String REQUEST_SEQUENCE_NUMBER = "REQUEST_SEQUENCE_NUMBER";

  public static final String RESULT_DATA = "RESULT_DATA";

  public static final String RESULT_DATA_URL = "RESULT_DATA_URL";

  public static final String STARTED_IND = "STARTED_IND";

  public static final String STRUCTURED_INPUT_DATA = "STRUCTURED_INPUT_DATA";

  public static final String STRUCTURED_RESULT_DATA = "STRUCTURED_RESULT_DATA";
}
