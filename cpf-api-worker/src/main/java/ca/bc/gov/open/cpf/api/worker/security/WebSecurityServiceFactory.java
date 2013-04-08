package ca.bc.gov.open.cpf.api.worker.security;

import ca.bc.gov.open.cpf.client.httpclient.DigestHttpClient;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractCachingSecurityService;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractSecurityServiceFactory;

public class WebSecurityServiceFactory extends AbstractSecurityServiceFactory {

  private final DigestHttpClient httpClient;

  public WebSecurityServiceFactory(final DigestHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public void close() {
  }

  @Override
  protected AbstractCachingSecurityService createSecurityService(
    final Module module,
    final String consumerKey) {
    return new WebSecurityService(httpClient, module, consumerKey);
  }

}
