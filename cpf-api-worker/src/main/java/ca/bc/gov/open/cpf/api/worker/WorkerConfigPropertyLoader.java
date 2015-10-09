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
package ca.bc.gov.open.cpf.api.worker;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;

import com.revolsys.collection.map.Maps;
import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.datatype.DataType;
import com.revolsys.datatype.DataTypes;
import com.revolsys.spring.config.BeanConfigurrer;
import com.revolsys.util.Property;
import com.revolsys.websocket.AsyncResult;
import com.revolsys.websocket.json.JsonAsyncSender;

public class WorkerConfigPropertyLoader extends BeanConfigurrer implements ConfigPropertyLoader {

  private String environmentName = "default";

  private final WorkerScheduler workerScheduler;

  public WorkerConfigPropertyLoader(final WorkerScheduler workerScheduler,
    final String environmentName) {
    this.workerScheduler = workerScheduler;
    this.environmentName = environmentName;
  }

  @Override
  public synchronized Map<String, Object> getConfigProperties(final String moduleName,
    final String componentName) {
    return getConfigProperties(this.environmentName, moduleName, componentName);
  }

  @Override
  @SuppressWarnings({
    "unchecked"
  })
  public synchronized Map<String, Object> getConfigProperties(final String environmentName,
    final String moduleName, final String componentName) {
    try {
      final JsonAsyncSender messageSender = this.workerScheduler.getMessageSender();
      if (messageSender != null) {
        final Map<String, Object> message = Maps.newLinkedHash("type", "moduleConfigLoad");
        message.put("moduleName", moduleName);
        message.put("environmentName", environmentName);
        message.put("componentName", componentName);
        return messageSender.sendAndWait(message, new AsyncResult<Map<String, Object>>() {
          @Override
          public <V> V getResult(final Map<String, Object> result) {
            final Map<String, Object> configProperties = new HashMap<>();
            final List<Map<String, Object>> configPropertyList = (List<Map<String, Object>>)result.get("properties");
            for (final Map<String, Object> configProperty : configPropertyList) {
              final String name = (String)configProperty.get("PROPERTY_NAME");
              if (Property.hasValue(name)) {
                final String stringValue = (String)configProperty.get("PROPERTY_VALUE");
                if (Property.hasValue(stringValue)) {
                  final String type = (String)configProperty.get("PROPERTY_VALUE_TYPE");
                  final DataType dataType = DataTypes.getType(QName.valueOf(type));
                  Object value = stringValue;
                  if (dataType != null) {
                    final Class<?> dataTypeClass = dataType.getJavaClass();
                    final StringConverter<?> converter = StringConverterRegistry.getInstance()
                      .getConverter(dataTypeClass);
                    if (converter != null) {
                      value = converter.toObject(stringValue);
                    }
                  }
                  configProperties.put(name, value);
                } else {
                  configProperties.put(name, null);
                }
              }
            }
            return (V)configProperties;
          }
        });
      }
    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error(
        "Unable to get config properties for " + moduleName, e);
    }
    return Collections.emptyMap();
  }

  @Override
  public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory)
    throws BeansException {
    try {
      final Map<String, Object> attributes = getConfigProperties(this.environmentName,
        "CPF_WORKER", "GLOBAL");
      setAttributes(attributes);
      super.postProcessBeanFactory(beanFactory);
    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error("Unable to load config", e.getCause());
    }
  }

}
