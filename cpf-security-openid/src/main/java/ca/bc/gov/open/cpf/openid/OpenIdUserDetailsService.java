package ca.bc.gov.open.cpf.openid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.security.service.UserAccountSecurityService;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectUtil;

/**
 * @author Paul Austin paul.austin@revolsys.com
 */
public class OpenIdUserDetailsService implements UserDetailsService {
  /** The flag to auto create users if they don't exist. */
  private boolean autoCreateUsers;

  /** The user type to use when finding the user by their external name. */
  private String userAccountClass;

  /** The class to use to check that the user is valid. */
  private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

  private UserAccountSecurityService userAccountSecurityService;

  private CpfDataAccessObject dataAccessObject;

  /**
   * Get the user type to use when finding the user by their external name.
   * 
   * @return The user type to use when finding the user by their external name.
   */
  public String getUserAccountClass() {
    return userAccountClass;
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

  @PostConstruct
  public void init() {
    dataAccessObject.createUserGroup("USER_TYPE", "OPENID", "OpenID All Users");
  }

  /**
   * Get the flag to auto create users if they don't exist.
   * 
   * @return The flag to auto create users if they don't exist.
   */
  public boolean isAutoCreateUsers() {
    return autoCreateUsers;
  }

  @Autowired
  @Required
  public void setDataAccessObject(CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

  /**
   * Load the external user which has the {@link #userAccountClass} and external
   * user name. If the user does not exist in the database create a new external
   * user record with a generated consumer key and consumer secret.
   * 
   * @param userAccountName The external user name to login as.
   * @return The UserDetals object for the user.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UserDetails loadUserByUsername(final String userAccountName) {
    DataObject user = dataAccessObject.getUserAccount(userAccountClass,
      userAccountName);
    if (user == null) {
      if (!autoCreateUsers) {
        throw new UsernameNotFoundException("Username or password incorrect");
      } else {
        final String consumerKey = UUID.randomUUID().toString().toLowerCase();
        final String consumerSecret = UUID.randomUUID()
          .toString()
          .toLowerCase();
        SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(consumerKey, consumerSecret));
        user = dataAccessObject.createUserAccount(userAccountClass,
          userAccountName, consumerKey, consumerSecret);
      }
    }
    final String userName = user.getValue(UserAccount.CONSUMER_KEY);
    final String userPassword = user.getValue(UserAccount.CONSUMER_SECRET);
    final boolean active = DataObjectUtil.getBoolean(user,
      UserAccount.ACTIVE_IND);
    List<String> groupNames = userAccountSecurityService.getGroupNames(user);
    List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    for (String groupName : groupNames) {
      authorities.add(new GrantedAuthorityImpl("ROLE_" + groupName));
    }
    final User userDetails = new User(userName, userPassword, active, true,
      true, true, authorities);

    if (userDetailsChecker != null) {
      userDetailsChecker.check(userDetails);
    }
    return userDetails;
  }

  /**
   * Set the flag to auto create users if they don't exist.
   * 
   * @param autoCreateUsers The flag to auto create users if they don't exist.
   */
  public void setAutoCreateUsers(final boolean autoCreateUsers) {
    this.autoCreateUsers = autoCreateUsers;
  }

  /**
   * Set the user type to use when finding the user by their external name.
   * 
   * @param userAccountClass The user type to use when finding the user by their
   *          external name.
   */
  public void setUserAccountClass(final String userAccountClass) {
    this.userAccountClass = userAccountClass;
  }

  @Resource(name = "userAccountSecurityService")
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
}
