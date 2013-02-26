package ca.bc.gov.open.cpf.plugin.impl.module;

import java.util.EventObject;

public class ModuleEvent extends EventObject {
  private static final long serialVersionUID = -3976150772050177003L;

  public static final String STOP = "STOP";

  public static final String START_FAILED = "START_FAILED";

  public static final String START = "START";

  public static final String SECURITY_CHANGED = "SECURITY_CHANGED";

  private final String action;

  public ModuleEvent(final Object object, final String action) {
    super(object);
    this.action = action;
  }

  public String getAction() {
    return action;
  }

  public Module getModule() {
    return (Module)getSource();
  }

  @Override
  public String toString() {
    return super.toString() + action;
  }
}
