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
package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.domain.UserGroup;
import ca.bc.gov.open.cpf.api.domain.UserGroupAccountXref;

import com.revolsys.collection.ArrayListOfMap;
import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.equals.Equals;
import com.revolsys.identifier.Identifier;
import com.revolsys.io.IoConstants;
import com.revolsys.record.Record;
import com.revolsys.record.query.Condition;
import com.revolsys.record.query.Q;
import com.revolsys.record.query.Query;
import com.revolsys.ui.html.decorator.FieldLabelDecorator;
import com.revolsys.ui.html.decorator.TableBody;
import com.revolsys.ui.html.fields.AutoCompleteTextField;
import com.revolsys.ui.html.fields.Field;
import com.revolsys.ui.html.fields.FieldWithSubmitButton;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.MenuElement;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.html.view.TableRow;
import com.revolsys.ui.model.Menu;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.Property;

@Controller
public class UserAccountUiBuilder extends CpfUiBuilder implements UserAccount {

  public UserAccountUiBuilder() {
    super("userAccount", USER_ACCOUNT, CONSUMER_KEY, "User Account", "User Accounts");
    setIdParameterName("consumerKey");
  }

  public void addMembersDataTable(final TabElementContainer container, final String prefix)
    throws NoSuchRequestHandlingMethodException {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put("serverSide", Boolean.TRUE);
    parameters.put("deferLoading", 0);
    parameters.put("tabbed", true);

    final String pageName = prefix + "List";
    final HttpServletRequest request = getRequest();
    final ElementContainer element = createDataTable(request, pageName, parameters);
    if (element != null) {

      final String addUrl = getPageUrl(prefix + "MemberAdd");
      final Form form = new Form(prefix + "MemberAdd", addUrl);

      final ElementContainer fieldContainer = new ElementContainer(new TableBody());
      form.add(fieldContainer);

      final String searchUrl = getPageUrl("searchLikeName");
      final AutoCompleteTextField consumerKeyField = new AutoCompleteTextField("consumerKey",
        searchUrl, true);

      final FieldWithSubmitButton submitField = new FieldWithSubmitButton(consumerKeyField, "add",
        "Add");
      final FieldLabelDecorator usernameLabel = new FieldLabelDecorator("Username");
      usernameLabel.setInstructions(
        "Search for a user by typing 3 or more consecutive characters from any part of the Consumer Key or User Account Name. The matching users will be displayed in a pop-up list. Select the required user and click the Add button.");
      submitField.setDecorator(usernameLabel);

      final TableRow row = new TableRow();
      row.add(submitField, usernameLabel);
      fieldContainer.add(row);

      element.add(0, form);

      final String tabId = getTypeName() + "_" + pageName;
      final String title = getPageTitle(pageName);
      container.add(tabId, title, element);
    }
  }

  public ModelAndView addUserGroupMembership(final HttpServletRequest request,
    final HttpServletResponse response, final String moduleName, final String userGroupName,
    final String moduleGroupName, final String consumerKey, final String parentPageName,
    final String tabName) throws NoSuchRequestHandlingMethodException {
    if (Property.hasValue(consumerKey)) {
      if (moduleName != null) {
        hasModule(request, moduleName);
      }
      final Record userGroup = getUserGroup(userGroupName);
      if (userGroup != null && (moduleGroupName == null
        || userGroup.getValue(UserGroup.MODULE_NAME).equals(moduleGroupName))) {

        final Record userAccount = getUserAccount(consumerKey);
        if (userAccount == null) {
          final ModelMap model = new ModelMap();
          model.put("body", "/WEB-INF/jsp/admin/groupMemberNotFound.jsp");
          model.put("consumerKey", consumerKey);
          return new ModelAndView("/jsp/template/page", model);
        } else {
          final CpfDataAccessObject dataAccessObject = getDataAccessObject();
          dataAccessObject.createUserGroupAccountXref(userGroup, userAccount);
          redirectToTab(UserGroup.USER_GROUP, parentPageName, tabName);
          return null;
        }
      }
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final ModelMap model = new ModelMap();
      model.put("body", "/WEB-INF/jsp/admin/groupMemberNull.jsp");
      return new ModelAndView("/jsp/template/page", model);
    }
  }

