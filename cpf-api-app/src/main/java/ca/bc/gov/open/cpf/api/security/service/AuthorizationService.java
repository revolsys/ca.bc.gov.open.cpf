package ca.bc.gov.open.cpf.api.security.service;

import java.util.Map;

public interface AuthorizationService {
  boolean canAccessResource(final String moduleName, String consumerKey,
    String resourceClass, String resourceId);

  boolean canAccessResource(final String moduleName, String consumerKey,
    String resourceClass, String resourceId, String actionName);

  boolean canPerformAction(final String moduleName, String consumerKey,
    String actionName);

  Map<String, Object> getUserAttributes(final String moduleName,
    String consumerKey);

  String getUserClass(String consumerKey);

  String getUsername(String consumerKey);

  boolean isInGroup(final String moduleName, String consumerKey,
    String groupName);
}
