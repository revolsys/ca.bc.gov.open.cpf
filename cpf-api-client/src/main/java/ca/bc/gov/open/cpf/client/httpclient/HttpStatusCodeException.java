package ca.bc.gov.open.cpf.client.httpclient;

public class HttpStatusCodeException extends RuntimeException {
  private final int statusCode;

  private final String statusMessage;

  public HttpStatusCodeException(final int statusCode,
    final String statusMessage) {
    super(statusCode + " " + statusMessage);
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

}
