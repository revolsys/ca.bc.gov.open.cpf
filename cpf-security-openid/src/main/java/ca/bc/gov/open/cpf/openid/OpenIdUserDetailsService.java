/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.openid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.security.service.UserAccountSecurityService;

import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordUtil;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;

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
    return this.userAccountClass;
  }

  public UserAccountSecurityService getUserAccountSecurityService() {
    return this.userAccountSecurityService;
  }

  /**
   * Get the class to use to check that the user is valid.
   *
   * @return The class to use to check that the user is valid.
   */
  public UserDetailsChecker getUserDetailsChecker() {
    return this.userDetailsChecker;
  }

  @PostConstruct
  public void init() {
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        this.dataAccessObject.createUserGroup("USER_TYPE", "OPENID", "OpenID All Users");
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  /**
   * Get the flag to auto create users if they don't exist.
   *
   * @return The flag to auto create users if they don't exist.
   */
  public boolean isAutoCreateUsers() {
    return this.autoCreateUsers;
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
  public UserDetails loadUserByUsername(final String userAccountName) {
    try (
      Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        Record user = this.dataAccessObject.getUserAccount(this.userAccountClass, userAccountName);
        if (user == null) {
          if (!this.autoCreateUsers) {
            throw new UsernameNotFoundException("Username or password incorrect");
          } else {
            final String consumerKey = UUID.randomUUID().toString().toLowerCase();
            final String consumerSecret = UUID.randomUUID().toString().toLowerCase();
            SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken(consumerKey, consumerSecret));
            user = this.dataAccessObject.createUserAccount(this.userAccountClass, userAccountName,
              consumerKey, consumerSecret);
          }
        }
        final String userName = user.getValue(UserAccount.CONSUMER_KEY);
        final String userPassword = user.getValue(UserAccount.CONSUMER_SECRET);
        final boolean active = RecordUtil.getBoolean(user, UserAccount.ACTIVE_IND);
        final List<String> groupNames = this.userAccountSecurityService.getGroupNames(user);
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String groupName : groupNames) {
          authorities.add(new SimpleGrantedAuthority(groupName));
          authorities.add(new SimpleGrantedAuthority("ROLE_" + groupName));
        }
        final User userDetails = new User(userName, userPassword, active, true, true, true,
          authorities);

        if (this.userDetailsChecker != null) {
          this.userDetailsChecker.check(userDetails);
        }
        return userDetails;
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  /**
   * Set the flag to auto create users if they don't exist.
   *
   * @param autoCreateUsers The flag to auto create users if they don't exist.
   */
  public void setAutoCreateUsers(final boolean autoCreateUsers) {
    this.autoCreateUsers = autoCreateUsers;
  }

  @Autowired
  @Required
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
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
