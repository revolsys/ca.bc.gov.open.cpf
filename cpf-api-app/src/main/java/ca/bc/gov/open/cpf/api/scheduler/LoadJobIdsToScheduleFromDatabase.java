package ca.bc.gov.open.cpf.api.scheduler;

import org.slf4j.LoggerFactory;

import com.revolsys.parallel.channel.ClosedException;

public class LoadJobIdsToScheduleFromDatabase implements Runnable {
  private boolean running;

  private final BatchJobService batchJobService;

  public LoadJobIdsToScheduleFromDatabase(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  public void run() {
    try {
      running = true;
      batchJobService.scheduleFromDatabase();
    } catch (final ClosedException e) {
    } catch (final Throwable e) {
      LoggerFactory.getLogger(LoadJobIdsToScheduleFromDatabase.class).error("Unable to schedule from database", e);
    } finally {
      running = false;
    }
  }
}
