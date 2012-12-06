package ca.bc.gov.open.cpf.api.scheduler;

import ca.bc.gov.open.cpf.api.domain.BatchJob;

public class BatchJobPreProcess extends AbstractBatchJobChannelProcess {

  public BatchJobPreProcess() {
    super(BatchJob.SUBMITTED);
  }

  @Override
  public void processJob(final long batchJobId) {
    final BatchJobService batchJobService = getBatchJobService();
    final long time = System.currentTimeMillis();
    final long lastChangedTime = batchJobService.setBatchJobStatus(batchJobId,
      BatchJob.SUBMITTED, BatchJob.CREATING_REQUESTS, time);

    if (lastChangedTime != -1) {
      batchJobService.preProcessBatchJob(batchJobId, time, lastChangedTime);
    }
  }

  @Override
  public void setBatchJobService(final BatchJobService batchJobService) {
    super.setBatchJobService(batchJobService);
    batchJobService.setPreProcess(this);
  }
}
