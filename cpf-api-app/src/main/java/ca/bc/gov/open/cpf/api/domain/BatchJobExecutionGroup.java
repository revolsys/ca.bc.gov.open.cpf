package ca.bc.gov.open.cpf.api.domain;

/**
 * The BatchJobExecutionGroup represents a group of request to execute a
 * BusinessApplication within a BatchJob. It contains the input and result data.
 */
public interface BatchJobExecutionGroup {

  String BATCH_JOB_ID = "BATCH_JOB_ID";

  String BATCH_JOB_EXECUTION_GROUP = "/CPF/CPF_BATCH_JOB_EXECUTION_GROUPS";

  String BATCH_JOB_EXECUTION_GROUP_ID = "BATCH_JOB_EXECUTION_GROUP_ID";

  String BATCH_JOB_RESULT_ID = "BATCH_JOB_RESULT_ID";

  String COMPLETED_IND = "COMPLETED_IND";

  String NUM_SUBMITTED_REQUESTS = "NUM_SUBMITTED_REQUESTS";

  String NUM_COMPLETED_REQUESTS = "NUM_COMPLETED_REQUESTS";

  String NUM_FAILED_REQUESTS = "NUM_FAILED_REQUESTS";

  String INPUT_DATA = "INPUT_DATA";

  String INPUT_DATA_CONTENT_TYPE = "INPUT_DATA_CONTENT_TYPE";

  String INPUT_DATA_URL = "INPUT_DATA_URL";

  String SEQUENCE_NUMBER = "SEQUENCE_NUMBER";

  String RESULT_DATA = "RESULT_DATA";

  String RESULT_DATA_URL = "RESULT_DATA_URL";

  String STARTED_IND = "STARTED_IND";

  String STRUCTURED_INPUT_DATA = "STRUCTURED_INPUT_DATA";

  String STRUCTURED_RESULT_DATA = "STRUCTURED_RESULT_DATA";
}