  @RequestMapping(value = {
    "/admin/userAccounts/searchLikeName"
  }, method = RequestMethod.GET)
  @ResponseBody
  public ArrayListOfMap<Object> autocompleteUserName(final HttpServletRequest request,
    final HttpServletResponse response, @RequestParam("term") final String term)
      throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin();
    request.setAttribute(IoConstants.JSON_LIST_ROOT_PROPERTY, Boolean.TRUE);
    final CpfDataAccessObject cpfDataAccessObject = getDataAccessObject();
    final List<Record> userAccounts = cpfDataAccessObject.getUserAccountsLikeName(term);

    final ArrayListOfMap<Object> results = new ArrayListOfMap<Object>();
    for (final Record userAccount : userAccounts) {
      final String accountClass = userAccount.getValue(USER_ACCOUNT_CLASS);
      final String accountName = userAccount.getValue(USER_NAME);
      final String username = userAccount.getValue(CONSUMER_KEY);
      final String label = username + " (" + accountName + " - " + accountClass + ")";
      final Map<String, Object> autoComplete = new LinkedHashMap<>();
      autoComplete.put("label", label);
      autoComplete.put("value", username);
      results.add(autoComplete);
    }
    return results;
  }

  public Object createUserGroupMembersList(final HttpServletRequest request,
    final HttpServletResponse response, final String prefix, final String moduleName,
    final String userGroupName, final String groupModuleName)
      throws NoSuchRequestHandlingMethodException {
    final String pageName = prefix + "List";
    if (isDataTableCallback(request)) {
      if (moduleName != null) {
        hasModule(request, moduleName);
      }
      final Record userGroup = getUserGroup(userGroupName);
      if (userGroup != null && (moduleName == null
        || userGroup.getValue(UserGroup.MODULE_NAME).equals(groupModuleName))) {

        final Identifier userGroupId = userGroup.getIdentifier();
        final Condition equal = Q.equal(UserGroupAccountXref.USER_GROUP_ID, userGroupId);
        final Query query = new Query(UserAccount.USER_ACCOUNT, equal);
        query.setFromClause("CPF.CPF_USER_ACCOUNTS T"
          + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_ACCOUNT_ID = X.USER_ACCOUNT_ID");

        return createDataTableMap(request, getRecordStore(), query, pageName);
      }
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      return redirectToTab(UserGroup.USER_GROUP, prefix + "View", pageName);
    }
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}/members/add"
  }, method = RequestMethod.POST)
  public ModelAndView pageModuleAdminUserGroupMemberAdd(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("userGroupName") final String userGroupName,
    @RequestParam("consumerKey") final String consumerKey) throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    return addUserGroupMembership(request, response, moduleName, userGroupName,
      "ADMIN_MODULE_" + moduleName, consumerKey, "moduleAdminView", "moduleAdminGroupList");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}/members/{consumerKey}/delete"
  }, method = RequestMethod.POST)
  public void pageModuleAdminUserGroupMemberDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("userGroupName") final String userGroupName,
    @PathVariable("consumerKey") final String consumerKey,
    @RequestParam("confirm") final Boolean confirm) throws ServletException {
    checkAdminOrModuleAdmin(moduleName);
    removeUserGroupMembership(request, response, moduleName, userGroupName,
      "ADMIN_MODULE_" + moduleName, consumerKey, confirm, "moduleAdminView",
      "moduleAdminGroupList");

  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}/members"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleAdminUserGroupMemberList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("userGroupName") final String userGroupName)
      throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    return createUserGroupMembersList(request, response, "moduleAdminGroup", moduleName,
      userGroupName, "ADMIN_MODULE_" + moduleName);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/members/add"
  }, method = RequestMethod.POST)
  public ModelAndView pageModuleUserGroupMemberAdd(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("userGroupName") final String userGroupName,
    @RequestParam("consumerKey") final String consumerKey) throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    return addUserGroupMembership(request, response, moduleName, userGroupName, moduleName,
      consumerKey, "moduleView", "moduleGroupList");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/members/{consumerKey}/delete"
  }, method = RequestMethod.POST)
  public void pageModuleUserGroupMemberDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("userGroupName") final String userGroupName,
    @PathVariable("consumerKey") final String consumerKey,
    @RequestParam("confirm") final Boolean confirm) throws ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    removeUserGroupMembership(request, response, moduleName, userGroupName, moduleName, consumerKey,
      confirm, "moduleView", "moduleGroupList");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/members"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleUserGroupMemberList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("userGroupName") final String userGroupName)
      throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    return createUserGroupMembersList(request, response, "moduleGroup", moduleName, userGroupName,
      moduleName);
  }

  @RequestMapping(value = {
    "/admin/userAccounts/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageUserAccountAdd(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    final Map<String, Object> defaultValues = new HashMap<>();
    defaultValues.put(USER_ACCOUNT_CLASS, USER_ACCOUNT_CLASS_CPF);
    defaultValues.put(CONSUMER_SECRET, UUID.randomUUID().toString().replaceAll("-", ""));
    defaultValues.put(ACTIVE_IND, "1");
    final TabElementContainer tabs = (TabElementContainer)super.createObjectAddPage(defaultValues,
      null, "preInsert");
    final ElementContainer page = (ElementContainer)tabs.getElements().get(0);
    final List<Element> elements = page.getElements();
    final MenuElement menuView = (MenuElement)elements.get(elements.size() - 1);
    final Menu menu = menuView.getMenu();
    menu.addMenuItem("Generate Consumer Secret", "javascript:generateConsumerSecret()");
    return page;
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/delete"
  }, method = RequestMethod.POST)
  public void pageUserAccountDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("consumerKey") final String consumerKey)
      throws IOException, ServletException {
    checkHasAnyRole(ADMIN);

    final Record userAccount = getUserAccount(consumerKey);
    if (userAccount != null && userAccount.getValue(USER_ACCOUNT_CLASS).equals("CPF")) {

      getDataAccessObject().deleteUserAccount(userAccount);
      redirectPage("list");
    }
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageUserAccountEdit(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable("consumerKey") String consumerKey)
      throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    final Record userAccount = getUserAccount(consumerKey);
    if (USER_ACCOUNT_CLASS_CPF.equals(userAccount.getValue(USER_ACCOUNT_CLASS))) {
      final ElementContainer page = (ElementContainer)super.createObjectEditPage(userAccount, null);
      final List<Element> elements = page.getElements();
      final MenuElement menuView = (MenuElement)elements.get(elements.size() - 1);
      final Menu menu = menuView.getMenu();
      menu.addMenuItem("Generate Consumer Secret", "javascript:generateConsumerSecret()");
      return page;
    } else {
      return super.createObjectEditPage(userAccount, "active");
    }
  }

  @RequestMapping(value = {
    "/admin/userAccounts"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageUserAccountList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    checkHasAnyRole(ADMIN);
    HttpServletUtils.setAttribute("title", "User Accounts");
    return createDataTableHandler(request, "list");
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public ElementContainer pageUserAccountView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("consumerKey") final String consumerKey)
      throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    final Record userAccount = getUserAccount(consumerKey);
    if (userAccount != null) {
      final String userAccountClass = userAccount.getValue(USER_ACCOUNT_CLASS);
      HttpServletUtils.setPathVariable("userAccountClass", userAccountClass);
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, userAccount, null);

      final Map<String, Object> parameters = new HashMap<>();
      parameters.put("serverSide", Boolean.TRUE);
      addTabDataTable(tabs, UserGroup.USER_GROUP, "userAccountList", parameters);
      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/members/add"
  }, method = RequestMethod.POST)
  public ModelAndView pageUserGroupMemberAdd(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("userGroupName") final String userGroupName,
    @RequestParam("consumerKey") final String consumerKey) throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    return addUserGroupMembership(request, response, null, userGroupName, null, consumerKey,
      "groupView", "groupList");
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/members/{consumerKey}/delete"
  }, method = RequestMethod.POST)
  public void pageUserGroupMemberDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("userGroupName") final String userGroupName,
    @PathVariable("consumerKey") final String consumerKey,
    @RequestParam("confirm") final Boolean confirm) throws ServletException {
    checkHasAnyRole(ADMIN);
    removeUserGroupMembership(request, response, null, userGroupName, null, consumerKey, confirm,
      "groupView", "groupList");

  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/members"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageUserGroupMemberList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("userGroupName") final String userGroupName)
      throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin();
    return createUserGroupMembersList(request, response, "group", null, userGroupName, null);
  }

  @Override
  public void postUpdate(final Record userAccount) {
    super.postUpdate(userAccount);
    final String consumerKey = userAccount.getString(CONSUMER_KEY);
    final String oldConsumerKey = HttpServletUtils.getAttribute("oldConsumerKey");
    if (!Equals.equal(consumerKey, oldConsumerKey)) {
      final CpfDataAccessObject dataAccessObject = getDataAccessObject();
      dataAccessObject.updateJobUserId(oldConsumerKey, consumerKey);
    }
  }

  @Override
  public boolean preInsert(final Form form, final Record userAccount) {
    final Field consumerKeyField = form.getField(CONSUMER_KEY);
    String consumerKey = consumerKeyField.getValue();
    consumerKey = consumerKey.toLowerCase();
    consumerKeyField.setValue(consumerKey);
    userAccount.setValue(CONSUMER_KEY, consumerKey);
    userAccount.setValue(USER_NAME, consumerKey);

    if (!consumerKey.matches("[a-z0-9_]+")) {
      consumerKeyField.addValidationError("Can only contain the characters a-z, 0-9, and _.");
      return false;
    } else {
      final Record account = getUserAccount(consumerKey);
      if (account == null) {
        return true;
      } else {
        consumerKeyField.addValidationError("Consumer key is already used.");
        return false;
      }
    }
  }

  @Override
  public boolean preUpdate(final Form form, final Record userAccount) {
    if (USER_ACCOUNT_CLASS_CPF.equals(userAccount.getValue(USER_ACCOUNT_CLASS))) {
      final String oldConsumerKey = userAccount.getString(CONSUMER_KEY);
      final Long userAccountId = userAccount.getLong(USER_ACCOUNT_ID);
      final Field consumerKeyField = form.getField(CONSUMER_KEY);
      String consumerKey = consumerKeyField.getValue();
      consumerKey = consumerKey.toLowerCase();
      consumerKeyField.setValue(consumerKey);
      userAccount.setValue(CONSUMER_KEY, consumerKey);
      userAccount.setValue(USER_NAME, consumerKey);
      if (!consumerKey.matches("[a-z0-9_]+")) {
        consumerKeyField.addValidationError("Can only contain the characters a-z, 0-9, and _.");
        return false;
      } else {
        final Record account = getUserAccount(consumerKey);
        if (account == null || account.getLong(USER_ACCOUNT_ID).equals(userAccountId)) {
          HttpServletUtils.setAttribute("oldConsumerKey", oldConsumerKey);
          HttpServletUtils.setPathVariable("consumerKey", consumerKey);
          return true;
        } else {
          consumerKeyField.addValidationError("Consumer key is already used");
          return false;
        }
      }
    } else {
      return true;
    }
  }

  protected void removeUserGroupMembership(final HttpServletRequest request,
    final HttpServletResponse response, final String moduleName, final String userGroupName,
    final String moduleGroupName, final String consumerKey, final Boolean confirm,
    final String parentPageName, final String tabName) throws ServletException {
    if (moduleName != null) {
      hasModule(request, moduleName);
    }
    final Record userGroup = getUserGroup(userGroupName);
    if (userGroup != null && (moduleGroupName == null
      || userGroup.getValue(UserGroup.MODULE_NAME).equals(moduleGroupName))) {

      final Record userAccount = getUserAccount(consumerKey);
      if (userAccount != null) {
        if (BooleanStringConverter.getBoolean(confirm)) {
          final CpfDataAccessObject dataAccessObject = getDataAccessObject();
          dataAccessObject.deleteUserGroupAccountXref(userGroup, userAccount);
        }
        redirectToTab(UserGroup.USER_GROUP, parentPageName, tabName);
        return;
      }
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

}
