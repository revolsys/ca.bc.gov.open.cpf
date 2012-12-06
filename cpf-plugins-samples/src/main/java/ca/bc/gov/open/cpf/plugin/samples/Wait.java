package ca.bc.gov.open.cpf.plugin.samples;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

@BusinessApplicationPlugin(
    name = "Wait",
    version = "1.0.0",
    instantModePermission = "permitAll")
public class Wait {
  private long waitTime = 1000;

  public void execute() {
    synchronized (this) {
      try {
        wait(waitTime);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @ResultAttribute
  public long getWaitTime() {
    return waitTime;
  }

  @JobParameter
  @RequestParameter
  public void setWaitTime(final long waitTime) {
    this.waitTime = waitTime;
  }
}
