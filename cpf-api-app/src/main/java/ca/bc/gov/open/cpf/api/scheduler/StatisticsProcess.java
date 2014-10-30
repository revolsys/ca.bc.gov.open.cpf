package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;

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

  @Override
  @PreDestroy
  protected void destroy() {
    this.batchJobService.saveAllStatistics();
  }

  @Override
  protected void preRun(final Channel<Map<String, ? extends Object>> in) {
    super.preRun(in);
    this.batchJobService.collateAllStatistics();
  }

  @Override
  protected void process(final Channel<Map<String, ? extends Object>> in,
    final Map<String, ? extends Object> values) {
    try {
      if (Boolean.TRUE == values.get(COLLATE)) {
        this.batchJobService.collateAllStatistics();
      } else if (Boolean.TRUE == values.get(SAVE)) {
        @SuppressWarnings("unchecked")
        final List<String> businessApplicationNames = (List<String>)values.get("businessApplicationNames");
        this.batchJobService.saveStatistics(businessApplicationNames);
      } else {
        final Date time = (Date)values.get("time");
        final String businessApplicationName = (String)values.get("businessApplicationName");
        for (final String durationType : BusinessApplicationStatistics.DURATION_TYPES) {
          final String statisticsId = BusinessApplicationStatistics.getId(
            durationType, time);
          final BusinessApplicationStatistics statistics = this.batchJobService.getStatistics(
            businessApplicationName, statisticsId);
          statistics.addStatistics(values);
        }
      }
    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error(
        "Unable to save statistics:" + values, e);

    }
  }

  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
    batchJobService.setStatisticsProcess(this);
  }
}
