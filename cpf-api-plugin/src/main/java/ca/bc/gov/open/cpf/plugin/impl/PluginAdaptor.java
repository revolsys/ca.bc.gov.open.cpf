package ca.bc.gov.open.cpf.plugin.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;

import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.AttributeProperties;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.io.LazyHttpPostOutputStream;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.ExceptionUtil;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.util.MathUtil;
import com.revolsys.util.Property;
import com.revolsys.util.UrlUtil;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PluginAdaptor {

  private static final String[] INPUT_DATA_PARAMETER_NAMES = new String[] {
    "inputDataUrl", "inputDataContentType"
  };

  private final Object plugin;

  private final BusinessApplication application;

  private Map<String, Object> responseFields;

  private SecurityService securityService;

  private Map<String, Object> testParameters = new HashMap<>();

  private final List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

  private static final List<String> INTERNAL_PROPERTY_NAMES = Arrays.asList(
    "sequenceNumber", "resultNumber");

  private final Map<String, Object> parameters = new HashMap<String, Object>();

  private final AppLog appLog;

  private Map<String, Object> customizationProperties = Collections.emptyMap();

  public PluginAdaptor(final BusinessApplication application,
    final Object plugin, String executionId, final String logLevel) {
    this.application = application;
    this.plugin = plugin;
    if (!StringUtils.hasText(logLevel)) {
      executionId = String.valueOf(System.currentTimeMillis());
    }
    appLog = new AppLog(application.getName(), executionId, logLevel);
    try {
      final Class<? extends Object> pluginClass = plugin.getClass();
      final Method setAppLogMethod = pluginClass.getMethod("setAppLog",
        AppLog.class);
      try {
        setAppLogMethod.invoke(plugin, appLog);
      } catch (final IllegalAccessException e) {
        ExceptionUtil.throwCauseException(e);
      } catch (final InvocationTargetException e) {
        ExceptionUtil.throwCauseException(e);
      }
    } catch (final NoSuchMethodException e) {
    }
  }

  public void addTestParameter(final String name, final Object value) {
    testParameters.put(name, value);
  }

  @SuppressWarnings("unchecked")
  public void execute() {
    final String resultListProperty = application.getResultListProperty();
    if (application.isHasCustomizationProperties()) {
      customizationProperties = (Map<String, Object>)Property.get(plugin,
        "customizationProperties");
    }

    final boolean testMode = application.isTestModeEnabled()
      && BooleanStringConverter.isTrue(testParameters.get("cpfPluginTest"));
    try {
      if (testMode) {
        double minTime = CollectionUtil.getDouble(testParameters,
          "cpfMinExecutionTime", -1.0);
        double maxTime = CollectionUtil.getDouble(testParameters,
          "cpfMaxExecutionTime", -1.0);
        final double meanTime = CollectionUtil.getDouble(testParameters,
          "cpfMeanExecutionTime", -1.0);
        final double standardDeviation = CollectionUtil.getDouble(
          testParameters, "cpfStandardDeviation", -1.0);
        double executionTime;
        if (standardDeviation <= 0) {
          if (minTime < 0) {
            minTime = 0.0;
          }
          if (maxTime < minTime) {
            maxTime = minTime + 10;
          }
          executionTime = MathUtil.randomRange(minTime, maxTime);
        } else {
          executionTime = MathUtil.randomGaussian(meanTime, standardDeviation);
        }
        if (minTime >= 0 && executionTime < minTime) {
          executionTime = minTime;
        }
        if (maxTime > 0 && maxTime > minTime && executionTime > maxTime) {
          executionTime = maxTime;
        }
        final long milliSeconds = (long)(executionTime * 1000);
        if (milliSeconds > 0) {
          ThreadUtil.pause(milliSeconds);
        }
        if (application.isHasTestExecuteMethod()) {
          MethodUtils.invokeExactMethod(plugin, "testExecute", new Object[0]);
        }
      } else {
        MethodUtils.invokeExactMethod(plugin, "execute", new Object[0]);
      }
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException)cause;
      } else if (cause instanceof Error) {
        throw (Error)cause;
      } else {
        throw new RuntimeException("Unable to invoke execute on "
          + application.getName(), cause);
      }
    } catch (final Throwable t) {
      throw new RuntimeException("Unable to invoke execute on "
        + application.getName(), t);
    }
    if (resultListProperty == null) {
      this.responseFields = getResult(plugin, false, testMode);
      results.add(responseFields);
    } else {
      final List<Object> resultObjects = JavaBeanUtil.getProperty(plugin,
        resultListProperty);
      if (resultObjects == null || resultObjects.isEmpty()) {
        if (testMode) {
          final double meanNumResults = CollectionUtil.getDouble(
            testParameters, "cpfMeanNumResults", 3.0);
          final int numResults = (int)Math.round(MathUtil.randomGaussian(
            meanNumResults, meanNumResults / 5));
          for (int i = 0; i < numResults; i++) {
            final Map<String, Object> result = getResult(plugin, true, testMode);
            results.add(result);
          }
        }
      } else {
        for (final Object resultObject : resultObjects) {
          final Map<String, Object> result = getResult(resultObject, true,
            false);
          results.add(result);
        }
      }
    }
  }

  public BusinessApplication getApplication() {
    return application;
  }

  public AppLog getAppLog() {
    return appLog;
  }

  public Map<String, Object> getResponseFields() {
    return responseFields;
  }

  private Map<String, Object> getResult(final Object resultObject,
    final boolean resultList, final boolean test) {
    final DataObjectMetaData resultMetaData = application.getResultMetaData();
    final Map<String, Object> result = new HashMap<String, Object>();
    for (final Attribute attribute : resultMetaData.getAttributes()) {
      final String fieldName = attribute.getName();
      if (!INTERNAL_PROPERTY_NAMES.contains(fieldName)) {
        Object value = null;
        try {
          value = Property.getSimple(resultObject, fieldName);
        } catch (final Throwable t) {
          if (!test) {
            throw new IllegalArgumentException("Could not read property "
              + application.getName() + "." + fieldName, t);
          }
        }
        if (value == null) {
          value = attribute.getDefaultValue();
          if (value == null) {
            final Class<?> typeClass = attribute.getTypeClass();
            if (Boolean.class.isAssignableFrom(typeClass)) {
              value = true;
            } else if (Number.class.isAssignableFrom(typeClass)) {
              value = 123;
            } else if (URL.class.isAssignableFrom(typeClass)) {
              String urlString = "http://www.test.com/";
              final int length = attribute.getLength();
              if (length > 0 && length < 4) {
                urlString = urlString.substring(0, length);
              }
              value = UrlUtil.getUrl(urlString);
            } else if (String.class.isAssignableFrom(typeClass)) {
              value = "test";
              final int length = attribute.getLength();
              if (length > 0 && length < 4) {
                value = ((String)value).substring(0, length);
              }
            } else if (LineString.class.isAssignableFrom(typeClass)) {
              value = GeometryFactory.WGS84.createLineString(new DoubleCoordinatesList(
                2, -125, 53, -125.1, 53));
            } else if (Polygon.class.isAssignableFrom(typeClass)) {
              final BoundingBox boundingBox = new BoundingBox(
                GeometryFactory.WGS84, -125, 53, -125.1, 53);
              value = boundingBox.toPolygon(10);
            } else if (MultiLineString.class.isAssignableFrom(typeClass)) {
              final LineString line = GeometryFactory.WGS84.createLineString(new DoubleCoordinatesList(
                2, -125, 53, -125.1, 53));
              value = GeometryFactory.WGS84.createMultiLineString(line);
            } else if (MultiPolygon.class.isAssignableFrom(typeClass)) {
              final BoundingBox boundingBox = new BoundingBox(
                GeometryFactory.WGS84, -125, 53, -125.1, 53);
              final Polygon polygon = boundingBox.toPolygon(10);
              value = GeometryFactory.WGS84.createMultiPolygon(polygon);
            } else if (Date.class.isAssignableFrom(typeClass)) {
              final Timestamp time = new Timestamp(System.currentTimeMillis());
              value = StringConverterRegistry.toObject(typeClass, time);
            } else if (GeometryCollection.class.isAssignableFrom(typeClass)
              || MultiPoint.class.isAssignableFrom(typeClass)) {
              final Point point = GeometryFactory.WGS84.createPoint(-125, 53);
              value = GeometryFactory.WGS84.createMultiPoint(point);
            } else if (Geometry.class.isAssignableFrom(typeClass)
              || Point.class.isAssignableFrom(typeClass)) {
              value = GeometryFactory.WGS84.createPoint(-125, 53);
            } else {
              value = "Unknown";
            }
          }
          result.put(fieldName, value);
        } else {
          if (value instanceof Geometry) {
            Geometry geometry = (Geometry)value;
            GeometryFactory geometryFactory = attribute.getProperty(AttributeProperties.GEOMETRY_FACTORY);
            if (geometryFactory == GeometryFactory.getFactory()) {
              geometryFactory = GeometryFactory.getFactory(geometry);
            }
            final int srid = CollectionUtil.getInteger(parameters,
              "resultSrid", geometryFactory.getSRID());
            final int numAxis = CollectionUtil.getInteger(parameters,
              "resultNumAxis", geometryFactory.getNumAxis());
            final double scaleXY = CollectionUtil.getDouble(parameters,
              "resultScaleFactorXy", geometryFactory.getScaleXY());
            final double scaleZ = CollectionUtil.getDouble(parameters,
              "resultScaleFactorZ", geometryFactory.getScaleZ());

            geometryFactory = GeometryFactory.getFactory(srid, numAxis,
              scaleXY, scaleZ);
            geometry = geometryFactory.createGeometry(geometry);
            if (geometry.getSRID() == 0) {
              throw new IllegalArgumentException(
                "Geometry does not have a coordinate system (SRID) specified");
            }
            final Boolean validateGeometry = attribute.getProperty(AttributeProperties.VALIDATE_GEOMETRY);
            if (validateGeometry == true) {
              if (!geometry.isValid()) {
                throw new IllegalArgumentException("Geometry is not valid for"
                  + application.getName() + "." + fieldName);
              }
            }
            result.put(fieldName, geometry);
          } else {
            result.put(fieldName, value);
          }
        }

      }
    }
    final Map<String, Object> customizationProperties = new LinkedHashMap<String, Object>(
      this.customizationProperties);
    if (resultList && application.isHasResultListCustomizationProperties()) {
      final Map<String, Object> resultListProperties = Property.get(
        resultObject, "customizationProperties");
      if (resultListProperties != null) {
        customizationProperties.putAll(resultListProperties);
      }
    }
    result.put("customizationProperties", customizationProperties);
    return result;
  }

  public List<Map<String, Object>> getResults() {
    return results;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  @SuppressWarnings("resource")
  public void setParameters(final Map<String, ? extends Object> parameters) {
    for (final Entry<String, ? extends Object> entry : parameters.entrySet()) {
      final String parameterName = entry.getKey();
      if (parameterName.startsWith("cpf")) {
        final Object parameterValue = entry.getValue();
        testParameters.put(parameterName, parameterValue);
      }
    }

    final DataObjectMetaDataImpl requestMetaData = application.getRequestMetaData();
    for (final Attribute attribute : requestMetaData.getAttributes()) {
      final String parameterName = attribute.getName();
      final Object parameterValue = parameters.get(parameterName);
      if (parameterValue == null) {
        if (attribute.isRequired()) {
          throw new IllegalArgumentException(parameterName + " is required");
        }
      } else {
        setPluginProperty(parameterName, parameterValue);
      }
    }
    if (application.isPerRequestInputData()) {
      for (final String parameterName : INPUT_DATA_PARAMETER_NAMES) {
        final Object parameterValue = parameters.get(parameterName);
        setPluginProperty(parameterName, parameterValue);
      }
    }
    if (application.isPerRequestResultData()) {
      final String resultDataContentType = (String)parameters.get("resultDataContentType");
      final String resultDataUrl = (String)parameters.get("resultDataUrl");
      OutputStream resultData;
      if (resultDataUrl == null) {
        resultData = (OutputStream)parameters.get("resultData");
        if (resultData == null) {
          try {
            final File file = File.createTempFile("cpf", ".out");
            resultData = new FileOutputStream(file);
            Logger.getLogger(getClass()).info("Writing result to " + file);
          } catch (final IOException e) {
            resultData = System.out;
          }
        }
      } else {
        resultData = new LazyHttpPostOutputStream(resultDataUrl,
          resultDataContentType);
      }
      setPluginProperty("resultData", resultData);
      setPluginProperty("resultDataContentType", resultDataContentType);
    }
    if (application.isSecurityServiceRequired()) {
      if (securityService == null) {
        throw new IllegalArgumentException("Security service is required");
      } else {
        setPluginProperty("securityService", securityService);
      }
    }
  }

  public void setPluginProperty(final String parameterName,
    final Map<String, ? extends Object> parameters) {
    final Object parameterValue = parameters.get(parameterName);
    setPluginProperty(parameterName, parameterValue);
  }

  public void setPluginProperty(final String parameterName,
    Object parameterValue) {
    try {
      final DataObjectMetaDataImpl requestMetaData = application.getRequestMetaData();
      final Class<?> attributeClass = requestMetaData.getAttributeClass(parameterName);
      if (attributeClass != null) {
        parameterValue = StringConverterRegistry.toObject(attributeClass,
          parameterValue);
      }
      BeanUtils.setProperty(this.plugin, parameterName, parameterValue);
      this.parameters.put(parameterName, parameterValue);
    } catch (final Throwable t) {
      throw new IllegalArgumentException(this.application.getName() + "."
        + parameterName + " could not be set", t);
    }
  }

  public void setSecurityService(final SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setTestParameters(final Map<String, Object> testParameters) {
    this.testParameters = new HashMap<>(testParameters);
  }

  @Override
  public String toString() {
    return application.getName() + ": " + plugin.toString();
  }
}
