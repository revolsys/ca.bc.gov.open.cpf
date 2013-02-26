package ca.bc.gov.open.cpf.api.security.service;

import java.util.Map;

import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractCachingSecurityService;

public class AuthorizationServiceUserSecurityService extends
  AbstractCachingSecurityService {

  private AuthorizationService authorizationService;

  public AuthorizationServiceUserSecurityService(
    final AuthorizationService authorizationService, final Module module,
    final String consumerKey, final String userClass, final String username) {
    super(module, consumerKey, userClass, username);
    this.authorizationService = authorizationService;

  }

  @Override
  public void close() {
    super.close();
    authorizationService = null;
  }

  @Override
  protected Boolean loadActionPermission(final String actionName) {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    if (authorizationService.canPerformAction(moduleName, consumerKey,
      actionName)) {
      return true;
    } else {
      return super.loadActionPermission(actionName);
    }
  }

  @Override
  protected Boolean loadGroupPermission(final String groupName) {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    if (authorizationService.isInGroup(moduleName, consumerKey, groupName)) {
      return true;
    } else {
      return super.loadGroupPermission(groupName);
    }
  }

  @Override
  protected Boolean loadResourceAccessPermission(final String resourceClass,
    final String resourceId, final String actionName) {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    if (authorizationService.canAccessResource(moduleName, consumerKey,
      resourceClass, resourceId, actionName)) {
      return true;
    } else {
      return super.loadResourceAccessPermission(resourceClass, resourceId,
        actionName);
    }
  }

  @Override
  protected Map<String, Object> loadUserAttributes() {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    return authorizationService.getUserAttributes(moduleName, consumerKey);
  }
}
