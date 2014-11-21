package ca.bc.gov.open.cpf.api.scheduler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.collection.SetQueue;
import com.revolsys.parallel.NamedThreadFactory;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.channel.MultiInputSelector;
import com.revolsys.parallel.channel.store.Buffer;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.parallel.process.Process;
import com.revolsys.parallel.process.ProcessNetwork;
import com.revolsys.transaction.SendToChannelAfterCommit;
import com.revolsys.util.Property;

public class BatchJobScheduler extends ThreadPoolExecutor implements Process,
PropertyChangeListener {
  /** The batch job service used to interact with the database. */
  private BatchJobService batchJobService;

  private final Map<String, Set<Long>> queuedJobIdsByBusinessApplication = new HashMap<>();

  private final Map<String, Set<Long>> scheduledJobIdsByBusinessApplication = new HashMap<>();

  private final long timeout = 60000;

  private Channel<String> groupFinished;

  private int taskCount = 0;

  private final Object monitor = new Object();

  private Channel<BatchJobScheduleInfo> scheduleFinished;

  private long errorTime;

  private final Map<String, Integer> scheduledGroupCountByBusinessApplication = new HashMap<String, Integer>();

  private final Channel<BatchJobScheduleInfo> in = new Channel<>(new Buffer<>(
      new SetQueue<BatchJobScheduleInfo>()));

  private ProcessNetwork processNetwork;

  private String beanName;

  @Resource(name = "cpfConfig")
  private CpfConfig config;

  public BatchJobScheduler() {
    super(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1),
      new NamedThreadFactory());
    setKeepAliveTime(60, TimeUnit.SECONDS);
  }

  private synchronized int addScheduledGroupCount(
    final String businessApplicationName, final int num) {
    synchronized (this.scheduledGroupCountByBusinessApplication) {
      int count = getScheduledGroupCount(businessApplicationName);
      count += num;
      this.scheduledGroupCountByBusinessApplication.put(
        businessApplicationName, count);
      return count;
    }
  }

  @Override
  protected void afterExecute(final Runnable r, final Throwable t) {
    synchronized (this.monitor) {
      this.taskCount--;
      this.monitor.notifyAll();
    }
  }

  public void createExecutionGroup(final String businessApplicationName,
    final Long batchJobId) {
    final BatchJobScheduleInfo resultJobInfo = new BatchJobScheduleInfo(
      businessApplicationName, batchJobId);
    try {
      if (this.batchJobService.scheduleBatchJobExecutionGroups(batchJobId)) {
        resultJobInfo.setActions(BatchJobScheduleInfo.SCHEDULE_FINISHED,
          BatchJobScheduleInfo.SCHEDULE);
      } else {
        resultJobInfo.setActions(BatchJobScheduleInfo.NO_GROUP_SCHEDULED,
          BatchJobScheduleInfo.SCHEDULE_FINISHED);
      }
    } catch (final Throwable t) {
      LoggerFactory.getLogger(BatchJobScheduler.class).error(t.getMessage(), t);
      resultJobInfo.setActions(BatchJobScheduleInfo.NO_GROUP_SCHEDULED,
        BatchJobScheduleInfo.SCHEDULE_FINISHED, BatchJobScheduleInfo.SCHEDULE);
    } finally {
      this.scheduleFinished.write(resultJobInfo);
    }
  }

  @Override
  public void execute(final Runnable command) {
    if (command != null) {
      synchronized (this.monitor) {
        while (!isShutdown()) {
          while (this.taskCount >= getMaximumPoolSize()) {
            ThreadUtil.pause(this.monitor);
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

  public Channel<BatchJobScheduleInfo> getIn() {
    return this.in;
  }

  public Object getMonitor() {
    return this.monitor;
  }

  @Override
  public ProcessNetwork getProcessNetwork() {
    return this.processNetwork;
  }

  private Set<Long> getQueuedJobIds(final String businessApplicationName) {
    Set<Long> jobIds = this.queuedJobIdsByBusinessApplication.get(businessApplicationName);
    if (jobIds == null) {
      jobIds = new LinkedHashSet<Long>();
      this.queuedJobIdsByBusinessApplication.put(businessApplicationName,
        jobIds);
    }
    return jobIds;
  }

  private int getScheduledGroupCount(final String businessApplicationName) {
    final Integer count = this.scheduledGroupCountByBusinessApplication.get(businessApplicationName);
    if (count == null) {
      return 0;
    } else {
      return count;
    }
  }

  private Set<Long> getScheduledJobIds(final String businessApplicationName) {
    Set<Long> jobIds = this.scheduledJobIdsByBusinessApplication.get(businessApplicationName);
    if (jobIds == null) {
      jobIds = new LinkedHashSet<Long>();
      this.scheduledJobIdsByBusinessApplication.put(businessApplicationName,
        jobIds);
    }
    return jobIds;
  }

  public void groupFinished(final BatchJobRequestExecutionGroup group) {
    SendToChannelAfterCommit.send(this.groupFinished,
      group.getBusinessApplicationName());
  }

  private void init() {
    this.in.readConnect();
    this.groupFinished = new Channel<>(getBeanName() + ".groupFinished",
        new Buffer<String>());
    this.groupFinished.readConnect();
    this.groupFinished.writeConnect();
    this.scheduleFinished = new Channel<>(getBeanName() + ".scheduleFinished",
        new Buffer<BatchJobScheduleInfo>());
    this.scheduleFinished.readConnect();
    this.scheduleFinished.writeConnect();
    final CpfConfig config = getConfig();
    Property.addListener(config, "preProcessPoolSize", this);
    final int preProcessPoolSize = config.getPreProcessPoolSize();
    setMaximumPoolSize(preProcessPoolSize);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final String propertyName = event.getPropertyName();
    if ("schedulerPoolSize".equals(propertyName)) {
      final Integer poolSize = (Integer)event.getNewValue();
      setMaximumPoolSize(poolSize);
    }
  }

  private void queueJob(final String businessApplicationName,
    final Long batchJobId) {
    final Set<Long> queuedJobIds = getQueuedJobIds(businessApplicationName);
    queuedJobIds.add(batchJobId);
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

  protected void run(final Channel<BatchJobScheduleInfo> in) {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.info("Started");
    init();
    final long timeout = this.timeout;
    final LoadJobIdsToScheduleFromDatabase loadJobIds = new LoadJobIdsToScheduleFromDatabase(
      this.batchJobService);
    final MultiInputSelector selector = new MultiInputSelector();
    final List<Channel<?>> channels = Arrays.asList(this.scheduleFinished,
      this.groupFinished, in);
    while (true) {
      try {
        final int index = selector.select(timeout, channels);
        if (index == -1) {
          if (!loadJobIds.isRunning()) {
            execute(loadJobIds);
          }
        } else if (index == 1) {
          final String businessApplicationName = this.groupFinished.read();
          addScheduledGroupCount(businessApplicationName, -1);
          scheduleQueuedJobs(businessApplicationName);
        } else {
          if (System.currentTimeMillis() - this.errorTime < 60000) {
            ThreadUtil.pause(60000);
          }
          Channel<BatchJobScheduleInfo> channel;
          if (index == 0) {
            channel = this.scheduleFinished;
          } else {
            channel = in;
          }
          final BatchJobScheduleInfo jobInfo = channel.read();
          final Long batchJobId = jobInfo.getBatchJobId();
          final String businessApplicationName = jobInfo.getBusinessApplicationName();
          final Set<Long> scheduledJobIds = getScheduledJobIds(businessApplicationName);

          final List<String> actions = jobInfo.getActions();
          if (actions.contains(BatchJobScheduleInfo.NO_GROUP_SCHEDULED)) {
            addScheduledGroupCount(businessApplicationName, -1);
          }
          if (actions.contains(BatchJobScheduleInfo.SCHEDULE_FINISHED)) {
            scheduledJobIds.remove(batchJobId);
          }

          if (actions.contains(BatchJobScheduleInfo.SCHEDULE)) {
            queueJob(businessApplicationName, batchJobId);
          }
          scheduleQueuedJobs(businessApplicationName);
        }
      } catch (final ClosedException e) {
        log.info("Stopped");
        return;
      } catch (final Throwable t) {
        LoggerFactory.getLogger(BatchJobScheduler.class).error(
          "Error scheduling jobs", t);
      }
    }
  }

  private void scheduleQueuedJobs(final String businessApplicationName) {
    final BusinessApplication businessApplication = this.batchJobService.getBusinessApplication(businessApplicationName);
    if (businessApplication != null
        && businessApplication.getModule().isStarted()) {
      final int maxCount = businessApplication.getMaxConcurrentRequests();

      final Set<Long> queuedJobIds = getQueuedJobIds(businessApplicationName);
      final Set<Long> scheduledJobsIds = getScheduledJobIds(businessApplicationName);

      final Iterator<Long> queuedJobIter = queuedJobIds.iterator();
      while (queuedJobIter.hasNext()
          && getScheduledGroupCount(businessApplicationName) < maxCount) {
        final Long batchJobId = queuedJobIter.next();
        if (!scheduledJobsIds.contains(batchJobId)) {
          queuedJobIter.remove();
          scheduledJobsIds.add(batchJobId);
          addScheduledGroupCount(businessApplicationName, 1);
          final Runnable runnable = new InvokeMethodRunnable(this,
            "createExecutionGroup", businessApplicationName, batchJobId);
          execute(runnable);
        }
      }
    } else {
      this.queuedJobIdsByBusinessApplication.remove(businessApplicationName);
    }
  }

  /**
   * Set the batch job service used to interact with the database.
   *
   * @param batchJobService The batch job service used to interact with the
   *          database.
   */
  @Resource(name = "batchJobService")
  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
    batchJobService.setScheduler(this);
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
