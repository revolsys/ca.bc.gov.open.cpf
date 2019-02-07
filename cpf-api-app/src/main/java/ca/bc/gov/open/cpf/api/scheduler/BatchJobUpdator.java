package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import ca.bc.gov.open.cpf.api.domain.BatchJob;

import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;

public class BatchJobUpdator implements Runnable {

  private boolean running = true;

  private final Set<BatchJob> jobs = new LinkedHashSet<>();

  public BatchJobUpdator() {
    final Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.start();
  }

  private BatchJob getBatchJob() {
    synchronized (this.jobs) {
      while (this.running && this.jobs.isEmpty()) {
        try {
          this.jobs.wait();
        } catch (final InterruptedException e) {
          this.running = false;
          return null;
        }
      }
      final Iterator<BatchJob> iterator = this.jobs.iterator();
      final BatchJob job = iterator.next();
      iterator.remove();
      return job;
    }
  }

  @Override
  public void run() {
    while (this.running) {
      final BatchJob batchJob = getBatchJob();
      if (batchJob != null) {
        try (
          Transaction transaction = batchJob.getRecordStore()
            .newTransaction(Propagation.REQUIRED)) {
          batchJob.update();
        }
      }
    }
  }

  public void stop() {
    this.running = false;
    synchronized (this.jobs) {
      this.jobs.notifyAll();
    }
  }

  public void updateJob(final BatchJob batchJob) {
    synchronized (this.jobs) {
      this.jobs.add(batchJob);
      this.jobs.notifyAll();
    }
  }
}
