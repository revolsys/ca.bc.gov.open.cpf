package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.collection.SetQueue;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ChannelDataStore;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.channel.MultiInputSelector;
import com.revolsys.parallel.channel.store.Buffer;
import com.revolsys.parallel.process.AbstractInOutProcess;
import com.revolsys.parallel.process.InvokeMethodRunnable;
import com.revolsys.transaction.SendToChannelAfterCommit;
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

  private Channel<BatchJobScheduleInfo> groupFinished;

  private Channel<BatchJobScheduleInfo> scheduleFinished;

  public BatchJobScheduler() {
    setOutBufferSize(100);
  }

  private int addScheduledGroupCount(final String businessApplicationName,
    final int num) {
    int count = getScheduledGroupCount(businessApplicationName);
    count += num;
    scheduledGroupCountByBusinessApplication.put(businessApplicationName, count);
    return count;
  }

  public void createExecutionGroup(final String businessApplicationName,
    final Long batchJobId) {
    final BatchJobScheduleInfo resultJobInfo = new BatchJobScheduleInfo(
      businessApplicationName, batchJobId);
    try {
      if (batchJobService.scheduleBatchJobExecutionGroups(batchJobId)) {
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
      scheduleFinished.write(resultJobInfo);
    }
  }

  @Override
  protected ChannelDataStore<BatchJobScheduleInfo> createInDataStore() {
    return new Buffer<BatchJobScheduleInfo>(
      new SetQueue<BatchJobScheduleInfo>());
  }

  /**
   * Get the batch job service used to interact with the database.
   * 
   * @return The batch job service used to interact with the database.
   */
  public BatchJobService getBatchJobService() {
    return batchJobService;
  }

  private Set<Long> getQueuedJobIds(final String businessApplicationName) {
    Set<Long> jobIds = queuedJobIdsByBusinessApplication.get(businessApplicationName);
    if (jobIds == null) {
      jobIds = new LinkedHashSet<Long>();
      queuedJobIdsByBusinessApplication.put(businessApplicationName, jobIds);
    }
    return jobIds;
  }

  private int getScheduledGroupCount(final String businessApplicationName) {
    final Integer count = scheduledGroupCountByBusinessApplication.get(businessApplicationName);
    if (count == null) {
      return 0;
    } else {
      return count;
    }
  }

  private Set<Long> getScheduledJobIds(final String businessApplicationName) {
    Set<Long> jobIds = scheduledJobIdsByBusinessApplication.get(businessApplicationName);
    if (jobIds == null) {
      jobIds = new LinkedHashSet<Long>();
      scheduledJobIdsByBusinessApplication.put(businessApplicationName, jobIds);
    }
    return jobIds;
  }

  public void groupFinished(final String businessApplicationName,
    final Long batchJobId, final BatchJobRequestExecutionGroup group) {
    final BatchJobScheduleInfo batchJobInfo = new BatchJobScheduleInfo(
      businessApplicationName, batchJobId);
    batchJobInfo.setGroup(group);
    SendToChannelAfterCommit.send(groupFinished, batchJobInfo);
  }

  @Override
  protected void init() {
    groupFinished = new Channel<BatchJobScheduleInfo>(getBeanName()
      + ".groupFinished", new Buffer<BatchJobScheduleInfo>());
    groupFinished.readConnect();
    groupFinished.writeConnect();
    scheduleFinished = new Channel<BatchJobScheduleInfo>(getBeanName()
      + ".scheduleFinished", new Buffer<BatchJobScheduleInfo>());
    scheduleFinished.readConnect();
    scheduleFinished.writeConnect();
    getIn();
  }

  private void queueGroup(final BatchJobRequestExecutionGroup group) {
    final Long batchJobId = group.getBatchJobId();
    Set<BatchJobRequestExecutionGroup> groups;
    synchronized (queuedFinishedGroupsByJobId) {
      groups = queuedFinishedGroupsByJobId.get(batchJobId);
      if (groups == null) {
        groups = new LinkedHashSet<BatchJobRequestExecutionGroup>();
        queuedFinishedGroupsByJobId.put(batchJobId, groups);
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
    final long timeout = this.timeout;
    final LoadJobIdsToScheduleFromDatabase loadJobIds = new LoadJobIdsToScheduleFromDatabase(
      batchJobService);
    final MultiInputSelector selector = new MultiInputSelector();
    @SuppressWarnings("unchecked")
    final List<Channel<BatchJobScheduleInfo>> channels = Arrays.asList(
      scheduleFinished, groupFinished, in);
    while (true) {
      try {
        final int index = selector.select(timeout, channels);
        if (index == -1) {
          if (!loadJobIds.isRunning()) {
            out.write(loadJobIds);
          }
        } else {
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
        return;
      } catch (final Throwable t) {
        LoggerFactory.getLogger(BatchJobScheduler.class).error("Error scheduling jobs", t);
      }
    }
  }

  private void scheduleQueuedGroups(final Channel<Runnable> out) {
    synchronized (queuedFinishedGroupsByJobId) {
      for (final Entry<Long, Set<BatchJobRequestExecutionGroup>> entry : queuedFinishedGroupsByJobId.entrySet()) {
        final Long batchJobId = entry.getKey();
        final Set<BatchJobRequestExecutionGroup> groups = entry.getValue();

        String businessApplicationName = null;
        if (groups != null && !groups.isEmpty()) {
          final BatchJobRequestExecutionGroup group = CollectionUtil.get(
            groups, 0);
          businessApplicationName = group.getBusinessApplicationName();
        }

        if (businessApplicationName != null) {
          final Runnable runnable = new InvokeMethodRunnable(this,
            "updateBatchJobCounts", businessApplicationName, batchJobId);
          out.write(runnable);
        }
      }
    }

  }

  private void scheduleQueuedJobs(final Channel<Runnable> out,
    final String businessApplicationName) {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
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
      queuedJobIdsByBusinessApplication.remove(businessApplicationName);
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
    Set<BatchJobRequestExecutionGroup> groups;
    synchronized (queuedFinishedGroupsByJobId) {
      groups = queuedFinishedGroupsByJobId.remove(batchJobId);
    }
    final BatchJobScheduleInfo resultJobInfo = new BatchJobScheduleInfo(
      businessApplicationName, batchJobId,
      BatchJobScheduleInfo.SCHEDULE_FINISHED);
    try {
      if (groups != null) {
        batchJobService.setBatchJobExecutingCounts(businessApplicationName,
          batchJobId, groups);
      }
    } finally {
      scheduleFinished.write(resultJobInfo);
    }
  }

}
