package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.AbstractInOutProcess;
import com.revolsys.parallel.process.InvokeMethodRunnable;

public abstract class AbstractBatchJobChannelProcess extends
  AbstractInOutProcess<Long, Runnable> {
  private final String jobStatusToProcess;

  /** The batch job service used to interact with the database. */
  private BatchJobService batchJobService;

  private final long timeout = 600000;

  private final Set<Long> scheduledIds = Collections.synchronizedSet(new LinkedHashSet<Long>());

  public AbstractBatchJobChannelProcess(final String jobStatusToProcess) {
    this.jobStatusToProcess = jobStatusToProcess;
    setInBufferSize(-1);
    setOutBufferSize(100);
  }

  /**
   * Get the batch job service used to interact with the database.
   * 
   * @return The batch job service used to interact with the database.
   */
  public BatchJobService getBatchJobService() {
    return batchJobService;
  }

  protected CpfDataAccessObject getDataAccessObject() {
    return batchJobService.getDataAccessObject();
  }

  protected void postRun(final Channel<Long> in, final Channel<Runnable> out) {
  }

  protected void preRun(final Channel<Long> in, final Channel<Runnable> out) {
  }

  public abstract boolean processJob(final long batchJobId);

  public void processJobWrapper(final long batchJobId) {
    try {
      if (processJob(batchJobId)) {
        scheduledIds.remove(batchJobId);
      } else {
        schedule(batchJobId);
      }
    } finally {
      schedule(batchJobId);
    }
  }

  @Override
  protected void run(final Channel<Long> in, final Channel<Runnable> out) {
    preRun(in, out);
    try {
      batchJobService.scheduleFromDatabase(jobStatusToProcess);
      while (true) {
        batchJobService.waitIfTablespaceError(getClass());
        final Long batchJobId = in.read(timeout);
        if (batchJobId == null) {
          if (scheduledIds.isEmpty()) {
            batchJobService.scheduleFromDatabase(jobStatusToProcess);
          }
        } else {
          schedule(batchJobId);
        }
      }
    } finally {
      postRun(in, out);
    }
  }

  public void schedule(final Long batchJobId) {
    if (!scheduledIds.contains(batchJobId)) {
      scheduledIds.add(batchJobId);
      final Runnable runnable = new InvokeMethodRunnable(this,
        "processJobWrapper", batchJobId);
      getOut().write(runnable);
    }
  }

  public void scheduleFromDatabase(final String moduleName,
    final String businessApplicationName) {
    batchJobService.scheduleFromDatabase(moduleName, businessApplicationName,
      jobStatusToProcess);
  }

  /**
   * Set the batch job service used to interact with the database.
   * 
   * @param batchJobService The batch job service used to interact with the
   *          database.
   */
  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }
}
