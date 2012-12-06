package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Arrays;
import java.util.List;

public class BatchJobScheduleInfo {

  public static final String SCHEDULE = "SCHEDULE";

  public static final String NO_GROUP_SCHEDULED = "NO_GROUP_SCHEDULED";

  public static final String SCHEDULE_FINISHED = "SCHEDULE_FINISHED";

  private final String businessApplicationName;

  private final Long batchJobId;

  private List<String> actions;

  private BatchJobRequestExecutionGroup group;

  public BatchJobScheduleInfo(final String businessApplicationName,
    final Long batchJobId, final String... actions) {
    this.businessApplicationName = businessApplicationName;
    this.batchJobId = batchJobId;
    setActions(actions);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof BatchJobScheduleInfo) {
      final BatchJobScheduleInfo jobInfo = (BatchJobScheduleInfo)obj;
      return batchJobId.equals(jobInfo.batchJobId);
    }
    return false;
  }

  public List<String> getActions() {
    return actions;
  }

  public Long getBatchJobId() {
    return batchJobId;
  }

  public String getBusinessApplicationName() {
    return businessApplicationName;
  }

  public BatchJobRequestExecutionGroup getGroup() {
    return group;
  }

  @Override
  public int hashCode() {
    return batchJobId.hashCode();
  }

  public void setActions(final String... actions) {
    this.actions = Arrays.asList(actions);
  }

  public void setGroup(final BatchJobRequestExecutionGroup group) {
    this.group = group;
  }
}
