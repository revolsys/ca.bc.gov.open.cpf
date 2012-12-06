package ca.bc.gov.open.cpf.plugin.api.module;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationRegistry;

public interface ModuleLoader {
  BusinessApplicationRegistry getBusinessApplicationRegistry();

  public void refreshModules();

  void setBusinessApplicationRegistry(
    BusinessApplicationRegistry businessApplicationRegistry);
}
