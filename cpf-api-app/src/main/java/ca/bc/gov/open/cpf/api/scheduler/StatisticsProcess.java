package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInProcess;

public class StatisticsProcess extends
  BaseInProcess<Map<String, ? extends Object>> {

  public static final String COLLATE = "COLLATE";

  public static final String SAVE = "SAVE";

  private BatchJobService batchJobService;

  public StatisticsProcess() {
    setInBufferSize(100000);
  }

  public void setBatchJobService(BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
    batchJobService.setStatisticsProcess(this);
  }

  @Override
  protected void preRun(Channel<Map<String, ? extends Object>> in) {
    super.preRun(in);
    batchJobService.collateAllStatistics();
  }

  @Override
  protected void process(Channel<Map<String, ? extends Object>> in,
    Map<String, ? extends Object> values) {
    if (Boolean.TRUE == values.get(COLLATE)) {
      batchJobService.collateAllStatistics();
    } else if (Boolean.TRUE == values.get(SAVE)) {
      @SuppressWarnings("unchecked")
      List<String> businessApplicationNames = (List<String>)values.get("businessApplicationNames");
      batchJobService.saveStatistics(businessApplicationNames);
    } else {
      Date time = (Date)values.get("time");
      String businessApplicationName = (String)values.get("businessApplicationName");
      for (String durationType : BusinessApplicationStatistics.DURATION_TYPES) {
        String statisticsId = BusinessApplicationStatistics.getId(durationType,
          time);
        BusinessApplicationStatistics statistics = batchJobService.getStatistics(
          businessApplicationName, statisticsId);
        statistics.addStatistics(values);
      }
    }
  }

  @PreDestroy
  protected void destroy() {
    batchJobService.saveAllStatistics();
  }
}
