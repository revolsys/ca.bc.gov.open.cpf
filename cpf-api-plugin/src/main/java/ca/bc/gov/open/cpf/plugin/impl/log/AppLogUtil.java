package ca.bc.gov.open.cpf.plugin.impl.log;

import org.springframework.util.StopWatch;

import ca.bc.gov.open.cpf.plugin.api.log.AppLog;

import com.revolsys.spring.InvokeMethodAfterCommit;

public final class AppLogUtil {
  public static void info(final AppLog log, final String message,
    final StopWatch stopWatch) {
    if (log.isInfoEnabled()) {
      try {
        if (stopWatch.isRunning()) {
          stopWatch.stop();
        }
      } catch (final IllegalStateException e) {
      }
      final long time = stopWatch.getTotalTimeMillis();
      log.info(message + ", time=" + time);
    }
  }

  public static void infoAfterCommit(final AppLog log, final String message) {
    if (log != null && message != null) {
      InvokeMethodAfterCommit.invoke(log, "info", message);
    }
  }

  public static void infoAfterCommit(final AppLog log, final String message,
    final StopWatch stopWatch) {
    if (log != null && stopWatch != null && message != null) {
      InvokeMethodAfterCommit.invoke(AppLogUtil.class, "info", log, message,
        stopWatch);
    }
  }
}
