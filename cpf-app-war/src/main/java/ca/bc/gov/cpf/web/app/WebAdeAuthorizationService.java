package ca.bc.gov.cpf.web.app;

import java.util.Map;

import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

import ca.bc.gov.open.cpf.api.security.service.BaseAuthorizationService;
import ca.bc.gov.webade.Application;
import ca.bc.gov.webade.http.HttpRequestUtils;
import ca.bc.gov.webade.user.GUID;
import ca.bc.gov.webade.user.UserCredentials;
import ca.bc.gov.webade.user.WebADEUserInfo;
import ca.bc.gov.webade.user.service.UserInfoService;
import ca.bc.gov.webade.user.service.UserInfoServiceException;

public class WebAdeAuthorizationService extends BaseAuthorizationService
  implements ServletContextAware {

  private ServletContext context;

  @Override
  public void setServletContext(final ServletContext context) {
    this.context = context;
  }

  private Application getApplication() {
    return HttpRequestUtils.getApplication(context);
  }

  @PreDestroy
  public void destroy() {
    this.context = null;
  }

  @Override
  public Map<String, Object> getUserAttributes(
    final String businessApplicationName,
    final String consumerKey) {
    final Map<String, Object> userAttributes = super.getUserAttributes(
      businessApplicationName, consumerKey);
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

  private WebADEUserInfo getUserInfo(
    final String businessApplicationName,
    final String consumerKey) {
    try {
      if (getApplication() == null) {
        return null;
      } else {
        final UserInfoService userInfoService = getApplication().getUserInfoService();
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUserGuid(new GUID(consumerKey));
        final WebADEUserInfo userInfo = userInfoService.getWebADEUserInfo(userCredentials);
        return userInfo;
      }
    } catch (final UserInfoServiceException e) {
      throw new RuntimeException("Unable to get WebADE user info for "
        + consumerKey, e);
    }
  }

}
