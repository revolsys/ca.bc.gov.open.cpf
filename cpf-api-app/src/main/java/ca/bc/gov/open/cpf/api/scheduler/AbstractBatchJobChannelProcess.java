package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

import com.revolsys.parallel.ThreadUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;
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
    return this.batchJobService;
  }

  protected CpfDataAccessObject getDataAccessObject() {
    return this.batchJobService.getDataAccessObject();
  }

  protected void postRun(final Channel<Long> in, final Channel<Runnable> out) {
  }

  protected void preRun(final Channel<Long> in, final Channel<Runnable> out) {
  }

  public abstract boolean processJob(final long batchJobId);

  public void processJobWrapper(final long batchJobId) {
    this.scheduledIds.remove(batchJobId);
    final boolean success = processJob(batchJobId);
    if (!success) {
      schedule(batchJobId);
    }
  }

  @Override
  protected void run(final Channel<Long> in, final Channel<Runnable> out) {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.info("Started");
    preRun(in, out);
    try {
      scheduleFromDatabase();
      while (true) {
        try {
          this.batchJobService.waitIfTablespaceError(getClass());
          final Long batchJobId = in.read(this.timeout);
          if (batchJobId == null) {
            if (this.scheduledIds.isEmpty()) {
              scheduleFromDatabase();
            }
          } else {
            schedule(batchJobId);
          }
        } catch (final ClosedException e) {
          throw e;
        } catch (final Throwable e) {
          log.error("Waiting 60 seconds due to unexpected error", e);
          ThreadUtil.pause(60 * 1000);
        }
      }
    } catch (final ClosedException e) {
    } catch (final Throwable e) {
      log.error("Shutting down due to unexpected error", e);
    } finally {
      postRun(in, out);
    }
    log.info("Stopped");
  }

  public void schedule(final Long batchJobId) {
    if (!this.scheduledIds.contains(batchJobId)) {
      this.scheduledIds.add(batchJobId);
      final Runnable runnable = new InvokeMethodRunnable(this,
        "processJobWrapper", batchJobId);
      getOut().write(runnable);
    }
  }

  protected void scheduleFromDatabase() {
    try {
      this.batchJobService.scheduleFromDatabase(this.jobStatusToProcess);
    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error(
        "Waiting 60 seconds due to: Unable to schedule from database", e);
      ThreadUtil.pause(60 * 1000);
    }
  }

  public void scheduleFromDatabase(final String moduleName,
    final String businessApplicationName) {
    this.batchJobService.scheduleFromDatabase(moduleName,
      businessApplicationName, this.jobStatusToProcess);
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
