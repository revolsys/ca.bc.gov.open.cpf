package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
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
import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;

import com.revolsys.collection.ArrayListOfMap;
import com.revolsys.collection.ResultPager;
import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.ui.html.decorator.FieldLabelDecorator;
import com.revolsys.ui.html.decorator.TableBody;
import com.revolsys.ui.html.fields.AutoCompleteTextField;
import com.revolsys.ui.html.fields.Field;
import com.revolsys.ui.html.fields.FieldWithSubmitButton;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.html.view.TableRow;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class UserAccountUiBuilder extends CpfUiBuilder implements
  CpfMethodSecurityExpressions {

  public UserAccountUiBuilder() {
    super("userAccount", UserAccount.USER_ACCOUNT, UserAccount.CONSUMER_KEY,
      "User Account", "User Accounts");
    setIdParameterName("consumerKey");
  }

  public void addMembersDataTable(final TabElementContainer container,
    final String prefix) throws NoSuchRequestHandlingMethodException {
    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("serverSide", Boolean.TRUE);
    parameters.put("deferLoading", 0);
    parameters.put("tabbed", true);

    final String pageName = prefix + "List";
    final HttpServletRequest request = getRequest();
    final ElementContainer element = createDataTable(request, pageName,
      parameters);
    if (element != null) {

      final String addUrl = getPageUrl(prefix + "MemberAdd");
      final Form form = new Form(prefix + "MemberAdd", addUrl);

      final ElementContainer fieldContainer = new ElementContainer(
        new TableBody());
      form.add(fieldContainer);

      final String searchUrl = getPageUrl("searchLikeName");
      final AutoCompleteTextField consumerKeyField = new AutoCompleteTextField(
        "consumerKey", searchUrl, true);

      final FieldWithSubmitButton submitField = new FieldWithSubmitButton(
        consumerKeyField, "add", "Add");
      final FieldLabelDecorator usernameLabel = new FieldLabelDecorator(
        "Username");
      usernameLabel.setInstructions("Search for a user by typing 3 or more consecutive characters from any part of the Consumer Key or User Account Name. The matching users will be displayed in a pop-up list. Select the required user and click the Add button.");
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
    final HttpServletResponse response, final String moduleName,
    final String userGroupName, final String moduleGroupName,
    final String consumerKey, final String parentPageName, final String tabName)
    throws NoSuchRequestHandlingMethodException {
    if (StringUtils.hasText(consumerKey)) {
      if (moduleName != null) {
        hasModule(request, moduleName);
      }
      final DataObject userGroup = getUserGroup(userGroupName);
      if (userGroup != null
        && (moduleGroupName == null || userGroup.getValue(UserGroup.MODULE_NAME)
          .equals(moduleGroupName))) {

        final DataObject userAccount = getUserAccount(consumerKey);
        if (userAccount == null) {
          final ModelMap model = new ModelMap();
          model.put("body", "/WEB-INF/jsp/admin/groupMemberNotFound.jsp");
          model.put("consumerKey", consumerKey);
          return new ModelAndView("/jsp/template/page", model);
        } else {
          final CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
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
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY_OR_ANY_MODULE_ADMIN)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public ArrayListOfMap<Object> autocompleteUserName(
    final HttpServletRequest request, final HttpServletResponse response,
    @RequestParam final String term) throws IOException, ServletException {
    request.setAttribute(IoConstants.JSON_LIST_ROOT_PROPERTY, Boolean.TRUE);
    final CpfDataAccessObject cpfDataAccessObject = getCpfDataAccessObject();
    final List<DataObject> userAccounts = cpfDataAccessObject.getUserAccountsLikeName(term);

    final ArrayListOfMap<Object> results = new ArrayListOfMap<Object>();
    for (final DataObject userAccount : userAccounts) {
      final String accountClass = userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS);
      final String accountName = userAccount.getValue(UserAccount.USER_NAME);
      final String username = userAccount.getValue(UserAccount.CONSUMER_KEY);
      final String label = username + " (" + accountName + " - " + accountClass
        + ")";
      final Map<String, Object> autoComplete = new LinkedHashMap<String, Object>();
      autoComplete.put("label", label);
      autoComplete.put("value", username);
      results.add(autoComplete);
    }
    return results;
  }

  public Object createUserGroupMembersList(final HttpServletRequest request,
    final HttpServletResponse response, final String prefix,
    final String moduleName, final String userGroupName,
    final String groupModuleName) throws NoSuchRequestHandlingMethodException {
    final String pageName = prefix + "List";
    if (isDataTableCallback(request)) {
      if (moduleName != null) {
        hasModule(request, moduleName);
      }
      final DataObject userGroup = getUserGroup(userGroupName);
      if (userGroup != null
        && (moduleName == null || userGroup.getValue(UserGroup.MODULE_NAME)
          .equals(groupModuleName))) {

        final CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
        final ResultPager<DataObject> userAccounts = dataAccessObject.getUserAccountsForUserGroup(userGroup);
        return createDataTableMap(request, userAccounts, pageName);
      }
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      return redirectToTab(UserGroup.USER_GROUP, prefix + "View", pageName);
    }
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}/members/add"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ModelAndView pageModuleAdminUserGroupMemberAdd(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String userGroupName,
    @RequestParam final String consumerKey) throws IOException,
    ServletException {
    return addUserGroupMembership(request, response, moduleName, userGroupName,
      "ADMIN_MODULE_" + moduleName, consumerKey, "moduleAdminView",
      "moduleAdminGroupList");
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}/members/{consumerKey}/delete"
      }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void pageModuleAdminUserGroupMemberDelete(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String userGroupName,
    @PathVariable final String consumerKey, @RequestParam final Boolean confirm)
    throws ServletException {
    removeUserGroupMembership(request, response, moduleName, userGroupName,
      "ADMIN_MODULE_" + moduleName, consumerKey, confirm, "moduleAdminView",
      "moduleAdminGroupList");

  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}/members"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageModuleAdminUserGroupMemberList(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String userGroupName) throws IOException,
    ServletException {
    return createUserGroupMembersList(request, response, "moduleAdminGroup",
      moduleName, userGroupName, "ADMIN_MODULE_" + moduleName);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/members/add"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ModelAndView pageModuleUserGroupMemberAdd(
    final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable final String moduleName,
    @PathVariable final String userGroupName,
    @RequestParam final String consumerKey) throws IOException,
    ServletException {
    return addUserGroupMembership(request, response, moduleName, userGroupName,
      moduleName, consumerKey, "moduleView", "moduleGroupList");
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/userGroups/{userGroupName}/members/{consumerKey}/delete"
      }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void pageModuleUserGroupMemberDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String userGroupName,
    @PathVariable final String consumerKey, @RequestParam final Boolean confirm)
    throws ServletException {
    removeUserGroupMembership(request, response, moduleName, userGroupName,
      moduleName, consumerKey, confirm, "moduleView", "moduleGroupList");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/members"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageModuleUserGroupMemberList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String userGroupName) throws IOException,
    ServletException {
    return createUserGroupMembersList(request, response, "moduleGroup",
      moduleName, userGroupName, moduleName);
  }

  @RequestMapping(value = {
    "/admin/userAccounts/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element pageUserAccountAdd(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException, ServletException {
    final Map<String, Object> defaultValues = new HashMap<String, Object>();
    defaultValues.put("USER_ACCOUNT_CLASS", "CPF");
    defaultValues.put("ACTIVE_IND", "1");
    return super.createObjectAddPage(defaultValues, null, "preInsert");
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/delete"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void pageUserAccountDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String consumerKey)
    throws IOException, ServletException {

    final DataObject userAccount = getUserAccount(consumerKey);
    if (userAccount != null
      && userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS).equals("CPF")) {

      getCpfDataAccessObject().deleteUserAccount(userAccount);
      redirectPage("list");
    }
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element pageUserAccountEdit(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String consumerKey)
    throws IOException, ServletException {
    final DataObject userAccount = getUserAccount(consumerKey);
    return super.createObjectEditPage(userAccount, null);
  }

  @RequestMapping(value = {
    "/admin/userAccounts"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageUserAccountList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    return createDataTableHandler(request, "list");
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public ElementContainer pageUserAccountView(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String consumerKey)
    throws IOException, ServletException {
    final DataObject userAccount = getUserAccount(consumerKey);
    if (userAccount != null) {
      final String userAccountClass = userAccount.getValue(UserAccount.USER_ACCOUNT_CLASS);
      HttpServletUtils.setPathVariable("userAccountClass", userAccountClass);
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, userAccount, null);

      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("serverSide", Boolean.TRUE);
      addTabDataTable(tabs, UserGroup.USER_GROUP, "userAccountList", parameters);
      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/members/add"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ModelAndView pageUserGroupMemberAdd(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable final String userGroupName,
    @RequestParam final String consumerKey) throws IOException,
    ServletException {
    return addUserGroupMembership(request, response, null, userGroupName, null,
      consumerKey, "groupView", "groupList");
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/members/{consumerKey}/delete"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void pageUserGroupMemberDelete(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable final String userGroupName,
    @PathVariable final String consumerKey, @RequestParam final Boolean confirm)
    throws ServletException {
    removeUserGroupMembership(request, response, null, userGroupName, null,
      consumerKey, confirm, "groupView", "groupList");

  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/members"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageUserGroupMemberList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String userGroupName)
    throws IOException, ServletException {
    return createUserGroupMembersList(request, response, "group", null,
      userGroupName, null);
  }

  @Override
  public boolean preInsert(final Form form, final DataObject userAccount) {
    final Field consumerKeyField = form.getField(UserAccount.CONSUMER_KEY);
    String consumerKey = consumerKeyField.getValue();
    consumerKey = consumerKey.toLowerCase();
    consumerKeyField.setValue(consumerKey);
    userAccount.setValue(UserAccount.CONSUMER_KEY, consumerKey);
    userAccount.setValue(UserAccount.USER_NAME, consumerKey);

    final DataObject account = getUserAccount(consumerKey);
    if (account == null) {
      return true;
    } else {
      consumerKeyField.addValidationError("Consumer key is already used");
      return false;
    }
  }

  @Override
  public boolean preUpdate(final Form form, final DataObject userAccount) {
    final Long userAccountId = DataObjectUtil.getLong(userAccount,
      UserAccount.USER_ACCOUNT_ID);
    final Field consumerKeyField = form.getField(UserAccount.CONSUMER_KEY);
    String consumerKey = consumerKeyField.getValue();
    consumerKey = consumerKey.toLowerCase();
    consumerKeyField.setValue(consumerKey);
    userAccount.setValue(UserAccount.CONSUMER_KEY, consumerKey);
    userAccount.setValue(UserAccount.USER_NAME, consumerKey);

    final DataObject account = getUserAccount(consumerKey);
    if (account == null
      || DataObjectUtil.getLong(account, UserAccount.USER_ACCOUNT_ID) == userAccountId) {
      HttpServletUtils.setPathVariable("consumerKey", consumerKey);
      return true;
    } else {
      consumerKeyField.addValidationError("Consumer key is already used");
      return false;
    }
  }

  protected void removeUserGroupMembership(final HttpServletRequest request,
    final HttpServletResponse response, final String moduleName,
    final String userGroupName, final String moduleGroupName,
    final String consumerKey, final Boolean confirm,
    final String parentPageName, final String tabName) throws ServletException {
    if (moduleName != null) {
      hasModule(request, moduleName);
    }
    final DataObject userGroup = getUserGroup(userGroupName);
    if (userGroup != null
      && (moduleGroupName == null || userGroup.getValue(UserGroup.MODULE_NAME)
        .equals(moduleGroupName))) {

      final DataObject userAccount = getUserAccount(consumerKey);
      if (userAccount != null) {
        if (BooleanStringConverter.getBoolean(confirm)) {
          final CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
          dataAccessObject.deleteUserGroupAccountXref(userGroup, userAccount);
        }
        redirectToTab(UserGroup.USER_GROUP, parentPageName, tabName);
        return;
      }
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

}
