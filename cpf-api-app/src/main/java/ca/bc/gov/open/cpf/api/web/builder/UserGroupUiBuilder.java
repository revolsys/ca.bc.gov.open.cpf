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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.domain.UserGroup;
import ca.bc.gov.open.cpf.api.domain.UserGroupAccountXref;
import ca.bc.gov.open.cpf.api.domain.UserGroupPermission;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.format.xml.XmlWriter;
import com.revolsys.record.Record;
import com.revolsys.ui.html.fields.Field;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class UserGroupUiBuilder extends CpfUiBuilder {

  private static final List<String> GLOBAL_GROUP_NAMES = Arrays.asList("ADMIN", "USER_TYPE",
    "GLOBAL", "WORKER");

  public UserGroupUiBuilder() {
    super("userGroup", UserGroup.USER_GROUP, UserGroup.USER_GROUP_NAME, "User Group", "User Groups");
    setIdParameterName("userGroupName");
  }

  public void adminUserGroupLink(final XmlWriter out, final Object object) {
    final Record userGroup = (Record)object;

    final Map<String, String> parameterNames = new HashMap<String, String>();
    parameterNames.put("userGroupName", "userGroupName");

    final Map<String, Object> linkObject = new HashMap<String, Object>();
    final Object userGroupName = userGroup.getValue(UserGroup.USER_GROUP_NAME);
    linkObject.put(UserGroup.USER_GROUP_NAME, userGroupName);
    linkObject.put("userGroupName", userGroupName);

    final String moduleName = userGroup.getValue(UserGroup.MODULE_NAME);
    String pageName;
    if (GLOBAL_GROUP_NAMES.contains(moduleName)) {
      pageName = "groupView";
    } else if (moduleName.startsWith("ADMIN_MODULE_")) {
      pageName = "moduleAdminView";
      linkObject.put("moduleName", moduleName.substring(13));
      parameterNames.put("moduleName", "moduleName");
    } else {
      pageName = "moduleView";
      linkObject.put("moduleName", moduleName);
      parameterNames.put("moduleName", "moduleName");
    }

    serializeLink(out, linkObject, UserGroup.USER_GROUP_NAME, pageName, parameterNames);
  }

  public Element createUserGroupView(final HttpServletRequest request,
    final HttpServletResponse response, final String prefix, final String membersPrefix,
    final String moduleName, final String userGroupName, final List<String> moduleNames)
    throws NoSuchRequestHandlingMethodException {
    if (moduleName != null) {
      hasModule(request, moduleName);
    }

    final Record userGroup = getUserGroup(userGroupName);
    if (userGroup != null
      && (moduleName == null || moduleNames.contains(userGroup.getValue(UserGroup.MODULE_NAME)))) {
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, userGroup, prefix);

      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("serverSide", Boolean.TRUE);

      if (!userGroup.getValue(UserGroup.MODULE_NAME).equals("USER_TYPE")) {
        final UserAccountUiBuilder userAccountUiBuilder = getBuilder(UserAccount.USER_ACCOUNT);
        userAccountUiBuilder.addMembersDataTable(tabs, membersPrefix);
      }

      addTabDataTable(tabs, UserGroupPermission.USER_GROUP_PERMISSION, prefix + "List", parameters);

      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  public List<String> getUserGroupModuleNames(final String moduleName) {
    return Arrays.asList(moduleName, "USER_TYPE", "GLOBAL");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleAdminList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName)
    throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();

    final Map<String, Object> filter = new HashMap<String, Object>();
    filter.put(UserGroup.MODULE_NAME, "ADMIN_MODULE_" + moduleName);

    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response, "moduleAdminList", Module.class,
      "view", parameters);

  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Element pageModuleAdminView(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable("userGroupName") String userGroupName,
    @PathVariable("moduleName") final String moduleName) throws IOException, ServletException {
    checkAdminOrModuleAdmin(moduleName);
    return createUserGroupView(request, response, "moduleAdmin", "moduleAdminGroup", moduleName,
      userGroupName, Arrays.asList("ADMIN_MODULE_" + moduleName));

  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageModuleUserGroupAdd(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName)
    throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put(UserGroup.ACTIVE_IND, 1);
    parameters.put(UserGroup.MODULE_NAME, moduleName);
    parameters.put(UserGroup.USER_GROUP_NAME, moduleName + "_");
    return createObjectAddPage(parameters, "module", "preInsert");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/delete"
  }, method = RequestMethod.POST)
  public void pageModuleUserGroupDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("userGroupName") final String userGroupName) throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Record userGroup = getUserGroup(userGroupName);
    if (userGroup != null && userGroup.getValue(UserGroup.MODULE_NAME).equals(moduleName)) {
      final CpfDataAccessObject dataAccessObject = getDataAccessObject();
      dataAccessObject.deleteUserGroup(userGroup);
      redirectPage("moduleList");
    }
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageModuleUserGroupEdit(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable("userGroupName") String userGroupName,
    @PathVariable("moduleName") final String moduleName) throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("moduleName", moduleName);

    final Record userGroup = getUserGroup(userGroupName);
    if (userGroup != null && userGroup.getValue(UserGroup.MODULE_NAME).equals(moduleName)) {
      return createObjectEditPage(userGroup, "module");
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageModuleUserGroupList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName)
    throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();

    final Map<String, Object> filter = new HashMap<String, Object>();
    final List<String> moduleNames = getUserGroupModuleNames(moduleName);
    filter.put("MODULE_NAME", moduleNames);

    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response, "moduleList", Module.class, "view",
      parameters);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Element pageModuleUserGroupView(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable("userGroupName") String userGroupName,
    @PathVariable("moduleName") final String moduleName) throws IOException, ServletException {
    checkAdminOrAnyModuleAdmin(moduleName);
    return createUserGroupView(request, response, "module", "moduleGroup", moduleName,
      userGroupName, getUserGroupModuleNames(moduleName));
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/userGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageUserAccountList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("consumerKey") final String consumerKey)
    throws IOException, NoSuchRequestHandlingMethodException {
    checkHasAnyRole(ADMIN);
    final Record userAccount = getUserAccount(consumerKey);
    if (userAccount != null) {

      final Map<String, Object> parameters = new HashMap<String, Object>();

      parameters.put("fromClause", "CPF.CPF_USER_GROUPS T"
        + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_GROUP_ID = X.USER_GROUP_ID");

      final Map<String, Object> filter = new HashMap<>();
      filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccount.getIdentifier());

      parameters.put("filter", filter);

      return createDataTableHandlerOrRedirect(request, response, "userAccountList",
        UserAccount.USER_ACCOUNT, "view", parameters);

    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/userGroups/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageUserGroupAdd(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException, ServletException {
    final Map<String, Object> defaultValues = new HashMap<String, Object>();
    defaultValues.put(UserGroup.ACTIVE_IND, 1);
    defaultValues.put(UserGroup.MODULE_NAME, "GLOBAL");
    defaultValues.put(UserGroup.USER_GROUP_NAME, "GLOBAL_");
    return super.createObjectAddPage(defaultValues, "group", "preInsert");
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/delete"
  }, method = RequestMethod.POST)
  public void pageUserGroupDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("userGroupName") final String userGroupName)
    throws IOException, ServletException {
    checkHasAnyRole(ADMIN);

    final Record userGroup = getUserGroup(userGroupName);
    if (userGroup != null && userGroup.getValue(UserGroup.MODULE_NAME).equals("GLOBAL")) {
      final CpfDataAccessObject dataAccessObject = getDataAccessObject();
      dataAccessObject.deleteUserGroup(userGroup);
      redirectPage("groupList");
    }
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  public Element pageUserGroupEdit(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable("userGroupName") String userGroupName)
    throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    final Record userGroup = getUserGroup(userGroupName);
    return super.createObjectEditPage(userGroup, "group");
  }

  @RequestMapping(value = {
    "/admin/userGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object pageUserGroupList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    checkHasAnyRole(ADMIN);
    HttpServletUtils.setAttribute("title", "User Groups");
    return createDataTableHandler(request, "groupList");
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/userGroups/{userGroupName}/delete"
  }, method = RequestMethod.POST)
  public void pageUserGroupMemberDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("userGroupName") final String userGroupName,
    @PathVariable("consumerKey") final String consumerKey,
    @RequestParam("confirm") final Boolean confirm) throws ServletException {
    checkHasAnyRole(ADMIN);
    final Record userGroup = getUserGroup(userGroupName);
    if (userGroup != null) {

      final Record userAccount = getUserAccount(consumerKey);
      if (userAccount != null) {
        if (BooleanStringConverter.getBoolean(confirm)) {
          final CpfDataAccessObject dataAccessObject = getDataAccessObject();
          dataAccessObject.deleteUserGroupAccountXref(userGroup, userAccount);
        }
        redirectToTab(UserAccount.USER_ACCOUNT, "view", "userAccountList");
        return;
      }
    }

  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Element pageUserGroupView(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable("userGroupName") String userGroupName)
    throws IOException, ServletException {
    checkHasAnyRole(ADMIN);
    return createUserGroupView(request, response, "group", "group", null, userGroupName, null);
  }

  @Override
  public boolean preInsert(final Form form, final Record userGroup) {
    final Field nameField = form.getField(UserGroup.USER_GROUP_NAME);
    String groupName = nameField.getValue();
    groupName = groupName.toUpperCase();
    nameField.setValue(groupName);
    userGroup.setValue(UserGroup.USER_GROUP_NAME, groupName);

    final Record group = getUserGroup(groupName);
    if (group == null) {
      final String moduleName = userGroup.getValue(UserGroup.MODULE_NAME);
      if (!groupName.startsWith(moduleName + "_")) {
        nameField.addValidationError("Must start with " + moduleName + "_");
        return false;
      } else if (!groupName.matches("[A-Z0-9_]+")) {
        nameField.addValidationError("Can only contain the characters A-Z, 0-9, and _.");
        return false;
      } else {
        final int groupNameLength = groupName.length();
        final int minLength = moduleName.length() + 2;
        if (groupNameLength < minLength) {

          nameField.addValidationError("Group name must have a suffix");
          return false;
        }
      }
      return true;
    } else {
      nameField.addValidationError("Group name is already used");
      return false;
    }
  }

  @Override
  public boolean preUpdate(final Form form, final Record userGroup) {
    final Long userGroupId = userGroup.getLong(UserGroup.USER_GROUP_ID);
    final Field nameField = form.getField(UserGroup.USER_GROUP_NAME);
    String groupName = nameField.getValue();
    groupName = groupName.toUpperCase();
    nameField.setValue(groupName);
    userGroup.setValue(UserGroup.USER_GROUP_NAME, groupName);

    final Record group = getUserGroup(groupName);
    if (group == null || group.getLong(UserGroup.USER_GROUP_ID).equals(userGroupId)) {
      final String moduleName = userGroup.getValue(UserGroup.MODULE_NAME);
      if (!groupName.startsWith(moduleName + "_")) {
        nameField.addValidationError("Group name must start with " + moduleName + "_");
        return false;
      } else if (!groupName.matches("[A-Z0-9_]+")) {
        nameField.addValidationError("Can only contain the characters A-Z, 0-9, and _.");
        return false;
      } else {
        final int groupNameLength = groupName.length();
        final int minLength = moduleName.length() + 2;
        if (groupNameLength < minLength) {

          nameField.addValidationError("Group name must have a suffix");
          return false;
        }
      }
      HttpServletUtils.setPathVariable("userGroupName", groupName);
      return true;
    } else {
      nameField.addValidationError("Group name is already used");
      return false;
    }
  }

  public void userGroupName(final XmlWriter out, final Object object) {
    final Record record = (Record)object;
    final long userGroupId = record.getLong(UserGroup.USER_GROUP_ID);

    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    final Record userGroup = dataAccessObject.getUserGroup(userGroupId);

    final String name = userGroup.getValue(UserGroup.USER_GROUP_NAME);
    out.text(name);
  }
}
