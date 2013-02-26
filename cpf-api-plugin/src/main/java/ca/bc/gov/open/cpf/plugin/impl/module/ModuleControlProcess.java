package ca.bc.gov.open.cpf.plugin.impl.module;

import java.util.Map;

import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.AbstractProcess;

public class ModuleControlProcess extends AbstractProcess {

  private Channel<Map<String, Object>> in;

  private BusinessApplicationRegistry businessApplicationRegistry;

  public ModuleControlProcess(
    BusinessApplicationRegistry businessApplicationRegistry,
    Channel<Map<String, Object>> in) {
    this.businessApplicationRegistry = businessApplicationRegistry;
    this.in = in;
    in.readConnect();
  }

  @Override
  public void run() {
    try {
      while (true) {
        Map<String, Object> control = in.read();
        try {
          String moduleName = (String)control.get("moduleName");
          String action = (String)control.get("action");
          ClassLoaderModule module = (ClassLoaderModule)businessApplicationRegistry.getModule(moduleName);
          if (module != null) {
            if ("start".equals(action)) {
              module.doStart();
            } else if ("restart".equals(action)) {
              module.doRestart();
            } else if ("stop".equals(action)) {
              module.doStop();
            }
          }
        } catch (ThreadDeath t) {
          return;
        } catch (Throwable t) {
          LoggerFactory.getLogger(getClass()).error(
            "Unable to perform module action: " + control, t);
        }
      }

    } finally {
      in.readDisconnect();
      in = null;
    }
  }

  public void setIn(Channel<Map<String, Object>> in) {
    this.in = in;
    in.readConnect();
  }
}
