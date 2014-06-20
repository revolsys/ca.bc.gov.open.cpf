package ca.bc.gov.open.cpf.api.scheduler;

import javax.annotation.Resource;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

public class BatchJobPreProcess extends AbstractBatchJobChannelProcess {

  public BatchJobPreProcess() {
    super(BatchJob.SUBMITTED);
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
  @Resource(name = "batchJobService")
  public void setBatchJobService(final BatchJobService batchJobService) {
    super.setBatchJobService(batchJobService);
    batchJobService.setPreProcess(this);
  }
}
