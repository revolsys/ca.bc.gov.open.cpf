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
package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Arrays;
import java.util.List;

public class BatchJobScheduleInfo {

  public static final String SCHEDULE = "SCHEDULE";

  private final String businessApplicationName;

  private final Long batchJobId;

  private List<String> actions;

  public BatchJobScheduleInfo(final String businessApplicationName,
    final Long batchJobId, final String... actions) {
    this.businessApplicationName = businessApplicationName;
    this.batchJobId = batchJobId;
    setActions(actions);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof BatchJobScheduleInfo) {
      final BatchJobScheduleInfo jobInfo = (BatchJobScheduleInfo)obj;
      return this.batchJobId.equals(jobInfo.batchJobId);
    }
    return false;
  }

  public List<String> getActions() {
    return this.actions;
  }

  public Long getBatchJobId() {
    return this.batchJobId;
  }

  public String getBusinessApplicationName() {
    return this.businessApplicationName;
  }

  @Override
  public int hashCode() {
    return this.batchJobId.hashCode();
  }

  public void setActions(final String... actions) {
    this.actions = Arrays.asList(actions);
  }
}
