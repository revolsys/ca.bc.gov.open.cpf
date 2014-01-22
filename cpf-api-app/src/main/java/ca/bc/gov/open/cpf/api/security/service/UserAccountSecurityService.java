package ca.bc.gov.open.cpf.api.security.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.domain.UserGroup;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectUtil;

public class UserAccountSecurityService {
  private final List<GroupNameService> grantedAuthorityServices = new ArrayList<GroupNameService>();

  private CpfDataAccessObject dataAccessObject;

  public void addGrantedAuthorityService(
    final GroupNameService grantedAuthorityService) {
    grantedAuthorityServices.add(grantedAuthorityService);
  }

  public CpfDataAccessObject getDataAccessObject() {
    return dataAccessObject;
  }

  public List<String> getGroupNames(final DataObject userAccount) {
    final TransactionStatus transaction = dataAccessObject.createNewTransaction();
    try {
      final List<String> groupNames = new ArrayList<String>();
      try {
        if (userAccount != null
          && DataObjectUtil.getBoolean(userAccount, UserAccount.ACTIVE_IND)) {
          final String userType = userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS);
          groupNames.add("USER");
          groupNames.add(userType);
          final Set<DataObject> groups = dataAccessObject.getUserGroupsForUserAccount(userAccount);
          if (groups != null) {
            for (final DataObject userGroup : groups) {
              final String groupName = userGroup.getValue(UserGroup.USER_GROUP_NAME);
              groupNames.add(groupName);
            }
          }
          for (final GroupNameService authorityService : grantedAuthorityServices) {
            final List<String> names = authorityService.getGroupNames(userAccount);
            if (names != null) {
              groupNames.addAll(names);
            }
          }
        }
        dataAccessObject.commit(transaction);
      } catch (final Throwable t) {
        LoggerFactory.getLogger(UserAccountSecurityService.class).error(
          "Unable to load authorities for user "
            + userAccount.getValue(UserAccount.CONSUMER_KEY), t);
      }
      return groupNames;
    } catch (final Throwable e) {
      dataAccessObject.handleException(transaction, e);
      return null;
    }
  }

  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

}
