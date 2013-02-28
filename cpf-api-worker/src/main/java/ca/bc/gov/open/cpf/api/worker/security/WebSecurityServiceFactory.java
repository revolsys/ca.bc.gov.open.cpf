package ca.bc.gov.open.cpf.api.worker.security;

import ca.bc.gov.open.cpf.client.httpclient.OAuthHttpClientPool;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractCachingSecurityService;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractSecurityServiceFactory;

public class WebSecurityServiceFactory extends AbstractSecurityServiceFactory {

  private final OAuthHttpClientPool httpClientPool;

  public WebSecurityServiceFactory(final OAuthHttpClientPool httpClientPool) {
    this.httpClientPool = httpClientPool;
  }

  @Override
  public void close() {
  }

  @Override
  protected AbstractCachingSecurityService createSecurityService(
    final Module module,
    final String consumerKey) {
    return new WebSecurityService(httpClientPool, module, consumerKey);
  }

}
