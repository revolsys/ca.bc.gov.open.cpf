/*
 * Copyright © 2008-2015, Province of British Columbia
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
