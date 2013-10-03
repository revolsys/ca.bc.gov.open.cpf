package ca.bc.gov.open.cpf.plugin.impl.module;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.FixedWindowRollingPolicy;
import org.apache.log4j.rolling.RollingFileAppender;
import org.apache.log4j.rolling.SizeBasedTriggeringPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.plugin.api.AllowedValues;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.GeometryConfiguration;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;
import ca.bc.gov.open.cpf.plugin.api.ResultList;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.ConfigPropertyLoader;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.impl.log.ModuleLog;

import com.revolsys.collection.ArrayUtil;
import com.revolsys.collection.AttributeMap;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.AttributeProperties;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.io.AbstractMapReaderFactory;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.MapReaderFactory;
import com.revolsys.io.Reader;
import com.revolsys.spring.config.AttributesBeanConfigurer;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.util.Property;
import com.revolsys.util.UrlUtil;
import com.vividsolutions.jts.geom.Geometry;

public class ClassLoaderModule implements Module {

  private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderModule.class);

  @SuppressWarnings("unchecked")
  private static final Class<? extends Annotation>[] STANDARD_METHOD_EXCLUDE_ANNOTATIONS = ArrayUtil.create(
    JobParameter.class, RequestParameter.class, Required.class);

  private static final Map<String, Class<?>[]> STANDARD_METHODS = new HashMap<String, Class<?>[]>();

  private GenericApplicationContext applicationContext;

  private boolean applicationsLoaded;

  private List<String> businessApplicationNames = Collections.emptyList();

  private BusinessApplicationRegistry businessApplicationRegistry;

  private Map<String, BusinessApplication> businessApplicationsByName = Collections.emptyMap();

  private Map<BusinessApplication, String> businessApplicationsToBeanNames = Collections.emptyMap();

  private ClassLoader classLoader;

  private ConfigPropertyLoader configPropertyLoader;

  private URL configUrl;

  private final List<CoordinateSystem> coordinateSystems = EpsgCoordinateSystems.getCoordinateSystems(Arrays.asList(
    4326, 4269, 3005, 26907, 26908, 26909, 26910, 26911));

  private boolean enabled = false;

  private Set<String> groupNamesToDelete;

  private boolean initialized;

  private String moduleError;

  private final String name;

  private Map<String, Set<ResourcePermission>> permissionsByGroupName = new HashMap<String, Set<ResourcePermission>>();

  private boolean remoteable;

  private Date startedDate;

  {
    STANDARD_METHODS.put("setInputDataUrl", new Class<?>[] {
      URL.class
    });
    STANDARD_METHODS.put("setInputDataContentType", new Class<?>[] {
      String.class
    });
    STANDARD_METHODS.put("setResultSrid", new Class<?>[] {
      Integer.TYPE
    });
    STANDARD_METHODS.put("setResultNumAxis", new Class<?>[] {
      Integer.TYPE
    });
    STANDARD_METHODS.put("setResultScaleFactorXy", new Class<?>[] {
      Double.TYPE
    });
    STANDARD_METHODS.put("setResultScaleFactorZ", new Class<?>[] {
      Double.TYPE
    });
    STANDARD_METHODS.put("setResultData", new Class<?>[] {
      OutputStream.class
    });
    STANDARD_METHODS.put("setResultContentType", new Class<?>[] {
      String.class
    });
  }

  private boolean started = false;

  public ClassLoaderModule(
    final BusinessApplicationRegistry businessApplicationRegistry,
    final String moduleName) {
    this.name = moduleName;
    this.businessApplicationRegistry = businessApplicationRegistry;
  }

  public ClassLoaderModule(
    final BusinessApplicationRegistry businessApplicationRegistry,
    final String moduleName, final ClassLoader classLoader,
    final ConfigPropertyLoader configPropertyLoader, final URL configUrl) {
    this.businessApplicationRegistry = businessApplicationRegistry;
    this.name = moduleName;
    this.classLoader = classLoader;
    this.configPropertyLoader = configPropertyLoader;
    this.configUrl = configUrl;
  }

  @Override
  public void addModuleError(final String moduleError) {
    LOG.error("Unable to initialize module: " + this + ": " + moduleError);
    if (StringUtils.hasText(this.moduleError)) {
      this.moduleError += '\n' + moduleError;
    } else {
      this.moduleError = moduleError;
    }
    stop();
  }

  public void addModuleError(final String message, final Throwable e) {
    LOG.error("Unable to initialize module: " + this + ": " + message, e);
    final String trace = message + '\n' + ExceptionUtil.toString(e);
    if (StringUtils.hasText(this.moduleError)) {
      this.moduleError += '\n' + trace;
    } else {
      this.moduleError = trace;
    }
    stop();
  }

  public void addModuleError(final Throwable e) {
    LOG.error("Unable to initialize module: " + this, e);
    final String trace = ExceptionUtil.toString(e);
    if (StringUtils.hasText(this.moduleError)) {
      this.moduleError += '\n' + trace;
    } else {
      this.moduleError = trace;
    }
    stop();
  }

  private void addResourcePermissions(
    final Map<String, Set<ResourcePermission>> resourcePermissionsByGroupName,
    final String groupName, final Map<String, Object> pluginGroup) {
    final List<Map<String, Object>> permissions = CollectionUtil.get(
      pluginGroup, "permissions", null);
    final List<ResourcePermission> resourcePermissions = ResourcePermission.getPermissions(permissions);
    resourcePermissionsByGroupName.put(groupName,
      new HashSet<ResourcePermission>(resourcePermissions));
  }

  private void checkStandardMethod(final Method method,
    final Class<?>[] standardMethodParameters) {
    final Class<?> pluginClass = method.getDeclaringClass();
    final String pluginClassName = pluginClass.getName();
    final String methodName = method.getName();
    final Class<?>[] methodParameters = method.getParameterTypes();
    if (methodParameters.length != standardMethodParameters.length) {
      throw new IllegalArgumentException(pluginClassName + "." + methodName
        + " must have the parameters "
        + Arrays.toString(standardMethodParameters));
    }
    for (int i = 0; i < standardMethodParameters.length; i++) {
      final Class<?> parameter1 = standardMethodParameters[i];
      final Class<?> parameter2 = methodParameters[i];
      if (parameter1 != parameter2) {
        throw new IllegalArgumentException(pluginClassName + "." + methodName
          + " must have the parameters "
          + Arrays.toString(standardMethodParameters));
      }

    }
    for (final Class<? extends Annotation> annotationClass : STANDARD_METHOD_EXCLUDE_ANNOTATIONS) {
      if (!method.isAnnotationPresent(annotationClass)) {
        throw new IllegalArgumentException(pluginClassName + "." + methodName
          + " standard method must not have annotation " + annotationClass);
      }
    }
  }

  @Override
  public void clearModuleError() {
    moduleError = null;
  }

  private void closeAppLogAppender(final String businessApplicationName) {
    final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(businessApplicationName);
    synchronized (logger) {
      logger.setLevel(Level.DEBUG);
      logger.removeAllAppenders();
      logger.setAdditivity(true);
    }
  }

  @Override
  @PreDestroy
  public void destroy() {
    stop();
    classLoader = null;
    businessApplicationRegistry = null;
  }

  @Override
  public synchronized void disable() {
    if (this.enabled) {
      this.enabled = false;
    }
    if (isStarted()) {
      stop();
    }
    this.initialized = false;
  }

  public void doRestart() {
    stop();
    start();
  }

  public void doStart() {
    if (isEnabled() && !isStarted()) {
      final StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      ModuleLog.info(name, "Start", "Begin", null);
      clearModuleError();
      try {
        initializeGroupPermissions();
        preLoadApplications();
        if (!hasError()) {
          for (final String businessApplicationName : getBusinessApplicationNames()) {
            LOG.info("Found business application " + businessApplicationName
              + " from " + configUrl);
          }
          loadBusinessApplications();
          businessApplicationRegistry.clearModuleToAppCache();
          if (hasError()) {
            businessApplicationRegistry.moduleEvent(this,
              ModuleEvent.START_FAILED);
          } else {
            businessApplicationRegistry.moduleEvent(this, ModuleEvent.START);
          }
        }
      } catch (final Throwable e) {
        addModuleError(e);
      }
      ModuleLog.info(name, "Start", "End", stopWatch, null);
    }
  }

  public void doStop() {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    ModuleLog.info(name, "Stop", "Begin", null);
    started = false;
    applicationsLoaded = false;
    if (applicationContext != null && applicationContext.isActive()) {
      applicationContext.close();
    }
    for (final String businessApplicationName : businessApplicationNames) {
      closeAppLogAppender(businessApplicationName);
    }
    applicationContext = null;
    businessApplicationsByName = Collections.emptyMap();
    businessApplicationsToBeanNames = Collections.emptyMap();
    businessApplicationNames = Collections.emptyList();
    permissionsByGroupName = null;
    groupNamesToDelete = null;
    businessApplicationRegistry.clearModuleToAppCache();
    try {
      businessApplicationRegistry.moduleEvent(this, ModuleEvent.STOP);
    } finally {
      startedDate = null;
    }
    ModuleLog.info(name, "Stop", "End", stopWatch, null);
  }

  @Override
  public synchronized void enable() {
    if (!this.enabled) {
      this.enabled = true;
    }
    if (!isInitialized()) {
      this.initialized = true;
      start();
    }
  }

  private synchronized GenericApplicationContext getApplicationContext() {
    loadApplications();
    return applicationContext;
  }

  @Override
  public BusinessApplication getBusinessApplication(
    final String businessApplicationName) {
    if (businessApplicationName == null) {
      return null;
    } else {
      return businessApplicationsByName.get(businessApplicationName);
    }
  }

  @Override
  public List<String> getBusinessApplicationNames() {
    return businessApplicationNames;
  }

  @Override
  public PluginAdaptor getBusinessApplicationPlugin(
    final BusinessApplication application, final String executionId,
    String logLevel) {
    if (application == null) {
      return null;
    } else {
      final String businessApplicationName = application.getName();
      if (logLevel == null) {
        logLevel = application.getLogLevel();
      }
      Object plugin;
      final GenericApplicationContext applicationContext = getApplicationContext();
      if (applicationContext == null) {
        throw new IllegalArgumentException("Unable to instantiate plugin "
          + businessApplicationName + ": unable to get application context");
      } else {
        try {
          final String beanName = businessApplicationsToBeanNames.get(application);
          plugin = applicationContext.getBean(beanName);
          final PluginAdaptor pluginAdaptor = new PluginAdaptor(application,
            plugin, executionId, logLevel);
          return pluginAdaptor;
        } catch (final Throwable t) {
          throw new IllegalArgumentException("Unable to instantiate plugin "
            + businessApplicationName, t);
        }
      }
    }
  }

  @Override
  public PluginAdaptor getBusinessApplicationPlugin(
    final String businessApplicationName, final String executionId,
    final String logLevel) {
    final BusinessApplication application = getBusinessApplication(businessApplicationName);
    if (application == null) {
      return null;
    } else {
      return getBusinessApplicationPlugin(application, executionId, logLevel);
    }
  }

  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return businessApplicationRegistry;
  }

  @Override
  public List<BusinessApplication> getBusinessApplications() {
    final List<BusinessApplication> businessApplications = new ArrayList<BusinessApplication>();
    for (final String businessApplicationName : getBusinessApplicationNames()) {
      final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
      if (businessApplication != null) {
        businessApplications.add(businessApplication);
      }
    }
    return businessApplications;
  }

  private BusinessApplication getBusinessApplicaton(final String moduleName,
    final Class<?> pluginClass) {
    final String className = pluginClass.getName();
    final BusinessApplicationPlugin pluginMetadata = pluginClass.getAnnotation(BusinessApplicationPlugin.class);
    if (pluginMetadata == null) {
      throw new IllegalArgumentException(className
        + " does not have the annotation " + BusinessApplicationPlugin.class);
    } else {
      String businessApplicationName = pluginMetadata.name();
      if (businessApplicationName == null
        || businessApplicationName.trim().length() == 0) {
        businessApplicationName = className.substring(
          className.lastIndexOf('.') + 1).replaceAll("Plugin$", "");
      }

      final BusinessApplication businessApplication = new BusinessApplication(
        pluginMetadata, this, businessApplicationName);
      businessApplication.setCoordinateSystems(coordinateSystems);

      final String descriptionUrl = pluginMetadata.descriptionUrl();
      businessApplication.setDescriptionUrl(descriptionUrl);

      final String description = pluginMetadata.description();
      businessApplication.setDescription(description);

      final String[] compatibleVersions = pluginMetadata.compatibleVersions();
      businessApplication.setCompatibleVersions(compatibleVersions);

      final String title = pluginMetadata.title();
      if (title != null && title.trim().length() > 0) {
        businessApplication.setTitle(title);
      }

      String version = pluginMetadata.version();
      if (version == null || version.trim().length() == 0) {
        version = "1.0.0";
      }
      businessApplication.setVersion(version);

      final String instantModePermission = pluginMetadata.instantModePermission();
      businessApplication.setInstantModePermission(instantModePermission);

      final String batchModePermission = pluginMetadata.batchModePermission();
      businessApplication.setBatchModePermission(batchModePermission);

      final boolean perRequestInputData = pluginMetadata.perRequestInputData();
      businessApplication.setPerRequestInputData(perRequestInputData);

      final boolean perRequestResultData = pluginMetadata.perRequestResultData();
      businessApplication.setPerRequestResultData(perRequestResultData);

      final int maxRequestsPerJob = pluginMetadata.maxRequestsPerJob();
      businessApplication.setMaxRequestsPerJob(maxRequestsPerJob);

      final int numRequestsPerWorker = pluginMetadata.numRequestsPerWorker();
      businessApplication.setNumRequestsPerWorker(numRequestsPerWorker);

      final int maxConcurrentRequests = pluginMetadata.maxConcurrentRequests();
      businessApplication.setMaxConcurrentRequests(maxConcurrentRequests);

      final String logLevel = pluginMetadata.logLevel();
      businessApplication.setLogLevel(logLevel);

      final GeometryConfiguration geometryConfiguration = pluginClass.getAnnotation(GeometryConfiguration.class);
      if (geometryConfiguration != null) {
        final GeometryFactory geometryFactory = getGeometryFactory(
          GeometryFactory.getFactory(), className, geometryConfiguration);
        businessApplication.setGeometryFactory(geometryFactory);
        final boolean validateGeometry = geometryConfiguration.validate();
        businessApplication.setValidateGeometry(validateGeometry);
      }

      Method resultListMethod = null;
      final List<Method> methods = JavaBeanUtil.getMethods(pluginClass);
      for (final Method method : methods) {
        processParameter(pluginClass, businessApplication, method);
        processResultAttribute(pluginClass, businessApplication, method, false);
        if (method.isAnnotationPresent(ResultList.class)) {
          if (resultListMethod == null) {
            resultListMethod = method;
          } else {
            throw new IllegalArgumentException("Business Application "
              + businessApplicationName
              + " may only have one method with the annotation "
              + ResultList.class);
          }
        }
      }
      if (perRequestResultData) {
        final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
        try {
          pluginClass.getMethod("setResultData", OutputStream.class);
        } catch (final Throwable e) {
          throw new IllegalArgumentException(
            "Business Application "
              + businessApplicationName
              + " must have a public voud setResultData(OutputStrean resultData) method",
            e);
        }
        try {
          pluginClass.getMethod("setResultDataContentType", String.class);
        } catch (final Throwable e) {
          throw new IllegalArgumentException(
            "Business Application "
              + businessApplicationName
              + " must have a public voud setResultDataContentType(String resultContentType) method",
            e);
        }
        businessApplication.setPerRequestResultData(true);
        if (resultMetaData.getAttributeCount() > 0) {
          throw new IllegalArgumentException("Business Application "
            + businessApplicationName
            + " cannot have a setResultData method and result fields");
        } else if (resultListMethod != null) {
          throw new IllegalArgumentException(
            "Business Application "
              + businessApplicationName
              + " cannot have a setResultData method and a method with the annotation "
              + ResultList.class);
        }

      } else {
        if (resultListMethod == null) {
          final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
          if (resultMetaData.getAttributeCount() == 0) {
            throw new IllegalArgumentException("Business Application "
              + businessApplicationName + " must have result fields");
          }
        } else {
          processResultListMethod(businessApplication, resultListMethod);
        }
      }

      final IoFactoryRegistry ioFactoryRegistry = IoFactoryRegistry.getInstance();
      final String[] inputDataContentTypes = pluginMetadata.inputDataContentTypes();
      if (perRequestInputData) {
        if (inputDataContentTypes.length == 0) {
          businessApplication.addInputDataContentType("*/*", "Any content type");
        } else {
          for (final String contentType : inputDataContentTypes) {
            businessApplication.addInputDataContentType(contentType,
              contentType);
          }
        }
      } else {
        if (inputDataContentTypes.length == 0) {
          final Set<MapReaderFactory> factories = ioFactoryRegistry.getFactories(MapReaderFactory.class);
          for (final MapReaderFactory factory : factories) {
            if (factory.isSingleFile()) {
              if (factory.isCustomAttributionSupported()) {
                for (final String contentType : factory.getMediaTypes()) {
                  final String typeDescription = factory.getName() + " ("
                    + contentType + ")";
                  businessApplication.addInputDataContentType(contentType,
                    typeDescription);
                }
              }
            }
          }
        } else {
          for (final String contentType : inputDataContentTypes) {
            final MapReaderFactory factory = ioFactoryRegistry.getFactoryByMediaType(
              MapReaderFactory.class, contentType);
            if (factory.isSingleFile()) {
              if (factory.isCustomAttributionSupported()) {
                final String typeDescription = factory.getName() + " ("
                  + contentType + ")";
                businessApplication.addInputDataContentType(contentType,
                  typeDescription);
              }
            }
          }
        }
      }

      final String[] resultDataContentTypes = pluginMetadata.resultDataContentTypes();
      if (perRequestResultData) {
        if (resultDataContentTypes.length == 0) {
          businessApplication.addResultDataContentType("*/*",
            "Any content type");
        } else {
          for (final String contentType : resultDataContentTypes) {
            businessApplication.addResultDataContentType(contentType,
              contentType);
          }
        }
      } else {
        final boolean hasResultGeometry = businessApplication.isHasGeometryResultAttribute();
        if (resultDataContentTypes.length == 0) {
          final Set<DataObjectWriterFactory> writerFactories = ioFactoryRegistry.getFactories(DataObjectWriterFactory.class);
          for (final DataObjectWriterFactory factory : writerFactories) {
            if (factory.isSingleFile()) {
              if (!hasResultGeometry || factory.isGeometrySupported()) {
                if (factory.isCustomAttributionSupported()) {
                  for (final String contentType : factory.getMediaTypes()) {
                    final String typeDescription = factory.getName() + " ("
                      + contentType + ")";
                    businessApplication.addResultDataContentType(contentType,
                      typeDescription);
                  }
                }
              }
            }
          }
        } else {
          for (final String contentType : resultDataContentTypes) {
            final DataObjectWriterFactory factory = ioFactoryRegistry.getFactoryByMediaType(
              DataObjectWriterFactory.class, contentType);
            if (factory.isSingleFile()) {
              if (!hasResultGeometry || factory.isGeometrySupported()) {
                if (factory.isCustomAttributionSupported()) {
                  final String typeDescription = factory.getName() + " ("
                    + contentType + ")";
                  businessApplication.addResultDataContentType(contentType,
                    typeDescription);
                }
              }
            }
          }
        }
      }

      final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
      final Attribute resultDataContentType = requestMetaData.getAttribute("resultDataContentType");
      final Map<String, String> resultDataContentTypeMap = businessApplication.getResultDataContentTypes();
      resultDataContentType.setAllowedValues(resultDataContentTypeMap);

      final String defaultResultDataContentType = BusinessApplication.getDefaultMimeType(resultDataContentTypeMap);
      resultDataContentType.setDefaultValue(defaultResultDataContentType);

      try {
        pluginClass.getMethod("setSecurityService", SecurityService.class);
        businessApplication.setSecurityServiceRequired(true);
      } catch (final NoSuchMethodException e) {
      } catch (final Throwable e) {
        throw new IllegalArgumentException(
          "Business Application "
            + businessApplicationName
            + " has a setSecurityService(SecurityService) method but there was an error accessing it",
          e);
      }

      final Map<String, Object> configProperties = getConfigProperties(
        moduleName, "APP_" + businessApplicationName.toUpperCase());
      Property.set(businessApplication, configProperties);
      return businessApplication;
    }
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  private Map<String, Object> getConfigProperties(final String moduleName,
    final String componentName) {
    if (configPropertyLoader == null) {
      return new HashMap<String, Object>();
    } else {
      final Map<String, Object> configProperties = configPropertyLoader.getConfigProperties(
        moduleName, componentName);
      return configProperties;
    }
  }

  public ConfigPropertyLoader getConfigPropertyLoader() {
    return configPropertyLoader;
  }

  @Override
  public URL getConfigUrl() {
    return configUrl;
  }

  /**
   * Get the geometry factory instance for the specified geometry configuration.
   * 
   * @param geometryFactory
   * @param message The message to prefix any log messages with.
   * @param geometryConfiguration The geometry configuration.
   * @return The geometry factory.
   */
  private GeometryFactory getGeometryFactory(
    final GeometryFactory geometryFactory, final String message,
    final GeometryConfiguration geometryConfiguration) {
    int srid = geometryConfiguration.srid();
    if (srid < 0) {
      LOG.warn(message + " srid must be >= 0");
      srid = geometryFactory.getSRID();
    } else if (srid == 0) {
      srid = geometryFactory.getSRID();
    }
    int numAxis = geometryConfiguration.numAxis();
    if (numAxis == 0) {
      numAxis = geometryFactory.getNumAxis();
    } else if (numAxis < 2) {
      LOG.warn(message + " numAxis must be >= 2");
      numAxis = 2;
    } else if (numAxis > 3) {
      LOG.warn(message + " numAxis must be <= 3");
      numAxis = 3;
    }
    double scaleXy = geometryConfiguration.scaleFactorXy();
    if (scaleXy == 0) {
      scaleXy = geometryFactory.getScaleXY();
    } else if (scaleXy < 0) {
      LOG.warn(message + " scaleXy must be >= 0");
      scaleXy = geometryFactory.getScaleXY();
    }
    double scaleZ = geometryConfiguration.scaleFactorZ();
    if (scaleXy == 0) {
      scaleXy = geometryFactory.getScaleZ();
    } else if (scaleZ < 0) {
      LOG.warn(message + " scaleZ must be >= 0");
      scaleZ = geometryFactory.getScaleZ();
    }
    return GeometryFactory.getFactory(srid, numAxis, scaleXy, scaleZ);
  }

  public Set<String> getGroupNamesToDelete() {
    return groupNamesToDelete;
  }

  @Override
  public List<URL> getJarUrls() {
    if (isEnabled()) {
      final ClassLoader classLoader = getClassLoader();
      if (classLoader instanceof URLClassLoader) {
        final URLClassLoader urlClassLoader = (URLClassLoader)classLoader;
        final URL[] urls = urlClassLoader.getURLs();
        final List<URL> jarUrls = new ArrayList<URL>();
        for (final URL url : urls) {
          jarUrls.add(url);
        }
        return jarUrls;
      }
    }
    return Collections.emptyList();
  }

  @Override
  public String getModuleDescriptor() {
    if (classLoader == null) {
      return "Class Loader Module undefined";
    } else {
      return classLoader.toString();
    }
  }

  @Override
  public String getModuleError() {
    return moduleError;
  }

  @Override
  public String getModuleType() {
    return "ClassLoader";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, Set<ResourcePermission>> getPermissionsByGroupName() {
    return permissionsByGroupName;
  }

  @Override
  public Date getStartedDate() {
    return startedDate;
  }

  @Override
  public long getStartedTime() {
    if (startedDate == null) {
      return -1;
    } else {
      return startedDate.getTime();
    }
  }

  private List<Map<String, Object>> getUserGroupMaps() {
    try {
      final ClassLoader classLoader = getClassLoader();
      if (!hasError()) {
        final String parentUrl = UrlUtil.getParentString(configUrl);
        final Enumeration<URL> urls = classLoader.getResources("META-INF/ca.bc.gov.open.cpf.plugin.UserGroups.json");
        while (urls.hasMoreElements()) {
          final URL userGroups = urls.nextElement();
          if (userGroups.toString().startsWith(parentUrl)) {
            final Resource resource = new UrlResource(userGroups);
            final Reader<Map<String, Object>> reader = AbstractMapReaderFactory.mapReader(resource);
            return reader.read();
          }
        }
      }
    } catch (final Throwable t) {
      addModuleError(t);
    }
    return Collections.emptyList();
  }

  @Override
  public boolean hasBusinessApplication(final String businessApplicationName) {
    return getBusinessApplication(businessApplicationName) != null;
  }

  public boolean hasError() {
    return StringUtils.hasText(moduleError);
  }

  private void initAppLogAppender(final String businessApplicationName,
    final Date date) {
    final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(businessApplicationName);
    synchronized (logger) {
      logger.removeAllAppenders();
      logger.setLevel(Level.DEBUG);
      final File logDirectory = businessApplicationRegistry.getAppLogDirectory();
      if (logDirectory == null
        || !(logDirectory.exists() || logDirectory.mkdirs())) {
        logger.setAdditivity(true);
      } else {
        logger.setAdditivity(false);

        final File appLogDirectory = new File(logDirectory,
          businessApplicationName);
        appLogDirectory.mkdirs();

        final SimpleDateFormat dateFormat = new SimpleDateFormat(
          "yyyyMMdd-hhmmssS");
        final String activeFileName = FileUtil.getFile(appLogDirectory,
          businessApplicationName + "-" + dateFormat.format(date) + ".log")
          .toString();
        final FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setActiveFileName(activeFileName);
        final String fileNamePattern = FileUtil.getFile(appLogDirectory,
          businessApplicationName + "-" + dateFormat.format(date) + ".%i.log")
          .toString();
        rollingPolicy.setFileNamePattern(fileNamePattern);

        final RollingFileAppender appender = new RollingFileAppender();
        appender.setName(businessApplicationName);
        appender.setFile(activeFileName);
        appender.setLayout(new PatternLayout("%d\t%p\t%c\t%m%n"));
        appender.setRollingPolicy(rollingPolicy);
        appender.setTriggeringPolicy(new SizeBasedTriggeringPolicy(
          1024 * 1024 * 10));
        appender.activateOptions();
        logger.addAppender(appender);
      }
    }
  }

  private void initializeGroupPermissions() {
    try {
      final Map<String, Set<ResourcePermission>> permissionsByGroupName = new HashMap<String, Set<ResourcePermission>>();
      final Set<String> groupNamesToDelete = new HashSet<String>();
      final Map<String, Map<String, Object>> groupsByName = new HashMap<String, Map<String, Object>>();
      for (final Map<String, Object> pluginGroup : getUserGroupMaps()) {
        String groupName = (String)pluginGroup.get("name");
        if (groupName == null) {
          addModuleError("A UserGroup must have a name: " + pluginGroup);
        } else {
          groupName = groupName.toUpperCase();
          final String action = (String)pluginGroup.get("action");
          if (groupsByName.containsKey(groupName)) {
            addModuleError("A UserGroup must have a unique name: "
              + pluginGroup);
          } else if (groupNamesToDelete.contains(groupName)) {
            addModuleError("A UserGroup cannot be deleted and created in the same file: "
              + pluginGroup);
          } else if (groupName.startsWith("ROLE_" + name.toUpperCase())
            && "delete".equalsIgnoreCase(action)) {
            groupNamesToDelete.add(groupName);
          } else {
            groupsByName.put(groupName, pluginGroup);
            addResourcePermissions(permissionsByGroupName, groupName,
              pluginGroup);
          }
        }
      }
      this.permissionsByGroupName = permissionsByGroupName;
      this.groupNamesToDelete = groupNamesToDelete;
    } catch (final Throwable t) {
      addModuleError(t);
    }
  }

  @Override
  public boolean isApplicationsLoaded() {
    return applicationsLoaded;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public boolean isGroupNameValid(final String groupName) {
    return true;
  }

  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public boolean isRelaodable() {
    return false;
  }

  @Override
  public boolean isRemoteable() {
    return remoteable;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public void loadApplications() {
    if (isStarted() && !isApplicationsLoaded()) {

      LOG.debug("Loading spring config file " + configUrl);
      try {
        final ClassLoader classLoader = getClassLoader();
        applicationContext = new GenericApplicationContext();
        applicationContext.setClassLoader(classLoader);

        AnnotationConfigUtils.registerAnnotationConfigProcessors(
          applicationContext, null);
        final AttributesBeanConfigurer attributesConfig = new AttributesBeanConfigurer(
          applicationContext);
        applicationContext.addBeanFactoryPostProcessor(attributesConfig);
        registerConfigPropertyBeans(name, applicationContext, configUrl);

        final XmlBeanDefinitionReader beanReader = new XmlBeanDefinitionReader(
          applicationContext);
        beanReader.setBeanClassLoader(classLoader);
        beanReader.loadBeanDefinitions(new UrlResource(configUrl));
        if (applicationContext.containsBeanDefinition("beanImports")) {
          @SuppressWarnings("unchecked")
          final List<String> beanImports = (List<String>)applicationContext.getBean("beanImports");
          for (final String beanImport : beanImports) {
            try {
              final Resource[] resources = applicationContext.getResources(beanImport);
              for (final Resource resource : resources) {
                beanReader.loadBeanDefinitions(resource);
              }
            } catch (final Throwable e) {
              addModuleError("Error loading bean import " + beanImport
                + " from " + configUrl, e);
            }
          }
        }
        if (!hasError()) {
          applicationContext.refresh();
          applicationsLoaded = true;
        }
      } catch (final Throwable t) {
        addModuleError(t);
      } finally {
      }
    }
  }

  private BusinessApplication loadBusinessApplication(
    final Map<String, BusinessApplication> businessApplicationsByName,
    final String moduleName, final String pluginClassName) {
    try {
      if (isEnabled()) {
        final ClassLoader classLoader = getClassLoader();
        LOG.info("Loading plugin " + pluginClassName);
        final Class<?> pluginClass = Class.forName(pluginClassName.trim(),
          true, classLoader);
        final BusinessApplication businessApplication = getBusinessApplicaton(
          moduleName, pluginClass);
        final String pluginName = businessApplication.getName();
        businessApplicationsByName.put(pluginName, businessApplication);
        return businessApplication;
      }
    } catch (final Throwable e) {
      addModuleError("Error loading plugin " + pluginClassName, e);
    }
    return null;

  }

  @SuppressWarnings("unchecked")
  private void loadBusinessApplications() {
    final Date date = new Date(System.currentTimeMillis());
    final Set<String> businessApplicationNames = new TreeSet<String>();
    final Map<BusinessApplication, String> businessApplicationsToBeanNames = new HashMap<BusinessApplication, String>();
    moduleError = null;
    final Map<String, BusinessApplication> businessApplicationsByName = new HashMap<String, BusinessApplication>();
    LOG.debug("Loading spring config file " + configUrl);
    final GenericApplicationContext applicationContext = new GenericApplicationContext();
    try {
      final ClassLoader classLoader = getClassLoader();
      applicationContext.setClassLoader(classLoader);
      final XmlBeanDefinitionReader beanReader = new XmlBeanDefinitionReader(
        applicationContext);
      beanReader.setBeanClassLoader(classLoader);
      beanReader.loadBeanDefinitions(new UrlResource(configUrl));

      Map<String, Map<String, Object>> propertiesByName;
      if (applicationContext.containsBean("properties")) {
        propertiesByName = (Map<String, Map<String, Object>>)applicationContext.getBean("properties");
      } else {
        propertiesByName = Collections.emptyMap();
      }
      Map<String, Object> defaultProperties = propertiesByName.get("default");
      if (defaultProperties == null) {
        defaultProperties = Collections.emptyMap();
      }
      for (final String beanName : applicationContext.getBeanDefinitionNames()) {
        try {
          final BeanDefinition beanDefinition = applicationContext.getBeanDefinition(beanName);
          final String pluginClassName = beanDefinition.getBeanClassName();
          if (applicationContext.findAnnotationOnBean(beanName,
            BusinessApplicationPlugin.class) != null) {
            final String scope = beanDefinition.getScope();
            if (BeanDefinition.SCOPE_PROTOTYPE.equals(scope)) {
              final BusinessApplication businessApplication = loadBusinessApplication(
                businessApplicationsByName, name, pluginClassName);
              if (businessApplication != null) {
                final String businessApplicationName = businessApplication.getName();
                initAppLogAppender(businessApplicationName, date);
                businessApplicationNames.add(businessApplicationName);
                businessApplicationsToBeanNames.put(businessApplication,
                  beanName);
                businessApplication.setProperties(defaultProperties);
                final Map<String, Object> properties = propertiesByName.get(businessApplicationName);
                businessApplication.setProperties(properties);
                final String componentName = "APP_" + businessApplicationName;
                final ConfigPropertyLoader propertyLoader = getConfigPropertyLoader();
                final String moduleName = getName();
                if (propertyLoader != null) {
                  final Map<String, Object> configProperties = propertyLoader.getConfigProperties(
                    moduleName, componentName);
                  if (configProperties != null) {
                    for (final Entry<String, Object> entry : configProperties.entrySet()) {
                      final String propertyName = entry.getKey();
                      final Object propertyValue = entry.getValue();
                      try {
                        JavaBeanUtil.setProperty(businessApplication,
                          propertyName, propertyValue);
                      } catch (final Throwable t) {
                        LOG.error("Unable to set " + businessApplicationName
                          + "." + propertyName + "=" + propertyValue);
                      }
                    }
                  }
                }
              }
            } else {
              addModuleError("Plugin bean scope " + scope
                + " != expected value prototype " + pluginClassName + " from "
                + configUrl);
            }
          } else if (beanName.equals("properties")) {

          } else if (!beanName.equals("beanImports")
            && !beanName.equals("name") && !beanName.equals("properties")) {
            addModuleError("Plugin spring file cannot have any non-plugin beans "
              + beanName + " class=" + pluginClassName + " from " + configUrl);
          }
        } catch (final Throwable e) {
          addModuleError("Error loading plugin " + beanName + " from "
            + configUrl, e);
        }
      }
      registerConfigPropertyBeans(name, applicationContext, configUrl);
    } finally {
      applicationContext.close();
    }
    if (hasError()) {
      stop();
    } else {
      this.businessApplicationNames = new ArrayList<String>(
        businessApplicationNames);
      if (startedDate == null) {
        startedDate = date;
      }
      started = true;
      this.businessApplicationsByName = businessApplicationsByName;
      this.businessApplicationsToBeanNames = businessApplicationsToBeanNames;
    }
  }

  protected void preLoadApplications() {

  }

  @SuppressWarnings("unchecked")
  private void processParameter(final Class<?> pluginClass,
    final BusinessApplication businessApplication, final Method method) {
    final String methodName = method.getName();

    String descriptionUrl = null;
    final JobParameter jobParameterMetadata = method.getAnnotation(JobParameter.class);
    if (jobParameterMetadata != null) {
      final String jobDescriptionUrl = jobParameterMetadata.descriptionUrl();
      if (StringUtils.hasText(jobDescriptionUrl)) {
        descriptionUrl = jobDescriptionUrl;
      }
    }
    final RequestParameter requestParameterMetadata = method.getAnnotation(RequestParameter.class);
    if (requestParameterMetadata != null) {
      final String requestDescriptionUrl = requestParameterMetadata.descriptionUrl();
      if (StringUtils.hasText(requestDescriptionUrl)) {
        descriptionUrl = requestDescriptionUrl;
      }
    }
    final boolean requestParameter = requestParameterMetadata != null;
    final boolean jobParameter = jobParameterMetadata != null;
    if (requestParameter || jobParameter) {
      final Class<?>[] parameterTypes = method.getParameterTypes();
      final Class<?>[] standardMethodParameters = STANDARD_METHODS.get(methodName);
      if (standardMethodParameters == null) {
        if (methodName.startsWith("set") && parameterTypes.length == 1) {
          String description;
          int length;
          int scale;
          if (requestParameter) {
            description = requestParameterMetadata.description();
            length = requestParameterMetadata.length();
            scale = requestParameterMetadata.scale();
          } else {
            description = jobParameterMetadata.description();
            length = jobParameterMetadata.length();
            scale = jobParameterMetadata.scale();
          }
          final String parameterName = methodName.substring(3, 4).toLowerCase()
            + methodName.substring(4);
          final boolean required = method.getAnnotation(Required.class) != null;
          final AllowedValues allowedValuesMetadata = method.getAnnotation(AllowedValues.class);
          String[] allowedValues = {};
          if (allowedValuesMetadata != null) {
            allowedValues = allowedValuesMetadata.value();
          }

          final boolean perRequestInputData = businessApplication.isPerRequestInputData();
          final Class<?> parameterType = parameterTypes[0];
          if (perRequestInputData) {
            if (requestParameter) {
              throw new IllegalArgumentException(
                pluginClass.getName()
                  + "."
                  + method
                  + " cannot be a RequestParameter, only setInputDataUrl(Url url) or setInputDataContentType(Url url) can be specified for per request input data plug-ins");
            }
          }

          final DataType dataType = DataTypes.getType(parameterType);
          if (dataType == null) {
            throw new IllegalArgumentException(pluginClass.getName() + "."
              + method.getName() + " has an unsupported return type "
              + parameterType);
          } else {
            int index = -1;
            if (jobParameterMetadata != null) {
              final int jobParameterIndex = jobParameterMetadata.index();
              if (jobParameterIndex != -1) {
                index = jobParameterIndex;
              }
            }
            if (index == -1 && requestParameterMetadata != null) {
              final int requestParameterIndex = requestParameterMetadata.index();
              if (requestParameterIndex != -1) {
                index = 100000 + requestParameterIndex;
              }
            }
            final Attribute attribute = new Attribute(parameterName, dataType,
              length, scale, required, description);
            attribute.setAllowedValues(Arrays.asList(allowedValues));

            final DefaultValue defaultValueMetadata = method.getAnnotation(DefaultValue.class);
            if (defaultValueMetadata != null) {
              final String defaultValueString = defaultValueMetadata.value();
              final Class<Object> dataTypeClass = (Class<Object>)dataType.getJavaClass();
              final Object defaultValue = StringConverterRegistry.toObject(
                dataTypeClass, defaultValueString);
              attribute.setDefaultValue(defaultValue);
            }

            if (jobParameter) {
              attribute.setProperty(BusinessApplication.JOB_PARAMETER, true);
            }
            if (requestParameter) {
              attribute.setProperty(BusinessApplication.REQUEST_PARAMETER, true);
            }
            final GeometryConfiguration geometryConfiguration = pluginClass.getAnnotation(GeometryConfiguration.class);
            if (Geometry.class.isAssignableFrom(parameterType)) {
              GeometryFactory geometryFactory = businessApplication.getGeometryFactory();
              boolean validateGeometry = businessApplication.isValidateGeometry();
              if (geometryConfiguration != null) {
                geometryFactory = getGeometryFactory(geometryFactory,
                  businessApplication.getName() + "." + methodName,
                  geometryConfiguration);
                businessApplication.setGeometryFactory(geometryFactory);
                validateGeometry = geometryConfiguration.validate();
              }
              attribute.setProperty(AttributeProperties.GEOMETRY_FACTORY,
                geometryFactory);
              attribute.setProperty(AttributeProperties.VALIDATE_GEOMETRY,
                validateGeometry);
            } else if (geometryConfiguration != null) {
              throw new IllegalArgumentException(
                pluginClass.getName()
                  + "."
                  + method.getName()
                  + " cannot have a geometry configuration as is not a geometry attribute");
            }
            if (descriptionUrl != null) {
              attribute.setProperty("descriptionUrl", descriptionUrl);
            }
            businessApplication.addRequestAttribute(index, attribute);
          }
        } else {
          throw new IllegalArgumentException(pluginClass.getName() + "."
            + method.getName() + " has the " + RequestParameter.class.getName()
            + " or " + JobParameter.class.getName()
            + " annotation but is not a setXXX(value) method");
        }
      } else {
        checkStandardMethod(method, standardMethodParameters);
      }
    }
  }

  private void processResultAttribute(final Class<?> pluginClass,
    final BusinessApplication businessApplication, final Method method,
    final boolean resultList) {
    final String methodName = method.getName();

    final ResultAttribute fieldMetadata = method.getAnnotation(ResultAttribute.class);
    if (methodName.equals("getCustomizationProperties")) {
      if (method.getParameterTypes().length == 0
        && method.getReturnType() == Map.class
        && Modifier.isPublic(method.getModifiers())) {
        if (resultList) {
          businessApplication.setHasResultListCustomizationProperties(true);
        } else {
          businessApplication.setHasCustomizationProperties(true);
        }
      } else {
        throw new IllegalArgumentException(
          "Method must have signature public Map<String,Object> getCustomizationProperties() not "
            + method);
      }

    } else if (fieldMetadata != null) {
      if ((methodName.startsWith("get") && methodName.length() > 3)
        || (methodName.startsWith("is") && methodName.length() > 2)) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
          final String attributeName = JavaBeanUtil.getPropertyName(methodName);
          final String description = fieldMetadata.description();
          final Class<?> returnType = method.getReturnType();
          final DataType dataType = DataTypes.getType(returnType);
          if (dataType == null) {
            throw new IllegalArgumentException(pluginClass.getName() + "."
              + method.getName() + " has an unsupported return type "
              + returnType);
          } else {
            final int index = fieldMetadata.index();
            final int length = fieldMetadata.length();
            final int scale = fieldMetadata.scale();
            final boolean required = method.getAnnotation(Required.class) != null;
            final Attribute attribute = new Attribute(attributeName, dataType,
              length, scale, required, description);
            final GeometryConfiguration geometryConfiguration = method.getAnnotation(GeometryConfiguration.class);
            if (Geometry.class.isAssignableFrom(returnType)) {
              GeometryFactory geometryFactory = businessApplication.getGeometryFactory();
              boolean validateGeometry = businessApplication.isValidateGeometry();
              if (geometryConfiguration != null) {
                geometryFactory = getGeometryFactory(geometryFactory,
                  businessApplication.getName() + "." + methodName,
                  geometryConfiguration);
                businessApplication.setGeometryFactory(geometryFactory);
                validateGeometry = geometryConfiguration.validate();
              }
              attribute.setProperty(AttributeProperties.GEOMETRY_FACTORY,
                geometryFactory);
              attribute.setProperty(AttributeProperties.VALIDATE_GEOMETRY,
                validateGeometry);
            } else if (geometryConfiguration != null) {
              throw new IllegalArgumentException(
                pluginClass.getName()
                  + "."
                  + method.getName()
                  + " cannot have a geometry configuration as is not a geometry attribute");
            }
            businessApplication.addResultAttribute(index, attribute);
          }
        }
      } else {
        throw new IllegalArgumentException(pluginClass.getName() + "."
          + method.getName() + " has the " + ResultAttribute.class.getName()
          + " annotation but is not a getXXX() or isXXX() method");
      }
    }
  }

  private void processResultListMethod(
    final BusinessApplication businessApplication, final Method resultListMethod) {
    final String businessApplicationName = businessApplication.getName();
    DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
    if (resultMetaData.getAttributeCount() > 0) {
      throw new IllegalArgumentException("Business Application "
        + businessApplicationName
        + " may not have result fields and the annotation " + ResultList.class);
    }
    final String methodName = resultListMethod.getName();
    final String resultListProperty = JavaBeanUtil.getPropertyName(methodName);
    businessApplication.setResultListProperty(resultListProperty);
    try {
      final Class<?> resultClass = JavaBeanUtil.getTypeParameterClass(
        resultListMethod, List.class);
      for (final Method method : JavaBeanUtil.getMethods(resultClass)) {
        processResultAttribute(resultClass, businessApplication, method, true);
      }
      resultMetaData = businessApplication.getResultMetaData();
      if (resultMetaData.getAttributeCount() == 0) {
        throw new IllegalArgumentException("Business Application "
          + businessApplicationName + " result class " + resultClass.getName()
          + " must have result fields");
      }
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Business Application "
        + businessApplicationName + " method " + e.getMessage());
    }
  }

  private void registerConfigPropertyBeans(final String moduleName,
    final GenericApplicationContext applicationContext, final URL url) {
    final Map<String, Object> configProperties = getConfigProperties(
      moduleName, "MODULE_BEAN_PROPERTY");

    if (!configProperties.isEmpty()) {
      final GenericBeanDefinition propertiesBeanDefinition = new GenericBeanDefinition();
      propertiesBeanDefinition.setBeanClass(AttributeMap.class);
      final ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
      constructorArgumentValues.addGenericArgumentValue(configProperties);
      propertiesBeanDefinition.setConstructorArgumentValues(constructorArgumentValues);
      final String beanName = BeanDefinitionReaderUtils.generateBeanName(
        propertiesBeanDefinition, applicationContext);
      applicationContext.registerBeanDefinition(beanName,
        propertiesBeanDefinition);
    }
  }

  @Override
  public void restart() {
    getBusinessApplicationRegistry().restartModule(name);
  }

  public void setClassLoader(final ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public void setConfigPropertyLoader(
    final ConfigPropertyLoader configPropertyLoader) {
    this.configPropertyLoader = configPropertyLoader;
  }

  protected void setConfigUrl(final URL configUrl) {
    this.configUrl = configUrl;
  }

  protected void setRemoteable(final boolean remoteable) {
    this.remoteable = remoteable;
  }

  public void setStartedDate(final Date date) {
    this.startedDate = date;
  }

  @Override
  @PostConstruct
  public void start() {
    final BusinessApplicationRegistry businessApplicationRegistry = getBusinessApplicationRegistry();
    if (businessApplicationRegistry != null) {
      businessApplicationRegistry.startModule(name);
    }
  }

  @Override
  public void stop() {
    final BusinessApplicationRegistry businessApplicationRegistry = getBusinessApplicationRegistry();
    if (businessApplicationRegistry != null) {
      businessApplicationRegistry.stopModule(name);
    }
  }

}
