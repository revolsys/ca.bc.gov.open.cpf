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

import java.util.Map;

import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractCachingSecurityService;

public class AuthorizationServiceUserSecurityService extends AbstractCachingSecurityService {

  private AuthorizationService authorizationService;

  public AuthorizationServiceUserSecurityService(final AuthorizationService authorizationService,
    final Module module, final String consumerKey, final String userClass, final String username) {
    super(module, consumerKey, userClass, username);
    this.authorizationService = authorizationService;

  }

  @Override
  public void close() {
    super.close();
    this.authorizationService = null;
  }

  @Override
  protected Boolean loadActionPermission(final String actionName) {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    if (this.authorizationService.canPerformAction(moduleName, consumerKey, actionName)) {
      return true;
    } else {
      return super.loadActionPermission(actionName);
    }
  }

  @Override
  protected Boolean loadGroupPermission(final String groupName) {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    if (this.authorizationService.isInGroup(moduleName, consumerKey, groupName)) {
      return true;
    } else {
      return super.loadGroupPermission(groupName);
    }
  }

  @Override
  protected Boolean loadResourceAccessPermission(final String resourceClass,
    final String resourceId, final String actionName) {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    if (this.authorizationService.canAccessResource(moduleName, consumerKey, resourceClass,
      resourceId, actionName)) {
      return true;
    } else {
      return super.loadResourceAccessPermission(resourceClass, resourceId, actionName);
    }
  }

  @Override
  protected Map<String, Object> loadUserAttributes() {
    final String consumerKey = getConsumerKey();
    final String moduleName = getModuleName();
    return this.authorizationService.getUserAttributes(moduleName, consumerKey);
  }
}
