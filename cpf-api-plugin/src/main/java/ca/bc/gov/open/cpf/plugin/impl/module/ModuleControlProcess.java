/*
 * Copyright © 2008-2016, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.plugin.impl.module;

import java.util.Map;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;

import com.revolsys.logging.Logs;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.process.AbstractProcess;

public class ModuleControlProcess extends AbstractProcess {
  private Channel<Map<String, Object>> in;

  private BusinessApplicationRegistry businessApplicationRegistry;

  private boolean running;

  public ModuleControlProcess(final BusinessApplicationRegistry businessApplicationRegistry,
    final Channel<Map<String, Object>> in) {
    this.businessApplicationRegistry = businessApplicationRegistry;
    this.in = in;
    in.readConnect();
  }

  @Override
  public void run() {
    this.running = true;
    try {
      while (this.running && !ThreadUtil.isInterrupted()) {
        final Map<String, Object> control = this.in.read(5000);
        if (control != null) {
          try {
            final String moduleName = (String)control.get("moduleName");
            final String action = (String)control.get("action");
            final ClassLoaderModule module = (ClassLoaderModule)this.businessApplicationRegistry
              .getModule(moduleName);
            if (module != null) {
              if ("start".equals(action)) {
                module.startDo();
              } else if ("restart".equals(action)) {
                module.restartDo();
              } else if ("stop".equals(action)) {
                module.stopDo();
              }
            }
          } catch (final Throwable t) {
            Logs.error(this, "Unable to perform module action: " + control, t);
          }
        }
      }

    } catch (final ClosedException e) {
      return;
    } finally {
      this.running = false;
      try {
        this.in.readDisconnect();
      } finally {
        this.in = null;
        this.businessApplicationRegistry = null;
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
