package ca.bc.gov.cpf.web.app;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;

public class SiteminderAuthenticationFilter extends
  AbstractPreAuthenticatedProcessingFilter {
  /**
   * Read and returns the header named by {@code principalRequestHeader} from
   * the request.
   * 
   * @throws PreAuthenticatedCredentialsNotFoundException if the header is
   *           missing and {@code exceptionIfHeaderMissing} is set to
   *           {@code true}.
   */
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    String principal = request.getHeader("SMGOV_USERGUID");
    if (principal == null) {
      throw new PreAuthenticatedCredentialsNotFoundException(
        " SM_USER header not found in request.");
    } else {
      return principal;
    }
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "";
  }
}
