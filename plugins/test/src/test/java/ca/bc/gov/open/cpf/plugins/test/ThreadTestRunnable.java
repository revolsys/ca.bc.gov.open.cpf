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
package ca.bc.gov.open.cpf.plugins.test;

import java.util.Map;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationPluginExecutor;

import com.revolsys.logging.Logs;

public class ThreadTestRunnable implements Runnable {
  private final ThreadTest test;

  private final int index;

  public ThreadTestRunnable(final ThreadTest test, final int index) {
    this.test = test;
    this.index = index;
  }

  @Override
  public void run() {
    this.test.threadStarted();
    try {
      final BusinessApplicationPluginExecutor executor = this.test.getExecutor();
      final String businessApplicationName = this.test.getBusinessApplicationName();
      final int count = this.test.getIterationCount();
      for (int i = 0; i < count; i++) {
        final int requestSequenceNumber = this.index + count * i;
        final Map<String, Object> inputData = this.test.getTestData(requestSequenceNumber);
        Object results;
        if (executor.hasResultsList(businessApplicationName)) {
          results = executor.executeList(businessApplicationName, inputData);
        } else {
          results = executor.execute(businessApplicationName, inputData);
        }
        Logs.info(this, "Request " + requestSequenceNumber + "\n" + results);
      }
    } finally {
      this.test.threadStopped();
    }
  }

  @Override
  public String toString() {
    return String.valueOf(this.index);
  }
}
