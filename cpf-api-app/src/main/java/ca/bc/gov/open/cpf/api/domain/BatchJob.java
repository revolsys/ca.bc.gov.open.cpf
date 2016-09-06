/*
 * Copyright © 2008-2016, Province of British Columbia
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
import com.revolsys.datatype.DataType;
import com.revolsys.identifier.Identifier;
import com.revolsys.io.PathName;
import com.revolsys.record.DelegatingRecord;
import com.revolsys.record.Record;
import com.revolsys.record.RecordState;
import com.revolsys.record.io.format.json.Json;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;

public class BatchJob extends DelegatingRecord implements Common {
  public static final PathName BATCH_JOB = PathName.newPathName("/CPF/CPF_BATCH_JOBS");

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
    final String completedGroupRange = record.getString(COMPLETED_GROUP_RANGE);
    this.completedGroups = RangeSet.newRangeSet(completedGroupRange);
    final String completedRequestsRange = record.getString(COMPLETED_REQUEST_RANGE);
    this.completedRequests = RangeSet.newRangeSet(completedRequestsRange);
    final String failedRequestsRange = record.getString(FAILED_REQUEST_RANGE);
    this.failedRequests = RangeSet.newRangeSet(failedRequestsRange);
    final int groupCount = record.getInteger(NUM_SUBMITTED_GROUPS);
    setGroupCount(groupCount);
    this.groupsToProcess.remove(this.completedGroups);
  }

  public synchronized void addCompletedGroup(final long groupSequenceNumber) {
    if (!isCancelled()) {
      this.scheduledGroups.remove(groupSequenceNumber);
      this.completedGroups.add(groupSequenceNumber);
    }
  }

  public synchronized RangeSet addCompletedRequests(final String range) {
    final RangeSet rangeSet = RangeSet.newRangeSet(range);
    if (!isCancelled()) {
      this.completedRequests.addRanges(rangeSet);
    }
    return rangeSet;
  }

  public synchronized RangeSet addFailedRequests(final String range) {
    final RangeSet rangeSet = RangeSet.newRangeSet(range);
    if (!isCancelled()) {
      this.failedRequests.addRanges(rangeSet);
    }
    return rangeSet;
  }

  public synchronized boolean cancelJob(final BatchJobService batchJobService) {
    final boolean cancelled = !isCancelled();
    final Identifier batchJobId = getIdentifier();
    batchJobService.getDataAccessObject().clearBatchJob(batchJobId);
    setStatus(batchJobService, BatchJobStatus.CANCELLED);
    this.completedRequests.clear();
    this.completedGroups.clear();
    final int numSubmittedRequests = getInteger(NUM_SUBMITTED_REQUESTS, 0);
    if (numSubmittedRequests == 0) {
      this.failedRequests.clear();
    } else {
      this.failedRequests.addRange(1, numSubmittedRequests);
    }
    this.groups.clear();
    this.groupsToProcess.clear();
    this.resheduledGroups.clear();
    this.scheduledGroups.clear();
    update();
    return cancelled;
  }

  public void cancelScheduledGroup(final long groupSequenceNumber) {
    if (this.scheduledGroups.contains(groupSequenceNumber)) {
      this.groupsToProcess.add(groupSequenceNumber);
      this.scheduledGroups.remove(groupSequenceNumber);
    }
  }

  public Map<String, String> getBusinessApplicationParameters() {
    final String jobParameters = getString(BUSINESS_APPLICATION_PARAMS);
    final Map<String, String> parameters = Json.toMap(jobParameters);
    return parameters;
  }

  public int getCompletedCount() {
    return this.completedRequests.size();
  }

  public String getCompletedGroups() {
    return this.completedGroups.toString();
  }

  public String getCompletedRequests() {
    return this.completedRequests.toString();
  }

  public int getFailedCount() {
    return this.failedRequests.size();
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

  public synchronized BatchJobRequestExecutionGroup getNextGroup(
    final BusinessApplication businessApplication) {
    if (isCancelled()) {
      return null;
    } else if (!this.resheduledGroups.isEmpty()) {
      return this.resheduledGroups.removeFirst();
    }
    final Object nextValue;
    if (this.groupsToProcess.size() == 0) {
      nextValue = null;
    } else {
      nextValue = this.groupsToProcess.removeFirst();
    }
    if (nextValue instanceof Number) {
      final Integer sequenceNumber = ((Number)nextValue).intValue();
      this.scheduledGroups.add(sequenceNumber);

      final String userId = getString(USER_ID);

      final Map<String, String> businessApplicationParameterMap = this
        .getBusinessApplicationParameters();
      final String resultDataContentType = getString(RESULT_DATA_CONTENT_TYPE);

      final Timestamp now = new Timestamp(System.currentTimeMillis());
      setValue(LAST_SCHEDULED_TIMESTAMP, now);
      final BatchJobRequestExecutionGroup group = new BatchJobRequestExecutionGroup(userId, this,
        businessApplication, businessApplicationParameterMap, resultDataContentType, now,
        sequenceNumber);
      this.groups.add(group);
      return group;
    } else {
      return null;
    }
  }

  public int getNumCompletedRequests() {
    return this.completedRequests.size();
  }

  public int getNumFailedRequests() {
    return this.failedRequests.size();
  }

  public int getNumSubmittedGroups() {
    return getInteger(NUM_SUBMITTED_GROUPS, 0);
  }

  public String getScheduledGroups() {
    return this.scheduledGroups.toString();
  }

  public String getStatus() {
    return getString(JOB_STATUS);
  }

  public boolean hasAvailableGroup() {
    return this.groupsToProcess.size() > 0;
  }

  public boolean isCancelled() {
    final String status = getStatus();
    return status.equals(BatchJobStatus.CANCELLED);
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
    final String status = getStatus();
    return status.equals(BatchJobStatus.PROCESSING);
  }

  public boolean isStatus(final String status) {
    final String jobStatus = getStatus();
    return jobStatus.equals(jobStatus);
  }

  public synchronized void removeGroup(final BatchJobRequestExecutionGroup group) {
    this.groups.remove(group);
  }

  public void rescheduleGroup(final BatchJobRequestExecutionGroup group) {
    this.resheduledGroups.add(group);
  }

  public synchronized void reset() {
    this.resheduledGroups.clear();
    this.groups.clear();
    for (final AbstractRange<?> range : this.scheduledGroups.getRanges()) {
      this.groupsToProcess.addRange(range);
    }
    this.scheduledGroups.clear();
  }

  public synchronized void setGroupCount(final int groupCount) {
    setValue(NUM_SUBMITTED_GROUPS, groupCount);
    this.groupsToProcess.clear();
    if (groupCount != 0) {
      this.groupsToProcess.addRange(1, groupCount);
    }
  }

  public synchronized void setStatus(final BatchJobService batchJobService,
    final String jobStatus) {
    final long time = System.currentTimeMillis();
    setStatus(batchJobService, jobStatus, time);
  }

  public void setStatus(final BatchJobService batchJobService, final String jobStatus,
    final long time) {
    final Timestamp timestamp = new Timestamp(time);
    setValue(JOB_STATUS, jobStatus);
    final String username = CpfDataAccessObject.getUsername();
    setValue(WHEN_STATUS_CHANGED, timestamp);
    setValue(Common.WHEN_UPDATED, timestamp);
    setValue(Common.WHO_UPDATED, username);

    final RecordStore recordStore = batchJobService.getRecordStore();
    final Record batchJobStatusChange = recordStore
      .newRecord(BatchJobStatusChange.BATCH_JOB_STATUS_CHANGE);
    batchJobStatusChange.setValue(BatchJobStatusChange.BATCH_JOB_ID, this, BATCH_JOB_ID);
    batchJobStatusChange.setValue(BatchJobStatusChange.JOB_STATUS, jobStatus);
    batchJobStatusChange.setValue(Common.WHEN_CREATED, timestamp);
    batchJobStatusChange.setValue(Common.WHO_UPDATED, username);
    final CpfDataAccessObject dataAccessObject = batchJobService.getDataAccessObject();
    dataAccessObject.write(batchJobStatusChange);
  }

  public synchronized boolean setStatus(final BatchJobService batchJobService,
    final String oldJobStatus, final String newJobStatus) {
    try (
      Transaction transaction = batchJobService.newTransaction(Propagation.REQUIRED)) {
      final String jobStatus = getStatus();
      if (DataType.equal(jobStatus, oldJobStatus)) {
        setStatus(batchJobService, newJobStatus);
        update();
        return true;
      } else {
        return false;
      }
    }
  }

  @Override
  public String toString() {
    return getIdentifier().toString();
  }

  public synchronized void update() {
    setValue(COMPLETED_GROUP_RANGE, this.completedGroups.toString());
    setValue(FAILED_REQUEST_RANGE, this.failedRequests.toString());
    setValue(COMPLETED_REQUEST_RANGE, this.completedRequests.toString());
    final RecordState state = getState();
    if (state == RecordState.MODIFIED) {
      final RecordStore recordStore = getRecordStore();
      recordStore.updateRecord(this);
    } else if (state == RecordState.NEW) {
      final RecordStore recordStore = getRecordStore();
      recordStore.insertRecord(this);
    }
  }
}
