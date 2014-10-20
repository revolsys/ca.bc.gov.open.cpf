package ca.bc.gov.cpf.web.app;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;

import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.UrlUtil;

public class SiteminderLogout implements LogoutSuccessHandler {
  private static final List<String> DELETE_COOKIES = Arrays.asList(
    "WEBADEUSERGUID", "SMSESSION");

  private String logoutUrl;

  public String getLogoutUrl() {
    return logoutUrl;
  }

  public void setLogoutUrl(String logoutUrl) {
    this.logoutUrl = logoutUrl;
  }

  public void onLogoutSuccess(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication) throws IOException, ServletException {
    for (Cookie cookie : request.getCookies()) {
      if (DELETE_COOKIES.contains(cookie.getName())) {
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
      }
    }

    String url = HttpServletUtils.getAbsoluteUrl("/secure/");

    if (StringUtils.hasText(logoutUrl)) {
      Map<String, String> parameters = new LinkedHashMap<String, String>();
      parameters.put("returl", url);
      parameters.put("retname", "Concurrent Processing Framework");
      url = UrlUtil.getUrl(HttpServletUtils.getAbsoluteUrl(logoutUrl), parameters);
    }
    response.sendRedirect(url);
  }
}
