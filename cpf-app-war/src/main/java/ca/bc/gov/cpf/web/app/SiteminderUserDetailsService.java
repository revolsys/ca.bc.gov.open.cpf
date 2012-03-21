package ca.bc.gov.cpf.web.app;

import java.util.Collection;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ca.bc.gov.open.cpf.api.dao.UserAccountDao;
import ca.bc.gov.open.cpf.api.dao.UserGroupDao;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.domain.UserGroup;
import ca.bc.gov.open.cpf.api.security.service.UserAccountSecurityService;

import com.revolsys.ui.web.utils.HttpRequestUtils;

public class SiteminderUserDetailsService implements UserDetailsService {

  private static final String ROLE_BCGOV = "ROLE_BCGOV";

  private static final String ROLE_BCGOV_EXTERNAL = "ROLE_BCGOV_EXTERNAL";

  private static final String ROLE_BCGOV_INTERNAL = "ROLE_BCGOV_INTERNAL";

  private static final String USER_ACCOUNT_CLASS = "BCGOV";

  /** The data access object for {@link UserAccount} objects. */
  private UserAccountDao userAccountDao;

  private UserAccountSecurityService userAccountSecurityService;

  /** The class to use to check that the user is valid. */
  private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

  /** The data access object for {@link UserGroup} objects. */
  private UserGroupDao userGroupDao;

  /**
   * Get the data access object for {@link UserAccount} objects.
   * 
   * @return The data access object for {@link UserAccount} objects.
   */
  public UserAccountDao getUserAccountDao() {
    return userAccountDao;
  }

  public UserAccountSecurityService getUserAccountSecurityService() {
    return userAccountSecurityService;
  }

  /**
   * Get the class to use to check that the user is valid.
   * 
   * @return The class to use to check that the user is valid.
   */
  public UserDetailsChecker getUserDetailsChecker() {
    return userDetailsChecker;
  }

  public UserGroupDao getUserGroupDao() {
    return userGroupDao;
  }

  @PostConstruct
  public void init() {
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV", "BC Government All Users");
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV_INTERNAL", "BC Government Internal Users");
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV_EXTERNAL", "BC Government External Users");
  }

  /**
   * Load the external user which has the {@link #userAccountClass} and external
   * user name. If the user does not exist in the database create a new external
   * user record with a generated consumer key and consumer secret.
   * 
   * @param guid The external user name to login as.
   * @return The UserDetals object for the user.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UserDetails loadUserByUsername(final String userGuid) {
    UserAccount user = userAccountDao.getByUserAccountName(USER_ACCOUNT_CLASS,
      userGuid);

    String username;
    if (user == null) {
      final HttpServletRequest request = HttpRequestUtils.getHttpServletRequest();
      final String userType = request.getHeader("SMGOV_USERTYPE");
      final SecurityContext context = SecurityContextHolder.getContext();
      username = request.getHeader("SM_UNIVERSALID").toLowerCase();
      username = username.replace('\\', ':');
      final int index = username.indexOf(':');
      if (index == -1) {
        if (userType.equalsIgnoreCase("INTERNAL")) {
          username = "idir:" + username;
        } else {
          username = "bceid:" + username;
        }
      }

      final String consumerSecret = UUID.randomUUID().toString();
      user = new UserAccount();
      user.setUserAccountName(userGuid);
      user.setUserAccountClass(USER_ACCOUNT_CLASS);
      user.setConsumerKey(username);
      user.setConsumerSecret(consumerSecret);
      final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        username, consumerSecret);
      context.setAuthentication(authentication);
      userAccountDao.persist(user);
    } else {
      username = user.getConsumerKey();

    }

    final String userPassword = user.getConsumerSecret();
    final boolean active = user.isActive();
    Collection<GrantedAuthority> authorities = userAccountSecurityService.getGrantedAuthorities(user);

    String userTypeRole;
    if (username.startsWith("idir:")) {
      userTypeRole = ROLE_BCGOV_INTERNAL;
    } else {
      userTypeRole = ROLE_BCGOV_EXTERNAL;
    }
    authorities.add(new GrantedAuthorityImpl(userTypeRole));
    authorities.add(new GrantedAuthorityImpl(ROLE_BCGOV));
    final User userDetails = new User(username, userPassword, active, true,
      true, true, authorities);

    if (userDetailsChecker != null) {
      userDetailsChecker.check(userDetails);
    }
    return userDetails;
  }

  /**
   * Set the data access object for {@link UserAccount} objects.
   * 
   * @param userAccountDao The data access object for {@link UserAccount}
   *          objects.
   */
  public void setUserAccountDao(final UserAccountDao userAccountDao) {
    this.userAccountDao = userAccountDao;
  }

  @Resource(name = "userAccountSecurityService")
  @Required
  public void setUserAccountSecurityService(
    final UserAccountSecurityService userAccountSecurityService) {
    this.userAccountSecurityService = userAccountSecurityService;
  }

  /**
   * Set the class to use to check that the user is valid.
   * 
   * @param userDetailsChecker The class to use to check that the user is valid.
   */
  public void setUserDetailsChecker(final UserDetailsChecker userDetailsChecker) {
    this.userDetailsChecker = userDetailsChecker;
  }

  public void setUserGroupDao(final UserGroupDao userGroupDao) {
    this.userGroupDao = userGroupDao;
  }
}
