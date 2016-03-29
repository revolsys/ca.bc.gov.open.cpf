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

import java.beans.PropertyChangeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobStatus;

import com.revolsys.identifier.Identifier;
import com.revolsys.util.Property;

public class BatchJobPostProcess extends AbstractBatchJobChannelProcess {

  public BatchJobPostProcess() {
    super(BatchJobStatus.PROCESSED);
  }

  @PostConstruct
  public void init() {
    final CpfConfig config = getConfig();
    Property.addListener(config, "postProcessPoolSize", this);
    final int postProcessPoolSize = config.getPreProcessPoolSize();
    setMaximumPoolSize(postProcessPoolSize);
  }

  @Override
  protected boolean processJob(final Identifier batchJobId) {
    final BatchJobService batchJobService = getBatchJobService();
    final BatchJob batchJob = batchJobService.getBatchJob(batchJobId);
    if (batchJob != null) {
      final long time = System.currentTimeMillis();
      if (batchJob.setStatus(batchJobService, BatchJobStatus.PROCESSED, BatchJobStatus.CREATING_RESULTS) || batchJob.isStatus(BatchJobStatus.CREATING_RESULTS)) {
        final long lastChangedTime = System.currentTimeMillis();
        return batchJobService.postProcessBatchJob(batchJobId, time, lastChangedTime);
      }
    }
    return true;
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final String propertyName = event.getPropertyName();
    if ("postProcessPoolSize".equals(propertyName)) {
      final Integer poolSize = (Integer)event.getNewValue();
      setMaximumPoolSize(poolSize);
    }
  }

  @Override
  @Resource(name = "batchJobService")
  public void setBatchJobService(final BatchJobService batchJobService) {
    super.setBatchJobService(batchJobService);
    batchJobService.setPostProcess(this);
  }
}
