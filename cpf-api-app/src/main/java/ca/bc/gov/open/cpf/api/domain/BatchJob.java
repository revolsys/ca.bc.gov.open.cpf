package ca.bc.gov.open.cpf.api.domain;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.collection.range.AbstractRange;
import com.revolsys.collection.range.RangeSet;
import com.revolsys.data.record.DelegatingRecord;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.RecordState;
import com.revolsys.format.json.Json;
import com.revolsys.io.PathName;

public class BatchJob extends DelegatingRecord implements Common {

  public static final PathName BATCH_JOB = PathName.create("/CPF/CPF_BATCH_JOBS");

  public static final String BATCH_JOB_ID = "BATCH_JOB_ID";

  public static final String BUSINESS_APPLICATION_NAME = "BUSINESS_APPLICATION_NAME";

  public static final String BUSINESS_APPLICATION_PARAMS = "BUSINESS_APPLICATION_PARAMS";

  public static final String COMPLETED_GROUP_RANGE = "COMPLETED_GROUP_RANGE";

  public static final String COMPLETED_REQUEST_RANGE = "COMPLETED_REQUEST_RANGE";

  public static final String COMPLETED_TIMESTAMP = "COMPLETED_TIMESTAMP";

  public static final String GROUP_SIZE = "GROUP_SIZE";

  public static final String INPUT_DATA_CONTENT_TYPE = "INPUT_DATA_CONTENT_TYPE";

  public static final String JOB_STATUS = "JOB_STATUS";

  public static final String LAST_SCHEDULED_TIMESTAMP = "LAST_SCHEDULED_TIMESTAMP";

  public static final String NOTIFICATION_URL = "NOTIFICATION_URL";

  public static final String FAILED_REQUEST_RANGE = "FAILED_REQUEST_RANGE";

  public static final String NUM_SUBMITTED_GROUPS = "NUM_SUBMITTED_GROUPS";

  public static final String NUM_SUBMITTED_REQUESTS = "NUM_SUBMITTED_REQUESTS";

  public static final String PROPERTIES = "PROPERTIES";

  public static final String RESULT_DATA_CONTENT_TYPE = "RESULT_DATA_CONTENT_TYPE";

  public static final String STRUCTURED_INPUT_DATA_URL = "STRUCTURED_INPUT_DATA_URL";

  public static final String USER_ID = "USER_ID";

  public static final String WHEN_STATUS_CHANGED = "WHEN_STATUS_CHANGED";

  private final RangeSet completedGroups;

  private final RangeSet completedRequests;

  private final RangeSet failedRequests;

  private final Set<BatchJobRequestExecutionGroup> groups = new HashSet<>();

  private final RangeSet groupsToProcess = new RangeSet();

  private final LinkedList<BatchJobRequestExecutionGroup> resheduledGroups = new LinkedList<>();

  private final RangeSet scheduledGroups = new RangeSet();

  public BatchJob(final Record record) {
    super(record);
    final String completedGroupRange = record.getString(BatchJob.COMPLETED_GROUP_RANGE);
    this.completedGroups = RangeSet.create(completedGroupRange);
    final String completedRequestsRange = record.getString(BatchJob.COMPLETED_REQUEST_RANGE);
    this.completedRequests = RangeSet.create(completedRequestsRange);
    final String failedRequestsRange = record.getString(BatchJob.FAILED_REQUEST_RANGE);
    this.failedRequests = RangeSet.create(failedRequestsRange);
    final int groupCount = record.getInteger(BatchJob.NUM_SUBMITTED_GROUPS);
    setGroupCount(groupCount);
    this.groupsToProcess.remove(this.completedGroups);
  }

  public void addCompletedGroup(final long groupSequenceNumber) {
    synchronized (this.scheduledGroups) {
      this.scheduledGroups.remove(groupSequenceNumber);
    }
    synchronized (this.completedGroups) {
      this.completedGroups.add(groupSequenceNumber);
    }
  }

  public RangeSet addCompletedRequests(final String range) {
    final RangeSet rangeSet = RangeSet.create(range);
    synchronized (this.completedRequests) {
      this.completedRequests.addRanges(rangeSet);
    }
    return rangeSet;
  }

  public RangeSet addFailedRequests(final String range) {
    final RangeSet rangeSet = RangeSet.create(range);
    synchronized (this.failedRequests) {
      this.failedRequests.addRanges(rangeSet);
    }
    return rangeSet;
  }

  public Map<String, String> getBusinessApplicationParameters() {
    final String jobParameters = getValue(BatchJob.BUSINESS_APPLICATION_PARAMS);
    final Map<String, String> parameters = Json.toMap(jobParameters);
    return parameters;
  }

