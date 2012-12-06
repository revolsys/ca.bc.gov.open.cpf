package ca.bc.gov.open.cpf.plugin.api.log;

import java.util.Map;

import org.springframework.util.StopWatch;

import ca.bc.gov.open.cpf.plugin.api.log.appender.ModuleLogAppender;
import ca.bc.gov.open.cpf.plugin.api.log.appender.Slf4jModuleLogAppender;

import com.revolsys.spring.InvokeMethodAfterCommit;

public final class ModuleLog {
  public static final String ERROR = "ERROR";

  public static final String INFO = "INFO";

  public static final String DEBUG = "DEBUG";

  private static ModuleLogAppender appender;

  public static void debug(
    final String moduleName,
    final String category,
    final String message,
    final long durationInMillis,
    final Map<String, ? extends Object> data) {
    final ModuleLogAppender appender = getAppender();
    appender.log(DEBUG, moduleName, category, message, durationInMillis, data);
  }

  public static void debug(
    final String moduleName,
    final String category,
    final String message,
    final Map<String, ? extends Object> data) {
    debug(moduleName, category, message, 0, data);
  }

  public static void debug(
    final String moduleName,
    final String category,
    final String message,
    final StopWatch stopWatch,
    final Map<String, ? extends Object> data) {
    if (stopWatch.isRunning()) {
      stopWatch.stop();
    }
    final long time = stopWatch.getTotalTimeMillis();
    debug(moduleName, category, message, time, data);
  }

  public static void debugAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final long durationInMillis,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "debug", moduleName,
      category, message, durationInMillis, data);
  }

  public static void debugAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "debug", moduleName,
      category, message, data);
  }

  public static void debugAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final StopWatch stopWatch,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "debug", moduleName,
      category, message, stopWatch, data);
  }

  public static void error(
    final String moduleName,
    final String category,
    final String message,
    final long durationInMillis,
    final Map<String, ? extends Object> data) {
    final ModuleLogAppender appender = getAppender();
    appender.log(ERROR, moduleName, category, message, durationInMillis, data);
  }

  public static void error(
    final String moduleName,
    final String category,
    final String message,
    final Map<String, ? extends Object> data) {
    error(moduleName, category, message, 0, data);
  }

  public static void error(
    final String moduleName,
    final String category,
    final String message,
    final StopWatch stopWatch,
    final Map<String, ? extends Object> data) {
    if (stopWatch.isRunning()) {
      stopWatch.stop();
    }
    final long time = stopWatch.getTotalTimeMillis();
    error(moduleName, category, message, time, data);
  }

  public static void errorAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final long durationInMillis,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "error", moduleName,
      category, message, durationInMillis, data);
  }

  public static void errorAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "error", moduleName,
      category, message, data);
  }

  public static void errorAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final StopWatch stopWatch,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "error", moduleName,
      category, message, stopWatch, data);
  }

  private static synchronized ModuleLogAppender getAppender() {
    if (appender == null) {
      appender = new Slf4jModuleLogAppender();
    }
    return appender;
  }

  public static void info(
    final String moduleName,
    final String category,
    final String message,
    final long durationInMillis,
    final Map<String, ? extends Object> data) {
    final ModuleLogAppender appender = getAppender();
    appender.log(INFO, moduleName, category, message, durationInMillis, data);
  }

  public static void info(
    final String moduleName,
    final String category,
    final String message,
    final Map<String, ? extends Object> data) {
    info(moduleName, category, message, 0, data);
  }

  public static void info(
    final String moduleName,
    final String category,
    final String message,
    final StopWatch stopWatch,
    final Map<String, ? extends Object> data) {
    if (stopWatch.isRunning()) {
      stopWatch.stop();
    }
    final long time = stopWatch.getTotalTimeMillis();
    info(moduleName, category, message, time, data);
  }

  public static void infoAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final long durationInMillis,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "info", moduleName,
      category, message, durationInMillis, data);
  }

  public static void infoAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "info", moduleName,
      category, message, data);
  }

  public static void infoAfterCommit(
    final String moduleName,
    final String category,
    final String message,
    final StopWatch stopWatch,
    final Map<String, ? extends Object> data) {
    InvokeMethodAfterCommit.invoke(ModuleLog.class, "info", moduleName,
      category, message, stopWatch, data);
  }

  public static synchronized void setAppender(final ModuleLogAppender appender) {
    ModuleLog.appender = appender;
  }

  private ModuleLog() {
  }
}
