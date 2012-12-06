package ca.bc.gov.open.cpf.api.domain;

public enum BatchJobErrorCode {
  /**
   */
  BAD_RESPONSE_DATA(
    "Invalid response data returned from the Business Application"),
  /** */
  RECOVERABLE_EXCEPTION("A temporary exception that can be recovered from"),
  /**
    */
  MISSING_REQUIRED_PARAMETER("A required request or job parameter is missing"),
  /**
   */
  INVALID_PARAMETER_VALUE(
    "A supplied request or job parameter has an invalid type or value"),
  /**
   */
  INPUT_DATA_UNREADABLE("Unable to read batch job input data"),
  /**
   */
  BAD_NUMBER_REQUESTS("Invalid number of batch job requests"),
  /**
   */
  TOO_MANY_REQUESTS("Too many batch job requests for this business application"),
  /**
   */
  BAD_INPUT_DATA_TYPE("Invalid mime type for request input data"),
  /**
   */
  BAD_INPUT_DATA_VALUE("Illegal value in request input data"),
  /**
   */
  BAD_INPUT_DATA_VALUES("Illegal combination of values in request input data"),
  /**
   */
  BAD_RESULT_DATA_TYPE("Invalid mime type for result data"),
  /**
   */
  ERROR_PROCESSING_REQUEST("An unknown error occurred processing the request");

  private final String description;

  private BatchJobErrorCode(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return description;
  }
}
