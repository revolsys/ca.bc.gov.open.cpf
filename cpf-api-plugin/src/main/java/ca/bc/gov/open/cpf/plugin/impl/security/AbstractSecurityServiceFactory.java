package ca.bc.gov.open.cpf.plugin.impl.security;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEvent;

public abstract class AbstractSecurityServiceFactory implements
  SecurityServiceFactory {

  private final Map<Module, Map<String, SecurityService>> securityServicesByModuleAndUser = new WeakHashMap<Module, Map<String, SecurityService>>();

  private final Map<String, Long> securityServiceAges = new HashMap<String, Long>();

  private final int maxAge = 15 * 60 * 1000;

  protected abstract SecurityService createSecurityService(
    Module module,
    String consumerKey);

  @Override
  public SecurityService getSecurityService(
    final Module module,
    final String consumerKey) {
    final String key = module.getName() + ":" + consumerKey;
    synchronized (securityServicesByModuleAndUser) {
      Map<String, SecurityService> securityServicesByUser = securityServicesByModuleAndUser.get(module);
      if (securityServicesByUser == null) {
        securityServicesByUser = new HashMap<String, SecurityService>();
        securityServicesByModuleAndUser.put(module, securityServicesByUser);
      }
      SecurityService securityService = securityServicesByUser.get(consumerKey);
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
    String action = event.getAction();
    if (action.equals(ModuleEvent.STOP)
      || action.equals(ModuleEvent.SECURITY_CHANGED)) {
      synchronized (securityServicesByModuleAndUser) {
        final Module module = event.getModule();
        final Map<String, SecurityService> securityServicesByUser = securityServicesByModuleAndUser.remove(module);
        if (securityServicesByUser != null) {
          for (final SecurityService securityService : securityServicesByUser.values()) {
            securityService.close();
          }
        }
      }
    }
  }
}
