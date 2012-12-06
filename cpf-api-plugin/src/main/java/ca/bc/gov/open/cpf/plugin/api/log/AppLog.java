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

import com.revolsys.io.csv.CsvMapWriter;
import com.revolsys.util.ExceptionUtil;

public class AppLog {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS");

  private String logLevel = "ERROR";

  private final List<Map<String, String>> logRecords = new ArrayList<Map<String, String>>();

  private static final Logger LOG = LoggerFactory.getLogger(AppLog.class);

  public AppLog() {
  }

  public AppLog(final String logLevel) {
    this.logLevel = logLevel;
  }

  public void debug(final String message) {
    if (isDebugEnabled()) {
      LOG.debug(message);
      log("DEBUG", message);
    }
  }

  public void error(final String message) {
    LOG.error(message);
    log("ERROR", message);
  }

  public void error(final String message, final Throwable exception) {
    LOG.error(message, exception);
    log("ERROR", message + "\n" + ExceptionUtil.toString(exception));
  }

  public String getLogContent() {
    final StringWriter out = new StringWriter();
    final CsvMapWriter writer = new CsvMapWriter(out);
    for (final Map<String, String> logRecord : logRecords) {
      writer.write(logRecord);
    }
    writer.close();

    return out.toString();
  }

  public List<Map<String, String>> getLogRecords() {
    return logRecords;
  }

  public void info(final String message) {
    if (isInfoEnabled()) {
      LOG.info(message);
      log("INFO", message);
    }
  }

  public boolean isDebugEnabled() {
    return logLevel.equals("DEBUG");
  }

  public boolean isInfoEnabled() {
    return logLevel.equals("DEBUG") || logLevel.equals("INFO");
  }

  private synchronized void log(final String level, final String message) {
    final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));
    final Map<String, String> logRecord = new LinkedHashMap<String, String>();
    logRecord.put("date", date);
    logRecord.put("level", level);
    logRecord.put("message", message);
    logRecords.add(logRecord);
  }

  public void setLogLevel(final String logLevel) {
    this.logLevel = logLevel;
  }
}
