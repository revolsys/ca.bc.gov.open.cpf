package ca.bc.gov.open.cpf.plugin.impl.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

public class ModuleEvent extends EventObject {
  private static final long serialVersionUID = -3976150772050177003L;

  public static final String STOP = "STOP";

  public static final String START_FAILED = "START_FAILED";

  public static final String START = "START";

  public static final String SECURITY_CHANGED = "SECURITY_CHANGED";

  private final String action;

  private List<String> businessApplicationNames = Collections.emptyList();

  public ModuleEvent(final Object object, final String action) {
    super(object);
    this.action = action;
  }

  public String getAction() {
    return this.action;
  }

  public List<String> getBusinessApplicationNames() {
    return this.businessApplicationNames;
  }

  public Module getModule() {
    return (Module)getSource();
  }

  public void setBusinessApplicationNames(
    final List<String> businessApplicationNames) {
    this.businessApplicationNames = new ArrayList<>(businessApplicationNames);
  }

  @Override
  public String toString() {
    return super.toString() + this.action;
  }
}
