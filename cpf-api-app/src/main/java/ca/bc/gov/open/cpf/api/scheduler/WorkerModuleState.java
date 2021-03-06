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

import java.util.Date;

public class WorkerModuleState {

  private String name;

  private boolean enabled;

  private String moduleError;

  private String status = "INITIALIZING";

  private long startedTime;

  public WorkerModuleState(final String name) {
    this.name = name;
  }

  public String getModuleError() {
    return this.moduleError;
  }

  public String getName() {
    return this.name;
  }

  public Date getStartedDate() {
    if (this.startedTime > 0) {
      return new Date(this.startedTime);
    } else {
      return null;
    }
  }

  public long getStartedTime() {
    return this.startedTime;
  }

  public String getStatus() {
    return this.status;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public boolean isStarted() {
    return "Started".equals(getStatus());
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      this.startedTime = 0;
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
