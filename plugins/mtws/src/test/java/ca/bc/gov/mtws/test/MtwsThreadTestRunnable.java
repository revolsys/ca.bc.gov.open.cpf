package ca.bc.gov.mtws.test;

import java.util.Map;

import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationPluginExecutor;

public class MtwsThreadTestRunnable implements Runnable {
  private final MtwsThreadTest test;

  private final int index;

  public MtwsThreadTestRunnable(final MtwsThreadTest test, final int index) {
    this.test = test;
    this.index = index;
  }

  @Override
  public void run() {
    test.threadStarted();
    try {
      final BusinessApplicationPluginExecutor executor = test.getExecutor();
      final String businessApplicationName = test.getBusinessApplicationName();
      final int count = test.getIterationCount();
      for (int i = 0; i < count; i++) {
        final int requestSequenceNumber = index + count * i;
        final Map<String, Object> inputData = test.getTestData(requestSequenceNumber);
        Object results;
        if (executor.hasResultsList(businessApplicationName)) {
          results = executor.executeList(businessApplicationName, inputData);
        } else {
          results = executor.execute(businessApplicationName, inputData);
        }
        LoggerFactory.getLogger(getClass()).info(
          "Request " + requestSequenceNumber + "\n" + results);
      }
    } finally {
      test.threadStopped();
    }
  }

  @Override
  public String toString() {
    return String.valueOf(index);
  }
}
