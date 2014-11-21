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
      return this.batchJobId.equals(jobInfo.batchJobId);
    }
    return false;
  }

  public List<String> getActions() {
    return this.actions;
  }

  public Long getBatchJobId() {
    return this.batchJobId;
  }

  public String getBusinessApplicationName() {
    return this.businessApplicationName;
  }

  @Override
  public int hashCode() {
    return this.batchJobId.hashCode();
  }

  public void setActions(final String... actions) {
    this.actions = Arrays.asList(actions);
  }
}
