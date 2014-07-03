package ca.bc.gov.open.cpf.api.worker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.client.httpclient.DigestHttpClient;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;

import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.data.types.DataType;
import com.revolsys.data.types.DataTypes;
import com.revolsys.spring.config.BeanConfigurrer;

public class InternalWebServiceConfigPropertyLoader extends BeanConfigurrer
  implements ConfigPropertyLoader {

  private String environmentName = "default";

  private DigestHttpClient httpClient;

  public InternalWebServiceConfigPropertyLoader(DigestHttpClient httpClient,
    String environmentName) {
    this.httpClient = httpClient;
    this.environmentName = environmentName;
  }

  @Override
  public synchronized Map<String, Object> getConfigProperties(
    final String moduleName, final String componentName) {
    return getConfigProperties(environmentName, moduleName, componentName);
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
    throws BeansException {
    try {
      Map<String, Object> attributes = getConfigProperties(environmentName,
        "CPF_WORKER", "GLOBAL");
      setAttributes(attributes);
      super.postProcessBeanFactory(beanFactory);
    } catch (Throwable e) {
      LoggerFactory.getLogger(getClass()).error("Unable to load config",
        e.getCause());
    }
  }

  @Override
  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  public synchronized Map<String, Object> getConfigProperties(
    final String environmentName, final String moduleName,
    final String componentName) {
    Map<String, Object> configProperties = new HashMap<String, Object>();
    try {
      final String url = httpClient.getUrl("/worker/modules/" + moduleName
        + "/config/" + environmentName + "/" + componentName);
      final Map result = httpClient.getJsonResource(url);
      final List<Map<String, Object>> configPropertyList = (List<Map<String, Object>>)result.get("properties");
      for (Map<String, Object> configProperty : configPropertyList) {
        String name = (String)configProperty.get("PROPERTY_NAME");
        if (StringUtils.hasText(name)) {
          String stringValue = (String)configProperty.get("PROPERTY_VALUE");
          if (StringUtils.hasText(stringValue)) {
            String type = (String)configProperty.get("PROPERTY_VALUE_TYPE");
            DataType dataType = DataTypes.getType(QName.valueOf(type));
            Object value = stringValue;
            if (dataType != null) {
              final Class<?> dataTypeClass = (Class<?>)dataType.getJavaClass();
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

    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error(
        "Unable to get config properties for " + moduleName, e);
    }
    return configProperties;
  }

}
