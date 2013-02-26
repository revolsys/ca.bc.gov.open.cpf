package ca.bc.gov.open.cpf.plugin.impl.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.bc.gov.open.cpf.plugin.impl.module.Module;

public class MockSecurityService extends AbstractCachingSecurityService {

  private boolean accessAllResources = true;

  public MockSecurityService(final Module module, final String username) {
    super(module, username);
  }

  @Override
  public boolean canAccessResource(
    final String resourceClass,
    final String resourceId) {
    return canAccessResource(resourceClass, resourceId, null);
  }

  @Override
  protected Boolean loadActionPermission(final String actionName) {
    if (accessAllResources) {
      return true;
    } else {
      return super.loadActionPermission(actionName);
    }
  }

  @Override
  protected Boolean loadGroupPermission(final String groupName) {
    if (accessAllResources) {
      return true;
    } else {
      return super.loadGroupPermission(groupName);
    }
  }

  @Override
  protected Boolean loadResourceAccessPermission(
    final String resourceClass,
    final String resourceId,
    final String actionName) {
    if (accessAllResources) {
      return true;
    } else {
      return super.loadResourceAccessPermission(resourceClass, resourceId,
        actionName);
    }
  }

  public void setAccessAllResources(final boolean accessAllResources) {
    this.accessAllResources = accessAllResources;
  }

  public void setAuthorizedActions(final List<String> authorizedActions) {
    for (final String authorizedAction : authorizedActions) {
      setActionPermission(authorizedAction, Boolean.TRUE);
    }
  }

  @Override
  public void setConsumerKey(final String consumerKey) {
    super.setConsumerKey(consumerKey);
  }

  public void setGroupNames(final List<String> groupNames) {
    for (final String groupName : groupNames) {
      setGroupPermission(groupName, Boolean.TRUE);
    }
  }

  public void setGroupNames(final String... groupNames) {
    setGroupNames(Arrays.asList(groupNames));
  }

  /**
   * Set the map from resource class to the map from resource ids to the list of
   * actions for those resources.
   * 
   * @param resourceActions
   */
  public void setResourceActions(
    final Map<String, Map<String, List<String>>> resourceActions) {
    for (final Entry<String, Map<String, List<String>>> resourceClassEntry : resourceActions.entrySet()) {
      final String resourceClass = resourceClassEntry.getKey();
      final Map<String, List<String>> resourceIdMap = resourceClassEntry.getValue();
      for (final Entry<String, List<String>> resourceIdEntry : resourceIdMap.entrySet()) {
        final String resourceId = resourceIdEntry.getKey();
        final List<String> actionNames = resourceIdEntry.getValue();
        for (final String actionName : actionNames) {
          setResourceAccessPermission(resourceClass, resourceId, actionName,
            Boolean.TRUE);
        }
      }
    }
  }

  @Override
  public void setUserAttributes(final Map<String, Object> userAttributes) {
    super.setUserAttributes(userAttributes);
  }

  @Override
  protected void setUserClass(final String userClass) {
    super.setUserClass(userClass);
  }

  @Override
  public void setUsername(final String username) {
    super.setUsername(username);
  }

}
