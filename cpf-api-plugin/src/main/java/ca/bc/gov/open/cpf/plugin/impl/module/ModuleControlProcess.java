package ca.bc.gov.open.cpf.plugin.impl.module;

import java.util.Map;

import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;

import com.revolsys.parallel.ThreadUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.process.AbstractProcess;

public class ModuleControlProcess extends AbstractProcess {

  private Channel<Map<String, Object>> in;

  private BusinessApplicationRegistry businessApplicationRegistry;

  private boolean running;

  public ModuleControlProcess(
    final BusinessApplicationRegistry businessApplicationRegistry,
    final Channel<Map<String, Object>> in) {
    this.businessApplicationRegistry = businessApplicationRegistry;
    this.in = in;
    in.readConnect();
  }

  @Override
  public void run() {
    this.running = true;
    try {
      while (running && !ThreadUtil.isInterrupted()) {
        final Map<String, Object> control = in.read(5000);
        if (control != null) {
          try {
            final String moduleName = (String)control.get("moduleName");
            final String action = (String)control.get("action");
            final ClassLoaderModule module = (ClassLoaderModule)businessApplicationRegistry.getModule(moduleName);
            if (module != null) {
              if ("start".equals(action)) {
                module.doStart();
              } else if ("restart".equals(action)) {
                module.doRestart();
              } else if ("stop".equals(action)) {
                module.doStop();
              }
            }
          } catch (final Throwable t) {
            LoggerFactory.getLogger(getClass()).error(
              "Unable to perform module action: " + control, t);
          }
        }
      }

    } catch (final ClosedException e) {
      return;
    } finally {
      running = false;
      try {
        in.readDisconnect();
      } finally {
        in = null;
        businessApplicationRegistry = null;
      }
    }
  }

  public void setIn(final Channel<Map<String, Object>> in) {
    this.in = in;
    in.readConnect();
  }

  @Override
  public void stop() {
    this.running = false;
  }
}
