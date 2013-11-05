package ca.bc.gov.open.cpf.api.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.api.domain.ConfigProperty;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.module.ModuleLoader;
import ca.bc.gov.open.cpf.plugin.impl.module.ResourcePermission;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.io.AbstractMapReaderFactory;
import com.revolsys.io.Reader;
import com.revolsys.maven.MavenRepository;
import com.revolsys.spring.InputStreamResource;

public class ConfigPropertyModuleLoader implements ModuleLoader {

  private static final String ACTION = "action";

  private static final String DELETE = "delete";

  private static final String ENABLED = "enabled";

  private static final String MAVEN_MODULE_ID = "mavenModuleId";

  private BusinessApplicationRegistry businessApplicationRegistry;

  private CpfDataAccessObject dataAccessObject;

  private final Set<String> excludeMavenIds = new LinkedHashSet<String>();

  private BatchJobService batchJobService;

  private MavenRepository mavenRepository;

  private final Map<String, ConfigPropertyModule> modulesByName = new LinkedHashMap<String, ConfigPropertyModule>();

  public ConfigPropertyModuleLoader() {
    final String cpfVersion = getClass().getPackage()
      .getImplementationVersion();
    excludeMavenIds.add("ca.bc.gov.open.cpf:cpf-api-app:" + cpfVersion);
    excludeMavenIds.add("ca.bc.gov.open.cpf:cpf-api-plugin:" + cpfVersion);
    excludeMavenIds.add("ca.bc.gov.open.cpf:cpf-api-client:" + cpfVersion);
  }

  public void deleteModule(final Module module) {
    final String moduleName = module.getName();
    modulesByName.remove(moduleName);
    businessApplicationRegistry.unloadModule(module);
    dataAccessObject.deleteUserGroupsForModule(moduleName);
    dataAccessObject.deleteUserGroupsForModule("ADMIM_" + moduleName);
    dataAccessObject.deleteConfigPropertiesForModule(moduleName);
  }

  public BatchJobService getBatchJobService() {
    return batchJobService;
  }

