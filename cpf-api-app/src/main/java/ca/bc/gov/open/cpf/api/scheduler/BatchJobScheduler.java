package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.collection.SetQueue;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ChannelValueStore;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.channel.MultiInputSelector;
import com.revolsys.parallel.channel.store.Buffer;
import com.revolsys.parallel.process.AbstractInOutProcess;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.SendToChannelAfterCommit;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.CollectionUtil;

public class BatchJobScheduler extends
AbstractInOutProcess<BatchJobScheduleInfo, Runnable> {
  /** The batch job service used to interact with the database. */
  private BatchJobService batchJobService;

  private final Map<String, Integer> scheduledGroupCountByBusinessApplication = new HashMap<String, Integer>();

  private final Map<String, Set<Long>> queuedJobIdsByBusinessApplication = new HashMap<String, Set<Long>>();

  private final Map<Long, Set<BatchJobRequestExecutionGroup>> queuedFinishedGroupsByJobId = new LinkedHashMap<Long, Set<BatchJobRequestExecutionGroup>>();

  private final Map<String, Set<Long>> scheduledJobIdsByBusinessApplication = new HashMap<String, Set<Long>>();

  private final long timeout = 600000;

  private final Set<Long> updateCountsJobIds = new HashSet<>();

  private long errorTime;

  private Channel<BatchJobScheduleInfo> groupFinished;

  private Channel<BatchJobScheduleInfo> scheduleFinished;

  public BatchJobScheduler() {
    setOutBufferSize(100);
  }

  private int addScheduledGroupCount(final String businessApplicationName,
    final int num) {
    int count = getScheduledGroupCount(businessApplicationName);
    count += num;
    this.scheduledGroupCountByBusinessApplication.put(businessApplicationName,
      count);
    return count;
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
  protected ChannelValueStore<BatchJobScheduleInfo> createInValueStore() {
    return new Buffer<BatchJobScheduleInfo>(
        new SetQueue<BatchJobScheduleInfo>());
  }

  /**
   * Get the batch job service used to interact with the database.
   *
   * @return The batch job service used to interact with the database.
   */
  public BatchJobService getBatchJobService() {
    return this.batchJobService;
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

  public void groupFinished(final String businessApplicationName,
    final Long batchJobId, final BatchJobRequestExecutionGroup group) {
    final BatchJobScheduleInfo batchJobInfo = new BatchJobScheduleInfo(
      businessApplicationName, batchJobId);
    batchJobInfo.setGroup(group);
    SendToChannelAfterCommit.send(this.groupFinished, batchJobInfo);
  }

  @Override
  protected void init() {
    this.groupFinished = new Channel<BatchJobScheduleInfo>(getBeanName()
        + ".groupFinished", new Buffer<BatchJobScheduleInfo>());
    this.groupFinished.readConnect();
    this.groupFinished.writeConnect();
    this.scheduleFinished = new Channel<BatchJobScheduleInfo>(getBeanName()
        + ".scheduleFinished", new Buffer<BatchJobScheduleInfo>());
    this.scheduleFinished.readConnect();
    this.scheduleFinished.writeConnect();
    getIn();
  }

  private void queueGroup(final BatchJobRequestExecutionGroup group) {
    final Long batchJobId = group.getBatchJobId();
    Set<BatchJobRequestExecutionGroup> groups;
    synchronized (this.queuedFinishedGroupsByJobId) {
      groups = this.queuedFinishedGroupsByJobId.get(batchJobId);
      if (groups == null) {
        groups = new LinkedHashSet<BatchJobRequestExecutionGroup>();
        this.queuedFinishedGroupsByJobId.put(batchJobId, groups);
      }
      groups.add(group);
    }
  }

  private void queueJob(final String businessApplicationName,
    final Long batchJobId) {
    final Set<Long> queuedJobIds = getQueuedJobIds(businessApplicationName);
    queuedJobIds.add(batchJobId);
  }

  @Override
  protected void run(final Channel<BatchJobScheduleInfo> in,
    final Channel<Runnable> out) {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.info("Started");
    final long timeout = this.timeout;
    final LoadJobIdsToScheduleFromDatabase loadJobIds = new LoadJobIdsToScheduleFromDatabase(
      this.batchJobService);
    final MultiInputSelector selector = new MultiInputSelector();
    final List<Channel<BatchJobScheduleInfo>> channels = Arrays.asList(
      this.scheduleFinished, this.groupFinished, in);
    while (true) {
      try {
        final int index = selector.select(timeout, channels);
        if (index == -1) {
          if (!loadJobIds.isRunning()) {
            out.write(loadJobIds);
          }
        } else {
          if (System.currentTimeMillis() - this.errorTime < 60000) {
            ThreadUtil.pause(60000);
          }
          final BatchJobScheduleInfo jobInfo = channels.get(index).read();
          final Long batchJobId = jobInfo.getBatchJobId();
          final String businessApplicationName = jobInfo.getBusinessApplicationName();
          final Set<Long> scheduledJobIds = getScheduledJobIds(businessApplicationName);

          final BatchJobRequestExecutionGroup group = jobInfo.getGroup();
          if (group == null) {
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

          } else {
            addScheduledGroupCount(businessApplicationName, -1);
            queueGroup(group);
          }
          scheduleQueuedJobs(out, businessApplicationName);
        }
        scheduleQueuedGroups(out);
      } catch (final ClosedException e) {
        log.info("Stopped");
        return;
      } catch (final Throwable t) {
        LoggerFactory.getLogger(BatchJobScheduler.class).error(
          "Error scheduling jobs", t);
      }
    }
  }

  private void scheduleQueuedGroups(final Channel<Runnable> out) {
    synchronized (this.queuedFinishedGroupsByJobId) {
      for (final Entry<Long, Set<BatchJobRequestExecutionGroup>> entry : this.queuedFinishedGroupsByJobId.entrySet()) {
        final Long batchJobId = entry.getKey();
        final Set<BatchJobRequestExecutionGroup> groups = entry.getValue();

        String businessApplicationName = null;
        if (groups != null && !groups.isEmpty()) {
          final BatchJobRequestExecutionGroup group = CollectionUtil.get(
            groups, 0);
          businessApplicationName = group.getBusinessApplicationName();
        }

        if (businessApplicationName != null) {
          if (this.updateCountsJobIds.add(batchJobId)) {
            final Runnable runnable = new InvokeMethodRunnable(this,
              "updateBatchJobCounts", businessApplicationName, batchJobId);
            out.write(runnable);
          }
        }
      }
    }

  }

  private void scheduleQueuedJobs(final Channel<Runnable> out,
    final String businessApplicationName) {
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
          out.write(runnable);
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
  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
    batchJobService.setScheduler(this);
  }

  public void updateBatchJobCounts(final String businessApplicationName,
    final Long batchJobId) {
    final AppLog log = this.batchJobService.getAppLog(businessApplicationName);
    Set<BatchJobRequestExecutionGroup> groups;
    synchronized (this.queuedFinishedGroupsByJobId) {
      groups = this.queuedFinishedGroupsByJobId.remove(batchJobId);
    }
    try (
        Transaction transaction = this.batchJobService.getDataAccessObject()
        .createTransaction(Propagation.REQUIRES_NEW)) {
      if (groups != null) {
        try {
          this.batchJobService.setBatchJobExecutingCounts(
            businessApplicationName, batchJobId, groups);
        } catch (final Throwable e) {
          this.errorTime = System.currentTimeMillis();
          synchronized (this.queuedFinishedGroupsByJobId) {
            final Set<BatchJobRequestExecutionGroup> newGroups = this.queuedFinishedGroupsByJobId.remove(batchJobId);
            if (newGroups == null) {
              this.queuedFinishedGroupsByJobId.put(batchJobId, groups);
            } else {
              newGroups.addAll(groups);
            }
          }
          transaction.setRollbackOnly();
          log.error(
            "Pausing 60 seconds: Unable to update group counts for job #"
                + batchJobId, e);
        }
      }
    } catch (final Throwable e) {
      this.errorTime = System.currentTimeMillis();
      synchronized (this.queuedFinishedGroupsByJobId) {
        final Set<BatchJobRequestExecutionGroup> newGroups = this.queuedFinishedGroupsByJobId.remove(batchJobId);
        if (newGroups == null) {
          this.queuedFinishedGroupsByJobId.put(batchJobId, groups);
        } else {
          newGroups.addAll(groups);
        }
      }
      log.error("Pausing 60 seconds: Unable to update group counts for job #"
          + batchJobId, e);
    } finally {
      synchronized (this.queuedFinishedGroupsByJobId) {
        this.updateCountsJobIds.remove(batchJobId);
      }
      final BatchJobScheduleInfo resultJobInfo = new BatchJobScheduleInfo(
        businessApplicationName, batchJobId,
        BatchJobScheduleInfo.SCHEDULE_FINISHED);
      this.scheduleFinished.write(resultJobInfo);
    }
  }

}
