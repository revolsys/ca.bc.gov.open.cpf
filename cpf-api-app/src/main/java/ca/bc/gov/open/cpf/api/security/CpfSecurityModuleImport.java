package ca.bc.gov.open.cpf.api.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.config.BeanIds;

import com.revolsys.spring.ModuleImport;

public class CpfSecurityModuleImport extends ModuleImport {
  private List<String> paths = new ArrayList<String>();

  @SuppressWarnings("unchecked")
  @Override
  protected void afterPostProcessBeanDefinitionRegistry(
    final BeanDefinitionRegistry registry) {
    final GenericApplicationContext beanFactory = getApplicationContext(registry);

    final BeanDefinition filterChainProxy = registry.getBeanDefinition(BeanIds.FILTER_CHAIN_PROXY);
    if (filterChainProxy == null) {
      registerTargetBeanDefinition(registry, beanFactory,
        BeanIds.FILTER_CHAIN_PROXY, BeanIds.FILTER_CHAIN_PROXY);
      registerTargetBeanDefinition(registry, beanFactory,
        BeanIds.SPRING_SECURITY_FILTER_CHAIN,
        BeanIds.SPRING_SECURITY_FILTER_CHAIN);
    } else {
      final Filter moduleFilterChainProxy = beanFactory.getBean(
        BeanIds.FILTER_CHAIN_PROXY, Filter.class);
      if (moduleFilterChainProxy != null) {
        final Map<String, List<?>> mergedFilterMap = new ManagedMap<String, List<?>>();

        final MutablePropertyValues propertyValues = filterChainProxy.getPropertyValues();
        final PropertyValue filterChainMap = propertyValues.getPropertyValue("filterChainMap");
        final Map<String, List<?>> filterMap = (Map<String, List<?>>)filterChainMap.getValue();

        for (final String path : paths) {
          mergedFilterMap.put(path,
            Collections.singletonList(moduleFilterChainProxy));
        }
        mergedFilterMap.putAll(filterMap);

        propertyValues.removePropertyValue(filterChainMap);
        propertyValues.add("filterChainMap", mergedFilterMap);
      }
    }
  }

  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(final List<String> paths) {
    this.paths = paths;
  }

}
