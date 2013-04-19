package ca.bc.gov.open.cpf.plugin.impl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.io.AbstractObjectWithProperties;
import com.revolsys.util.CaseConverter;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.MathUtil;
import com.vividsolutions.jts.geom.Geometry;

/**
 * The BusinessApplication describes a business application which can be invoked
 * via the CPF. It contains all the metadata to generate a capabilities document
 * for the service and for the CPF to be able to invoke the application.
 * 
 * @author paustin
 * @version 1.0
 */
public class BusinessApplication extends AbstractObjectWithProperties implements
  Comparable<BusinessApplication> {

  public static final String JOB_PARAMETER = BusinessApplication.class.getName()
    + "/JOB_PARAMETER";

  public static final String CORE_PARAMETER = BusinessApplication.class.getName()
    + "/CORE_PARAMETER";

  public static final String REQUEST_PARAMETER = BusinessApplication.class.getName()
    + "/REQUEST_PARAMETER";

  public static String getDefaultMimeType(final Map<String, String> mimeTypeMap) {
    String defaultValue = "application/json";
    final Set<String> mimeTypes = mimeTypeMap.keySet();
    if (!mimeTypes.contains(defaultValue)) {
      if (mimeTypes.isEmpty()) {
        defaultValue = "*/*";
      } else {
        defaultValue = CollectionUtil.get(mimeTypes, 0);
      }
    }
    return defaultValue;
  }

  private Expression batchModeExpression;

  private String batchModePermission;

  /**
   * The compatibleVersions defines the versions of the BusinessApplication
   * which are compatible with the currentVersion.
   */
  private List<String> compatibleVersions;

  private List<CoordinateSystem> coordinateSystems;

  private String description;

  /**
   * The descriptionUrl is a link to a URL which provides more detailed
   * instructions on using the business application.
   */
  private String descriptionUrl;

  private GeometryFactory geometryFactory = GeometryFactory.getFactory();

  private boolean hasGeometryRequestAttribute = false;

  private boolean hasGeometryResultAttribute = false;

  private boolean hasNonGeometryRequestAttribute;

  /**
   * The id field is the unique identifier for the BusinessApplication.
   */
  private String id = UUID.randomUUID().toString();

  /**
   * The inputDataContentTypes is the list of supported MIME content types the
   * BusinessApplication can accept for input data.
   */
  private Map<String, String> inputDataContentTypes = new LinkedHashMap<String, String>();

  private Expression instantModeExpression;

  private String instantModePermission;

  private String logLevel = "ERROR";

  private int maxConcurrentRequests;

  /**
   * The maxRequestsPerJob is the maximum number of requests a user can submit
   * in one job to the BusinessApplication.
   */
  private int maxRequestsPerJob = Integer.MAX_VALUE;

  private Module module;

  /**
   * The name is the name of the BusinessApplication.
   */
  private String name;

  private int numRequestsPerWorker = 1;

  /**
   * The perRequestInputData flag indicates if the input data must be specified
   * as opaque data for each BatchJobRequest (true) or in one structured data
   * file for the whole Batch Job (false).
   */
  private boolean perRequestInputData;

  /**
   * The perRequestResultData flag indicates if the result data will be returned
   * as one opaque data file for each BatchJobRequest (true) or in one
   * structured data file for the whole Batch Job (false).
   */
  private boolean perRequestResultData;

  private BusinessApplicationPlugin pluginMetadata;

  private final Map<Integer, Attribute> requestAttributeMap = new TreeMap<Integer, Attribute>();

  private final Map<String, Attribute> requestAttributeByNameMap = new TreeMap<String, Attribute>();

  private DataObjectMetaDataImpl requestMetaData;

  private final Map<Integer, Attribute> resultAttributeMap = new TreeMap<Integer, Attribute>();

  /**
   * The resultDataContentTypes is the list of supported MIME content types the
   * BusinessApplication can accept for result data.
   */
  private Map<String, String> resultDataContentTypes = new LinkedHashMap<String, String>();

  private String resultListProperty;

  private DataObjectMetaDataImpl resultMetaData;

  private boolean securityServiceRequired;

  /**
   * The name is the name of the BusinessApplication.
   */
  private String title;

  private boolean validateGeometry;

  /**
   * The version defines the current version of the BusinessApplication.
   */
  private String version;

  private boolean hasCustomizationProperties;

  public BusinessApplication(final BusinessApplicationPlugin pluginMetadata,
    final Module module, final String name) {
    this.pluginMetadata = pluginMetadata;
    this.module = module;
    this.name = name;
    requestMetaData = new DataObjectMetaDataImpl("/" + name);
    resultMetaData = new DataObjectMetaDataImpl("/" + name);
  }

  public BusinessApplication(String name) {
    this.name = name;
    this.title = name;
  }

  public void addInputDataContentType(final String contentType,
    final String description) {
    inputDataContentTypes.put(contentType, description);
    inputDataContentTypes = CollectionUtil.sortByValues(inputDataContentTypes);
  }

  public void addRequestAttribute(int index, final Attribute attribute) {
    if (attribute == null) {
      throw new RuntimeException("Unknwon attribute");
    }
    if (index == -1) {
      index = 200000 + requestAttributeMap.size();
    }
    if (requestAttributeMap.containsKey(index)) {
      throw new IllegalArgumentException("Business Application " + getName()
        + " Duplicate index for " + RequestParameter.class + " on "
        + attribute.getName());
    } else {
      if (Geometry.class.isAssignableFrom(attribute.getType().getJavaClass())) {
        hasGeometryRequestAttribute = true;
      } else {
        hasNonGeometryRequestAttribute = true;
      }
      requestAttributeMap.put(index, attribute);
    }
    requestAttributeByNameMap.put(attribute.getName(), attribute);
  }

  public void addResultAttribute(int index, final Attribute attribute) {
    if (index == -1) {
      index = 100000 + resultAttributeMap.size();
    }
    if (resultAttributeMap.containsKey(index)) {
      throw new IllegalArgumentException("Business Application " + getName()
        + " Duplicate index for " + ResultAttribute.class + " on "
        + attribute.getName());
    } else {
      if (Geometry.class.isAssignableFrom(attribute.getType().getJavaClass())) {
        hasGeometryResultAttribute = true;
      }
      resultAttributeMap.put(index, attribute);
    }
  }

  public void addResultDataContentType(final String contentType,
    final String description) {
    resultDataContentTypes.put(contentType, description);
    resultDataContentTypes = CollectionUtil.sortByValues(resultDataContentTypes);
  }

  /**
   * Compare the business applications, return in alphabetical order (ignoring
   * case), followed by versions with the lowest version number first.
   * 
   * @param businessApplication
   */
  @Override
  public int compareTo(final BusinessApplication businessApplication) {
    if (businessApplication == this) {
      return 0;
    } else {
      final int nameCompare = getName().compareToIgnoreCase(
        businessApplication.getName());
      if (nameCompare == 0) {
        final String version1 = getVersion().replaceAll("TRUNK", "")
          .replaceAll("-SNAPSHOT", "");

        final String version2 = businessApplication.getVersion();
        final double[] parts1 = MathUtil.toDoubleArraySplit(version1, "\\.");
        final double[] parts2 = MathUtil.toDoubleArraySplit(version2, "\\.");
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
          double v1 = 0;
          if (i < parts1.length) {
            v1 = parts1[i];
          }
          double v2 = 0;
          if (i < parts2.length) {
            v2 = parts2[i];
          }
          final int partCompare = Double.compare(v1, v2);
          if (partCompare != 0) {
            return partCompare;
          }
        }
        return getId().compareTo(getId());
      } else {
        return nameCompare;
      }
    }
  }

  public Expression getBatchModeExpression() {
    return batchModeExpression;
  }

  public String getBatchModePermission() {
    return batchModePermission;
  }

  public List<String> getCompatibleVersions() {
    return compatibleVersions;
  }

  public List<CoordinateSystem> getCoordinateSystems() {
    return coordinateSystems;
  }

  public String getDescription() {
    return description;
  }

  public String getDescriptionUrl() {
    return descriptionUrl;
  }

  public GeometryFactory getGeometryFactory() {
    return geometryFactory;
  }

  public String getId() {
    return id;
  }

  public Map<String, String> getInputDataContentTypes() {
    return inputDataContentTypes;
  }

  public Expression getInstantModeExpression() {
    return instantModeExpression;
  }

  public String getInstantModePermission() {
    return instantModePermission;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public int getMaxConcurrentRequests() {
    return maxConcurrentRequests;
  }

  public int getMaxRequestsPerJob() {
    return maxRequestsPerJob;
  }

  public Module getModule() {
    return module;
  }

  public String getModuleName() {
    return module.getName();
  }

  public String getName() {
    return name;
  }

  public int getNumRequestsPerWorker() {
    return numRequestsPerWorker;
  }

  public BusinessApplicationPlugin getPluginMetadata() {
    return pluginMetadata;
  }

  public synchronized DataObjectMetaDataImpl getRequestMetaData() {
    if (requestMetaData.getAttributeCount() == 0) {
      if (requestAttributeMap.size() > 0) {
        requestMetaData.addAttribute("requestSequenceNumber", DataTypes.INT);

        if (hasGeometryRequestAttribute) {
          final Attribute requestSrid = new Attribute(
            "srid",
            DataTypes.INT,
            false,
            "The coordinate system code of the source geometry. This value is used if the input data file does not specify a coordinate system.");
          requestSrid.setProperty(BusinessApplication.CORE_PARAMETER, true);
          requestSrid.setProperty(BusinessApplication.JOB_PARAMETER, true);
          Integer defaultSrid = null;
          for (final CoordinateSystem coordinateSystem : coordinateSystems) {
            final int srid = coordinateSystem.getId();
            if (defaultSrid == null || srid == 3005) {
              defaultSrid = 3005;
            }
            final String name = coordinateSystem.getName();
            requestSrid.addAllowedValue(srid, name);
          }
          requestSrid.setDefaultValue(defaultSrid);
          requestMetaData.addAttribute(requestSrid);

        }

        final Attribute resultDataContentType = new Attribute(
          "resultDataContentType",
          DataTypes.STRING,
          false,
          "The MIME type of the result data specified to be returned after running the request.");
        resultDataContentType.setProperty(BusinessApplication.CORE_PARAMETER,
          true);
        resultDataContentType.setProperty(BusinessApplication.JOB_PARAMETER,
          true);

        requestMetaData.addAttribute(resultDataContentType);

        if (hasGeometryResultAttribute) {
          final Attribute resultSrid = new Attribute("resultSrid",
            DataTypes.INT, false,
            "The coordinate system code of the projection for the result geometry.");
          resultSrid.setProperty(BusinessApplication.CORE_PARAMETER, true);
          resultSrid.setProperty(BusinessApplication.JOB_PARAMETER, true);
          Integer defaultSrid = null;
          for (final CoordinateSystem coordinateSystem : coordinateSystems) {
            final int srid = coordinateSystem.getId();
            if (defaultSrid == null || srid == 3005) {
              defaultSrid = 3005;
            }
            final String name = coordinateSystem.getName();
            resultSrid.addAllowedValue(srid, name);
            resultSrid.setDefaultValue(defaultSrid);
          }
          requestMetaData.addAttribute(resultSrid);

          final Attribute resultNumAxis = new Attribute(
            "resultNumAxis",
            DataTypes.INT,
            false,
            "The number of coordinate axis in the result geometry (e.g. 2 for 2D or 3 for 3D).");
          resultNumAxis.setProperty(BusinessApplication.CORE_PARAMETER, true);
          resultNumAxis.setProperty(BusinessApplication.JOB_PARAMETER, true);
          resultNumAxis.addAllowedValue(2, "2D");
          resultNumAxis.addAllowedValue(3, "3D");
          resultNumAxis.setDefaultValue(2);
          requestMetaData.addAttribute(resultNumAxis);

          final Attribute resultScaleFactorXy = new Attribute(
            "resultScaleFactorXy",
            DataTypes.INT,
            false,
            "The scale factor to apply the x, y coordinates. The scale factor is 1 / minimum unit. For example if the minimum unit was 1mm (0.001) the scale factor is 1000 (1 / 0.001).");
          resultScaleFactorXy.setProperty(BusinessApplication.CORE_PARAMETER,
            true);
          resultScaleFactorXy.setProperty(BusinessApplication.JOB_PARAMETER,
            true);
          resultScaleFactorXy.setDefaultValue(1000);
          requestMetaData.addAttribute(resultScaleFactorXy);

          final Attribute resultScaleFactorZ = new Attribute(
            "resultScaleFactorZ",
            DataTypes.INT,
            false,
            "The scale factor to apply the z coordinate. The scale factor is 1 / minimum unit. For example if the minimum unit was 1mm (0.001) the scale factor is 1000 (1 / 0.001).");
          resultScaleFactorZ.setProperty(BusinessApplication.CORE_PARAMETER,
            true);
          resultScaleFactorZ.setDefaultValue(1000);
          resultScaleFactorZ.setProperty(BusinessApplication.JOB_PARAMETER,
            true);
          requestMetaData.addAttribute(resultScaleFactorZ);

        }
        for (final Attribute attribute : requestAttributeMap.values()) {
          requestMetaData.addAttribute(attribute);
        }
      }
    }

    return requestMetaData;
  }

  public Map<String, String> getResultDataContentTypes() {
    return resultDataContentTypes;
  }

  public String getResultListProperty() {
    return resultListProperty;
  }

  public synchronized DataObjectMetaData getResultMetaData() {
    if (resultMetaData.getAttributeCount() == 0) {
      if (resultAttributeMap.size() > 0) {
        resultMetaData.addAttribute(new Attribute("sequenceNumber",
          DataTypes.INT, true,
          "The index of the request record that this result relates to."));
        if (resultListProperty != null) {
          resultMetaData.addAttribute(new Attribute("resultNumber",
            DataTypes.INT, true,
            "The index of the result record within the result for a request."));
        }
        for (final Attribute attribute : resultAttributeMap.values()) {
          String description = attribute.getDescription();
          if (!StringUtils.hasText(description)) {
            final String name = attribute.getName();
            final Attribute requestAttribute = requestAttributeByNameMap.get(name);
            if (requestAttribute != null) {
              description = requestAttribute.getDescription();
              attribute.setDescription(description);
            }
          }
          resultMetaData.addAttribute(attribute);
        }
      }
    }
    return resultMetaData;
  }

  public String getTitle() {
    if (title == null && name != null) {
      title = CaseConverter.toCapitalizedWords(name);
    }
    return title;
  }

  public String getVersion() {
    return version;
  }

  public boolean isCoreParameter(final String attributeName) {
    final Attribute attribute = requestMetaData.getAttribute(attributeName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist"
        + attributeName);
    } else {
      return attribute.getProperty(CORE_PARAMETER) == Boolean.TRUE;
    }
  }

  public boolean isEnabled() {
    return module.isEnabled();
  }

  public boolean isHasCustomizationProperties() {
    return hasCustomizationProperties;
  }

  public boolean isHasGeometryRequestAttribute() {
    return hasGeometryRequestAttribute;
  }

  public boolean isHasGeometryResultAttribute() {
    return hasGeometryResultAttribute;
  }

  public boolean isHasNonGeometryRequestAttribute() {
    return hasNonGeometryRequestAttribute;
  }

  public boolean isInfoLogEnabled() {
    return logLevel.equals("INFO") || logLevel.equals("DEBUG");
  }

  public boolean isJobParameter(final String attributeName) {
    final Attribute attribute = requestMetaData.getAttribute(attributeName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist"
        + attributeName);
    } else {
      return attribute.getProperty(JOB_PARAMETER) == Boolean.TRUE;
    }
  }

  public boolean isPerRequestInputData() {
    return perRequestInputData;
  }

  public boolean isPerRequestResultData() {
    return perRequestResultData;
  }

  public boolean isRequestAttributeValid(final String name, final Object value) {
    return true;
  }

  public boolean isRequestParameter(final String attributeName) {
    final Attribute attribute = requestMetaData.getAttribute(attributeName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist"
        + attributeName);
    } else {
      return attribute.getProperty(REQUEST_PARAMETER) == Boolean.TRUE;
    }
  }

  public boolean isSecurityServiceRequired() {
    return securityServiceRequired;
  }

  public boolean isValidateGeometry() {
    return validateGeometry;
  }

  public boolean isVersionSupported(final String businessApplicationVersion) {
    if (version.equals(businessApplicationVersion)) {
      return true;
    } else {
      for (final String compatibleVersion : compatibleVersions) {
        if (compatibleVersion.equals(businessApplicationVersion)) {
          return true;
        }
      }
      return false;
    }
  }

  public void setBatchModePermission(final String batchModePermission) {
    if (StringUtils.hasText(batchModePermission)) {
      this.batchModePermission = batchModePermission;
    } else {
      this.batchModePermission = "permitAll";
    }
    this.batchModeExpression = new SpelExpressionParser().parseExpression(this.batchModePermission);
  }

  public void setCompatibleVersions(final List<String> compatibleVersions) {
    this.compatibleVersions = compatibleVersions;
  }

  public void setCompatibleVersions(final String... compatibleVersions) {
    setCompatibleVersions(Arrays.asList(compatibleVersions));
  }

  public void setCoordinateSystems(
    final List<CoordinateSystem> coordinateSystems) {
    this.coordinateSystems = coordinateSystems;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public void setDescriptionUrl(final String descriptionUrl) {
    this.descriptionUrl = descriptionUrl;
  }

  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public void setHasCustomizationProperties(
    final boolean hasCustomizationProperties) {
    this.hasCustomizationProperties = hasCustomizationProperties;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setInputDataContentTypes(
    final Map<String, String> inputDataContentTypes) {
    this.inputDataContentTypes = CollectionUtil.sortByValues(inputDataContentTypes);
  }

  public void setInstantModePermission(final String instantModePermission) {
    if (StringUtils.hasText(instantModePermission)) {
      this.instantModePermission = instantModePermission;
    } else {
      this.instantModePermission = "permitAll";
    }
    this.instantModeExpression = new SpelExpressionParser().parseExpression(this.instantModePermission);
  }

  public void setLogLevel(final String logLevel) {
    this.logLevel = logLevel;
  }

  public void setMaxConcurrentRequests(final int maxConcurrentRequests) {
    this.maxConcurrentRequests = maxConcurrentRequests;
  }

  public void setMaxRequestsPerJob(final int maxRequestsPerJob) {
    this.maxRequestsPerJob = maxRequestsPerJob;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setNumRequestsPerWorker(final int maximumRequestsPerNode) {
    this.numRequestsPerWorker = maximumRequestsPerNode;
  }

  public void setPerRequestInputData(final boolean perRequestInputData) {
    this.perRequestInputData = perRequestInputData;
  }

  public void setPerRequestResultData(final boolean perRequestResultData) {
    this.perRequestResultData = perRequestResultData;
  }

  public void setResultDataContentTypes(
    final Map<String, String> resultDataContentTypes) {
    this.resultDataContentTypes = CollectionUtil.sortByValues(resultDataContentTypes);
  }

  public void setResultListProperty(final String resultListProperty) {
    this.resultListProperty = resultListProperty;
  }

  public void setSecurityServiceRequired(final boolean securityServiceRequired) {
    this.securityServiceRequired = securityServiceRequired;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public void setValidateGeometry(final boolean validateGeometry) {
    this.validateGeometry = validateGeometry;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return name;
  }

}
