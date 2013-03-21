package ca.bc.gov.open.cpf.plugin.api;

/**
 * <p>A recoverable exception indicates that the cause of the exception can be
 * recovered from. If a plug-in throws subclasses exception the request will be sent
 * to another worker for execution.</p>
 * 
 * <p>This could be used if the connection to the database was unavailable at that time or if
 * some other resource was temprarily unnavailble.</p>
 */
@SuppressWarnings("serial")
public class RecoverableException extends RuntimeException {
  /**
   * Construct a new <code>RecoverableException</code>.
   */
  public RecoverableException() {
  }

  /**
   * Construct a new <code>RecoverableException</code>.
   * 
   * @param message The exception message.
   */
  public RecoverableException(final String message) {
    super(message);
  }

  /**
   * Construct a new <code>RecoverableException</code>.
   * 
   * @param message The exception message.
   * @param cause The cause of the exception.
   */
  public RecoverableException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Construct a new <code>RecoverableException</code>.
   * 
   * @param cause The cause of the exception.
   */
  public RecoverableException(final Throwable cause) {
    super(cause);
  }

}
