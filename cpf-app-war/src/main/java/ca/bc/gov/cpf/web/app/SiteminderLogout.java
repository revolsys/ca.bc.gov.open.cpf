package ca.bc.gov.cpf.web.app;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;

import com.revolsys.ui.web.config.Page;
import com.revolsys.util.UrlUtil;

public class SiteminderLogout implements LogoutSuccessHandler {
  private String logoutUrl;

  public String getLogoutUrl() {
    return logoutUrl;
  }

  public void setLogoutUrl(String logoutUrl) {
    this.logoutUrl = logoutUrl;
  }

  @Override
  public void onLogoutSuccess(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication) throws IOException, ServletException {
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equals("WEBADEUSERGUID")) {
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
      }
    }

    if (StringUtils.hasText(logoutUrl)) {
      Map<String, String> parameters = new LinkedHashMap<String, String>();
      parameters.put("returl", Page.getAbsoluteUrl("/secure/"));
      parameters.put("retname", "Cloud Processing Framework");
      String url = UrlUtil.getUrl(Page.getAbsoluteUrl(logoutUrl), parameters);
      response.sendRedirect(url);
    }
  }
}
