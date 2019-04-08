/*
 * Copyright © 2008-2016, Province of British Columbia
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
package ca.bc.gov.open.cpf.api.security.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jeometry.common.logging.Logs;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.domain.UserGroup;

import com.revolsys.record.Record;
import com.revolsys.record.Records;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;

public class UserAccountSecurityService {
  private final List<GroupNameService> grantedAuthorityServices = new ArrayList<>();

  private CpfDataAccessObject dataAccessObject;

  public void addGrantedAuthorityService(final GroupNameService grantedAuthorityService) {
    this.grantedAuthorityServices.add(grantedAuthorityService);
  }

  public CpfDataAccessObject getDataAccessObject() {
    return this.dataAccessObject;
  }

  public List<String> getGroupNames(final Record userAccount) {
    try (
      Transaction transaction = this.dataAccessObject.newTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final List<String> groupNames = new ArrayList<>();
        try {
          if (userAccount != null && Records.getBoolean(userAccount, UserAccount.ACTIVE_IND)) {
            final String userType = userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS);
            groupNames.add("USER");
            groupNames.add(userType);
            final Set<Record> groups = this.dataAccessObject
              .getUserGroupsForUserAccount(userAccount);
            if (groups != null) {
              for (final Record userGroup : groups) {
                final String groupName = userGroup.getValue(UserGroup.USER_GROUP_NAME);
                groupNames.add(groupName);
              }
            }
            for (final GroupNameService authorityService : this.grantedAuthorityServices) {
              final List<String> names = authorityService.getGroupNames(userAccount);
              if (names != null) {
                groupNames.addAll(names);
              }
            }
          }
        } catch (final Throwable t) {
          Logs.error(UserAccountSecurityService.class,
            "Unable to load authorities for user " + userAccount.getValue(UserAccount.CONSUMER_KEY),
            t);
        }
        return groupNames;
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

}