  @Override
  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return businessApplicationRegistry;
  }

  public DataObject getConfigProperty(final ConfigPropertyModule module,
    final Map<String, Object> property) {
    final DataObject configProperty = dataAccessObject.create(ConfigProperty.CONFIG_PROPERTY);
    String environmentName = (String)property.get("environmentName");
    if (!StringUtils.hasText(environmentName)) {
      environmentName = ConfigProperty.DEFAULT;
    }
    configProperty.setValue(ConfigProperty.ENVIRONMENT_NAME, environmentName);
    final String moduleName = module.getName();
    configProperty.setValue(ConfigProperty.MODULE_NAME, moduleName);
    configProperty.setValue(ConfigProperty.COMPONENT_NAME,
      ConfigProperty.MODULE_BEAN_PROPERTY);

    final String name = (String)property.get("name");
    if (!StringUtils.hasText(name)) {
      module.addModuleError("Config property must have a name " + property);
    }
    configProperty.setValue(ConfigProperty.PROPERTY_NAME, name);

    String type = (String)property.get("type");
    if (!StringUtils.hasText(type)) {
      type = "string";
    }
    configProperty.setValue(ConfigProperty.PROPERTY_VALUE_TYPE, type);

    final Object value = property.get("value");
    dataAccessObject.setConfigPropertyValue(configProperty, value);
    return configProperty;
  }

  public Map<String, List<String>> getDeletePropertiesByEnvironment(
    final ConfigPropertyModule module,
    final List<Map<String, Object>> pluginProperties) {
    final Map<String, List<String>> deletePropertiesByEnvironment = new HashMap<String, List<String>>();
    for (final Map<String, Object> property : pluginProperties) {
      final String action = (String)property.get(ACTION);
      if (DELETE.equals(action)) {
        String environmentName = (String)property.get("environmentName");
        if (!StringUtils.hasText(environmentName)) {
          environmentName = ConfigProperty.DEFAULT;
        }
        List<String> propertyNames = deletePropertiesByEnvironment.get(environmentName);
        if (propertyNames == null) {
          propertyNames = new ArrayList<String>();
          deletePropertiesByEnvironment.put(environmentName, propertyNames);
        }
        final String propertyName = (String)property.get("name");
        propertyNames.add(propertyName);
      }
    }
    return deletePropertiesByEnvironment;
  }

  public MavenRepository getMavenRepository() {
    return mavenRepository;
  }

  public Map<String, Map<String, DataObject>> getPropertiesByEnvironment(
    final ConfigPropertyModule module,
    final List<Map<String, Object>> pluginProperties) {
    final Map<String, Map<String, DataObject>> propertiesByEnvironment = new HashMap<String, Map<String, DataObject>>();
    for (final Map<String, Object> property : pluginProperties) {
      final String action = (String)property.get(ACTION);
      if (!DELETE.equals(action)) {
        final DataObject configProperty = getConfigProperty(module, property);
        final String environmentName = configProperty.getValue(ConfigProperty.ENVIRONMENT_NAME);
        Map<String, DataObject> propertiesByName = propertiesByEnvironment.get(environmentName);
        if (propertiesByName == null) {
          propertiesByName = new HashMap<String, DataObject>();
          propertiesByEnvironment.put(environmentName, propertiesByName);
        }
        final String propertyName = configProperty.getValue(ConfigProperty.PROPERTY_NAME);
        propertiesByName.put(propertyName, configProperty);
      }
    }
    return propertiesByEnvironment;
  }

  private boolean isConfigPropertyDeleted(
    final Map<String, List<String>> deletePropertiesByEnvironment,
    final String environmentName, final String propertyName) {
    boolean delete = false;
    final List<String> deleteProperties = deletePropertiesByEnvironment.get(environmentName);
    if (deleteProperties != null) {
      delete = deleteProperties.contains(propertyName);
    }
    if (!delete && !ConfigProperty.DEFAULT.equals(environmentName)) {
      delete = isConfigPropertyDeleted(deletePropertiesByEnvironment,
        ConfigProperty.DEFAULT, propertyName);
    }
    return delete;
  }

  public boolean isEnabledInConfig(final String moduleName) {
    final DataObject enabledProperty = dataAccessObject.getConfigProperty(
      ConfigProperty.DEFAULT, moduleName, ConfigProperty.MODULE_CONFIG, ENABLED);
    final String value = enabledProperty.getValue(ConfigProperty.PROPERTY_VALUE);
    final boolean enabled = !"false".equalsIgnoreCase(value);
    return enabled;
  }

  protected void refreshConfigProperties(final ConfigPropertyModule module) {
    try {
      final String moduleName = module.getName();
      final ClassLoader classLoader = module.getClassLoader();
      final InputStream in = classLoader.getResourceAsStream("META-INF/ca.bc.gov.open.cpf.plugin.ConfigProperties.json");
      if (in != null) {
        final InputStreamResource resource = new InputStreamResource(
          "properties.json", in);
        final Reader<Map<String, Object>> reader = AbstractMapReaderFactory.mapReader(resource);
        final List<Map<String, Object>> pluginProperties = reader.read();

        final Map<String, Map<String, DataObject>> propertiesByEnvironment = getPropertiesByEnvironment(
          module, pluginProperties);
        final Map<String, List<String>> deletePropertiesByEnvironment = getDeletePropertiesByEnvironment(
          module, pluginProperties);

        // Exclude and properties that already exist in the config database
        for (final DataObject configProperty : dataAccessObject.getConfigPropertiesForComponent(
          moduleName, ConfigProperty.MODULE_BEAN_PROPERTY)) {
          final String environmentName = configProperty.getValue(ConfigProperty.ENVIRONMENT_NAME);
          final String propertyName = configProperty.getValue(ConfigProperty.PROPERTY_NAME);
          final Map<String, DataObject> properties = propertiesByEnvironment.get(environmentName);
          if (properties != null) {
            properties.remove(propertyName);
          }
          if (isConfigPropertyDeleted(deletePropertiesByEnvironment,
            environmentName, propertyName)) {
            dataAccessObject.delete(configProperty);
          }
        }

        // Add config properties for new properties
        for (final Map<String, DataObject> properties : propertiesByEnvironment.values()) {
          for (final DataObject property : properties.values()) {
            dataAccessObject.write(property);
          }
        }
      }
    } catch (final Throwable t) {
      module.addModuleError(t);
    }
  }

  private void refreshMavenModule(final String moduleName,
    final String mavenModuleId) {
    ConfigPropertyModule module = modulesByName.get(moduleName);
    try {
      if (module != null && !module.getMavenModuleId().equals(mavenModuleId)) {
        businessApplicationRegistry.unloadModule(module);
        module = null;
      }
      final boolean newModule = module == null;
      final boolean enabled = isEnabledInConfig(moduleName);
      if (newModule) {
        final ConfigPropertyLoader configPropertyLoader = businessApplicationRegistry.getConfigPropertyLoader();
        module = new ConfigPropertyModule(this, businessApplicationRegistry,
          moduleName, mavenRepository, mavenModuleId, excludeMavenIds,
          configPropertyLoader);
        modulesByName.put(moduleName, module);
      }
      if (newModule) {
        businessApplicationRegistry.addModule(module);
      }
      if (enabled) {
        module.enable();
      } else {
        module.disable();
      }
    } catch (final Throwable e) {
      if (module == null) {
        LoggerFactory.getLogger(ConfigPropertyModuleLoader.class).error(
          "Unable to load module " + moduleName, e);
      } else {
        module.addModuleError(e);
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Override
  public void refreshModules() {
    synchronized (modulesByName) {
      final Map<String, Module> modulesToDelete = new HashMap<String, Module>(
        modulesByName);
      final Map<String, Module> modulesToUnload = new HashMap<String, Module>(
        modulesByName);

      final Map<String, String> modulesToRefresh = new HashMap<String, String>();

      for (final DataObject property : dataAccessObject.getConfigPropertiesForAllModules(
        ConfigProperty.DEFAULT, ConfigProperty.MODULE_CONFIG, MAVEN_MODULE_ID)) {
        final String moduleName = property.getValue(ConfigProperty.MODULE_NAME);
        final String mavenModuleId = property.getValue(ConfigProperty.PROPERTY_VALUE);
        final ConfigPropertyModule module = modulesByName.get(moduleName);
        if (module != null && mavenModuleId.equals(module.getMavenModuleId())) {
          modulesToUnload.remove(moduleName);
        }
        modulesToDelete.remove(moduleName);
        modulesToRefresh.put(moduleName, mavenModuleId);
      }
      for (final Module module : modulesToDelete.values()) {
        deleteModule(module);
      }
      for (final Module module : modulesToUnload.values()) {
        businessApplicationRegistry.unloadModule(module);
      }
      for (final Entry<String, String> entry : modulesToRefresh.entrySet()) {
        final String moduleName = entry.getKey();
        final String mavenModuleId = entry.getValue();
        refreshMavenModule(moduleName, mavenModuleId);
      }
    }
  }

  protected void refreshUserGroup(final ConfigPropertyModule module,
    final String groupNameSuffix, final String descriptionSuffix) {
    final String moduleName = module.getName();
    final String adminModuleName = "ADMIN_MODULE_" + moduleName;
    final String groupName = adminModuleName + groupNameSuffix;
    final String description = descriptionSuffix + moduleName;
    dataAccessObject.createUserGroup(adminModuleName, groupName, description);
  }

  protected void refreshUserGroups(final ConfigPropertyModule module) {
    try {
      final String moduleName = module.getName();

      final Map<String, Set<ResourcePermission>> permissionsByGroupName = module.getPermissionsByGroupName();
      final Set<String> groupNamesToDelete = module.getGroupNamesToDelete();

      if (!module.hasError()) {
        final Set<String> existingGroupNames = new HashSet<String>();
        // Exclude or delete user groups that already exist in the database
        for (final DataObject userGroup : dataAccessObject.getUserGroupsForModule(moduleName)) {
          final String groupName = userGroup.getValue(UserGroup.USER_GROUP_NAME);
          if (groupName.startsWith(moduleName.toUpperCase())) {
            if (groupNamesToDelete != null
              && groupNamesToDelete.contains(groupName)) {
              dataAccessObject.delete(userGroup);
            } else {
              existingGroupNames.add(groupName);
            }
          }
        }

        if (permissionsByGroupName != null) {
          for (final Entry<String, Set<ResourcePermission>> groupPermissions : permissionsByGroupName.entrySet()) {
            final String groupName = groupPermissions.getKey();
            if (groupName.startsWith(moduleName + "_")
              && !existingGroupNames.contains(groupName)) {

              dataAccessObject.createUserGroup(moduleName, groupName, null);
            }
            final DataObject group = dataAccessObject.getUserGroup(groupName);
            if (group == null) {
              module.getLog().error("Group not found " + groupName);
            } else {
              final Set<ResourcePermission> newPermissions = new HashSet<ResourcePermission>(
                groupPermissions.getValue());
              if (newPermissions != null && !newPermissions.isEmpty()) {
                for (final DataObject userGroupPermission : dataAccessObject.getUserGroupPermissions(
                  group, moduleName)) {
                  final ResourcePermission permission = new ResourcePermission(
                    userGroupPermission);
                  newPermissions.remove(permission);
                }
                for (final ResourcePermission newPermission : newPermissions) {

                  dataAccessObject.createUserGroupPermission(group, moduleName,
                    newPermission);
                }
              }
            }
          }
        }
      }
    } catch (final Throwable t) {
      module.addModuleError(t);
    }
  }

  @javax.annotation.Resource(name = "batchJobService")
  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }

  @Override
  public void setBusinessApplicationRegistry(
    final BusinessApplicationRegistry businessApplicationRegistry) {
    if (this.businessApplicationRegistry != businessApplicationRegistry) {
      businessApplicationRegistry.addModuleEventListener(batchJobService);
      this.businessApplicationRegistry = businessApplicationRegistry;
    }
  }

  @Resource(name = "cpfDataAccessObject")
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void setMavenModuleConfigProperties(final String moduleName,
    final String mavenModuleId, final boolean enabled) {
    final String environmentName = ConfigProperty.DEFAULT;
    final String componentName = ConfigProperty.MODULE_CONFIG;
    final String propertyName = MAVEN_MODULE_ID;
    DataObject moduleIdProperty = dataAccessObject.getConfigProperty(
      environmentName, moduleName, componentName, propertyName);
    if (moduleIdProperty == null) {
      moduleIdProperty = dataAccessObject.createConfigProperty(environmentName,
        moduleName, componentName, propertyName, mavenModuleId,
        DataTypes.STRING);
    } else {
      dataAccessObject.setConfigPropertyValue(moduleIdProperty, mavenModuleId);
      dataAccessObject.write(moduleIdProperty);
    }

    DataObject moduleEnabledProperty = dataAccessObject.getConfigProperty(
      environmentName, moduleName, componentName, ENABLED);
    if (moduleEnabledProperty == null) {
      moduleEnabledProperty = dataAccessObject.createConfigProperty(
        environmentName, moduleName, componentName, ENABLED, enabled,
        DataTypes.BOOLEAN);
    } else {
      dataAccessObject.setConfigPropertyValue(moduleEnabledProperty, enabled);
      dataAccessObject.write(moduleEnabledProperty);
    }
  }

  public void setMavenRepository(final MavenRepository mavenRepository) {
    this.mavenRepository = mavenRepository;
  }
}
