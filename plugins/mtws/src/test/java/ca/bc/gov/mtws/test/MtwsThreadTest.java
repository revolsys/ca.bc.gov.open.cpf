package ca.bc.gov.mtws.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.revolsys.spring.resource.FileSystemResource;
import com.revolsys.spring.resource.Resource;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationPluginExecutor;

import com.revolsys.io.map.MapReader;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.store.Buffer;

public class MtwsThreadTest {
  private static final String BUSINESS_APPLICATION_NAME = "MapTileByTileId";

  private static final int ITERATION_COUNT = 10;

  private static final int NUM_THREADS = 10;

  private static final Resource inputDataResource = new FileSystemResource(
    "src/data/NTS-250000-by-name.csv");

  public static void main(final String[] args) {
    final MtwsThreadTest test = new MtwsThreadTest();
    test.run();
  }

  private BusinessApplicationPluginExecutor executor = new BusinessApplicationPluginExecutor();

  private final Channel<Boolean> startChannel = new Channel<Boolean>(
    new Buffer<Boolean>(NUM_THREADS));

  private final Object startSync = new Object();

  private final Channel<Boolean> stopChannel = new Channel<Boolean>(
    new Buffer<Boolean>(NUM_THREADS));

  private List<Map<String, Object>> testData = new ArrayList<Map<String, Object>>();

  public MtwsThreadTest() {
    executor.setTestModeEnabled("MapTileByTileId", Boolean.TRUE);
  }

  public String getBusinessApplicationName() {
    return BUSINESS_APPLICATION_NAME;
  }

  public BusinessApplicationPluginExecutor getExecutor() {
    return executor;
  }

  public int getIterationCount() {
    return ITERATION_COUNT;
  }

  public Map<String, Object> getTestData(final int index) {
    final int i = index % testData.size();
    return testData.get(i);
  }

  public void run() {
    testData = MapReader.newMapReader(inputDataResource).toList();
    for (int i = 0; i < NUM_THREADS; i++) {
      final MtwsThreadTestRunnable runnable = new MtwsThreadTestRunnable(this,
        i);
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
    for (int i = 0; i < NUM_THREADS; i++) {
      startChannel.read();
    }
    synchronized (startSync) {
      startSync.notifyAll();
    }
  }

  protected void waitForAllThreadsToStop() {
    for (int i = 0; i < NUM_THREADS; i++) {
      stopChannel.read();
    }
  }
}
