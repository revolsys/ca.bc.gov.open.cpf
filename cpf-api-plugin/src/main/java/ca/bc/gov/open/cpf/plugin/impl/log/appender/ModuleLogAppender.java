package ca.bc.gov.open.cpf.plugin.impl.log.appender;

import java.util.Map;

import javax.annotation.PreDestroy;

public interface ModuleLogAppender {
  @PreDestroy
  void close();

  void log(
    String level,
    String moduleName,
    String category,
    String message,
    long durationInMillis,
    Map<String, ? extends Object> data);
}
