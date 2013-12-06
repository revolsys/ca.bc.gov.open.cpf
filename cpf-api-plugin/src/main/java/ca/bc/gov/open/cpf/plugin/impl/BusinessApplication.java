package ca.bc.gov.open.cpf.plugin.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.io.AbstractObjectWithProperties;
import com.revolsys.util.CaseConverter;
import com.revolsys.util.CollectionUtil;
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

  public static final String CORE_PARAMETER = BusinessApplication.class.getName()
    + "/CORE_PARAMETER";

  public static final String JOB_PARAMETER = BusinessApplication.class.getName()
    + "/JOB_PARAMETER";

  public static final String REQUEST_PARAMETER = BusinessApplication.class.getName()
    + "/REQUEST_PARAMETER";

  public static String getDefaultFileExtension(
    final Map<String, ?> fileExtensionMap) {
    final Collection<String> fileExtensions = fileExtensionMap.keySet();
    String defaultValue = "csv";
    if (!fileExtensions.contains(defaultValue)) {
      if (fileExtensions.isEmpty()) {
        defaultValue = "*";
      } else {
        defaultValue = CollectionUtil.get(fileExtensions, 0);
      }
    }
    return defaultValue;
  }

  public static String getDefaultMimeType(final Collection<String> mimeTypes) {
    String defaultValue = "application/json";
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

  private List<CoordinateSystem> coordinateSystems;

  private String description;

  /**
   * The descriptionUrl is a link to a URL which provides more detailed
   * instructions on using the business application.
   */
  private String descriptionUrl;

  private GeometryFactory geometryFactory = GeometryFactory.getFactory();

  private boolean hasCustomizationProperties;

  private boolean hasGeometryRequestAttribute = false;

  private boolean hasGeometryResultAttribute = false;

  private boolean hasNonGeometryRequestAttribute;

  private boolean hasResultListCustomizationProperties;

  private boolean hasTestExecuteMethod = false;

  /**
   * The id field is the unique identifier for the BusinessApplication.
   */
  private String id = UUID.randomUUID().toString();

  /**
   * The inputDataContentTypes is the list of supported MIME content types the
   * BusinessApplication can accept for input data.
   */
  private final Set<String> inputDataContentTypes = new TreeSet<>();

  private Map<String, String> inputDataFileExtensions = new LinkedHashMap<String, String>();

  private final Map<String, String> inputFileExtensionToContentType = new LinkedHashMap<String, String>();

  private Expression instantModeExpression;

  private String instantModePermission;

  private AppLog log;

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
   * as opaque data for each BatchJobExecutionGroup (true) or in one structured data
   * file for the whole Batch Job (false).
   */
  private boolean perRequestInputData;

  /**
   * The perRequestResultData flag indicates if the result data will be returned
   * as one opaque data file for each BatchJobExecutionGroup (true) or in one
   * structured data file for the whole Batch Job (false).
   */
  private boolean perRequestResultData;

  private BusinessApplicationPlugin pluginMetadata;

  private final Map<String, Attribute> requestAttributeByNameMap = new TreeMap<String, Attribute>();

  private final Map<Integer, Attribute> requestAttributeMap = new TreeMap<Integer, Attribute>();

  private DataObjectMetaDataImpl requestMetaData;

  private final Map<Integer, Attribute> resultAttributeMap = new TreeMap<Integer, Attribute>();

  /**
   * The resultDataContentTypes is the list of supported MIME content types the
   * BusinessApplication can accept for result data.
   */
  private final Set<String> resultDataContentTypes = new TreeSet<String>();

  private final Map<String, String> resultDataFileExtensions = new LinkedHashMap<String, String>();

  private final Map<String, String> resultFileExtensionToContentType = new LinkedHashMap<String, String>();

  private String resultListProperty;

  private DataObjectMetaDataImpl resultMetaData;

  private boolean securityServiceRequired;

  private boolean testModeEnabled = false;

  /**
   * The name is the name of the BusinessApplication.
   */
  private String title;

  private boolean validateGeometry;

  public BusinessApplication(final BusinessApplicationPlugin pluginMetadata,
    final Module module, final String name) {
    this.name = name;
    this.title = name;
    this.pluginMetadata = pluginMetadata;
    this.module = module;
    this.name = name;
    this.log = new AppLog(module.getName() + "." + name);
    this.requestMetaData = new DataObjectMetaDataImpl("/" + name);
    this.resultMetaData = new DataObjectMetaDataImpl("/" + name);
  }

  public BusinessApplication(final String name) {
    this.name = name;
    this.title = name;
  }

  public void addInputDataContentType(final String contentType,
    final String description, final String fileExtension) {
    this.inputDataContentTypes.add(contentType);

    this.inputDataFileExtensions.put(fileExtension, description);
    this.inputDataFileExtensions = CollectionUtil.sortByValues(this.inputDataFileExtensions);
    this.inputFileExtensionToContentType.put(fileExtension, contentType);
  }

  public void addRequestAttribute(int index, final Attribute attribute) {
    if (attribute == null) {
      throw new RuntimeException("Unknwon attribute");
    }
    if (index == -1) {
      index = 200000 + this.requestAttributeMap.size();
    }
    if (this.requestAttributeMap.containsKey(index)) {
      throw new IllegalArgumentException("Business Application " + getName()
        + " Duplicate index for " + RequestParameter.class + " on "
        + attribute.getName());
    } else {
      if (Geometry.class.isAssignableFrom(attribute.getType().getJavaClass())) {
        this.hasGeometryRequestAttribute = true;
      } else {
        this.hasNonGeometryRequestAttribute = true;
      }
      this.requestAttributeMap.put(index, attribute);
    }
    this.requestAttributeByNameMap.put(attribute.getName(), attribute);
  }

  public void addResultAttribute(int index, final Attribute attribute) {
    if (index == -1) {
      index = 100000 + this.resultAttributeMap.size();
    }
    if (this.resultAttributeMap.containsKey(index)) {
      throw new IllegalArgumentException("Business Application " + getName()
        + " Duplicate index for " + ResultAttribute.class + " on "
        + attribute.getName());
    } else {
      if (Geometry.class.isAssignableFrom(attribute.getType().getJavaClass())) {
        this.hasGeometryResultAttribute = true;
      }
      this.resultAttributeMap.put(index, attribute);
    }
  }

  public void addResultDataContentType(final String contentType,
    final String fileExtension, final String description) {
    this.resultDataContentTypes.add(contentType);
    this.resultDataFileExtensions.put(fileExtension, description);
    this.resultFileExtensionToContentType.put(fileExtension, contentType);
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
        return getId().compareTo(getId());
      } else {
        return nameCompare;
      }
    }
  }

  public Expression getBatchModeExpression() {
    return this.batchModeExpression;
  }

  public String getBatchModePermission() {
    return this.batchModePermission;
  }

  public List<CoordinateSystem> getCoordinateSystems() {
    return this.coordinateSystems;
  }

  public String getDefaultInputDataContentType() {
    return inputDataContentTypes.iterator().next();
  }

  public String getDefaultResultDataContentType() {
    return inputDataContentTypes.iterator().next();
  }

  public String getDescription() {
    return this.description;
  }

  public String getDescriptionUrl() {
    return this.descriptionUrl;
  }

  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  public String getId() {
    return this.id;
  }

  public String getInputContentType(final String fileExtension) {
    if (isInputContentTypeSupported(fileExtension)) {
      return fileExtension;
    } else {
      return inputFileExtensionToContentType.get(fileExtension);
    }
  }

  public Set<String> getInputDataContentTypes() {
    return this.inputDataContentTypes;
  }

  public Map<String, String> getInputDataFileExetensions() {
    return this.inputDataFileExtensions;
  }

  public Map<String, String> getInputFileExtensionToContentType() {
    return inputFileExtensionToContentType;
  }

  public Map<String, String> getInputFileExtensionToMediaType() {
    return inputFileExtensionToContentType;
  }

  public Expression getInstantModeExpression() {
    return this.instantModeExpression;
  }

  public String getInstantModePermission() {
    return this.instantModePermission;
  }

  public AppLog getLog() {
    return log;
  }

  public String getLogLevel() {
    return this.log.getLogLevel();
  }

  public int getMaxConcurrentRequests() {
    return this.maxConcurrentRequests;
  }

  public int getMaxRequestsPerJob() {
    return this.maxRequestsPerJob;
  }

  public Module getModule() {
    return this.module;
  }

  public String getModuleName() {
    return this.module.getName();
  }

  public String getName() {
    return this.name;
  }

  public int getNumRequestsPerWorker() {
    return this.numRequestsPerWorker;
  }

  public BusinessApplicationPlugin getPluginMetadata() {
    return this.pluginMetadata;
  }

  public synchronized DataObjectMetaDataImpl getRequestMetaData() {
    if (this.requestMetaData.getAttributeCount() == 0) {
      if (this.requestAttributeMap.size() > 0) {
        final Attribute requestSequenceNumber = this.requestMetaData.addAttribute(
          "requestSequenceNumber", DataTypes.INT);
        requestSequenceNumber.setProperty(BusinessApplication.CORE_PARAMETER,
          true);

        if (this.hasGeometryRequestAttribute) {
          final Attribute requestSrid = new Attribute(
            "srid",
            DataTypes.INT,
            false,
            "The coordinate system code of the source geometry. This value is used if the input data file does not specify a coordinate system.");
          requestSrid.setProperty(BusinessApplication.CORE_PARAMETER, true);
          requestSrid.setProperty(BusinessApplication.JOB_PARAMETER, true);
          Integer defaultSrid = null;
          for (final CoordinateSystem coordinateSystem : this.coordinateSystems) {
            final int srid = coordinateSystem.getId();
            if (defaultSrid == null || srid == 3005) {
              defaultSrid = 3005;
            }
            final String name = coordinateSystem.getName();
            requestSrid.addAllowedValue(srid, name);
          }
          requestSrid.setDefaultValue(defaultSrid);
          this.requestMetaData.addAttribute(requestSrid);

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

        this.requestMetaData.addAttribute(resultDataContentType);

        if (this.hasGeometryResultAttribute) {
          final Attribute resultSrid = new Attribute("resultSrid",
            DataTypes.INT, false,
            "The coordinate system code of the projection for the result geometry.");
          resultSrid.setProperty(BusinessApplication.CORE_PARAMETER, true);
          resultSrid.setProperty(BusinessApplication.JOB_PARAMETER, true);
          Integer defaultSrid = null;
          for (final CoordinateSystem coordinateSystem : this.coordinateSystems) {
            final int srid = coordinateSystem.getId();
            if (defaultSrid == null || srid == 3005) {
              defaultSrid = 3005;
            }
            final String name = coordinateSystem.getName();
            resultSrid.addAllowedValue(srid, name);
            resultSrid.setDefaultValue(defaultSrid);
          }
          this.requestMetaData.addAttribute(resultSrid);

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
          this.requestMetaData.addAttribute(resultNumAxis);

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
          this.requestMetaData.addAttribute(resultScaleFactorXy);

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
          this.requestMetaData.addAttribute(resultScaleFactorZ);

        }
        for (final Attribute attribute : this.requestAttributeMap.values()) {
          this.requestMetaData.addAttribute(attribute);
        }
      }
    }

    return this.requestMetaData;
  }

  public String getResultContentType(final String fileExtension) {
    if (isResultContentTypeSupported(fileExtension)) {
      return fileExtension;
    } else {
      return inputFileExtensionToContentType.get(fileExtension);
    }
  }

  public Set<String> getResultDataContentTypes() {
    return this.resultDataContentTypes;
  }

  public Map<String, String> getResultDataFileExtensions() {
    return resultDataFileExtensions;
  }

  public String getResultListProperty() {
    return this.resultListProperty;
  }

  public synchronized DataObjectMetaData getResultMetaData() {
    if (this.resultMetaData.getAttributeCount() == 0) {
      if (this.resultAttributeMap.size() > 0) {
        this.resultMetaData.addAttribute(new Attribute("sequenceNumber",
          DataTypes.INT, true,
          "The index of the request record that this result relates to."));
        if (this.resultListProperty != null) {
          this.resultMetaData.addAttribute(new Attribute("resultNumber",
            DataTypes.INT, true,
            "The index of the result record within the result for a request."));
        }
        for (final Attribute attribute : this.resultAttributeMap.values()) {
          final String name = attribute.getName();
          final Attribute requestAttribute = this.requestAttributeByNameMap.get(name);
          if (requestAttribute != null) {
            String description = attribute.getDescription();
            if (!StringUtils.hasText(description)) {
              description = requestAttribute.getDescription();
              attribute.setDescription(description);
            }
            Object defaultValue = attribute.getDefaultValue();
            if (defaultValue == null) {
              defaultValue = requestAttribute.getDefaultValue();
              attribute.setDefaultValue(defaultValue);
            }
          }

          this.resultMetaData.addAttribute(attribute);
        }
      }
    }
    return this.resultMetaData;
  }

  public String getTitle() {
    if (this.title == null && this.name != null) {
      this.title = CaseConverter.toCapitalizedWords(this.name);
    }
    return this.title;
  }

  public boolean isCoreParameter(final String attributeName) {
    final Attribute attribute = this.requestMetaData.getAttribute(attributeName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist"
        + attributeName);
    } else {
      return BooleanStringConverter.getBoolean(attribute.getProperty(CORE_PARAMETER));
    }
  }

  public boolean isEnabled() {
    return this.module.isEnabled();
  }

  public boolean isHasCustomizationProperties() {
    return this.hasCustomizationProperties;
  }

  public boolean isHasGeometryRequestAttribute() {
    return this.hasGeometryRequestAttribute;
  }

  public boolean isHasGeometryResultAttribute() {
    return this.hasGeometryResultAttribute;
  }

  public boolean isHasNonGeometryRequestAttribute() {
    return this.hasNonGeometryRequestAttribute;
  }

  public boolean isHasResultListCustomizationProperties() {
    return this.hasResultListCustomizationProperties;
  }

  public boolean isHasResultListProperty() {
    return getResultListProperty() != null;
  }

  public boolean isHasTestExecuteMethod() {
    return hasTestExecuteMethod;
  }

  public boolean isInfoLogEnabled() {
    return this.log.isInfoEnabled();
  }

  public boolean isInputContentTypeSupported(final String contentType) {
    return inputDataContentTypes.contains(contentType);
  }

  public boolean isJobParameter(final String attributeName) {
    final Attribute attribute = this.requestMetaData.getAttribute(attributeName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist"
        + attributeName);
    } else {
      return BooleanStringConverter.getBoolean(attribute.getProperty(JOB_PARAMETER));
    }
  }

  public boolean isPerRequestInputData() {
    return this.perRequestInputData;
  }

  public boolean isPerRequestResultData() {
    return this.perRequestResultData;
  }

  public boolean isRequestAttributeValid(final String name, final Object value) {
    return true;
  }

  public boolean isRequestParameter(final String attributeName) {
    final Attribute attribute = this.requestMetaData.getAttribute(attributeName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist"
        + attributeName);
    } else {
      return BooleanStringConverter.getBoolean(attribute.getProperty(REQUEST_PARAMETER));
    }
  }

  public boolean isResultContentTypeSupported(final String contentType) {
    return resultDataContentTypes.contains(contentType);
  }

  public boolean isSecurityServiceRequired() {
    return this.securityServiceRequired;
  }

  public boolean isTestModeEnabled() {
    return testModeEnabled;
  }

  public boolean isValidateGeometry() {
    return this.validateGeometry;
  }

  public void setBatchModePermission(final String batchModePermission) {
    if (StringUtils.hasText(batchModePermission)) {
      this.batchModePermission = batchModePermission;
    } else {
      this.batchModePermission = "permitAll";
    }
    this.batchModeExpression = new SpelExpressionParser().parseExpression(this.batchModePermission);
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

  public void setHasResultListCustomizationProperties(
    final boolean hasResultListCustomizationProperties) {
    this.hasResultListCustomizationProperties = hasResultListCustomizationProperties;
  }

  public void setHasTestExecuteMethod(final boolean hasTestExecuteMethod) {
    this.hasTestExecuteMethod = hasTestExecuteMethod;
  }

  public void setId(final String id) {
    this.id = id;
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
    this.log.setLogLevel(logLevel);
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

  public void setResultListProperty(final String resultListProperty) {
    this.resultListProperty = resultListProperty;
  }

  public void setSecurityServiceRequired(final boolean securityServiceRequired) {
    this.securityServiceRequired = securityServiceRequired;
  }

  public void setTestModeEnabled(final boolean testModeEnabled) {
    this.testModeEnabled = testModeEnabled;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public void setValidateGeometry(final boolean validateGeometry) {
    this.validateGeometry = validateGeometry;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
