package ca.bc.gov.open.cpf.plugin.impl.module;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;

public interface ModuleLoader {
  BusinessApplicationRegistry getBusinessApplicationRegistry();

  public void refreshModules();

  void setBusinessApplicationRegistry(
    BusinessApplicationRegistry businessApplicationRegistry);
}
