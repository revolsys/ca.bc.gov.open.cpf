package ca.bc.gov.open.cpf.plugin.impl.security;

import java.io.Closeable;
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

  private Map<String, Boolean> authorizedActions = new HashMap<String, Boolean>();

  private String consumerKey;

  private Map<String, Boolean> groupNames = new HashMap<String, Boolean>();

  private Module module;

  private Map<String, Map<String, Map<String, Boolean>>> resourceActions = new HashMap<String, Map<String, Map<String, Boolean>>>();

  private Map<String, Object> userAttributes;

  private String userClass;

  private String username;

  public AbstractCachingSecurityService(final Module module,
    final String username) {
    this(module, UUID.randomUUID().toString(), "http://open.gov.bc.ca/cpf",
      username);
  }

  public AbstractCachingSecurityService(final Module module,
    final String consumerKey, final String userClass, final String username) {
    this.module = module;
    this.consumerKey = consumerKey;
    this.userClass = userClass;
    this.username = username;
  }

  @Override
  public boolean canAccessResource(final String resourceClass,
    final String resourceId) {
    return canAccessResource(resourceClass, resourceId, ResourcePermission.ALL);
  }

  @Override
  public boolean canAccessResource(final String resourceClass,
    final String resourceId, final String actionName) {
    synchronized (resourceActions) {
      Map<String, Map<String, Boolean>> actionsForClass = resourceActions.get(resourceClass);
      if (actionsForClass == null) {
        actionsForClass = new HashMap<String, Map<String, Boolean>>();
        resourceActions.put(resourceClass, actionsForClass);
      }

      Map<String, Boolean> actionsForResource = actionsForClass.get(resourceId);
      if (actionsForResource == null) {
        actionsForResource = new HashMap<String, Boolean>();
        actionsForClass.put(resourceId, actionsForResource);
      }
      Boolean actionPermission = actionsForResource.get(actionName);
      if (actionPermission == null) {
        actionPermission = loadResourceAccessPermission(resourceClass,
          resourceId, actionName);
        actionsForResource.put(actionName, actionPermission);
      }
      return actionPermission;
    }
  }

  @Override
  public boolean canPerformAction(final String actionName) {
    synchronized (authorizedActions) {
      Boolean actionPermission = authorizedActions.get(actionName);
      if (actionPermission == null) {
        actionPermission = loadActionPermission(actionName);
        setActionPermission(actionName, actionPermission);
      }
      return actionPermission;
    }
  }

  public void close() {
    authorizedActions = null;
    consumerKey = null;
    groupNames = null;
    module = null;
    resourceActions = null;
    userAttributes = null;
    userClass = null;
    username = null;
  }

  @Override
  public String getConsumerKey() {
    return consumerKey;
  }

  public String getModuleName() {
    return module.getName();
  }

  @Override
  public Map<String, Object> getUserAttributes() {
    synchronized (userAttributes) {
      if (userAttributes == null) {
        setUserAttributes(loadUserAttributes());
      }
      return Collections.unmodifiableMap(userAttributes);
    }
  }

  @Override
  public String getUserClass() {
    return userClass;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isInGroup(final String groupName) {
    synchronized (groupNames) {
      Boolean groupPermission = groupNames.get(groupName);
      if (groupPermission == null) {
        groupPermission = loadGroupPermission(groupName);
        setGroupPermission(groupName, groupPermission);
      }
      return groupPermission;
    }
  }

  protected Boolean loadActionPermission(final String actionName) {
    return loadResourceAccessPermission(ResourcePermission.ALL,
      ResourcePermission.ALL, actionName);
  }

  protected Boolean loadGroupPermission(final String groupName) {
    final Map<String, Set<ResourcePermission>> permissionsByGroupName = module.getPermissionsByGroupName();
    return permissionsByGroupName.containsKey(groupName);
  }

  protected Boolean loadResourceAccessPermission(final String resourceClass,
    final String resourceId, final String actionName) {
    final Map<String, Set<ResourcePermission>> permissionsByGroupName = module.getPermissionsByGroupName();
    final ResourcePermission resource = new ResourcePermission(resourceClass,
      resourceId, actionName);
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

  protected void setActionPermission(final String actionName,
    final Boolean actionPermission) {
    authorizedActions.put(actionName, actionPermission);
  }

  protected void setConsumerKey(final String userId) {
    this.consumerKey = userId;
  }

  protected void setGroupPermission(final String groupName,
    final Boolean groupPermission) {
    groupNames.put(groupName, groupPermission);
  }

  protected void setResourceAccessPermission(final String resourceClass,
    final String resourceId, final String actionName,
    final Boolean accessPermission) {
    synchronized (resourceActions) {

      Map<String, Map<String, Boolean>> actionsForClass = resourceActions.get(resourceClass);
      if (actionsForClass == null) {
        actionsForClass = new HashMap<String, Map<String, Boolean>>();
        resourceActions.put(resourceClass, actionsForClass);
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
    return "SecurityService for " + username;
  }

}
