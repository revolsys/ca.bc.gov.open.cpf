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

public class BatchJob extends DelegatingRecord implements Common {

  public static final String BATCH_JOB = "/CPF/CPF_BATCH_JOBS";

  public static final String BATCH_JOB_ID = "BATCH_JOB_ID";

  public static final String BUSINESS_APPLICATION_NAME = "BUSINESS_APPLICATION_NAME";

  public static final String BUSINESS_APPLICATION_PARAMS = "BUSINESS_APPLICATION_PARAMS";

  public static final String COMPLETED_TIMESTAMP = "COMPLETED_TIMESTAMP";

  public static final String COMPLETED_GROUP_RANGE = "COMPLETED_GROUP_RANGE";

  public static final String GROUP_SIZE = "GROUP_SIZE";

  public static final String INPUT_DATA_CONTENT_TYPE = "INPUT_DATA_CONTENT_TYPE";

  public static final String JOB_STATUS = "JOB_STATUS";

  public static final String LAST_SCHEDULED_TIMESTAMP = "LAST_SCHEDULED_TIMESTAMP";

  public static final String NOTIFICATION_URL = "NOTIFICATION_URL";

  public static final String NUM_COMPLETED_REQUESTS = "NUM_COMPLETED_REQUESTS";

  public static final String NUM_FAILED_REQUESTS = "NUM_FAILED_REQUESTS";

  public static final String NUM_SUBMITTED_GROUPS = "NUM_SUBMITTED_GROUPS";

  public static final String NUM_COMPLETED_GROUPS = "NUM_COMPLETED_GROUPS";

  public static final String NUM_SUBMITTED_REQUESTS = "NUM_SUBMITTED_REQUESTS";

  public static final String PROPERTIES = "PROPERTIES";

  public static final String RESULT_DATA_CONTENT_TYPE = "RESULT_DATA_CONTENT_TYPE";

  public static final String STRUCTURED_INPUT_DATA_URL = "STRUCTURED_INPUT_DATA_URL";

  public static final String USER_ID = "USER_ID";

  public static final String WHEN_STATUS_CHANGED = "WHEN_STATUS_CHANGED";

  private final RangeSet completedGroups;

  private final RangeSet scheduledGroups = new RangeSet();

  private RangeSet groupsToProcess;

  private final RangeSet failedRequests = new RangeSet();

  private final LinkedList<BatchJobRequestExecutionGroup> resheduledGroups = new LinkedList<>();

  private final Set<BatchJobRequestExecutionGroup> groups = new HashSet<>();

  public BatchJob(final Record record) {
    super(record);
    final String completedRangeSpec = record.getString(BatchJob.COMPLETED_GROUP_RANGE);
    this.completedGroups = RangeSet.create(completedRangeSpec);
    final int groupCount = record.getInteger(BatchJob.NUM_SUBMITTED_GROUPS);
    setGroupCount(groupCount);
    this.groupsToProcess.remove(this.completedGroups);
  }

  public synchronized void addCompletedGroup(final long groupSequenceNumber) {
    this.scheduledGroups.remove(groupSequenceNumber);
    this.completedGroups.add(groupSequenceNumber);
  }

  public synchronized void addFailedRequest(final long sequenceNumber) {
    this.failedRequests.add(sequenceNumber);
  }

  public String getCompletedGroups() {
    return this.completedGroups.toString();
  }

  public Set<BatchJobRequestExecutionGroup> getGroups() {
    return this.groups;
  }

  public String getGroupsToProcess() {
    return this.groupsToProcess.toString();
  }

  public synchronized BatchJobRequestExecutionGroup getNextGroup(
    final BusinessApplication businessApplication) {
    if (!this.resheduledGroups.isEmpty()) {
      return this.resheduledGroups.removeFirst();
    } else if (this.groupsToProcess.size() == 0) {
      return null;
    } else {
      final Object nextValue = this.groupsToProcess.removeFirst();
      if (nextValue instanceof Number) {
        final Integer sequenceNumber = ((Number)nextValue).intValue();
        this.scheduledGroups.add(sequenceNumber);

        final String userId = getValue(BatchJob.USER_ID);

        final Map<String, String> businessApplicationParameterMap = BatchJobService.getBusinessApplicationParameters(this);
        final String resultDataContentType = getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);

        final BatchJobRequestExecutionGroup group = new BatchJobRequestExecutionGroup(userId, this,
          businessApplication, businessApplicationParameterMap, resultDataContentType,
          new Timestamp(System.currentTimeMillis()), sequenceNumber);
        this.groups.add(group);
        return group;
      } else {
        return null;
      }
    }
  }

  public synchronized Integer getNextGroupId() {
    if (this.groupsToProcess.size() == 0) {
      return null;
    } else {
      final Object nextValue = this.groupsToProcess.removeFirst();
      if (nextValue instanceof Number) {
        final Integer groupId = ((Number)nextValue).intValue();
        this.scheduledGroups.add(groupId);
        return groupId;
      } else {
        return null;
      }
    }
  }

  public String getScheduledGroups() {
    return this.scheduledGroups.toString();
  }

  public synchronized boolean hasAvailableGroup() {
    return this.groupsToProcess.size() > 0;
  }

  public synchronized boolean isCompleted() {
    if (hasAvailableGroup()) {
      return false;
    } else if (this.scheduledGroups.size() > 0) {
      return false;
    } else {
      return true;
    }
  }

  public boolean isProcessing() {
    final String status = getValue(JOB_STATUS);
    if (status.equals(BatchJobStatus.REQUESTS_CREATED)) {
      return true;
    } else if (status.equals(BatchJobStatus.PROCESSING)) {
      return true;
    } else {
      return false;
    }
  }

  public synchronized void removeGroup(final BatchJobRequestExecutionGroup group) {
    this.groups.remove(group);
  }

  public synchronized void reset() {
    this.resheduledGroups.clear();
    this.groups.clear();
    for (final AbstractRange<?> range : this.scheduledGroups.getRanges()) {
      this.groupsToProcess.addRange(range);
    }
    this.scheduledGroups.clear();
  }

  public void rescheduleGroup(final BatchJobRequestExecutionGroup group) {
    this.resheduledGroups.add(group);
  }

  public synchronized void setGroupCount(final int groupCount) {
    setValue(BatchJob.NUM_SUBMITTED_GROUPS, groupCount);
    if (groupCount == 0) {
      this.groupsToProcess = new RangeSet();
    } else {
      this.groupsToProcess = RangeSet.create(1, groupCount);
    }
  }
}
