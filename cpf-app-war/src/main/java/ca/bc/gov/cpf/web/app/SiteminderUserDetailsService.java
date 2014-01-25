package ca.bc.gov.cpf.web.app;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.security.service.GroupNameService;
import ca.bc.gov.open.cpf.api.security.service.UserAccountSecurityService;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.ui.web.utils.HttpServletUtils;

public class SiteminderUserDetailsService implements UserDetailsService,
  GroupNameService {

  private static final String BCGOV_ALL = "BCGOV_ALL";

  private static final String BCGOV_BUSINESS = "BCGOV_BUSINESS";

  private static final String BCGOV_EXTERNAL = "BCGOV_EXTERNAL";

  private static final String BCGOV_INDIVIDUAL = "BCGOV_INDIVIDUAL";

  private static final String BCGOV_INTERNAL = "BCGOV_INTERNAL";

  private static final String BCGOV_VERIFIED_INDIVIDUAL = "BCGOV_VERIFIED_INDIVIDUAL";

  private static final String USER_ACCOUNT_CLASS = "BCGOV";

  private CpfDataAccessObject dataAccessObject;

  private UserAccountSecurityService userAccountSecurityService;

  /** The class to use to check that the user is valid. */
  private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

  @PreDestroy
  public void close() {
    userAccountSecurityService = null;
    userDetailsChecker = null;
    dataAccessObject = null;
  }

  @Override
  public List<String> getGroupNames(final DataObject userAccount) {
    final List<String> groupNames = new ArrayList<String>();
    if (userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS).equals(
      USER_ACCOUNT_CLASS)) {
      final String username = userAccount.getValue(UserAccount.CONSUMER_KEY);
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

  public UserAccountSecurityService getUserAccountSecurityService() {
    return userAccountSecurityService;
  }

  public UserDetailsChecker getUserDetailsChecker() {
    return userDetailsChecker;
  }

  @PostConstruct
  public void init() {
    try (
      Transaction transaction = dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        dataAccessObject.createUserGroup("USER_TYPE", "BCGOV_ALL",
          "BC Government All Users");
        dataAccessObject.createUserGroup("USER_TYPE", "BCGOV_INTERNAL",
          "BC Government Internal Users");
        dataAccessObject.createUserGroup("USER_TYPE", "BCGOV_EXTERNAL",
          "BC Government External Users");
        dataAccessObject.createUserGroup("USER_TYPE", "BCGOV_BUSINESS",
          "BC Government External Business Users");
        dataAccessObject.createUserGroup("USER_TYPE", "BCGOV_INDIVIDUAL",
          "BC Government External Individual Users");
        dataAccessObject.createUserGroup("USER_TYPE",
          "BCGOV_VERIFIED_INDIVIDUAL",
          "BC Government External Verified Individual Users");
        userAccountSecurityService.addGrantedAuthorityService(this);
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  @Override
  public UserDetails loadUserByUsername(final String userGuid) {
    try (
      Transaction transaction = dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        DataObject user = dataAccessObject.getUserAccount(USER_ACCOUNT_CLASS,
          userGuid);

        String username;
        if (user == null) {
          final HttpServletRequest request = HttpServletUtils.getRequest();
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
          final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            username, consumerSecret);
          context.setAuthentication(authentication);

          user = dataAccessObject.createUserAccount(USER_ACCOUNT_CLASS,
            userGuid, username, consumerSecret);
        } else {
          username = user.getValue(UserAccount.CONSUMER_KEY);

        }

        final String userPassword = user.getValue(UserAccount.CONSUMER_SECRET);
        final boolean active = DataObjectUtil.getBoolean(user,
          UserAccount.ACTIVE_IND);
        final List<String> groupNames = userAccountSecurityService.getGroupNames(user);
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String groupName : groupNames) {
          authorities.add(new GrantedAuthorityImpl(groupName));
          authorities.add(new GrantedAuthorityImpl("ROLE_" + groupName));
        }

        final User userDetails = new User(username, userPassword, active, true,
          true, true, authorities);

        if (userDetailsChecker != null) {
          userDetailsChecker.check(userDetails);
        }
        return userDetails;
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  @Required
  public void setUserAccountSecurityService(
    final UserAccountSecurityService userAccountSecurityService) {
    this.userAccountSecurityService = userAccountSecurityService;
    dataAccessObject = userAccountSecurityService.getDataAccessObject();
  }

  public void setUserDetailsChecker(final UserDetailsChecker userDetailsChecker) {
    this.userDetailsChecker = userDetailsChecker;
  }

}
