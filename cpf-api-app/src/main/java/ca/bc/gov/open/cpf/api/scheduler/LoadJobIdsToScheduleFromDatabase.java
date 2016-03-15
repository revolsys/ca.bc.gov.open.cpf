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
package ca.bc.gov.open.cpf.api.scheduler;

import org.slf4j.LoggerFactory;

import com.revolsys.parallel.channel.ClosedException;

public class LoadJobIdsToScheduleFromDatabase implements Runnable {
  private boolean running;

  private final BatchJobService batchJobService;

  public LoadJobIdsToScheduleFromDatabase(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }

  public boolean isRunning() {
    return this.running;
  }

  @Override
  public void run() {
    try {
      this.running = true;
      this.batchJobService.scheduleFromDatabase();
    } catch (final ClosedException e) {
    } catch (final Throwable e) {
      LoggerFactory.getLogger(LoadJobIdsToScheduleFromDatabase.class)
        .error("Unable to schedule from database", e);
    } finally {
      this.running = false;
    }
  }
}
