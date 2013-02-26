package ca.bc.gov.open.cpf.plugin.impl.log.appender;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.plugin.impl.log.ModuleLog;

import com.revolsys.io.json.JsonMapIoFactory;

public class Slf4jModuleLogAppender extends AbstractModuleLogAppender {

  @Override
  public void log(
    final String level,
    final String moduleName,
    final String category,
    final String message,
    final long durationInMillis,
    final Map<String, ? extends Object> data) {
    final Logger log = LoggerFactory.getLogger("ca.bc.gov.open.cpf.module."
      + moduleName);
    final String text = category + '\t' + message + '\t' + durationInMillis
      + '\t' + JsonMapIoFactory.toString(data);
    if (level.equals(ModuleLog.INFO)) {
      log.info(text);
    } else if (level.equals(ModuleLog.ERROR)) {
      log.error(text);
    } else {
      log.debug(text);
    }
  }

}
