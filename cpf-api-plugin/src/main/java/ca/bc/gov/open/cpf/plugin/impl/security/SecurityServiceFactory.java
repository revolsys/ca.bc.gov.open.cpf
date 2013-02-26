package ca.bc.gov.open.cpf.plugin.impl.security;

import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleEventListener;

public interface SecurityServiceFactory extends ModuleEventListener {
  void close();

  SecurityService getSecurityService(Module module, String consumerKey);
}
