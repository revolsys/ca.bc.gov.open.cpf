package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Date;

public class WorkerModuleState {

  private String name;

  private boolean enabled;

  private String moduleError;

  private String status = "Initializing";

  private long startedTime;

  public WorkerModuleState(final String name) {
    this.name = name;
  }

  public String getModuleError() {
    return moduleError;
  }

  public String getName() {
    return name;
  }

  public Date getStartedDate() {
    if (startedTime > 0) {
      return new Date(startedTime);
    } else {
      return null;
    }
  }

  public long getStartedTime() {
    return startedTime;
  }

  public String getStatus() {
    return status;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isStarted() {
    return "Started".equals(getStatus());
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      startedTime = 0;
    }
  }

  public void setModuleError(final String moduleError) {
    this.moduleError = moduleError;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setStartedTime(final long startedTime) {
    this.startedTime = startedTime;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
