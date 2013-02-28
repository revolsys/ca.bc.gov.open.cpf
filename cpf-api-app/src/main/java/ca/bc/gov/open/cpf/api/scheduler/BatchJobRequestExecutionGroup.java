package ca.bc.gov.open.cpf.api.scheduler;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

public class BatchJobRequestExecutionGroup {
  private final long batchJobId;

  private final List<Long> batchJobRequestIds = new ArrayList<Long>();

  private final BusinessApplication businessApplication;

  private final Map<String, String> businessApplicationParameterMap;

  private long executionStartTime;

  private String id;

  private final String resultDataContentType;

  private final Timestamp scheduleTimestamp;

  private final String consumerKey;

  private final String moduleName;

  private int numCompletedRequests;

  private int numFailedRequests;

  public BatchJobRequestExecutionGroup(final String consumerKey,
    final long batchJobId, final BusinessApplication businessApplication,
    final Map<String, String> businessApplicationParameterMap,
    final String resultDataContentType, final Timestamp scheduleTimestamp) {
    this.consumerKey = consumerKey;
    this.batchJobId = batchJobId;
    this.businessApplication = businessApplication;
    this.moduleName = businessApplication.getModule().getName();
    this.businessApplicationParameterMap = businessApplicationParameterMap;
    this.resultDataContentType = resultDataContentType;
    this.scheduleTimestamp = scheduleTimestamp;
    resetId();
  }

  public void addBatchJobRequestId(final long batchJobRequestId) {
    batchJobRequestIds.add(batchJobRequestId);
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof BatchJobRequestExecutionGroup) {
      final BatchJobRequestExecutionGroup group = (BatchJobRequestExecutionGroup)other;
      return group.id.equals(id);
    } else {
      return false;
    }
  }

  public long getBatchJobId() {
    return batchJobId;
  }

  public List<Long> getBatchJobRequestIds() {
    return batchJobRequestIds;
  }

  public BusinessApplication getBusinessApplication() {
    return businessApplication;
  }

  public String getBusinessApplicationName() {
    return businessApplication.getName();
  }

  public Map<String, String> getBusinessApplicationParameterMap() {
    return businessApplicationParameterMap;
  }

  public String getconsumerKey() {
    return consumerKey;
  }

  public long getExecutionStartTime() {
    return executionStartTime;
  }

  public String getId() {
    return id;
  }

  public Module getModule() {
    return businessApplication.getModule();
  }

  public String getModuleName() {
    return moduleName;
  }

  public int getNumBatchJobRequests() {
    return batchJobRequestIds.size();
  }

  public int getNumCompletedRequests() {
    return numCompletedRequests;
  }

  public int getNumFailedRequests() {
    return numFailedRequests;
  }

  public String getResultDataContentType() {
    return resultDataContentType;
  }

  public Timestamp getScheduleTimestamp() {
    return scheduleTimestamp;
  }

  public Timestamp getStartedTimestamp() {
    return scheduleTimestamp;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public void resetId() {
    id = UUID.randomUUID().toString();
  }

  public void setExecutionStartTime(final long executionStartTime) {
    this.executionStartTime = executionStartTime;
  }

  public void setNumCompletedRequests(final int numCompletedRequests) {
    this.numCompletedRequests = numCompletedRequests;
  }

  public void setNumFailedRequests(final int numFailedRequests) {
    this.numFailedRequests = numFailedRequests;
  }

  @Override
  public String toString() {
    return id + "(" + String.valueOf(batchJobId) + ")";
  }
}
