package ca.bc.gov.open.cpf.api.domain;

/**
 * The BatchJobRequest represents a single request to execute a
 * BusinessApplication within a BatchJob. It contains the input data, result
 * data and any permanent errors.
 * 
 * @author Paul Austin
 */
public interface BatchJobRequest {

  String BATCH_JOB_ID = "BATCH_JOB_ID";

  String BATCH_JOB_REQUEST = "/CPF/CPF_BATCH_JOB_REQUESTS";

  String BATCH_JOB_REQUEST_ID = "BATCH_JOB_REQUEST_ID";

  String BATCH_JOB_RESULT_ID = "BATCH_JOB_RESULT_ID";

  String COMPLETED_IND = "COMPLETED_IND";

  String NUM_SUBMITTED_REQUESTS = "NUM_SUBMITTED_REQUESTS";

  String NUM_COMPLETED_REQUESTS = "NUM_COMPLETED_REQUESTS";

  String NUM_FAILED_REQUESTS = "NUM_FAILED_REQUESTS";

  String INPUT_DATA = "INPUT_DATA";

  String INPUT_DATA_CONTENT_TYPE = "INPUT_DATA_CONTENT_TYPE";

  String INPUT_DATA_URL = "INPUT_DATA_URL";

  String REQUEST_SEQUENCE_NUMBER = "REQUEST_SEQUENCE_NUMBER";

  String RESULT_DATA = "RESULT_DATA";

  String RESULT_DATA_URL = "RESULT_DATA_URL";

  String STARTED_IND = "STARTED_IND";

  String STRUCTURED_INPUT_DATA = "STRUCTURED_INPUT_DATA";

  String STRUCTURED_RESULT_DATA = "STRUCTURED_RESULT_DATA";
}
