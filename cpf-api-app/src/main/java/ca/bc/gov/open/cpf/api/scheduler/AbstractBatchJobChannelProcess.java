/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.api.scheduler;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

import com.revolsys.parallel.NamedThreadFactory;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.channel.store.Buffer;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.parallel.process.Process;
import com.revolsys.parallel.process.ProcessNetwork;

public abstract class AbstractBatchJobChannelProcess extends ThreadPoolExecutor
  implements Process, PropertyChangeListener {
  private final String jobStatusToProcess;

  /** The batch job service used to interact with the database. */
  private BatchJobService batchJobService;

  private final long timeout = 600000;

  @Resource(name = "cpfConfig")
  private CpfConfig config;

  private final Set<Long> scheduledIds = Collections.synchronizedSet(new LinkedHashSet<Long>());

  private final Channel<Long> in = new Channel<>(new Buffer<Long>());

  private ProcessNetwork processNetwork;

  private String beanName;

  private int taskCount = 0;

  public AbstractBatchJobChannelProcess(final String jobStatusToProcess) {
    super(0, 1, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
      new NamedThreadFactory());
    this.jobStatusToProcess = jobStatusToProcess;
    this.in.readConnect();
  }

  @Override
  protected void afterExecute(final Runnable r, final Throwable t) {
    synchronized (this.scheduledIds) {
      this.scheduledIds.notifyAll();
      this.taskCount--;
    }
  }

  @Override
  public void execute(final Runnable command) {
    if (command != null) {
      synchronized (this.scheduledIds) {
        while (!isShutdown()) {
          while (this.taskCount >= getMaximumPoolSize()) {
            ThreadUtil.pause(this.scheduledIds);
          }
          try {
            super.execute(command);
            this.taskCount++;
            return;
          } catch (final RejectedExecutionException e) {
          } catch (final RuntimeException e) {
            throw e;
          } catch (final Error e) {
            throw e;
          }
        }
      }
    }
  }

  /**
   * Get the batch job service used to interact with the database.
   *
   * @return The batch job service used to interact with the database.
   */
  public BatchJobService getBatchJobService() {
    return this.batchJobService;
  }

  @Override
  public String getBeanName() {
    return this.beanName;
  }

  public CpfConfig getConfig() {
    return this.config;
  }

  protected CpfDataAccessObject getDataAccessObject() {
    return this.batchJobService.getDataAccessObject();
  }

  public Channel<Long> getIn() {
    return this.in;
  }

  @Override
  public ProcessNetwork getProcessNetwork() {
    return this.processNetwork;
  }

  public abstract boolean processJob(final long batchJobId);

  public void processJobWrapper(final long batchJobId) {
    synchronized (this.scheduledIds) {
      this.scheduledIds.remove(batchJobId);
    }
    final boolean success = processJob(batchJobId);
    if (!success) {
      schedule(batchJobId);
    }
  }

  @Override
  public final void run() {
    boolean hasError = false;
    final Logger log = LoggerFactory.getLogger(getClass());
    try {
      log.debug("Start");
      run(this.in);
    } catch (final ClosedException e) {
      log.debug("Shutdown");
    } catch (final ThreadDeath e) {
      log.debug("Shutdown");
    } catch (final Throwable e) {
      log.error(e.getMessage(), e);
      hasError = true;
    } finally {
      if (this.in != null) {
        this.in.readDisconnect();
      }
    }
    if (hasError) {
      getProcessNetwork().stop();
    }
  }

  protected void run(final Channel<Long> in) {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.info("Started");
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
    }
    log.info("Stopped");
  }

  public void schedule(final Long batchJobId) {
    synchronized (this.scheduledIds) {
      if (!this.scheduledIds.contains(batchJobId)) {
        this.scheduledIds.add(batchJobId);
        final Runnable runnable = new InvokeMethodRunnable(this,
          "processJobWrapper", batchJobId);
        execute(runnable);
      }
    }
  }

  private void scheduleFromDatabase() {
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

  @Override
  public void setBeanName(final String beanName) {
    this.beanName = beanName;
    final ThreadFactory threadFactory = getThreadFactory();
    if (threadFactory instanceof NamedThreadFactory) {
      final NamedThreadFactory namedThreadFactory = (NamedThreadFactory)threadFactory;
      namedThreadFactory.setNamePrefix(beanName + "-pool");
    }
  }

  @Override
  public void setProcessNetwork(final ProcessNetwork processNetwork) {
    this.processNetwork = processNetwork;
  }

  @Override
  public String toString() {
    return this.beanName;
  }
}
