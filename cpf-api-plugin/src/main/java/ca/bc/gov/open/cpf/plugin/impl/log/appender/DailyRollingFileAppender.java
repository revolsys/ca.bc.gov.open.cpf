package ca.bc.gov.open.cpf.plugin.impl.log.appender;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class DailyRollingFileAppender extends Slf4jModuleLogAppender {
  // method is synchronized so shared instance is fine
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd");

  private File directory;

  private long dayEndTime = 0;

  private Map<String, FileAppender> appenderByModule = new HashMap<String, FileAppender>();

  private String date;

  public DailyRollingFileAppender(final File directory) {
    setDirectory(directory);
  }

  @Override
  public void close() {
    super.close();
    closeAppenders();
    appenderByModule = null;
    directory = null;
  }

  protected void closeAppenders() {
    if (appenderByModule != null) {
      for (final FileAppender appender : appenderByModule.values()) {
        appender.close();
      }
    }
  }

  @Override
  public synchronized void log(final String level, final String moduleName,
    final String category, final String message, final long durationInMillis,
    final Map<String, ? extends Object> data) {
    if (directory == null) {
      super.log(level, moduleName, category, message, durationInMillis, data);
    } else {
      final long time = System.currentTimeMillis();
      if (time >= dayEndTime) {
        closeAppenders();
        final Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = DATE_FORMAT.format(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        dayEndTime = calendar.getTimeInMillis();
      }
      FileAppender appender = appenderByModule.get(moduleName);
      if (appender == null) {
        final File file = new File(directory, moduleName + "/" + moduleName
          + "-" + date + ".csv");
        appender = new FileAppender(file);
        appenderByModule.put(moduleName, appender);
      }
      appender.log(time, level, moduleName, category, message,
        durationInMillis, data);
    }
  }

  public void setDirectory(final File directory) {
    if (directory == null) {
      this.directory = null;
    } else {
      if (directory.exists()) {
        this.directory = directory;
      } else {
        if (directory.mkdirs()) {
          this.directory = directory;
        } else {
          this.directory = null;
        }
      }
    }
  }
}
