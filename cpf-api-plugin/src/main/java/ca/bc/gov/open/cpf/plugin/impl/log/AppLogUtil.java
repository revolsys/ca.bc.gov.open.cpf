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
package ca.bc.gov.open.cpf.plugin.impl.log;

import org.springframework.util.StopWatch;

import ca.bc.gov.open.cpf.plugin.api.log.AppLog;

import com.revolsys.transaction.Transaction;

public final class AppLogUtil {
  public static void info(final AppLog log, final String message, final StopWatch stopWatch) {
    if (log.isInfoEnabled()) {
      try {
        if (stopWatch.isRunning()) {
          stopWatch.stop();
        }
      } catch (final IllegalStateException e) {
      }
      final long time = stopWatch.getTotalTimeMillis();
      log.info(message + "\ttime=" + time / 1000.0);
    }
  }

  public static void infoAfterCommit(final AppLog log, final String message) {
    if (log != null && message != null) {
      Transaction.afterCommit(() -> log.info(message));
    }
  }

  public static void infoAfterCommit(final AppLog log, final String message,
    final StopWatch stopWatch) {
    if (log != null && stopWatch != null && message != null) {
      Transaction.afterCommit(() -> AppLogUtil.info(log, message, stopWatch));
    }
  }
}
