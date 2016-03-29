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
package ca.bc.gov.open.cpf.plugin.impl.security;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEvent;

public abstract class AbstractSecurityServiceFactory implements SecurityServiceFactory {
  private final Map<Module, Map<String, AbstractCachingSecurityService>> securityServicesByModuleAndUser = new WeakHashMap<>();

  private final Map<String, Long> securityServiceAges = new HashMap<String, Long>();

  private final int maxAge = 5 * 60 * 1000;

  @Override
  public SecurityService getSecurityService(final Module module, final String consumerKey) {
    final String key = module.getName() + ":" + consumerKey;
    synchronized (this.securityServicesByModuleAndUser) {
      Map<String, AbstractCachingSecurityService> securityServicesByUser = this.securityServicesByModuleAndUser
        .get(module);
      if (securityServicesByUser == null) {
        securityServicesByUser = new HashMap<String, AbstractCachingSecurityService>();
        this.securityServicesByModuleAndUser.put(module, securityServicesByUser);
      }
      AbstractCachingSecurityService securityService = securityServicesByUser.get(consumerKey);
      if (securityService != null) {
        final Long age = this.securityServiceAges.get(key);
        if (age + this.maxAge < System.currentTimeMillis()) {
          securityService.close();
          securityService = null;
        }
      }
      if (securityService == null) {
        securityService = newSecurityService(module, consumerKey);
        securityServicesByUser.put(consumerKey, securityService);
        this.securityServiceAges.put(key, System.currentTimeMillis());
      }
      return securityService;
    }
  }

  @Override
  public void moduleChanged(final ModuleEvent event) {
    final String action = event.getAction();
    if (action.equals(ModuleEvent.STOP) || action.equals(ModuleEvent.SECURITY_CHANGED)) {
      synchronized (this.securityServicesByModuleAndUser) {
        final Module module = event.getModule();
        final Map<String, AbstractCachingSecurityService> securityServicesByUser = this.securityServicesByModuleAndUser
          .remove(module);
        if (securityServicesByUser != null) {
          for (final AbstractCachingSecurityService securityService : securityServicesByUser
            .values()) {
            securityService.close();
          }
        }
      }
    }
  }

  protected abstract AbstractCachingSecurityService newSecurityService(Module module,
    String consumerKey);
}
