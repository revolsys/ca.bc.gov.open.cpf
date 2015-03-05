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
package ca.bc.gov.open.cpf.plugin.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.data.equals.EqualsRegistry;
import com.revolsys.data.record.schema.FieldDefinition;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.data.record.schema.RecordDefinitionImpl;
import com.revolsys.data.types.DataTypes;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.io.AbstractObjectWithProperties;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.util.CaseConverter;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.Property;

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

  public static final String CORE_PARAMETER = BusinessApplication.class.getName()
    + "/CORE_PARAMETER";

  public static final String JOB_PARAMETER = BusinessApplication.class.getName()
    + "/JOB_PARAMETER";

  public static final String REQUEST_PARAMETER = BusinessApplication.class.getName()
    + "/REQUEST_PARAMETER";

  private String packageName;

  private String defaultResultDataContentType;

  private Expression batchModeExpression;

  private String batchModePermission;

  private List<CoordinateSystem> coordinateSystems;

  private String description;

  /**
   * The descriptionUrl is a link to a URL which provides more detailed
   * instructions on using the business application.
   */
  private String descriptionUrl;

  private GeometryFactory geometryFactory = GeometryFactory.floating3();

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
  private final Set<String> inputDataContentTypes = new LinkedHashSet<>();

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

  private BusinessApplicationPlugin pluginAnnotation;

  private final Map<String, FieldDefinition> requestAttributeByNameMap = new TreeMap<>();

  private final Map<Integer, FieldDefinition> requestAttributeMap = new TreeMap<>();

  private RecordDefinitionImpl requestRecordDefinition;

  private final Map<Integer, FieldDefinition> resultAttributeMap = new TreeMap<>();

  /**
   * The resultDataContentTypes is the list of supported MIME content types the
   * BusinessApplication can accept for result data.
   */
  private final Set<String> resultDataContentTypes = new LinkedHashSet<String>();

  private final Map<String, String> resultDataFileExtensions = new LinkedHashMap<String, String>();

  private final Map<String, String> resultFileExtensionToContentType = new LinkedHashMap<String, String>();

  private String resultListProperty;

  private RecordDefinitionImpl resultRecordDefinition;

  private boolean securityServiceRequired;

  private boolean testModeEnabled = false;

  /**
   * The name is the name of the BusinessApplication.
   */
  private String title;

  private boolean validateGeometry;

  private String defaultInputDataContentType;

  private String defaultInputDataFileExtension;

  private String defaultResultDataFileExtension;

  public BusinessApplication(final BusinessApplicationPlugin pluginAnnotation,
    final Module module, final String name) {
    this.name = name;
    this.title = name;
    this.pluginAnnotation = pluginAnnotation;
    this.module = module;
    this.name = name;
    this.log = new AppLog(module.getName() + "." + name);
    this.requestRecordDefinition = new RecordDefinitionImpl("/" + name);
    this.resultRecordDefinition = new RecordDefinitionImpl("/" + name);
  }

  public BusinessApplication(final String name) {
    this.name = name;
    this.title = name;
  }

  private void addFieldRequestSrid() {
    final FieldDefinition requestSrid = new FieldDefinition(
      "srid",
      DataTypes.INT,
      false,
      "The coordinate system code of the source geometry. This value is used if the input data file does not specify a coordinate system.");
    requestSrid.setProperty(BusinessApplication.CORE_PARAMETER, true);
    requestSrid.setProperty(BusinessApplication.JOB_PARAMETER, true);
    Integer firstSrid = null;
    Integer defaultValue = Property.getInteger(this, "srid");
    for (final CoordinateSystem coordinateSystem : this.coordinateSystems) {
      final int srid = coordinateSystem.getId();
      if (firstSrid == null || srid == 3005) {
        firstSrid = srid;
      }
      final String name = coordinateSystem.getName();
      requestSrid.addAllowedValue(srid, srid + " - " + name);
    }
    if (defaultValue == null) {
      defaultValue = firstSrid;
    }
    requestSrid.setDefaultValue(defaultValue);
    requestSrid.setMinValue(0);
    this.requestRecordDefinition.addField(requestSrid);
  }

  private void addFieldResultDataContentType() {
    final FieldDefinition resultDataContentType = new FieldDefinition(
      "resultDataContentType",
      DataTypes.STRING,
      false,
      "The MIME type of the result data specified to be returned after running the request.");
    resultDataContentType.setProperty(BusinessApplication.CORE_PARAMETER, true);
    resultDataContentType.setProperty(BusinessApplication.JOB_PARAMETER, true);

    if (this.defaultResultDataContentType == null) {
      this.defaultResultDataFileExtension = getDefaultFileExtension(this.resultFileExtensionToContentType);
      this.defaultResultDataContentType = getDefaultMimeType(this.resultDataContentTypes);
    }
    resultDataContentType.setDefaultValue(this.defaultResultDataFileExtension);
    this.requestRecordDefinition.addField(resultDataContentType);
  }

  private void addFieldResultNumAxis() {
    final FieldDefinition resultNumAxis = new FieldDefinition(
      "resultNumAxis",
      DataTypes.INT,
      false,
      "The number of coordinate axis in the result geometry (e.g. 2 for 2D or 3 for 3D).");
    resultNumAxis.setProperty(BusinessApplication.CORE_PARAMETER, true);
    resultNumAxis.setProperty(BusinessApplication.JOB_PARAMETER, true);
    resultNumAxis.addAllowedValue(2, "2D");
    resultNumAxis.addAllowedValue(3, "3D");
    Integer defaultValue = Property.getInteger(this, "resultNumAxis", 2);
    if (defaultValue < 2) {
      defaultValue = 2;
    } else if (defaultValue > 3) {
      defaultValue = 3;
    }
    resultNumAxis.setDefaultValue(defaultValue);
    resultNumAxis.setMinValue(2);
    resultNumAxis.setMaxValue(3);
    this.requestRecordDefinition.addField(resultNumAxis);
  }

  private void addFieldResultSrid() {
    final FieldDefinition resultSrid = new FieldDefinition("resultSrid",
      DataTypes.INT, false,
      "The coordinate system code of the projection for the result geometry.");
    resultSrid.setProperty(BusinessApplication.CORE_PARAMETER, true);
    resultSrid.setProperty(BusinessApplication.JOB_PARAMETER, true);
    Integer firstSrid = null;
    Integer defaultValue = Property.getInteger(this, "resultSrid");
    for (final CoordinateSystem coordinateSystem : this.coordinateSystems) {
      final int srid = coordinateSystem.getId();
      if (firstSrid == null || srid == 3005) {
        firstSrid = 3005;
      }
      final String name = coordinateSystem.getName();
      resultSrid.addAllowedValue(srid, srid + " - " + name);
    }
    if (defaultValue == null) {
      defaultValue = firstSrid;
    }
    resultSrid.setDefaultValue(defaultValue);
    resultSrid.setMinValue(0);
    this.requestRecordDefinition.addField(resultSrid);
  }

  private void addFieldScaleFactorXy() {
    final FieldDefinition resultScaleFactorXy = new FieldDefinition(
      "resultScaleFactorXy",
      DataTypes.DOUBLE,
      false,
      "The scale factor to apply the x, y coordinates. The scale factor is 1 / minimum unit. For example if the minimum unit was 1mm (0.001) the scale factor is 1000 (1 / 0.001).");
    resultScaleFactorXy.setProperty(BusinessApplication.CORE_PARAMETER, true);
    resultScaleFactorXy.setProperty(BusinessApplication.JOB_PARAMETER, true);

    double defaultValue = Property.getDouble(this, "resultScaleFactorXy", 1000);
    if (defaultValue < 0) {
      defaultValue = 1000;
    }
    resultScaleFactorXy.setDefaultValue(defaultValue);

    this.requestRecordDefinition.addField(resultScaleFactorXy);
  }

  private void addFieldScaleFactorZ() {
    final FieldDefinition resultScaleFactorZ = new FieldDefinition(
      "resultScaleFactorZ",
      DataTypes.DOUBLE,
      false,
      "The scale factor to apply the z coordinate. The scale factor is 1 / minimum unit. For example if the minimum unit was 1mm (0.001) the scale factor is 1000 (1 / 0.001).");
    resultScaleFactorZ.setProperty(BusinessApplication.CORE_PARAMETER, true);
    double defaultValue = Property.getDouble(this, "resultScaleFactorZ", 1000);
    if (defaultValue < 0) {
      defaultValue = 1000;
    }
    resultScaleFactorZ.setDefaultValue(defaultValue);
    resultScaleFactorZ.setProperty(BusinessApplication.JOB_PARAMETER, true);
    this.requestRecordDefinition.addField(resultScaleFactorZ);
  }

  public void addInputDataContentType(final String contentType,
    final String description, final String fileExtension) {
    final String inputDataContentType = Property.getString(this,
      "inputDataContentType");
    final String inputDataFileExtension = Property.getString(this,
      "inputDataFileExtension");
    if (isContentTypeOrFileExtensionEqual(inputDataContentType, contentType,
      fileExtension)
      || isContentTypeOrFileExtensionEqual(inputDataFileExtension, contentType,
        fileExtension)) {
      this.defaultInputDataContentType = contentType;
      this.defaultInputDataFileExtension = fileExtension;
    }

    this.inputDataContentTypes.add(contentType);

    this.inputDataFileExtensions.put(fileExtension, description);
    this.inputDataFileExtensions = CollectionUtil.sortByValues(this.inputDataFileExtensions);
    this.inputFileExtensionToContentType.put(fileExtension, contentType);
  }

  public void addRequestAttribute(int index, final FieldDefinition attribute) {
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

  public void addResultAttribute(int index, final FieldDefinition attribute) {
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
    final String resultDataContentType = Property.getString(this,
      "resultDataContentType");
    final String resultDataFileExtension = Property.getString(this,
      "resultDataFileExtension");
    if (isContentTypeOrFileExtensionEqual(resultDataContentType, contentType,
      fileExtension)
      || isContentTypeOrFileExtensionEqual(resultDataFileExtension,
        contentType, fileExtension)) {
      this.defaultResultDataContentType = contentType;
      this.defaultResultDataFileExtension = fileExtension;
    }

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
    return this.defaultInputDataContentType;
  }

  public String getDefaultInputDataFileExtension() {
    return this.defaultInputDataFileExtension;
  }

  public String getDefaultResultDataContentType() {
    return this.defaultResultDataContentType;
  }

  public String getDefaultResultDataFileExtension() {
    return this.defaultResultDataFileExtension;
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
      return this.inputFileExtensionToContentType.get(fileExtension);
    }
  }

  public Set<String> getInputDataContentTypes() {
    return this.inputDataContentTypes;
  }

  public Map<String, String> getInputDataFileExetensions() {
    return this.inputDataFileExtensions;
  }

  public Map<String, String> getInputFileExtensionToContentType() {
    return this.inputFileExtensionToContentType;
  }

  public Map<String, String> getInputFileExtensionToMediaType() {
    return this.inputFileExtensionToContentType;
  }

  public Expression getInstantModeExpression() {
    return this.instantModeExpression;
  }

  public String getInstantModePermission() {
    return this.instantModePermission;
  }

  public AppLog getLog() {
    return this.log;
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

  public String getPackageName() {
    return this.packageName;
  }

  public BusinessApplicationPlugin getPluginAnnotation() {
    return this.pluginAnnotation;
  }

  public synchronized RecordDefinitionImpl getRequestRecordDefinition() {
    if (this.requestRecordDefinition.getFieldCount() == 0) {
      if (this.requestAttributeMap.size() > 0) {
        final FieldDefinition requestSequenceNumber = this.requestRecordDefinition.addField(
          "requestSequenceNumber", DataTypes.INT);
        requestSequenceNumber.setProperty(BusinessApplication.CORE_PARAMETER,
          true);
        requestSequenceNumber.setMinValue(1);

        if (this.defaultInputDataContentType == null) {
          this.defaultInputDataFileExtension = getDefaultFileExtension(this.inputFileExtensionToContentType);
          this.defaultInputDataContentType = getDefaultMimeType(this.inputDataContentTypes);
        }

        if (this.hasGeometryRequestAttribute) {
          addFieldRequestSrid();
        }

        addFieldResultDataContentType();

        if (this.hasGeometryResultAttribute) {
          addFieldResultSrid();
          addFieldResultNumAxis();
          addFieldScaleFactorXy();
          addFieldScaleFactorZ();
        }
        for (final FieldDefinition attribute : this.requestAttributeMap.values()) {
          this.requestRecordDefinition.addField(attribute);
        }
      }
    }

    return this.requestRecordDefinition;
  }

  public String getResultContentType(final String fileExtension) {
    if (isResultContentTypeSupported(fileExtension)) {
      return fileExtension;
    } else {
      return this.resultFileExtensionToContentType.get(fileExtension);
    }
  }

  public Set<String> getResultDataContentTypes() {
    return this.resultDataContentTypes;
  }

  public Map<String, String> getResultDataFileExtensions() {
    return this.resultDataFileExtensions;
  }

  public String getResultListProperty() {
    return this.resultListProperty;
  }

  public synchronized RecordDefinition getResultRecordDefinition() {
    if (this.resultRecordDefinition.getFieldCount() == 0) {
      if (this.resultAttributeMap.size() > 0) {
        this.resultRecordDefinition.addField(new FieldDefinition(
          "sequenceNumber", DataTypes.INT, true,
          "The index of the request record that this result relates to."));
        if (this.resultListProperty != null) {
          this.resultRecordDefinition.addField(new FieldDefinition(
            "resultNumber", DataTypes.INT, true,
            "The index of the result record within the result for a request."));
        }
        for (final FieldDefinition attribute : this.resultAttributeMap.values()) {
          final String name = attribute.getName();
          final FieldDefinition requestAttribute = this.requestAttributeByNameMap.get(name);
          if (requestAttribute != null) {
            String description = attribute.getDescription();
            if (!Property.hasValue(description)) {
              description = requestAttribute.getDescription();
              attribute.setDescription(description);
            }
            Object defaultValue = attribute.getDefaultValue();
            if (defaultValue == null) {
              defaultValue = requestAttribute.getDefaultValue();
              attribute.setDefaultValue(defaultValue);
            }
          }

          this.resultRecordDefinition.addField(attribute);
        }
      }
    }
    return this.resultRecordDefinition;
  }

  public String getTitle() {
    if (this.title == null && this.name != null) {
      this.title = CaseConverter.toCapitalizedWords(this.name);
    }
    return this.title;
  }

  private boolean isContentTypeOrFileExtensionEqual(final String match,
    final String contentType, final String fileExtension) {
    if (Property.hasValue(match)) {
      return EqualsRegistry.equal(match, contentType)
        || EqualsRegistry.equal(match, fileExtension);
    } else {
      return false;
    }
  }

  public boolean isCoreParameter(final String fieldName) {
    final FieldDefinition attribute = this.requestRecordDefinition.getField(fieldName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist" + fieldName);
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
    return this.hasTestExecuteMethod;
  }

  public boolean isInfoLogEnabled() {
    return this.log.isInfoEnabled();
  }

  public boolean isInputContentTypeSupported(final String contentType) {
    return this.inputDataContentTypes.contains(contentType);
  }

  public boolean isJobParameter(final String fieldName) {
    final FieldDefinition attribute = this.requestRecordDefinition.getField(fieldName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist" + fieldName);
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

  public boolean isRequestParameter(final String fieldName) {
    final FieldDefinition attribute = this.requestRecordDefinition.getField(fieldName);
    if (attribute == null) {
      throw new IllegalArgumentException("Parameter does not exist" + fieldName);
    } else {
      return BooleanStringConverter.getBoolean(attribute.getProperty(REQUEST_PARAMETER));
    }
  }

  public boolean isResultContentTypeSupported(final String contentType) {
    return this.resultDataContentTypes.contains(contentType);
  }

  public boolean isSecurityServiceRequired() {
    return this.securityServiceRequired;
  }

  public boolean isTestModeEnabled() {
    return this.testModeEnabled;
  }

  public boolean isValidateGeometry() {
    return this.validateGeometry;
  }

  public void setBatchModePermission(final String batchModePermission) {
    if (Property.hasValue(batchModePermission)) {
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
    if (Property.hasValue(instantModePermission)) {
      this.instantModePermission = instantModePermission;
    } else {
      this.instantModePermission = "permitAll";
    }
    this.instantModeExpression = new SpelExpressionParser().parseExpression(this.instantModePermission);
  }

  public void setLogLevel(final String logLevel) {
    this.log.setLogLevel(logLevel);
    final Level level = Level.toLevel(logLevel);
    final String moduleName = getModuleName();
    Logger.getLogger(moduleName + "." + this.name).setLevel(level);
    // Tempory fix for geocoder logging
    Logger.getLogger(moduleName + ".ca").setLevel(level);
    Logger.getLogger(getPackageName()).setLevel(level);
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

  public void setPackageName(final String packageName) {
    this.packageName = packageName;
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
