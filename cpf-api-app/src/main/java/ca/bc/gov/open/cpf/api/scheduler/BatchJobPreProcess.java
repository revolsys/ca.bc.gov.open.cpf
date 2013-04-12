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

    if (getDataAccessObject().setBatchJobStatus(batchJobId, BatchJob.SUBMITTED,
      BatchJob.CREATING_REQUESTS)) {
      final long lastChangedTime = System.currentTimeMillis();
      batchJobService.preProcessBatchJob(batchJobId, time, lastChangedTime);
    }
  }

  @Override
  public void setBatchJobService(final BatchJobService batchJobService) {
    super.setBatchJobService(batchJobService);
    batchJobService.setPreProcess(this);
  }
}
