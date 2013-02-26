package ca.bc.gov.open.cpf.plugin.impl.module;

import java.util.EventListener;

public interface ModuleEventListener extends EventListener {
  void moduleChanged(ModuleEvent event);
}
