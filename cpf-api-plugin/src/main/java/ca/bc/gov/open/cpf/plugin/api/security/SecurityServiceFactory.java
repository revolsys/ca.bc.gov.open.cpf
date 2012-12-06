package ca.bc.gov.open.cpf.plugin.api.security;

import ca.bc.gov.open.cpf.plugin.api.module.Module;
import ca.bc.gov.open.cpf.plugin.api.module.ModuleEventListener;

public interface SecurityServiceFactory extends ModuleEventListener {
  void close();

  SecurityService getSecurityService(Module module, String consumerKey);
}
