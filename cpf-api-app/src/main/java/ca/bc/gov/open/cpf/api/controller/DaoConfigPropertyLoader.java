package ca.bc.gov.open.cpf.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;

import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;

public class DaoConfigPropertyLoader implements ConfigPropertyLoader {

  private CpfDataAccessObject dataAccessObject;

  private void addConfigProperties(final Map<String, Object> configProperties,
    final String environmentName, final String moduleName,
    final String componentName) {
    final List<DataObject> properties = dataAccessObject.getConfigPropertiesForModule(
      environmentName, moduleName, componentName);
    for (final DataObject configProperty : properties) {
      final String propertyName = configProperty.getValue(ConfigProperty.PROPERTY_NAME);
      final String stringValue = configProperty.getValue(ConfigProperty.PROPERTY_VALUE);
      final String type = configProperty.getValue(ConfigProperty.PROPERTY_VALUE_TYPE);
      final DataType dataType = DataTypes.getType(type);
      Object value = stringValue;
      if (dataType != null) {
        final Class<?> dataTypeClass = dataType.getJavaClass();
        final StringConverter<?> converter = StringConverterRegistry.getInstance()
          .getConverter(dataTypeClass);
        if (converter != null) {
          value = converter.toObject(stringValue);
        }
      }
      configProperties.put(propertyName, value);
    }
  }

  @PreDestroy
  public void close() {
    dataAccessObject = null;
  }

  @Override
  public Map<String, Object> getConfigProperties(final String moduleName,
    final String componentName) {
    final Map<String, Object> configProperties = new HashMap<String, Object>();
    addConfigProperties(configProperties, ConfigProperty.DEFAULT, moduleName,
      componentName);
    return configProperties;
  }

  @Override
  public Map<String, Object> getConfigProperties(final String environmentName,
    final String moduleName, final String componentName) {
    final Map<String, Object> configProperties = getConfigProperties(
      moduleName, componentName);
    addConfigProperties(configProperties, environmentName, moduleName,
      componentName);
    return configProperties;
  }

  @Resource(name = "cpfDataAccessObject")
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

}
