package ca.bc.gov.open.cpf.api.security.service;

import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractCachingSecurityService;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractSecurityServiceFactory;

public class AuthorizationServiceUserSecurityServiceFactory extends
  AbstractSecurityServiceFactory {

  private final AuthorizationService authorizationService;

  public AuthorizationServiceUserSecurityServiceFactory(
    final AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Override
  public void close() {
  }

  @Override
  protected AbstractCachingSecurityService createSecurityService(
    final Module module, final String consumerKey) {
    final String userAccountClass = authorizationService.getUserClass(consumerKey);
    final String userAccountName = authorizationService.getUsername(consumerKey);
    final AbstractCachingSecurityService securityService = new AuthorizationServiceUserSecurityService(
      authorizationService, module, consumerKey, userAccountClass,
      userAccountName);
    return securityService;
  }
}
