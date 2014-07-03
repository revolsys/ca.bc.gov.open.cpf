package ca.bc.gov.open.cpf.api.web.controller;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ca.bc.gov.open.cpf.api.domain.UserAccount;

import com.revolsys.data.record.Record;

public class ResetOAuthCredentialsByUserAccountController implements Controller {
  /** The user type to use when finding the user by their external name. */
  private String userAccountClass;

  private String successViewName;

  private String viewName;

  public String getSuccessViewName() {
    return successViewName;
  }

  /**
   * Get the user type to use when finding the user by their external name.
   * 
   * @return The user type to use when finding the user by their external name.
   */
  public String getUserAccountClass() {
    return userAccountClass;
  }

  public String getViewName() {
    return viewName;
  }

  @Override
  public ModelAndView handleRequest(final HttpServletRequest request,
    final HttpServletResponse response) throws Exception {
    if (request.getMethod().equals("POST")) {
      final Record user = (Record)request.getAttribute("userAccount");
      if (user != null) {
        user.setValue(UserAccount.CONSUMER_SECRET, UUID.randomUUID().toString());
        return new ModelAndView(successViewName);
      }
    }
    return new ModelAndView(viewName);
  }

  public void setSuccessViewName(final String successViewName) {
    this.successViewName = successViewName;
  }

  /**
   * Set the user type to use when finding the user by their external name.
   * 
   * @param userType The user type to use when finding the user by their
   *          external name.
   */
  public void setUserAccountClass(final String userType) {
    this.userAccountClass = userType;
  }

  public void setViewName(final String viewName) {
    this.viewName = viewName;
  }

}
