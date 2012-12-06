package ca.bc.gov.open.cpf.plugin.api.security;

import java.util.Map;

/**
 * The security service provides a mechanism for business applications to get
 * information about the user and enforce access control.
 */
public interface SecurityService {
  /**
   * Check to see if the user can access the resource.
   * 
   * @param resourceClass The type of resource.
   * @param resourceId The resource identifier.
   * @return True if the user can access the resource, false otherwise.
   */
  boolean canAccessResource(String resourceClass, String resourceId);

  /**
   * Check to see if the user can perform the action on the resource.
   * 
   * @param resourceClass The type of resource.
   * @param resourceId The resource identifier.
   * @param actionName The action name.
   * @return True if the user can perform the action on the resource, false
   *         otherwise.
   */
  boolean canAccessResource(
    String resourceClass,
    String resourceId,
    String actionName);

  /**
   * Check to see if the user is can perform the named action.
   * 
   * @param actionName The action name.
   * @return True if the user can perform the named action, false otherwise.
   */
  boolean canPerformAction(String actionName);

  void close();

  /**
   * Get the consumer key of the user.
   * 
   * @return The user's consumer key.
   */
  String getConsumerKey();

  /**
   * Get the additional attributes about the user.
   * 
   * @return The additional attributes about the user.
   */
  Map<String, Object> getUserAttributes();

  /**
   * Get the classifcation (type) of user account.
   * 
   * @return The classifcation (type) of user account.
   */
  String getUserClass();

  /**
   * Get the login username of the user.
   * 
   * @return The login username of the user.
   */
  String getUsername();

  /**
   * Check to see if the user is a member of the named group.
   * 
   * @param groupName The group name.
   * @return True if the user is a member of the group, false otherwise.
   */
  boolean isInGroup(String groupName);
}
