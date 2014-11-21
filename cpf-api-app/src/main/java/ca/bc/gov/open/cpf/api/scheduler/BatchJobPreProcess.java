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
