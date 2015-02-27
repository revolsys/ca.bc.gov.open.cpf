package ca.bc.gov.open.cpf.api.scheduler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.revolsys.parallel.channel.store.Overwrite;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.parallel.process.Process;
import com.revolsys.parallel.process.ProcessNetwork;
import com.revolsys.transaction.SendToChannelAfterCommit;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.Maps;
import com.revolsys.util.Property;

public class BatchJobScheduler extends ThreadPoolExecutor implements Process,
  PropertyChangeListener {
  /** The batch job service used to interact with the database. */
  private BatchJobService batchJobService;

  private final Map<String, Set<Long>> scheduledJobIdsByBusinessApplication = new HashMap<>();

  private final long timeout = 60000;

  private final Channel<Object> awakeChannel = new Channel<>(new Overwrite<>());

  private int taskCount = 0;

  private final Object monitor = new Object();

  private Channel<BatchJobScheduleInfo> scheduleFinished;

  private long errorTime;

  private final Map<String, AtomicInteger> scheduledGroupCountByBusinessApplication = new HashMap<>();

  private final Map<String, Set<BatchJobRequestExecutionGroup>> scheduledGroupsByBusinessApplication = new HashMap<>();

  private final Channel<BatchJobScheduleInfo> in = new Channel<>(new Buffer<>(
    new SetQueue<BatchJobScheduleInfo>()));

  private ProcessNetwork processNetwork;

  private String beanName;

  @Resource(name = "cpfConfig")
  private CpfConfig config;

  private final Map<Long, String> queuedJobIds = new LinkedHashMap<>();

  public BatchJobScheduler() {
    super(0, 1, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
      new NamedThreadFactory());
  }

  private synchronized int addScheduledGroupCount(
    final String businessApplicationName, final int num) {
    synchronized (this.scheduledGroupCountByBusinessApplication) {
      AtomicInteger count = this.scheduledGroupCountByBusinessApplication.get(businessApplicationName);
      if (count == null) {
        if (num < 0) {
          return 0;
        } else {
          count = new AtomicInteger();
          this.scheduledGroupCountByBusinessApplication.put(
            businessApplicationName, count);
        }
      }
      return count.addAndGet(num);
    }
  }

  @Override
  protected void afterExecute(final Runnable r, final Throwable t) {
    synchronized (this.monitor) {
      this.taskCount--;
      this.monitor.notifyAll();
    }
  }

  public void clearBusinessApplication(final String businessApplicationName) {
    synchronized (this.queuedJobIds) {
      for (final Iterator<Entry<Long, String>> iterator = this.queuedJobIds.entrySet()
        .iterator(); iterator.hasNext();) {
        final Entry<Long, String> entry = iterator.next();
        if (businessApplicationName.equals(entry.getValue())) {
          iterator.remove();
        }
      }
    }
    synchronized (businessApplicationName) {
      this.scheduledGroupsByBusinessApplication.remove(businessApplicationName);
      this.scheduledGroupCountByBusinessApplication.remove(businessApplicationName);
    }
    synchronized (this.scheduledJobIdsByBusinessApplication) {
      this.scheduledJobIdsByBusinessApplication.remove(businessApplicationName);
    }
  }

  public void createExecutionGroup(final String businessApplicationName,
    final Long batchJobId) {
    final BatchJobScheduleInfo resultJobInfo = new BatchJobScheduleInfo(
      businessApplicationName, batchJobId);
    try {
      final BatchJobRequestExecutionGroup group = this.batchJobService.scheduleBatchJobExecutionGroups(batchJobId);
      if (group != null) {
        synchronized (this.scheduledGroupCountByBusinessApplication) {
          Maps.addToSet(this.scheduledGroupsByBusinessApplication,
            businessApplicationName, group);
        }
        resultJobInfo.setActions(BatchJobScheduleInfo.SCHEDULE);
      }
    } catch (final Throwable t) {
      LoggerFactory.getLogger(BatchJobScheduler.class).error(t.getMessage(), t);
      resultJobInfo.setActions(BatchJobScheduleInfo.SCHEDULE);
    } finally {
      addScheduledGroupCount(businessApplicationName, -1);
      removeScheduledJobId(businessApplicationName, batchJobId);
    }
    this.scheduleFinished.write(resultJobInfo);
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

  private int getScheduledGroupCount(final String businessApplicationName) {
    synchronized (this.scheduledGroupCountByBusinessApplication) {
      final int scheduleCount = Maps.getInteger(
        this.scheduledGroupCountByBusinessApplication, businessApplicationName,
        0);
      final int groupCount = CollectionUtil.getCollectionSize(
        this.scheduledGroupsByBusinessApplication, businessApplicationName);
      return scheduleCount + groupCount;
    }
  }

  private Set<Long> getScheduledJobIds(final String businessApplicationName) {
    synchronized (this.scheduledJobIdsByBusinessApplication) {
      Set<Long> jobIds = this.scheduledJobIdsByBusinessApplication.get(businessApplicationName);
      if (jobIds == null) {
        jobIds = new LinkedHashSet<Long>();
        this.scheduledJobIdsByBusinessApplication.put(businessApplicationName,
          jobIds);
      }
      return jobIds;
    }
  }

  public void groupFinished(final BatchJobRequestExecutionGroup group) {
    final String businessApplicationName = group.getBusinessApplicationName();
    synchronized (this.scheduledGroupCountByBusinessApplication) {
      Maps.removeFromCollection(
        this.scheduledGroupsByBusinessApplication, businessApplicationName,
        group);
    }
    SendToChannelAfterCommit.send(this.awakeChannel, Integer.MAX_VALUE);
  }

  private void init() {
    this.in.readConnect();
    this.awakeChannel.readConnect();
    this.awakeChannel.writeConnect();
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
    synchronized (this.queuedJobIds) {
      this.queuedJobIds.put(batchJobId, businessApplicationName);
    }
  }

  private void removeScheduledJobId(final String businessApplicationName,
    final Long batchJobId) {
    synchronized (this.scheduledJobIdsByBusinessApplication) {
      Maps.removeFromCollection(
        this.scheduledJobIdsByBusinessApplication, businessApplicationName,
        batchJobId);
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

  protected void run(final Channel<BatchJobScheduleInfo> in) {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.info("Started");
    init();
    final long timeout = this.timeout;
    final LoadJobIdsToScheduleFromDatabase loadJobIds = new LoadJobIdsToScheduleFromDatabase(
      this.batchJobService);
    final MultiInputSelector selector = new MultiInputSelector();
    final List<Channel<?>> channels = Arrays.asList(this.scheduleFinished,
      this.awakeChannel, in);
    while (true) {
      try {
        final int index = selector.select(timeout, channels);
        if (index == -1) {
          if (!loadJobIds.isRunning()) {
            execute(loadJobIds);
          }
        } else if (index == 1) {
          this.awakeChannel.read();
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

          final List<String> actions = jobInfo.getActions();
          if (actions.contains(BatchJobScheduleInfo.SCHEDULE)) {
            queueJob(businessApplicationName, batchJobId);
          }
        }
        scheduleQueuedJobs();
      } catch (final ClosedException e) {
        log.info("Stopped");
        return;
      } catch (final Throwable t) {
        LoggerFactory.getLogger(BatchJobScheduler.class).error(
          "Error scheduling jobs", t);
      }
    }
  }

  private void scheduleQueuedJobs() {
    synchronized (this.queuedJobIds) {
      for (final Iterator<Entry<Long, String>> iterator = this.queuedJobIds.entrySet()
        .iterator(); iterator.hasNext();) {
        final Entry<Long, String> entry = iterator.next();
        final Long jobId = entry.getKey();
        final String businessApplicationName = entry.getValue();

        final BusinessApplication businessApplication = this.batchJobService.getBusinessApplication(businessApplicationName);
        if (businessApplication != null
          && businessApplication.getModule().isStarted()) {
          final int maxCount = businessApplication.getMaxConcurrentRequests();

          final Set<Long> scheduledJobsIds = getScheduledJobIds(businessApplicationName);

          if (getScheduledGroupCount(businessApplicationName) < maxCount) {
            if (!scheduledJobsIds.contains(jobId)) {
              iterator.remove();
              scheduledJobsIds.add(jobId);
              addScheduledGroupCount(businessApplicationName, 1);
              final Runnable runnable = new InvokeMethodRunnable(this,
                "createExecutionGroup", businessApplicationName, jobId);
              execute(runnable);
            }
          }
        }
      }
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
