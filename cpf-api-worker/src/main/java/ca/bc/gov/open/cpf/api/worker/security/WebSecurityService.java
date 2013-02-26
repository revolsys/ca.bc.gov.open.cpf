package ca.bc.gov.open.cpf.api.worker.security;

import java.util.LinkedHashMap;
import java.util.Map;

import ca.bc.gov.open.cpf.client.httpclient.OAuthHttpClient;
import ca.bc.gov.open.cpf.client.httpclient.OAuthHttpClientPool;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractCachingSecurityService;

import com.revolsys.util.UrlUtil;

public class WebSecurityService extends AbstractCachingSecurityService {

  private final OAuthHttpClientPool httpClientPool;

  public WebSecurityService(final OAuthHttpClientPool httpClientPool,
    final Module module, final String userId) {
    super(module, userId);
    this.httpClientPool = httpClientPool;
  }

  @Override
  protected Boolean loadActionPermission(final String actionName) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String moduleName = getModuleName();
      final String url = httpClient.getUrl("/worker/modules/" + moduleName
        + "/users/" + getUsername() + "/actions/" + actionName + "/hasAccess");
      final Map<String, Object> response = httpClient.getJsonResource(url);
      final Boolean actionPermission = (Boolean)response.get("hasAccess");
      return actionPermission;
    } catch (final Throwable e) {
      throw new RuntimeException(
        "Unable to get security action permission for " + getUsername()
          + " action=" + actionName, e);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  @Override
  protected Boolean loadGroupPermission(final String groupName) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String moduleName = getModuleName();
      final String url = httpClient.getUrl("/worker/modules/" + moduleName
        + "/users/" + getUsername() + "/groups/" + groupName + "/memberOf");
      final Map<String, Object> response = httpClient.getJsonResource(url);
      final Boolean groupPermission = (Boolean)response.get("memberOfGroup");
      return groupPermission;
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to get security group permission for "
        + getUsername() + " group=" + groupName, e);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  @Override
  protected Boolean loadResourceAccessPermission(
    final String resourceClass,
    final String resourceId,
    final String actionName) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String moduleName = getModuleName();
      String url = "/worker/modules/" + moduleName + "/users/"
        + getUsername() + "/resourcePermission";
      Map<String, Object> parameters = new LinkedHashMap<String, Object>();
      parameters.put("resourceClass", resourceClass);
      parameters.put("resourceId", resourceId);
      parameters.put("actionName", actionName);
      url = UrlUtil.getUrl(url, parameters);
      url = httpClient.getUrl(url);

      final Map<String, Object> response = httpClient.getJsonResource(url);
      final Boolean actionPermission = (Boolean)response.get("hasAccess");
      return actionPermission;
    } catch (final Throwable e) {
      throw new RuntimeException(
        "Unable to get security resource access permission for "
          + getUsername() + " resourcesClass=" + resourceClass + " resourceId="
          + resourceId + " action=" + actionName, e);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  @Override
  protected Map<String, Object> loadUserAttributes() {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String moduleName = getModuleName();
      final String url = httpClient.getUrl("/worker/modules/" + moduleName
        + "/users/" + getUsername() + "/attributes");
      return httpClient.getJsonResource(url);
    } catch (final Throwable e) {
      throw new RuntimeException("Unable to get user attributes for "
        + getUsername(), e);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }
}
