package ca.bc.gov.cpf.web.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import ca.bc.gov.open.cpf.api.security.service.GroupNameService;
import ca.bc.gov.open.cpf.api.security.service.UserAccountSecurityService;

import com.revolsys.ui.web.utils.HttpRequestUtils;

public class SiteminderUserDetailsService implements UserDetailsService,
  GroupNameService {

  private static final String BCGOV_ALL = "BCGOV_ALL";

  private static final String BCGOV_EXTERNAL = "BCGOV_EXTERNAL";

  private static final String BCGOV_INTERNAL = "BCGOV_INTERNAL";

  private static final String BCGOV_BUSINESS = "BCGOV_BUSINESS";

  private static final String BCGOV_INDIVIDUAL = "BCGOV_INDIVIDUAL";

  private static final String BCGOV_VERIFIED_INDIVIDUAL = "BCGOV_VERIFIED_INDIVIDUAL";

  private static final String USER_ACCOUNT_CLASS = "BCGOV";

  /** The data access object for {@link UserAccount} objects. */
  private UserAccountDao userAccountDao;

  private UserAccountSecurityService userAccountSecurityService;

  /** The class to use to check that the user is valid. */
  private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

  /** The data access object for {@link UserGroup} objects. */
  private UserGroupDao userGroupDao;

  @Override
  public List<String> getGroupNames(final UserAccount userAccount) {
    final List<String> groupNames = new ArrayList<String>();
    if (userAccount.getUserAccountClass().equals(USER_ACCOUNT_CLASS)) {
      final String username = userAccount.getConsumerKey();
      if (username.startsWith("idir:")) {
        groupNames.add(BCGOV_ALL);
        groupNames.add(BCGOV_INTERNAL);
      } else if (username.startsWith("bceid:")) {
        groupNames.add(BCGOV_ALL);
        groupNames.add(BCGOV_EXTERNAL);
        groupNames.add(BCGOV_BUSINESS);
      } else if (username.startsWith("vin:")) {
        groupNames.add(BCGOV_ALL);
        groupNames.add(BCGOV_EXTERNAL);
        groupNames.add(BCGOV_VERIFIED_INDIVIDUAL);
      } else if (username.startsWith("ind:")) {
        groupNames.add(BCGOV_ALL);
        groupNames.add(BCGOV_EXTERNAL);
        groupNames.add(BCGOV_INDIVIDUAL);
      }
    }
    return groupNames;
  }

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
      "BCGOV_ALL", "BC Government All Users");
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV_INTERNAL", "BC Government Internal Users");
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV_EXTERNAL", "BC Government External Users");
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV_BUSINESS", "BC Government External Business Users");
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV_INDIVIDUAL", "BC Government External Individual Users");
    userAccountSecurityService.createUserGroup(userGroupDao, "USER_TYPE",
      "BCGOV_VERIFIED_INDIVIDUAL",
      "BC Government External Verified Individual Users");
    userAccountSecurityService.addGrantedAuthorityService(this);
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
        } else if (userType.equalsIgnoreCase("BUSINESS")) {
          username = "bceid:" + username;
        } else if (userType.equalsIgnoreCase("VERIFIED INDIVIDUAL")) {
          username = "vin:" + username;
        } else if (userType.equalsIgnoreCase("INDIVIDUAL")) {
          username = "ind:" + username;
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
    List<String> groupNames = userAccountSecurityService.getGroupNames(user);
    List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    for (String groupName : groupNames) {
      authorities.add(new GrantedAuthorityImpl("ROLE_" + groupName));
    }

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
