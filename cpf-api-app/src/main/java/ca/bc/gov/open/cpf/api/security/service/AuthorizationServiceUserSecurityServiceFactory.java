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
package ca.bc.gov.open.cpf.api.security.service;

import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractCachingSecurityService;
import ca.bc.gov.open.cpf.plugin.impl.security.AbstractSecurityServiceFactory;

public class AuthorizationServiceUserSecurityServiceFactory extends AbstractSecurityServiceFactory {

  private final AuthorizationService authorizationService;

  public AuthorizationServiceUserSecurityServiceFactory(
    final AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Override
  public void close() {
  }

  @Override
  protected AbstractCachingSecurityService newSecurityService(final Module module,
    final String consumerKey) {
    final String userAccountClass = this.authorizationService.getUserClass(consumerKey);
    final String userAccountName = this.authorizationService.getUsername(consumerKey);
    final AbstractCachingSecurityService securityService = new AuthorizationServiceUserSecurityService(
      this.authorizationService, module, consumerKey, userAccountClass, userAccountName);
    return securityService;
  }
}
