package ca.bc.gov.open.cpf.plugin.samples;

import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;

@BusinessApplicationPlugin(
    name = "UserInfo",
    title = "User Information",
    batchModePermission = "denyAll",
    instantModePermission = "permitAll")
public class UserInfo {
  private String consumerKey;

  private SecurityService securityService;

  private boolean groupAccess;

  private boolean actionAccess;

  private boolean resourceAccess;

  private Map<String, Object> userAttributes;

  private String groupName = "UIS_PUBLIC";

  private String actionName = "view";

  private String resourceActionName = "view";

  private String resourceClass = "data";

  private String resourceId = "1";

  private String userClass;

  private String username;

  public void execute() {
    consumerKey = securityService.getConsumerKey();
    userClass = securityService.getUserClass();
    username = securityService.getUsername();
    userAttributes = securityService.getUserAttributes();
    groupAccess = securityService.isInGroup(groupName);
    actionAccess = securityService.canPerformAction(actionName);
    resourceAccess = securityService.canAccessResource(resourceClass,
      resourceId, resourceActionName);
  }

  public String getActionName() {
    return actionName;
  }

  @ResultAttribute
  public String getConsumerKey() {
    return consumerKey;
  }

  public String getGroupName() {
    return groupName;
  }

  public String getResourceActionName() {
    return resourceActionName;
  }

  public String getResourceClass() {
    return resourceClass;
  }

  public String getResourceId() {
    return resourceId;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  @ResultAttribute
  public Map<String, Object> getUserAttributes() {
    return userAttributes;
  }

  @ResultAttribute
  public String getUserClass() {
    return userClass;
  }

  @ResultAttribute
  public String getUsername() {
    return username;
  }

  @ResultAttribute
  public boolean isActionAccess() {
    return actionAccess;
  }

  @ResultAttribute
  public boolean isGroupAccess() {
    return groupAccess;
  }

  @ResultAttribute
  public boolean isResourceAccess() {
    return resourceAccess;
  }

  @RequestParameter
  public void setActionName(final String actionName) {
    this.actionName = actionName;
  }

  @RequestParameter
  public void setGroupName(final String groupName) {
    this.groupName = groupName;
  }

  @RequestParameter
  public void setResourceActionName(final String resourceActionName) {
    this.resourceActionName = resourceActionName;
  }

  @RequestParameter
  public void setResourceClass(final String resourceClass) {
    this.resourceClass = resourceClass;
  }

  @RequestParameter
  public void setResourceId(final String resourceId) {
    this.resourceId = resourceId;
  }

  public void setSecurityService(final SecurityService securityService) {
    this.securityService = securityService;
  }

}
