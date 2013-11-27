package ca.bc.gov.mtws.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationPluginExecutor;

import com.revolsys.io.AbstractMapReaderFactory;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.store.Buffer;

public class ThreadTest {
  public static void main(final String[] args) {
    final ThreadTest test = new ThreadTest();
    test.run();
  }

  private String businessApplicationName = "TestAllDataTypes";

  private final int iterationCount = 1;

  private final int numThreads = 10;

  private final Resource inputDataResource = new FileSystemResource(
    "src/data/NTS-250000-by-name.csv");

  private BusinessApplicationPluginExecutor executor = new BusinessApplicationPluginExecutor();

  private final Channel<Boolean> startChannel = new Channel<Boolean>(
    new Buffer<Boolean>(numThreads));

  private final Object startSync = new Object();

  private final Channel<Boolean> stopChannel = new Channel<Boolean>(
    new Buffer<Boolean>(numThreads));

  private List<Map<String, Object>> testData = new ArrayList<Map<String, Object>>();

  public ThreadTest() {
    executor.setTestModeEnabled(businessApplicationName, Boolean.TRUE);
  }

  public String getBusinessApplicationName() {
    return businessApplicationName;
  }

  public BusinessApplicationPluginExecutor getExecutor() {
    return executor;
  }

  public int getIterationCount() {
    return iterationCount;
  }

  public Map<String, Object> getTestData(final int index) {
    final int i = index % testData.size();
    return testData.get(i);
  }

  public void run() {
    if (inputDataResource.exists()) {
      testData = AbstractMapReaderFactory.mapReader(inputDataResource).read();
    } else {
      testData.add(Collections.<String, Object> emptyMap());
    }
    for (int i = 0; i < numThreads; i++) {
      final ThreadTestRunnable runnable = new ThreadTestRunnable(this, i);
      final Thread thread = new Thread(runnable, "Runner " + i);
      thread.start();
    }
    waitForAllThreadsToStart();
    waitForAllThreadsToStop();
    executor.close();
    executor = null;
    testData.clear();
    System.gc();
    System.gc();
    System.gc();
    System.gc();
    System.gc();
    synchronized (this) {
      try {
        wait();
      } catch (final InterruptedException e) {
      }
    }
  }

  public void setBusinessApplicationName(final String businessApplicationName) {
    this.businessApplicationName = businessApplicationName;
  }

  public void threadStarted() {
    startChannel.write(true);
    synchronized (startSync) {
      try {
        startSync.wait();
      } catch (final InterruptedException e) {
      }
    }
  }

  public void threadStopped() {
    stopChannel.write(true);
  }

  protected void waitForAllThreadsToStart() {
    for (int i = 0; i < numThreads; i++) {
      startChannel.read();
    }
    synchronized (startSync) {
      startSync.notifyAll();
    }
  }

  protected void waitForAllThreadsToStop() {
    for (int i = 0; i < numThreads; i++) {
      stopChannel.read();
    }
  }
}
