package ca.bc.gov.cpf.web.app;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

import com.revolsys.ui.web.config.Page;
import com.revolsys.util.UrlUtil;

public class WebAdeLogout implements LogoutHandler {
  private String logoutUrl;

  public String getLogoutUrl() {
    return logoutUrl;
  }

  public void setLogoutUrl(String logoutUrl) {
    this.logoutUrl = logoutUrl;
  }

  @Override
  public void logout(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
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
      String url = UrlUtil.getUrl(logoutUrl, parameters);
      try {
        response.sendRedirect(url);
      } catch (IOException e) {
      }
    }
  }
}
