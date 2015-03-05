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

import java.beans.PropertyChangeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

import com.revolsys.util.Property;

public class BatchJobPreProcess extends AbstractBatchJobChannelProcess {

  public BatchJobPreProcess() {
    super(BatchJob.SUBMITTED);
  }

  @PostConstruct
  public void init() {
    final CpfConfig config = getConfig();
    Property.addListener(config, "preProcessPoolSize", this);
    final int preProcessPoolSize = config.getPreProcessPoolSize();
    setMaximumPoolSize(preProcessPoolSize);
  }

  @Override
  public boolean processJob(final long batchJobId) {
    final BatchJobService batchJobService = getBatchJobService();
    final long time = System.currentTimeMillis();

    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    if (dataAccessObject.setBatchJobStatus(batchJobId, BatchJob.SUBMITTED,
      BatchJob.CREATING_REQUESTS)) {
      final long lastChangedTime = System.currentTimeMillis();
      return batchJobService.preProcessBatchJob(batchJobId, time,
        lastChangedTime);
    } else {
      return true;
    }
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final String propertyName = event.getPropertyName();
    if ("preProcessPoolSize".equals(propertyName)) {
      final Integer poolSize = (Integer)event.getNewValue();
      setMaximumPoolSize(poolSize);
    }
  }

  @Override
  @Resource(name = "batchJobService")
  public void setBatchJobService(final BatchJobService batchJobService) {
    super.setBatchJobService(batchJobService);
    batchJobService.setPreProcess(this);
  }
}
