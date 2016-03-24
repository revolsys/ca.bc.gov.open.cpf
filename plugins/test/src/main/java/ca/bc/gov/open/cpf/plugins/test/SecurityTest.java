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
package ca.bc.gov.open.cpf.plugins.test;

import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;

@BusinessApplicationPlugin
public class SecurityTest {

  private String resourceClass;

  private String resourceId;

  private String actionName;

  private String groupName;

  private Map<String, Object> attributes;

  private SecurityService securityService;

  private boolean canAccessResource;

  private boolean canPerformAction;

  private boolean inGroup;

  private String userClass;

  private String username;

  private String consumerKey;

  public void execute() {
    this.canAccessResource = this.securityService.canAccessResource(this.resourceClass,
      this.resourceId, this.actionName);
    this.canPerformAction = this.securityService.canPerformAction(this.actionName);
    this.attributes = this.securityService.getUserAttributes();
    this.inGroup = this.securityService.isInGroup(this.groupName);
    this.userClass = this.securityService.getUserClass();
    this.username = this.securityService.getUsername();
    this.consumerKey = this.securityService.getConsumerKey();
  }

  @ResultAttribute(index = 7)
  public String getActionName() {
    return this.actionName;
  }

  @ResultAttribute(index = 8)
  public String getAttributes() {
    return this.attributes.toString();
  }

  @ResultAttribute(index = 3)
  public String getConsumerKey() {
    return this.consumerKey;
  }

  @ResultAttribute(index = 4)
  public String getGroupName() {
    return this.groupName;
  }

  @ResultAttribute(index = 5)
  public String getResourceClass() {
    return this.resourceClass;
  }

  @ResultAttribute(index = 6)
  public String getResourceId() {
    return this.resourceId;
  }

  @ResultAttribute(index = 1)
  public String getUserClass() {
    return this.userClass;
  }

  @ResultAttribute(index = 2)
  public String getUsername() {
    return this.username;
  }

  @ResultAttribute(index = 11)
  public boolean isCanAccessResource() {
    return this.canAccessResource;
  }

  @ResultAttribute(index = 10)
  public boolean isCanPerformAction() {
    return this.canPerformAction;
  }

  @ResultAttribute(index = 9)
  public boolean isInGroup() {
    return this.inGroup;
  }

  @JobParameter(index = 4)
  @RequestParameter
  @DefaultValue("view")
  public void setActionName(final String actionName) {
    this.actionName = actionName;
  }

  @JobParameter(index = 1)
  @RequestParameter
  @DefaultValue("TEST_USER")
  public void setGroupName(final String groupName) {
    this.groupName = groupName;
  }

  @JobParameter(index = 2)
  @RequestParameter
  @DefaultValue("test")
  public void setResourceClass(final String resourceClass) {
    this.resourceClass = resourceClass;
  }

  @JobParameter(index = 3)
  @RequestParameter
  @DefaultValue("1")
  public void setResourceId(final String resoruceId) {
    this.resourceId = resoruceId;
  }

  public void setSecurityService(final SecurityService securityService) {
    this.securityService = securityService;
  }
}
