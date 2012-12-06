package ca.bc.gov.open.cpf.plugin.api;

import java.util.Map;

public interface ConfigPropertyLoader {
  Map<String, Object> getConfigProperties(
    String moduleName,
    String componentName);

  Map<String, Object> getConfigProperties(
    String environmentName,
    String moduleName,
    String componentName);
}
