/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;

import com.revolsys.datatype.DataType;
import com.revolsys.datatype.DataTypes;
import com.revolsys.record.Record;

public class DaoConfigPropertyLoader implements ConfigPropertyLoader {

  private CpfDataAccessObject dataAccessObject;

  private void addConfigProperties(final Map<String, Object> configProperties,
    final String environmentName, final String moduleName, final String componentName) {
    final List<Record> properties = this.dataAccessObject
      .getConfigPropertiesForModule(environmentName, moduleName, componentName);
    for (final Record configProperty : properties) {
      final String propertyName = configProperty.getValue(ConfigProperty.PROPERTY_NAME);
      final String stringValue = configProperty.getValue(ConfigProperty.PROPERTY_VALUE);
      final String type = configProperty.getValue(ConfigProperty.PROPERTY_VALUE_TYPE);
      final DataType dataType = DataTypes.getDataType(type);
      Object value = stringValue;
      if (dataType != null) {
        value = dataType.toObject(stringValue);
      }
      configProperties.put(propertyName, value);
    }
  }

  @PreDestroy
  public void close() {
    this.dataAccessObject = null;
  }

  @Override
  public Map<String, Object> getConfigProperties(final String moduleName,
    final String componentName) {
    final Map<String, Object> configProperties = new HashMap<>();
    addConfigProperties(configProperties, ConfigProperty.DEFAULT, moduleName, componentName);
    return configProperties;
  }

  @Override
  public Map<String, Object> getConfigProperties(final String environmentName,
    final String moduleName, final String componentName) {
    final Map<String, Object> configProperties = getConfigProperties(moduleName, componentName);
    addConfigProperties(configProperties, environmentName, moduleName, componentName);
    return configProperties;
  }

  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

}
