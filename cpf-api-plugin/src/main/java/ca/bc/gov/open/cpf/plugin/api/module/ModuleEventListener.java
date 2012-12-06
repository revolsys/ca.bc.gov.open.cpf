package ca.bc.gov.open.cpf.plugin.api.module;

import java.util.EventListener;

public interface ModuleEventListener extends EventListener {
  void moduleChanged(ModuleEvent event);
}
