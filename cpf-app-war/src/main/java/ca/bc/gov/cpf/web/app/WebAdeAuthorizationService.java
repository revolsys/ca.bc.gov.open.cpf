package ca.bc.gov.cpf.web.app;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.api.security.service.BaseAuthorizationService;
import ca.bc.gov.webade.Action;
import ca.bc.gov.webade.AppRoles;
import ca.bc.gov.webade.Application;
import ca.bc.gov.webade.Role;
import ca.bc.gov.webade.WebADEApplicationUtils;
import ca.bc.gov.webade.WebADEException;
import ca.bc.gov.webade.user.GUID;
import ca.bc.gov.webade.user.UserCredentials;
import ca.bc.gov.webade.user.WebADEUserInfo;
import ca.bc.gov.webade.user.WebADEUserPermissions;
import ca.bc.gov.webade.user.service.UserInfoService;
import ca.bc.gov.webade.user.service.UserInfoServiceException;

public class WebAdeAuthorizationService extends BaseAuthorizationService {
  private static final Logger LOG = LoggerFactory.getLogger(WebAdeAuthorizationService.class);

  private Map<String, Application> webAdeApplications = new HashMap<String, Application>();

  @Override
  public boolean canAccessResource(final String businessApplicationName,
    final String consumerKey, final String resourceClass,
    final String resourceId) {
    return canAccessResource(businessApplicationName, consumerKey,
      resourceClass, resourceId, "any");
  }

  @Override
  public boolean canAccessResource(final String businessApplicationName,
    final String consumerKey, final String resourceClass,
    final String resourceId, final String actionName) {
    return canPerformAction(businessApplicationName, consumerKey, resourceClass
      + ":" + resourceId + ":" + actionName);
  }

  @Override
  public boolean canPerformAction(final String businessApplicationName,
    final String consumerKey, final String actionName) {
    final Application webAdeApplication = getWebAdeApplication(businessApplicationName);
    if (webAdeApplication == null) {
      return false;
    } else {
      final WebADEUserPermissions userPermissions = getUserPermissions(
        businessApplicationName, consumerKey);
      if (userPermissions == null) {
        return false;
      } else {
        final boolean permission = userPermissions.canPerformAction(new Action(
          actionName));
        return permission;
      }
    }
  }

  @PreDestroy
  public void destroy() {
    for (final Application application : webAdeApplications.values()) {
      try {
        application.shutdown();
      } catch (final SecurityException e) {
        LOG.error("Unable to shutdown " + application.getApplicationCode());
      }
    }
    this.webAdeApplications = null;
  }

  @Override
  public Map<String, Object> getUserAttributes(
    final String businessApplicationName, final String consumerKey) {
    final Map<String, Object> userAttributes = new LinkedHashMap<String, Object>();
    final WebADEUserInfo userInfo = getUserInfo(businessApplicationName,
      consumerKey);
    if (userInfo != null) {
      final UserCredentials userCredentials = userInfo.getUserCredentials();
      userAttributes.put("sourceDirectory",
        userCredentials.getSourceDirectory());
      userAttributes.put("userTypeCode", userCredentials.getUserTypeCode());
      userAttributes.put("userName", userCredentials.getUserId());
      userAttributes.put("userGuid", userCredentials.getUserGuid());
      userAttributes.put("firstName", userInfo.getFirstName());
      userAttributes.put("middleInitial", userInfo.getMiddleInitial());
      userAttributes.put("lastName", userInfo.getLastName());
      userAttributes.put("displayName", userInfo.getDisplayName());
      userAttributes.put("emailAddress", userInfo.getEmailAddress());
      userAttributes.put("phoneNumber", userInfo.getPhoneNumber());
      userAttributes.put("expiryDate", userInfo.getExpiryDate());
    }
    return userAttributes;
  }

  protected UserCredentials getUserCredentials(
    final String businessApplicationName, final String consumerKey) {
    final UserCredentials userCredentials = new UserCredentials();
    final String username = getUsername(consumerKey);
    userCredentials.setUserGuid(new GUID(username));
    return userCredentials;
  }

  protected WebADEUserInfo getUserInfo(final String businessApplicationName,
    final String consumerKey) {
    final UserCredentials userCredentials = getUserCredentials(
      businessApplicationName, consumerKey);
    try {
      final Application webAdeApplication = getWebAdeApplication(businessApplicationName);
      if (webAdeApplication == null) {
        return null;
      } else {
        final UserInfoService userInfoService = webAdeApplication.getUserInfoService();
        final WebADEUserInfo userInfo = userInfoService.getWebADEUserInfo(userCredentials);
        return userInfo;
      }
    } catch (final UserInfoServiceException e) {
      throw new RuntimeException("Unable to get WebADE user info for "
        + userCredentials.getUserGuid(), e);
    }
  }

  protected WebADEUserPermissions getUserPermissions(
    final String businessApplicationName, final String consumerKey) {
    final UserCredentials userCredentials = getUserCredentials(
      businessApplicationName, consumerKey);
    try {
      final Application webAdeApplication = getWebAdeApplication(businessApplicationName);
      if (webAdeApplication == null) {
        return null;
      } else {
        final WebADEUserPermissions userPermissions = webAdeApplication.getWebADEUserPermissions(userCredentials);
        return userPermissions;
      }
    } catch (final WebADEException e) {
      throw new RuntimeException("Unable to get WebADE user permissions for "
        + userCredentials.getUserGuid(), e);
    }
  }

  /**
   * Get the WebADE {@link Application} for the business application name. If it
   * could not be created a log error will be created and the cache set to null
   * to prevent repeated attempts to create it. The applications are cached when
   * created. s
   * 
   * @param businessApplicationName
   * @return
   */
  public Application getWebAdeApplication(String businessApplicationName) {
    synchronized (webAdeApplications) {
      Application webAdeApplication = null;
      businessApplicationName = businessApplicationName.toUpperCase();
      if (webAdeApplications.containsKey(businessApplicationName)) {
        webAdeApplication = webAdeApplications.get(businessApplicationName);
      } else {
        try {
          webAdeApplication = WebADEApplicationUtils.createApplication(businessApplicationName);
        } catch (final WebADEException e) {
          LOG.error("Unable to get WebADE application for "
            + businessApplicationName);
        }
        webAdeApplications.put(businessApplicationName, webAdeApplication);
      }
      return webAdeApplication;
    }
  }

  @Override
  public boolean isInGroup(final String businessApplicationName,
    final String consumerKey, final String groupName) {
    final Application webAdeApplication = getWebAdeApplication(businessApplicationName);
    if (webAdeApplication == null) {
      return false;
    } else {
      final AppRoles roles = webAdeApplication.getRoles();
      if (roles == null) {
        return false;
      } else {
        final Role role = roles.getRole(groupName);
        final WebADEUserPermissions userPermissions = getUserPermissions(
          businessApplicationName, consumerKey);
        if (userPermissions == null) {
          return false;
        } else {
          final boolean permission = userPermissions.isUserInRole(role);
          return permission;
        }
      }
    }
  }

}
