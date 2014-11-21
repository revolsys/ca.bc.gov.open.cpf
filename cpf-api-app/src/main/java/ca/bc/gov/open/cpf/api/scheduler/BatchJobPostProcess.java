package ca.bc.gov.open.cpf.api.scheduler;

import java.beans.PropertyChangeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.domain.BatchJob;

import com.revolsys.util.Property;

public class BatchJobPostProcess extends AbstractBatchJobChannelProcess {

  public BatchJobPostProcess() {
    super(BatchJob.PROCESSED);
  }

  @PostConstruct
  public void init() {
    final CpfConfig config = getConfig();
    Property.addListener(config, "postProcessPoolSize", this);
    final int postProcessPoolSize = config.getPreProcessPoolSize();
    setMaximumPoolSize(postProcessPoolSize);
  }

  @Override
  public boolean processJob(final long batchJobId) {
    final BatchJobService batchJobService = getBatchJobService();
    final long time = System.currentTimeMillis();
    if (getDataAccessObject().setBatchJobStatus(batchJobId, BatchJob.PROCESSED,
      BatchJob.CREATING_RESULTS)) {
      final long lastChangedTime = System.currentTimeMillis();
      return batchJobService.postProcessBatchJob(batchJobId, time,
        lastChangedTime);
    } else {
      return true;
    }
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
