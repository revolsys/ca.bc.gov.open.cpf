package ca.bc.gov.open.cpf.api.scheduler;

import ca.bc.gov.open.cpf.api.domain.BatchJob;

public class BatchJobPostProcess extends AbstractBatchJobChannelProcess {

  public BatchJobPostProcess() {
    super(BatchJob.PROCESSED);
  }

  @Override
  public void processJob(final long batchJobId) {
    final BatchJobService batchJobService = getBatchJobService();
    final long time = System.currentTimeMillis();
    if (getDataAccessObject().setBatchJobStatus(batchJobId, BatchJob.PROCESSED,
      BatchJob.CREATING_RESULTS)) {
      final long lastChangedTime = System.currentTimeMillis();
      batchJobService.postProcessBatchJob(batchJobId, time, lastChangedTime);
    }
  }

  @Override
  public void setBatchJobService(final BatchJobService batchJobService) {
    super.setBatchJobService(batchJobService);
    batchJobService.setPostProcess(this);
  }
}
