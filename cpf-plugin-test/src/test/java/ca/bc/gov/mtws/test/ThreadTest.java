/*
 * Copyright © 2008-2015, Province of British Columbia
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
package ca.bc.gov.mtws.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.revolsys.spring.resource.FileSystemResource;
import com.revolsys.spring.resource.Resource;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationPluginExecutor;

import com.revolsys.io.map.MapReader;
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
      testData = MapReader.newMapReader(inputDataResource).toList();
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
