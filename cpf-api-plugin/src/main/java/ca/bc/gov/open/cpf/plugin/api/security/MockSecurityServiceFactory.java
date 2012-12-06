package ca.bc.gov.open.cpf.plugin.api.security;

import ca.bc.gov.open.cpf.plugin.api.module.Module;

public class MockSecurityServiceFactory extends AbstractSecurityServiceFactory {

  @Override
  public void close() {
  }

  @Override
  protected SecurityService createSecurityService(
    final Module module,
    final String consumerKey) {
    return new MockSecurityService(module, consumerKey);
  }

}
