package ca.bc.gov.open.cpf.api.worker;

import java.util.Map;

import javax.annotation.PreDestroy;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInProcess;

public class CpfWorkerMessageProcess extends BaseInProcess<Map<String, Object>> {
  private CpfWorkerScheduler scheduler;

  @Override
  @PreDestroy
  protected void destroy() {
    scheduler = null;
  }

  @Override
  protected void init() {
    super.init();
    setIn(scheduler.getInMessageChannel());
  }

  @Override
  protected void process(final Channel<Map<String, Object>> in,
    final Map<String, Object> message) {
    final String action = (String)message.get("action");
    final CpfWorkerScheduler scheduler = this.scheduler;
    if (scheduler != null) {
      if ("moduleStart".equals(action)) {
        scheduler.startModule(message);
      } else if ("moduleStop".equals(action)) {
        scheduler.stopModule(message);
      } else if ("cancelGroup".equals(action)) {
        scheduler.cancelGroup(message);
      }
    }
  }

  public void setScheduler(final CpfWorkerScheduler scheduler) {
    this.scheduler = scheduler;
  }
}
