package ca.bc.gov.open.cpf.client.api;

/**
 * <p>The callback interface is used by the CpfClient API to invoke code in a client application
 * to process a objects returned from the CPF REST API.</p>
 * 
 * <p>The use of a callback mechanism ensures that any connections to the server are closed
 * correctly after processing a request. The connections are closed even if there was a
 * communications error or an exception thrown by the callback.</p>
 *
 * <p>A callback can also used to reduce the memory overhead of a client application. The CpfClient
 * reads the each result from the server and calls the process method for each object in turn. Keeping
 * only one result in memory at a time.</p>
 *
 * @param <T> The Java class of the object to be processed by the callback.
 */
public interface Callback<T> {
  /**
   * Process the object.
   * 
   * @param object The object to process.
   */
  void process(T object);
}
