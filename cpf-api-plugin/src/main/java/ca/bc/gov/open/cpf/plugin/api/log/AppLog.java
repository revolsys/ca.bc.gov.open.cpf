package ca.bc.gov.open.cpf.plugin.api.log;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;

import com.revolsys.io.csv.CsvMapWriter;
import com.revolsys.util.ExceptionUtil;

/**
 * <p>The AppLog class is a logging API for use by a {@link BusinessApplicationPlugin} class.
 * Plug-in classes can record error, info and debug messages in the execute method. These messages
 * will be recorded in the Log4j log file (if that log level is enabled in the worker for the AppLog
 * class). The message will also be recorded in the module log file on the master if that log level
 * is enabled for the business application. This functionality allows viewing the logs for the
 * all the workers from the CPF admin console.</p>
 * 
 * <p>The plug-in must implement the following method on the {@link BusinessApplicationPlugin} class
 * to obtain a AppLog instance for this request.</p>
 * 
 * <pre class="prettyprint language-java">private AppLog appLog;

public void setAppLog(final AppLog appLog) {
  this.appLog = appLog;
}
</pre> 
 *
 */
public class AppLog {
  /** The date format for the log records. */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS");

  /** The local log instance. */
  private static final Logger LOG = LoggerFactory.getLogger(AppLog.class);

  /** The logging level (ERROR, INFO, DEBUG). */
  private String logLevel = "ERROR";

  /** The log records. */
  private final List<Map<String, String>> logRecords = new ArrayList<Map<String, String>>();

  /**
   * <p>Construct a new AppLog with the logLevel ERROR.</p>
   */
  public AppLog() {
  }

  /**
   * <p>Construct a new AppLog with the logLevel.</p>
   * 
   * @param logLevel The logging level (ERROR, INFO, DEBUG).
   */
  public AppLog(final String logLevel) {
    setLogLevel(logLevel);
  }

  /**
   * <p>Record the info message in the log if {@see #isInfoEnabled()} is true.</p>
   * 
   * @param message The message.
   */
  public void debug(final String message) {
    if (isDebugEnabled()) {
      LOG.debug(message);
      log("DEBUG", message);
    }
  }
  
  /**
   * <p>Record the error message in the log.</p>
   * 
   * @param message The message.
   */
  public void error(final String message) {
    LOG.error(message);
    log("ERROR", message);
  }

  /**
   * <p>Record the error message in the log with the exception.</p>
   * 
   * @param message The message.
   */
  public void error(final String message, final Throwable exception) {
    LOG.error(message, exception);
    if (exception == null) {
      error(message);
    } else {
      log("ERROR", message + "\n" + ExceptionUtil.toString(exception));
    }
  }

  /**
   * <p>Get the log records as a CSV encoded string.</p>
   * 
   * @return The log records as a CSV encoded string.
   */
  public String getLogContent() {
    final StringWriter out = new StringWriter();
    final CsvMapWriter writer = new CsvMapWriter(out);
    for (final Map<String, String> logRecord : logRecords) {
      writer.write(logRecord);
    }
    writer.close();

    return out.toString();
  }

  /**
   * <p>Get the logging level (ERROR, INFO, DEBUG).</p>
   * 
   * @param The logging level (ERROR, INFO, DEBUG).
   */
  public String getLogLevel() {
    return logLevel;
  }

  /**
   * <p>Get the log records.</p>
   * 
   * @return The log records.
   */
  public List<Map<String, String>> getLogRecords() {
    return logRecords;
  }

  /**
   * <p>Record the info message in the log if {@see #isInfoEnabled()} is true.</p>
   * 
   * @param message The message.
   */
  public void info(final String message) {
    if (isInfoEnabled()) {
      LOG.info(message);
      log("INFO", message);
    }
  }

  /**
   * <p>Check to see if debug level logging is enabled. Use this in an if block around
   * logging operations that create large amounts of log data to prevent that data from being
   * created if logging is not enabled.</p>
   * 
   * @return True if debug level logging is enabled.
   */
  public boolean isDebugEnabled() {
    return logLevel.equals("DEBUG");
  }

  /**
   * <p>Check to see if info or debug level logging is enabled. Use this in an if block around
   * logging operations that create large amounts of log data to prevent that data from being
   * created if logging is not enabled.</p>
   * 
   * @return True if info or debug level logging is enabled.
   */
  public boolean isInfoEnabled() {
    return logLevel.equals("DEBUG") || logLevel.equals("INFO");
  }

  /**
   * <p>Create a log message.</p>
   * 
   * @param logLevel The logging level (ERROR, INFO, DEBUG).
   * @param message The message.
   */
  private synchronized void log(final String logLevel, final String message) {
    final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));
    final Map<String, String> logRecord = new LinkedHashMap<String, String>();
    logRecord.put("date", date);
    logRecord.put("level", logLevel);
    logRecord.put("message", message);
    logRecords.add(logRecord);
  }

  /**
   * <p>Set the current logging level (ERROR, INFO, DEBUG).</p>
   * 
   * @param logLevel The logging level (ERROR, INFO, DEBUG).
   */
  public void setLogLevel(final String logLevel) {
    this.logLevel = logLevel;
  }
}
