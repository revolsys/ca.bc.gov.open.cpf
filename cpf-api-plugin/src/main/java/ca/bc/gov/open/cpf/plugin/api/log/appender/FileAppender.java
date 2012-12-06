package ca.bc.gov.open.cpf.plugin.api.log.appender;

import java.io.File;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import com.revolsys.io.FileUtil;
import com.revolsys.io.csv.CsvWriter;
import com.revolsys.io.json.JsonMapIoFactory;

public class FileAppender extends Slf4jModuleLogAppender {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS");

  private File file;

  private CsvWriter writer;

  public FileAppender(final File file) {
    setFile(file);
  }

  @Override
  @PreDestroy
  public synchronized void close() {
    if (writer != null) {
      writer.close();
    }
    writer = null;
    file = null;
  }

  public void log(final long time, final String level, final String moduleName,
    final String category, final String message, final long durationInMillis,
    Map<String, ? extends Object> data) {
    if (file == null) {
      super.log(level, moduleName, category, message, durationInMillis, data);
    } else {
      final Date date = new Date(time);
      final String formattedDate = DATE_FORMAT.format(date);
      Object batchJobId = null;
      if (data != null) {
        batchJobId = data.get("batchJobId");
        if (batchJobId != null) {
          data = new HashMap<String, Object>(data);
          data.remove("batchJobId");
        }
      }
      final String dataString = JsonMapIoFactory.toString(data);
      writer.write(formattedDate, level, moduleName, category, message,
        durationInMillis, batchJobId, dataString);
      writer.flush();
    }
  }

  @Override
  public synchronized void log(final String level, final String moduleName,
    final String category, final String message, final long durationInMillis,
    final Map<String, ? extends Object> data) {
    if (file == null) {
      super.log(level, moduleName, category, message, durationInMillis, data);
    } else {
      final long time = System.currentTimeMillis();
      log(time, level, moduleName, category, message, durationInMillis, data);
    }
  }

  public void setFile(final File file) {
    if (file != null) {
      if (file.exists()) {
        this.file = file;
      } else {
        final File directory = file.getParentFile();
        if (directory.exists()) {
          this.file = file;
        } else {
          if (directory.mkdirs()) {
            this.file = file;
          } else {
            this.file = null;
          }
        }
      }
    } else {
      this.file = null;
    }
    if (this.file != null) {
      final Writer fileWriter = FileUtil.getWriter(this.file);
      writer = new CsvWriter(fileWriter);
      writer.write("Date", "Level", "ModuleName", "Category", "Message",
        "DurationInMillis", "BatchJobId", "DataJsonMap");
    }
  }
}
