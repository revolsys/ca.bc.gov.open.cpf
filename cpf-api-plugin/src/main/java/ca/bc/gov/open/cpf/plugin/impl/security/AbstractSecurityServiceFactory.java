package ca.bc.gov.open.cpf.plugin.impl.security;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEvent;

public abstract class AbstractSecurityServiceFactory implements
  SecurityServiceFactory {

  private final Map<Module, Map<String, AbstractCachingSecurityService>> securityServicesByModuleAndUser = new WeakHashMap<Module, Map<String, AbstractCachingSecurityService>>();

  private final Map<String, Long> securityServiceAges = new HashMap<String, Long>();

  private final int maxAge = 5 * 60 * 1000;

  protected abstract AbstractCachingSecurityService createSecurityService(
    Module module, String consumerKey);

  @Override
  public SecurityService getSecurityService(final Module module,
    final String consumerKey) {
    final String key = module.getName() + ":" + consumerKey;
    synchronized (securityServicesByModuleAndUser) {
      Map<String, AbstractCachingSecurityService> securityServicesByUser = securityServicesByModuleAndUser.get(module);
      if (securityServicesByUser == null) {
        securityServicesByUser = new HashMap<String, AbstractCachingSecurityService>();
        securityServicesByModuleAndUser.put(module, securityServicesByUser);
      }
      AbstractCachingSecurityService securityService = securityServicesByUser.get(consumerKey);
      if (securityService != null) {
        final Long age = securityServiceAges.get(key);
        if (age + maxAge < System.currentTimeMillis()) {
          securityService.close();
          securityService = null;
        }
      }
      if (securityService == null) {
        securityService = createSecurityService(module, consumerKey);
        securityServicesByUser.put(consumerKey, securityService);
        securityServiceAges.put(key, System.currentTimeMillis());
      }
      return securityService;
    }
  }

  @Override
  public void moduleChanged(final ModuleEvent event) {
    final String action = event.getAction();
    if (action.equals(ModuleEvent.STOP)
      || action.equals(ModuleEvent.SECURITY_CHANGED)) {
      synchronized (securityServicesByModuleAndUser) {
        final Module module = event.getModule();
        final Map<String, AbstractCachingSecurityService> securityServicesByUser = securityServicesByModuleAndUser.remove(module);
        if (securityServicesByUser != null) {
          for (final AbstractCachingSecurityService securityService : securityServicesByUser.values()) {
            securityService.close();
          }
        }
      }
    }
  }
}
