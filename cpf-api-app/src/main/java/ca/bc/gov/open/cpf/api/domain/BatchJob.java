package ca.bc.gov.open.cpf.api.domain;

public interface BatchJob {
  String BATCH_JOB = "/CPF/CPF_BATCH_JOBS";

  String BATCH_JOB_ID = "BATCH_JOB_ID";

  String BUSINESS_APPLICATION_NAME = "BUSINESS_APPLICATION_NAME";

  String BUSINESS_APPLICATION_PARAMS = "BUSINESS_APPLICATION_PARAMS";

  String BUSINESS_APPLICATION_VERSION = "BUSINESS_APPLICATION_VERSION";

  /**
   * The cancelled BatchJobStatus indicates that request was cancelled and all requests and
   * results have been removed.
   */
  String CANCELLED = "cancelled";

  String COMPLETED_TIMESTAMP = "COMPLETED_TIMESTAMP";

  /**
   * The creatingRequests BatchJobStatus indicates the objects are being created
   * by downloading the input data (if required), splitting the input data into
   * BusinessApplicationRequests, and validating the data for each
   * BusinessApplicationRequests.
   */
  String CREATING_REQUESTS = "creatingRequests";

  /**
   * The creatingResults BatchJobStatus indicates that the BatchJobResults are
   * in the process of being created.
   */
  String CREATING_RESULTS = "creatingResults";

  /**
   * The downloadInitiated BatchJobStatus indicates that the user initiated the
   * download of the BatchJobResults.
   */
  String DOWNLOAD_INITIATED = "downloadInitiated";

  String GROUP_SIZE = "GROUP_SIZE";

  String INPUT_DATA_CONTENT_TYPE = "INPUT_DATA_CONTENT_TYPE";

  String JOB_STATUS = "JOB_STATUS";

  String LAST_SCHEDULED_TIMESTAMP = "LAST_SCHEDULED_TIMESTAMP";

  String NOTIFICATION_URL = "NOTIFICATION_URL";

  String NUM_COMPLETED_REQUESTS = "NUM_COMPLETED_REQUESTS";

  String NUM_FAILED_REQUESTS = "NUM_FAILED_REQUESTS";

  String NUM_SCHEDULED_GROUPS = "NUM_SCHEDULED_GROUPS";

  String NUM_SUBMITTED_GROUPS = "NUM_SUBMITTED_GROUPS";

  String NUM_COMPLETED_GROUPS = "NUM_COMPLETED_GROUPS";

  String NUM_SUBMITTED_REQUESTS = "NUM_SUBMITTED_REQUESTS";

  /**
   * The processed BatchJobStatus indicates that all the
   * BusinessApplicationRequests of the BatchJob have been either successfully
   * completed or permanently failed. The BatchJob is ready for the
   * BatchJobResults to be created.
   */
  String PROCESSED = "processed";

  /**
   * The processing BatchJobStatus indicates that the BatchJob has been
   * scheduled for execution by the BusinessApplication and some of the
   * BusinessApplicationRequests may currently be executing.
   */
  String PROCESSING = "processing";

  String PROPERTIES = "PROPERTIES";

  /**
   * The requestsCreated BatchJobStatus indicates the objects have been created
   * by downloading the input data (if required), splitting the input data into
   * BusinessApplicationRequests, and validating the data for each
   * BusinessApplicationRequests.
   */
  String REQUESTS_CREATED = "requestsCreated";

  String RESULT_DATA_CONTENT_TYPE = "RESULT_DATA_CONTENT_TYPE";

  /**
   * The resultsCreated BatchJobStatus indicates that the BatchJobResults have
   * been created from the BusinessApplicationRequests for the BatchJob. The
   * file is ready to be downloaded.
   */
  String RESULTS_CREATED = "resultsCreated";

  String STRUCTURED_INPUT_DATA = "STRUCTURED_INPUT_DATA";

  String STRUCTURED_INPUT_DATA_URL = "STRUCTURED_INPUT_DATA_URL";

  /**
   * The submitted BatchJobStatus indicates that job has been accepted by the
   * application as is ready for the BusinessApplicationRequests be created by
   * downloading the input data (if required), splitting the input data into
   * tasks, and validating the data for each task.
   */
  String SUBMITTED = "submitted";

  String USER_ID = "USER_ID";

  String WHEN_CREATED = "WHEN_CREATED";

  String WHEN_STATUS_CHANGED = "WHEN_STATUS_CHANGED";

}
