/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.plugin.api.security;

import java.util.Map;

/**
 * <p>The security service provides a mechanism for business applications to get
 * information about the user and enforce access control.</p>
 *
 * <p>The plug-in can use the methods on the security service to check if the user is in a
 * specific group, can access a resource or perform a specified action on a resource. If the user
 * does not have the correct permission then a subclass of RuntimeException can be thrown by the
 * plug-in with a description of why the user did not have the appropriate permission.</p>
 *
 * <p>To use the security service the plug-in must implement the following field and method within
 * the plug-in class. The CPF will invoke this method before the execute method to pass the
 * security service to the plug-in.</p>
 *
 * <figure><pre class="prettyprint language-java">private SecurityService securityService;

public void setSecurityService(SecurityService securityService) {
  this.securityService = securityService;
}</pre></figure>
 *
 * <p>See each of the methods below for examples of using the security service. All of the
 * examples assume that the above field and method are defined.</p>
 */
public interface SecurityService {
  /**
   * <p>Check to see if the user can access the resource.</p>
   *
   * <figure><pre class="prettyprint language-java">public void execute() {
  String resourceClass = "report";
  String resourceId = "demo";
  if (securityService.canAccessResource(resourceClass, resourceId)) {
    // Perform the request
  } else {
    throw new SecurityException("User cannot access the demo report");
  }
  }</pre></figure>
   *
   * @param resourceClass The type of resource.
   * @param resourceId The resource identifier.
   * @return True if the user can access the resource, false otherwise.
   */
  boolean canAccessResource(String resourceClass, String resourceId);

  /**
   * <p>Check to see if the user can perform the action on the resource.</p>
   *
   * <figure><pre class="prettyprint language-java">public void execute() {
  String resourceClass = "report";
  String resourceId = "demo";
  String actionName = "view";
  if (securityService.canAccessResource(resourceClass, resourceId, actionName)) {
    // Perform the request
  } else {
    throw new SecurityException("User cannot perform the view action on the demo report");
  }
  }</pre></figure>
   *
   * @param resourceClass The type of resource.
   * @param resourceId The resource identifier.
   * @param actionName The action name.
   * @return True if the user can perform the action on the resource, false
   *         otherwise.
   */
  boolean canAccessResource(String resourceClass, String resourceId, String actionName);

  /**
   * <p>Check to see if the user is can perform the named action.</p>
   *
   * <figure><pre class="prettyprint language-java">public void execute() {
  if (securityService.canPerformAction("view")) {
    // Perform the request
  } else {
    throw new SecurityException("User cannot perform the view action");
  }
  }</pre></figure>
   *
   *@param actionName The action name.
   * @return True if the user can perform the named action, false otherwise.
   */
  boolean canPerformAction(String actionName);

  /**
   * <p>Get the consumer key of the user.</p>
   *
   * @return The user's consumer key.
   */
  String getConsumerKey();

  /**
   * <p>Get the additional attributes about the user.</p>
   *
   * @return The additional attributes about the user.
   */
  Map<String, Object> getUserAttributes();

  /**
   * <p>Get the classification (type) of user account.</p>
   *
   * @return The classification (type) of user account.
   */
  String getUserClass();

  /**
   * <p>Get the login username of the user.</p>
   *
   * @return The login username of the user.
   */
  String getUsername();

  /**
   * <p>Check to see if the user is a member of the named group.</p>
   *
   * <figure><pre class="prettyprint language-java">public void execute() {
  if (securityService.isInGroup("DEMO_PARTNER")) {
    // Perform the request
  } else {
    throw new SecurityException("User is not a member of the DEMO_PARTNER_GROUP");
  }
  }</pre></figure>
   * @param groupName The group name.
   * @return True if the user is a member of the group, false otherwise.
   */
  boolean isInGroup(String groupName);
}
