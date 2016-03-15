/*
 * Copyright © 2008-2016, Province of British Columbia
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
package ca.bc.gov.open.cpf.plugin.impl.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ResourcePermission;

public abstract class AbstractCachingSecurityService implements SecurityService {

  private final Map<String, Boolean> authorizedActions = new HashMap<>();

  private String consumerKey;

  private Map<String, Boolean> groupNames = new HashMap<>();

  private Module module;

  private final Map<String, Map<String, Map<String, Boolean>>> resourceActions = new HashMap<>();

  private Map<String, Object> userAttributes = new HashMap<>();

  private String userClass;

  private String username;

  public AbstractCachingSecurityService(final Module module, final String username) {
    this(module, UUID.randomUUID().toString(), "http://open.gov.bc.ca/cpf", username);
  }

  public AbstractCachingSecurityService(final Module module, final String consumerKey,
    final String userClass, final String username) {
    this.module = module;
    this.consumerKey = consumerKey;
    this.userClass = userClass;
    this.username = username;
  }

  @Override
  public boolean canAccessResource(final String resourceClass, final String resourceId) {
    return canAccessResource(resourceClass, resourceId, ResourcePermission.ALL);
  }

  @Override
  public boolean canAccessResource(final String resourceClass, final String resourceId,
    final String actionName) {
    synchronized (this.resourceActions) {
      Map<String, Map<String, Boolean>> actionsForClass = this.resourceActions.get(resourceClass);
      if (actionsForClass == null) {
        actionsForClass = new HashMap<String, Map<String, Boolean>>();
        this.resourceActions.put(resourceClass, actionsForClass);
      }

      Map<String, Boolean> actionsForResource = actionsForClass.get(resourceId);
      if (actionsForResource == null) {
        actionsForResource = new HashMap<String, Boolean>();
        actionsForClass.put(resourceId, actionsForResource);
      }
      Boolean actionPermission = actionsForResource.get(actionName);
      if (actionPermission == null) {
        actionPermission = loadResourceAccessPermission(resourceClass, resourceId, actionName);
        actionsForResource.put(actionName, actionPermission);
      }
      return actionPermission;
    }
  }

  @Override
  public boolean canPerformAction(final String actionName) {
    synchronized (this.authorizedActions) {
      Boolean actionPermission = this.authorizedActions.get(actionName);
      if (actionPermission == null) {
        actionPermission = loadActionPermission(actionName);
        setActionPermission(actionName, actionPermission);
      }
      return actionPermission;
    }
  }

  public void close() {
    this.authorizedActions.clear();
    ;
    this.consumerKey = null;
    this.groupNames = null;
    this.module = null;
    this.resourceActions.clear();
    ;
    this.userAttributes.clear();
    ;
    this.userClass = null;
    this.username = null;
  }

  @Override
  public String getConsumerKey() {
    return this.consumerKey;
  }

  public String getModuleName() {
    return this.module.getName();
  }

  @Override
  public Map<String, Object> getUserAttributes() {
    synchronized (this.userAttributes) {
      if (this.userAttributes == null) {
        setUserAttributes(loadUserAttributes());
      }
      return Collections.unmodifiableMap(this.userAttributes);
    }
  }

  @Override
  public String getUserClass() {
    return this.userClass;
  }

  @Override
  public String getUsername() {
    return this.username;
  }

  @Override
  public boolean isInGroup(final String groupName) {
    synchronized (this.groupNames) {
      Boolean groupPermission = this.groupNames.get(groupName);
      if (groupPermission == null) {
        groupPermission = loadGroupPermission(groupName);
        setGroupPermission(groupName, groupPermission);
      }
      return groupPermission;
    }
  }

  protected Boolean loadActionPermission(final String actionName) {
    return loadResourceAccessPermission(ResourcePermission.ALL, ResourcePermission.ALL, actionName);
  }

  protected Boolean loadGroupPermission(final String groupName) {
    final Map<String, Set<ResourcePermission>> permissionsByGroupName = this.module
      .getPermissionsByGroupName();
    return permissionsByGroupName.containsKey(groupName);
  }

  protected Boolean loadResourceAccessPermission(final String resourceClass,
    final String resourceId, final String actionName) {
    final Map<String, Set<ResourcePermission>> permissionsByGroupName = this.module
      .getPermissionsByGroupName();
    final ResourcePermission resource = new ResourcePermission(resourceClass, resourceId,
      actionName);
    for (final Entry<String, Set<ResourcePermission>> entry : permissionsByGroupName.entrySet()) {
      final String groupName = entry.getKey();
      if (isInGroup(groupName)) {
        final Set<ResourcePermission> permissions = entry.getValue();
        for (final ResourcePermission permission : permissions) {
          if (permission.canAccess(resource)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  protected Map<String, Object> loadUserAttributes() {
    return Collections.emptyMap();
  }

  protected void setActionPermission(final String actionName, final Boolean actionPermission) {
    this.authorizedActions.put(actionName, actionPermission);
  }

  protected void setConsumerKey(final String userId) {
    this.consumerKey = userId;
  }

  protected void setGroupPermission(final String groupName, final Boolean groupPermission) {
    this.groupNames.put(groupName, groupPermission);
  }

  protected void setResourceAccessPermission(final String resourceClass, final String resourceId,
    final String actionName, final Boolean accessPermission) {
    synchronized (this.resourceActions) {

      Map<String, Map<String, Boolean>> actionsForClass = this.resourceActions.get(resourceClass);
      if (actionsForClass == null) {
        actionsForClass = new HashMap<String, Map<String, Boolean>>();
        this.resourceActions.put(resourceClass, actionsForClass);
      }

      Map<String, Boolean> actionsForResource = actionsForClass.get(resourceId);
      if (actionsForResource == null) {
        actionsForResource = new HashMap<String, Boolean>();
        actionsForClass.put(resourceId, actionsForResource);
      }
      actionsForResource.put(actionName, accessPermission);
    }
  }

  protected void setUserAttributes(final Map<String, Object> userAttributes) {
    this.userAttributes = userAttributes;
  }

  protected void setUserClass(final String userClass) {
    this.userClass = userClass;
  }

  protected void setUsername(final String username) {
    this.username = username;
  }

  @Override
  public String toString() {
    return "SecurityService for " + this.username;
  }

}
