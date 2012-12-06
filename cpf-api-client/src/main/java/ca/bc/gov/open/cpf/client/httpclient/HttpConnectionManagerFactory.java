package ca.bc.gov.open.cpf.client.httpclient;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionManagerFactory;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

public class HttpConnectionManagerFactory implements
  ClientConnectionManagerFactory {

  @SuppressWarnings("deprecation")
  @Override
  public ClientConnectionManager newInstance(
    final HttpParams params,
    final SchemeRegistry schemeRegistry) {
    return new ThreadSafeClientConnManager(params, schemeRegistry);
  }

}