  public String getCompletedGroups() {
    return this.completedGroups.toString();
  }

  public String getCompletedRequests() {
    return this.completedRequests.toString();
  }

  public String getFailedRequests() {
    return this.failedRequests.toString();
  }

  public Set<BatchJobRequestExecutionGroup> getGroups() {
    return this.groups;
  }

  public String getGroupsToProcess() {
    return this.groupsToProcess.toString();
  }

  public BatchJobRequestExecutionGroup getNextGroup(final BusinessApplication businessApplication) {
    synchronized (this.resheduledGroups) {
      if (!this.resheduledGroups.isEmpty()) {
        return this.resheduledGroups.removeFirst();
      }
    }
    final Object nextValue;
    synchronized (this.groupsToProcess) {
      if (this.groupsToProcess.size() == 0) {
        nextValue = null;
      } else {
        nextValue = this.groupsToProcess.removeFirst();
      }
    }
    if (nextValue instanceof Number) {
      final Integer sequenceNumber = ((Number)nextValue).intValue();
      synchronized (this.scheduledGroups) {
        this.scheduledGroups.add(sequenceNumber);
      }

      final String userId = getValue(BatchJob.USER_ID);

      final Map<String, String> businessApplicationParameterMap = BatchJobService
        .getBusinessApplicationParameters(this);
      final String resultDataContentType = getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

      final Timestamp now = new Timestamp(System.currentTimeMillis());
      setValue(BatchJob.LAST_SCHEDULED_TIMESTAMP, now);
      final BatchJobRequestExecutionGroup group = new BatchJobRequestExecutionGroup(userId, this,
        businessApplication, businessApplicationParameterMap, resultDataContentType, now,
        sequenceNumber);
      synchronized (this.groups) {
        this.groups.add(group);
      }
      return group;
    } else {
      return null;
    }
  }

  public int getNumCompletedRequests() {
    synchronized (this.completedRequests) {
      return this.completedRequests.size();
    }
  }

  public int getNumFailedRequests() {
    synchronized (this.failedRequests) {
      return this.failedRequests.size();
    }
  }

  public int getNumSubmittedGroups() {
    return getValue(NUM_SUBMITTED_GROUPS);
  }

  public String getScheduledGroups() {
    return this.scheduledGroups.toString();
  }

  public boolean hasAvailableGroup() {
    return this.groupsToProcess.size() > 0;
  }

  public boolean isCompleted() {
    if (hasAvailableGroup()) {
      return false;
    } else if (this.scheduledGroups.size() > 0) {
      return false;
    } else {
      return true;
    }
  }

  public boolean isCompleted(final int sequenceNumber) {
    return this.completedGroups.contains(sequenceNumber);
  }

  public boolean isProcessing() {
    final String status = getValue(JOB_STATUS);
    if (status.equals(BatchJobStatus.PROCESSING)) {
      return true;
    } else {
      return false;
    }
  }

  public void removeGroup(final BatchJobRequestExecutionGroup group) {
    synchronized (this.groups) {
      this.groups.remove(group);
    }
  }

  public void rescheduleGroup(final BatchJobRequestExecutionGroup group) {
    this.resheduledGroups.add(group);
  }

  public void reset() {
    synchronized (this.resheduledGroups) {
      this.resheduledGroups.clear();
    }
    synchronized (this.groups) {
      this.groups.clear();
    }
    synchronized (this.groupsToProcess) {
      synchronized (this.scheduledGroups) {
        for (final AbstractRange<?> range : this.scheduledGroups.getRanges()) {
          this.groupsToProcess.addRange(range);
        }
        this.scheduledGroups.clear();
      }
    }
  }

  public void setGroupCount(final int groupCount) {
    setValue(BatchJob.NUM_SUBMITTED_GROUPS, groupCount);
    synchronized (this.groupsToProcess) {
      this.groupsToProcess.clear();
      if (groupCount != 0) {
        this.groupsToProcess.addRange(1, groupCount);
      }
    }
  }

  @Override
  public String toString() {
    return getIdentifier().toString();
  }

  public synchronized void update() {
    synchronized (this.completedGroups) {
      setValue(COMPLETED_GROUP_RANGE, this.completedGroups.toString());
    }
    synchronized (this.failedRequests) {
      setValue(FAILED_REQUEST_RANGE, this.failedRequests.toString());
    }
    synchronized (this.completedRequests) {
      setValue(COMPLETED_REQUEST_RANGE, this.completedRequests.toString());
    }
    if (getState() == RecordState.Modified) {
      getRecordDefinition().getRecordStore().update(this);
    }

  }
}
