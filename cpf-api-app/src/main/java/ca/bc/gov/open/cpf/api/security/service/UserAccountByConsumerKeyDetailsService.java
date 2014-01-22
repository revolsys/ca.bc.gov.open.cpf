package ca.bc.gov.open.cpf.api.security.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.TransactionStatus;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectUtil;

public class UserAccountByConsumerKeyDetailsService implements
  UserDetailsService {

  private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

  private CpfDataAccessObject dataAccessObject;

  private UserAccountSecurityService userAccountSecurityService;

  public UserAccountSecurityService getUserAccountSecurityService() {
    return userAccountSecurityService;
  }

  public UserDetailsChecker getUserDetailsChecker() {
    return userDetailsChecker;
  }

  @Override
  public UserDetails loadUserByUsername(final String username)
    throws UsernameNotFoundException, DataAccessException {
    final TransactionStatus transaction = dataAccessObject.createNewTransaction();
    try {
      final String name = username.toLowerCase();
      final DataObject user = dataAccessObject.getUserAccount(name);
      if (user == null) {
        dataAccessObject.commit(transaction);
        throw new UsernameNotFoundException("Username or password incorrect");
      } else {
        final String userPassword = user.getValue(UserAccount.CONSUMER_SECRET);
        final boolean active = DataObjectUtil.getBoolean(user,
          UserAccount.ACTIVE_IND);
        final List<String> groupNames = userAccountSecurityService.getGroupNames(user);
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String groupName : groupNames) {
          authorities.add(new GrantedAuthorityImpl(groupName));
          authorities.add(new GrantedAuthorityImpl("ROLE_" + groupName));
        }
        final User userDetails = new User(name, userPassword, active, true,
          true, true, authorities);
        if (userDetailsChecker != null) {
          userDetailsChecker.check(userDetails);
        }
        dataAccessObject.commit(transaction);
        return userDetails;
      }
    } catch (final Throwable e) {
      dataAccessObject.handleException(transaction, e);
      return null;
    }
  }

  @Required
  @Resource(name = "userAccountSecurityService")
  public void setUserAccountSecurityService(
    final UserAccountSecurityService userAccountSecurityService) {
    this.userAccountSecurityService = userAccountSecurityService;
    dataAccessObject = userAccountSecurityService.getDataAccessObject();
  }

  @Required
  public void setUserDetailsChecker(final UserDetailsChecker userDetailsChecker) {
    this.userDetailsChecker = userDetailsChecker;
  }
}
