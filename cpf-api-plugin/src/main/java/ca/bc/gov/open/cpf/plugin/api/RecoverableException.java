package ca.bc.gov.open.cpf.plugin.api;

/**
 * A recoverable exception indicates that the cause of the exception can be
 * recovered from. If a plugion throws this exception the request will be sent
 * to another worker for execution.
 * 
 * @author Paul Austin
 */
public class RecoverableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RecoverableException() {
  }

  public RecoverableException(final String message) {
    super(message);
  }

  public RecoverableException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public RecoverableException(final Throwable cause) {
    super(cause);
  }

}
