package ca.bc.gov.open.cpf.api.web.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Blob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.web.builder.BatchJobResultUiBuilder;
import ca.bc.gov.open.cpf.api.web.builder.BatchJobUiBuilder;
import ca.bc.gov.open.cpf.api.web.builder.BusinessApplicationUiBuilder;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.impl.log.AppLogUtil;

import com.revolsys.converter.string.BooleanStringConverter;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.model.ArrayDataObject;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.data.model.types.SimpleDataType;
import com.revolsys.gis.model.data.equals.EqualsRegistry;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoConstants;
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.io.Writer;
import com.revolsys.io.json.JsonDataObjectIoFactory;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.spring.ByteArrayResource;
import com.revolsys.spring.InputStreamResource;
import com.revolsys.spring.InvokeMethodAfterCommit;
import com.revolsys.spring.OutputStreamResource;
import com.revolsys.spring.security.MethodSecurityExpressionRoot;
import com.revolsys.ui.html.HtmlUtil;
import com.revolsys.ui.html.builder.HtmlUiBuilder;
import com.revolsys.ui.html.decorator.CollapsibleBox;
import com.revolsys.ui.html.decorator.FieldNoLabelDecorator;
import com.revolsys.ui.html.decorator.TableBody;
import com.revolsys.ui.html.decorator.TableHeadingDecorator;
import com.revolsys.ui.html.fields.BigDecimalField;
import com.revolsys.ui.html.fields.BigIntegerField;
import com.revolsys.ui.html.fields.ByteField;
import com.revolsys.ui.html.fields.CheckBoxField;
import com.revolsys.ui.html.fields.DateField;
import com.revolsys.ui.html.fields.DateTimeField;
import com.revolsys.ui.html.fields.DoubleField;
import com.revolsys.ui.html.fields.EmailAddressField;
import com.revolsys.ui.html.fields.Field;
import com.revolsys.ui.html.fields.FileField;
import com.revolsys.ui.html.fields.FloatField;
import com.revolsys.ui.html.fields.IntegerField;
import com.revolsys.ui.html.fields.LongField;
import com.revolsys.ui.html.fields.SelectField;
import com.revolsys.ui.html.fields.ShortField;
import com.revolsys.ui.html.fields.SubmitField;
import com.revolsys.ui.html.fields.TextAreaField;
import com.revolsys.ui.html.fields.TextField;
import com.revolsys.ui.html.fields.TimestampField;
import com.revolsys.ui.html.fields.UrlField;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.serializer.KeySerializerTableSerializer;
import com.revolsys.ui.html.serializer.RowsTableSerializer;
import com.revolsys.ui.html.serializer.key.BooleanImageKeySerializer;
import com.revolsys.ui.html.serializer.key.KeySerializer;
import com.revolsys.ui.html.serializer.key.StringKeySerializer;
import com.revolsys.ui.html.view.DivElementContainer;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.ElementContainer;
import com.revolsys.ui.html.view.ListElement;
import com.revolsys.ui.html.view.RawContent;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.html.view.TableView;
import com.revolsys.ui.html.view.XmlTagElement;
import com.revolsys.ui.model.PageInfo;
import com.revolsys.ui.web.controller.PathAliasController;
import com.revolsys.ui.web.exception.PageNotFoundException;
import com.revolsys.ui.web.rest.interceptor.MediaTypeUtil;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.ui.web.utils.MultipartFileResource;
import com.revolsys.util.CaseConverter;
import com.revolsys.util.DateUtil;
import com.revolsys.util.ExceptionUtil;
import com.vividsolutions.jts.geom.Geometry;

/**
 * <p>The Cloud Processing Framework REST API allows client applications in Java, JavaScript or other
 * programming languages to query the available business applications, create cloud jobs and download
 * the results of cloud jobs on behalf of their users.<p>
 * 
 * <p>Most of the resources can return JSON or XML documents by appending the <code>.json</code> or <code>.xml</code>
 * file format extension to the URI Templates before any query string parameters. JSON is the preferred
 * format due to it's well defined mpa, list and value structure. Some resources that
 * always return a specific data type will ignore the file format extension if specified.</p>
 * 
 * <p>The CPF REST API can also be accessed directly using a web browser by not specifying a file format extension.
 * If a file format extension
 * is not included in the request then the HTML user interface will be displayed instead of the resources
 * described in this API. The HTML user interface allows full access to the CPF without any programming
 * experience required. The HTML user interface will be slightly different from the responses 
 * described in this API.</p>
 */
@Controller
public class CloudProcessingFramework {

  private static PageInfo addPage(final PageInfo parent, final Object path,
    final String title) {
    final String url = MediaTypeUtil.getUrlWithExtension(path.toString());
    return parent.addPage(url, title);
  }

  private static PageInfo addPage(final PageInfo parent, final String path,
    final String title, final String... methods) {
    final String url = MediaTypeUtil.getUrlWithExtension(path);
    return parent.addPage(url, title, methods);
  }

  private static void checkPermission(
    final BusinessApplication businessApplication) {
    final EvaluationContext evaluationContext = getSecurityEvaluationContext();
    if (!hasPermission(businessApplication, evaluationContext)) {
      throw new AccessDeniedException("No permission for business application "
        + businessApplication.getName());
    }
  }

  private static PageInfo createRootPageInfo(final String title) {
    HttpServletUtils.setAttribute("title", title);
    final PageInfo page = new PageInfo(title);
    final String url = HttpServletUtils.getFullRequestUrl();
    page.setUrl(url);
    return page;
  }

  private static EvaluationContext getSecurityEvaluationContext() {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    final MethodSecurityExpressionRoot root = new MethodSecurityExpressionRoot(
      authentication);
    final EvaluationContext evaluationContext = new StandardEvaluationContext(
      root);
    return evaluationContext;
  }

  private static boolean hasPermission(
    final BusinessApplication businessApplication,
    final EvaluationContext evaluationContext) {
    if (ExpressionUtils.evaluateAsBoolean(
      businessApplication.getBatchModeExpression(), evaluationContext)) {
      return true;
    } else if (ExpressionUtils.evaluateAsBoolean(
      businessApplication.getInstantModeExpression(), evaluationContext)) {
      return true;
    } else {
      return false;
    }

  }

  private static boolean hasPermission(final Expression expression) {
    final EvaluationContext evaluationContext = getSecurityEvaluationContext();
    final boolean permitted = ExpressionUtils.evaluateAsBoolean(expression,
      evaluationContext);
    return permitted;
  }

  private BatchJobService batchJobService;

  private BatchJobUiBuilder batchJobUiBuilder;

  private BatchJobResultUiBuilder batchJobResultUiBuilder;

  private BusinessApplicationUiBuilder businessAppBuilder;

  private CpfDataAccessObject dataAccessObject;

  private Map<String, RawContent> rawContent = new HashMap<String, RawContent>();

  /**
   * Construct a new CloudProcessingFramework.
   */
  public CloudProcessingFramework() {
  }

  private void addBatchJobStatusLink(final PageInfo page, final DataObject job) {
    final Number batchJobId = job.getIdValue();
    final String batchJobUrl = HttpServletUtils.getFullUrl("/ws/jobs/"
      + batchJobId + "/");
    final Timestamp timestamp = job.getValue(BatchJob.WHEN_CREATED);
    final PageInfo childPage = addPage(page, batchJobUrl, "Batch Job "
      + batchJobId + " Status");

    childPage.setAttribute("batchJobId", batchJobId);
    childPage.setAttribute("batchJobUrl", batchJobUrl);
    childPage.setAttribute("jobStatus", job.getValue(BatchJob.JOB_STATUS));
    childPage.setAttribute("creationTimestamp",
      DateUtil.format(DateFormat.DEFAULT, DateFormat.SHORT, timestamp));
  }

  private void addFieldRow(final ElementContainer fields,
    final Attribute attribute) {
    final Field field = getField(attribute);
    final String name = attribute.getName();
    final String label = CaseConverter.toCapitalizedWords(name);
    String instructions = attribute.getDescription();
    if (!StringUtils.hasText(instructions)) {
      instructions = field.getDefaultInstructions();
    }
    final String labelUrl = attribute.getProperty("descriptionUrl");
    TableHeadingDecorator.addRow(fields, field, labelUrl, label, instructions);
  }

  private void addFieldRow(final ElementContainer fields,
    final DataObjectMetaDataImpl metaData, final String name) {
    if (metaData.hasAttribute(name)) {
      final Attribute sridField = metaData.getAttribute(name);
      addFieldRow(fields, sridField);
    }
  }

  private void addGeometryFields(final ElementContainer fields,
    final BusinessApplication businessApplication) {
    final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
    addFieldRow(fields, requestMetaData, "srid");
    if (requestMetaData.hasAttribute("resultSrid")) {
      final ElementContainer container = new ElementContainer();

      final Attribute resultSrid = requestMetaData.getAttribute("resultSrid");
      final Field resultSridField = getField(resultSrid);
      FieldNoLabelDecorator.add(container, resultSridField,
        resultSrid.getDescription());

      final ElementContainer advancedFields = new ElementContainer(
        new TableBody());
      for (final String name : Arrays.asList("resultNumAxis",
        "resultScaleFactorXy", "resultScaleFactorZ")) {
        addFieldRow(advancedFields, requestMetaData, name);
      }

      final ElementContainer collapsibleBox = new ElementContainer(
        new CollapsibleBox("Advanced Geometry Parameters", false),
        advancedFields);
      container.add(collapsibleBox);
      TableHeadingDecorator.addRow(fields, container,
        "Result Coordinate System", null);
    }
  }

  private void addInputDataFields(final ElementContainer fields,
    final BusinessApplication businessApplication) {
    final Map<String, String> inputDataFileExtensions = businessApplication.getInputDataFileExetensions();
    final String defaultInputType = BusinessApplication.getDefaultFileExtension(inputDataFileExtensions);
    final SelectField inputDataContentType = new SelectField(
      "inputDataContentType", defaultInputType, true, inputDataFileExtensions);

    TableHeadingDecorator.addRow(
      fields,
      inputDataContentType,
      "Input Data Content Type",
      "The MIME type of the input data specified by an inputData or inputDataUrl parameter.");

    final UrlField inputDataUrl = new UrlField("inputDataUrl", false);
    TableHeadingDecorator.addRow(
      fields,
      inputDataUrl,
      "Input Data URL",
      "The http: URL to the file or resource containing input data. The CPF requires UTF-8 encoding for text files. Shapefiles may use a different encoding if a cpg file is provided.");

    final FileField inputData = new FileField("inputData", false);
    TableHeadingDecorator.addRow(
      fields,
      inputData,
      "Input Data",
      "The multi-part file containing the input data. The CPF requires UTF-8 encoding for text files. Shapefiles may use a different encoding if a cpg file is provided.");
  }

  private void addMultiInputDataFields(final ElementContainer fields,
    final BusinessApplication businessApplication) {
    final Map<String, String> inputDataContentTypes = businessApplication.getInputDataFileExetensions();
    final String defaultInputType = BusinessApplication.getDefaultFileExtension(inputDataContentTypes);
    final SelectField inputDataContentType = new SelectField(
      "inputDataContentType", defaultInputType, true, inputDataContentTypes);

    if (businessApplication.isPerRequestInputData()) {
      final ElementContainer container = new ElementContainer();
      addRawContent(container,
        "ca/bc/gov/open/cpf/api/web/service/multiInputDataPre.html");
      container.add(inputDataContentType);
      addRawContent(container,
        "ca/bc/gov/open/cpf/api/web/service/multiInputDataPost.html");
      TableHeadingDecorator.addRow(
        fields,
        container,
        "Input Data",
        "Use the 'Add File' or 'Add URL' buttons to add one or more input data files, then select the MIME type for each file and enter the URL or select the file.");

    } else {
      addInputDataFields(fields, businessApplication);
    }
  }

  private void addNotificationFields(final ElementContainer container) {
    final EmailAddressField emailField = new EmailAddressField(
      "notificationEmail", false);
    TableHeadingDecorator.addRow(container, emailField, "Notification Email",
      "The email address to send the job status to when the job is completed.");
    final UrlField urlField = new UrlField("notificationUrl", false);
    TableHeadingDecorator.addRow(
      container,
      urlField,
      "Notification URL",
      "The http: URL to be notified when the job is completed. A copy of the Job status will be posted to process running at this URL.");
  }

  private void addParameter(final List<Map<String, Object>> parameters,
    final Attribute attribute, final boolean perRequestInputData) {
    final String name = attribute.getName();
    final String typeDescription = attribute.getTypeDescription();
    final String description = attribute.getDescription();
    final boolean jobParameter = BooleanStringConverter.getBoolean(attribute.getProperty(BusinessApplication.JOB_PARAMETER));
    final boolean requestParameter = BooleanStringConverter.getBoolean(attribute.getProperty(BusinessApplication.REQUEST_PARAMETER));
    if (jobParameter || requestParameter) {
      final Collection<Object> allowedValues = attribute.getAllowedValues()
        .keySet();
      final String descriptionUrl = attribute.getProperty("descriptionUrl");
      addParameter(parameters, name, typeDescription, descriptionUrl,
        description, jobParameter, requestParameter, perRequestInputData,
        allowedValues);
    }
  }

  private void addParameter(final List<Map<String, Object>> parameters,
    final String name, final String typeDescription,
    final String descriptionUrl, final String description,
    final boolean jobParameter, final boolean requestParameter,
    final boolean perRequestInputData, final Collection<Object> allowedValues) {
    final Map<String, Object> parameter = new LinkedHashMap<String, Object>();
    parameter.put("name", name);
    parameter.put("type", typeDescription);
    parameter.put("description", description);
    parameter.put("descriptionUrl", descriptionUrl);
    parameter.put("jobParameter", jobParameter);
    if (!perRequestInputData) {
      parameter.put("requestParameter", requestParameter);
    }
    if (allowedValues != null && !allowedValues.isEmpty()) {
      parameter.put("allowedValues", allowedValues);
    }
    parameters.add(parameter);
  }

  private void addRawContent(final ElementContainer container,
    final String resource) {
    RawContent content = rawContent.get(resource);
    if (content == null) {
      synchronized (rawContent) {
        content = new RawContent(new ClassPathResource(resource));
        rawContent.put(resource, content);
      }
    }
    container.add(content);
  }

  private void addResultDataFields(final ElementContainer container,
    final BusinessApplication businessApplication, final String fieldName) {
    final Map<String, String> resultDataFileExtensions = businessApplication.getResultDataFileExtensions();
    final String defaultValue = BusinessApplication.getDefaultFileExtension(resultDataFileExtensions);

    final SelectField resultDataContentType = new SelectField(fieldName,
      defaultValue, true, resultDataFileExtensions);
    TableHeadingDecorator.addRow(
      container,
      resultDataContentType,
      "Result Data Content Type",
      "The MIME type of the result data specified to be returned after running the request");

  }

  private void addTestFields(final ElementContainer fields,
    final BusinessApplication businessApplication) {
    if (businessApplication.isTestModeEnabled()) {
      final CheckBoxField cpfPluginTest = new CheckBoxField("cpfPluginTest");
      TableHeadingDecorator.addRow(fields, cpfPluginTest, "Test Mode",
        "Enable test mode for the request.");

      final DoubleField minTime = new DoubleField("cpfMinExecutionTime", false,
        0);
      TableHeadingDecorator.addRow(fields, minTime, "Min Execution Time (s)",
        "The minimum execution time.");

      final DoubleField meanTime = new DoubleField("cpfMeanExecutionTime",
        false, 0);
      TableHeadingDecorator.addRow(fields, meanTime, "Mean Execution Time (s)",
        "The mean execution time using a gaussian distribution.");

      final DoubleField standardDeviation = new DoubleField(
        "cpfStandardDeviation", false, 2);
      TableHeadingDecorator.addRow(fields, standardDeviation,
        "Standard Deviation (s)",
        "The standard deviation for a gaussian distribution.");

      final DoubleField maxTime = new DoubleField("cpfMaxExecutionTime", false,
        10);
      TableHeadingDecorator.addRow(fields, maxTime, "Max Execution Time (s)",
        "The maximum execution time.");

      if (businessApplication.isHasResultListProperty()) {
        final DoubleField meanNumResults = new DoubleField("cpfMeanNumResults",
          false, 3);
        TableHeadingDecorator.addRow(fields, meanNumResults,
          "Mean Num Results",
          "The mean number of results for each request using a gaussian distribution.");
      }
    }
  }

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  public void addTestParameters(final BusinessApplication businessApplication,
    final Map parameters) {
    if (businessApplication.isTestModeEnabled()) {
      final HttpServletRequest request = HttpServletUtils.getRequest();
      for (final Enumeration<String> parameterNames = request.getParameterNames(); parameterNames.hasMoreElements();) {
        final String name = parameterNames.nextElement();
        if (name.startsWith("cpf")) {
          final Object value = request.getParameter(name);
          parameters.put(name, value);
        }
      }
    }
  }

  private void checkPermission(final Expression expression,
    final String accessDeniedMessage) {
    final boolean permitted = hasPermission(expression);
    if (!permitted) {
      throw new AccessDeniedException(accessDeniedMessage);
    }
  }

  @PreDestroy
  public void close() {
    this.batchJobService = null;
    this.batchJobUiBuilder = null;

    this.batchJobResultUiBuilder = null;
    this.businessAppBuilder = null;
    this.dataAccessObject = null;
    this.rawContent = null;
  }

  private DataObject createBatchJob() {
    final DataObject batchJob = dataAccessObject.create(BatchJob.BATCH_JOB);
    batchJob.setIdValue(dataAccessObject.getDataStore().createPrimaryIdValue(
      BatchJob.BATCH_JOB));
    final String prefix = PathAliasController.getAlias();
    if (prefix != null) {
      final Map<String, String> properties = new HashMap<String, String>();
      properties.put("webServicePrefix", prefix);
      batchJob.setValue(BatchJob.PROPERTIES,
        JsonMapIoFactory.toString(properties));
    }
    batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.SUBMITTED);
    batchJob.setValue(BatchJob.WHEN_STATUS_CHANGED,
      new Timestamp(System.currentTimeMillis()));
    batchJob.setValue(BatchJob.NUM_SUBMITTED_GROUPS, 0);
    batchJob.setValue(BatchJob.NUM_COMPLETED_GROUPS, 0);
    batchJob.setValue(BatchJob.GROUP_SIZE, 1);
    batchJob.setValue(BatchJob.NUM_SCHEDULED_GROUPS, 0);
    batchJob.setValue(BatchJob.NUM_COMPLETED_REQUESTS, 0);
    batchJob.setValue(BatchJob.NUM_FAILED_REQUESTS, 0);

    return batchJob;
  }

  /**
   * <p>Create a new cloud job containing multiple requests to be processed by the
   * business application.</p>
   * 
   * <p>The service parameters must be passed
   *  using the multipart/form-data encoding in the body of a HTTP POST request (e.g. a HTML form).</p>
   * 
   * <p>In addition to the standard parameters listed in the API each business
   * application has additional job and request parameters. The
   * <a href= "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsMultiple">Get Business Applications Multiple</a>
   * resource should be consulted to get the full list of supported parameters. </p>
   * 
   * <h5>Structured Input Data</h5>
   * 
   * <p>For structured input data business applications the requests are specified
   * using either a single inputData file or a single inputDataUrl can be
   * specified. The file must be in the <a href="../../fileFormats.html">file
   * format</a> specified by a single inputDataContentType. The contents of the
   * file must contain one record for each request to be processed in the batch
   * job. The fields of each record must contain the request parameters
   * supported by the business application. The names of the parameters are case
   * sensitive.</p>
   * 
   * <h5>Opaque input data</h5>
   * 
   * <p>For opaque input data (e.g. JPEG image, ESRI Shapefile) the requests can be
   * specified as one or more inputData files or one or more inputDataUrl
   * parameters. It is not possible to mix inputData and inputDataUrl parameters
   * in the same cloud job. If all the requests have the same content type a
   * single inputDataContentType can be specified. Otherwise an
   * inputDataContentType must be specified for each inputData or inputDataUrl
   * in the same order.</p>
   * 
   * <p class="note">NOTE: The maximum size including all parameters and protocol overhead of a
   * multi-part request is 20MB. Therefore inputDataUrl should be used instead
   * of inputData where possible.</p>
   * 
   * 
   * @param businessApplicationName The name of the business application.
   * @param inputDataContentTypes The MIME type of the input data specified by
   * an inputData or inputDataUrl parameter.
   * @param inputDataFiles The multi-part file containing the input data.
   * @param inputDataUrls The http: URL to the file or resource containing input
   * data.
   * @param srid The coordinate system code of the projection for the input geometry.
   * @param resultDataContentType The MIME type of the result data specified to
   * be returned after running the request.
   * @param resultSrid The coordinate system code of the projection for the
   * result geometry.
   * @param resultNumAxis The number of coordinate axis in the result geometry
   * (e.g. 2 for 2D or 3 for 3D).
   * @param resultScaleFactorXy The scale factor to apply the x, y coordinates.
   * The scale factor is 1 / minimum unit. For example if the minimum unit was
   * 1mm (0.001) the scale factor is 1000 (1 / 0.001).
   * @param resultScaleFactorZ The scale factor to apply the z coordinate. The
   * scale factor is 1 / minimum unit. For example if the minimum unit was 1mm
   * (0.001) the scale factor is 1000 (1 / 0.001).
   * @param notificationUrl The http: URL to be notified when the job is
   * completed. A copy of the Job status will be posted to process running at
   * this URL.
   * @param notificationEmail The email address to send the job status to when
   * the job is completed.
   * 
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/multiple"
  }, method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createJobWithMultipleRequests(
    @PathVariable final String businessApplicationName,
    @RequestParam(value = "inputDataContentType", required = false) final String[] inputDataContentTypes,
    @RequestParam(value = "inputData", required = false) List<MultipartFile> inputDataFiles,
    @RequestParam(value = "inputDataUrl", required = false) List<String> inputDataUrls,
    @RequestParam(required = false) final String srid,
    @RequestParam(required = false) final String resultDataContentType,
    @RequestParam(required = false, defaultValue = "3005") final int resultSrid,
    @RequestParam(required = false, defaultValue = "2") final int resultNumAxis,
    @RequestParam(required = false, defaultValue = "-1") final int resultScaleFactorXy,
    @RequestParam(required = false, defaultValue = "-1") final int resultScaleFactorZ,
    @RequestParam(required = false) String notificationUrl, @RequestParam(
        required = false) final String notificationEmail) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, "clientMultiple");
    if (businessApplication != null) {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final String consumerKey = getConsumerKey();

      final String defaultInputDataContentType = businessApplication.getDefaultInputDataContentType();
      final String defaultResultDataContentType = businessApplication.getDefaultResultDataContentType();

      final Map<String, String> businessApplicationParameters = new HashMap<String, String>();
      addTestParameters(businessApplication, businessApplicationParameters);

      final DataObject batchJob = createBatchJob();
      final Long batchJobId = batchJob.getValue(BatchJob.BATCH_JOB_ID);

      final AppLog log = businessApplication.getLog();
      log.info("Start\tJob submit multiple\tbatchJobId=" + batchJobId);

      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_NAME,
        businessApplicationName);
      batchJob.setValue(BatchJob.USER_ID, consumerKey);
      batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS, 0);

      batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE,
        defaultResultDataContentType);
      String inputDataContentType = defaultInputDataContentType;
      final List<String> inputContentTypes = new ArrayList<>();
      if (inputDataContentTypes == null) {
      } else {
        inputContentTypes.addAll(Arrays.asList(inputDataContentTypes));
        for (final ListIterator<String> iterator = inputContentTypes.listIterator(); iterator.hasNext();) {
          final String inputContentType = iterator.next();
          final String contentType = getInputMediaType(businessApplication,
            inputContentType);
          if (contentType == null) {
            throw new HttpMessageNotReadableException("inputDataContentType="
              + inputDataContentType + " is not supported.");
          } else {
            iterator.set(contentType);
          }
        }
        if (!businessApplication.isPerRequestInputData()) {
          if (inputContentTypes.size() == 1) {
            inputDataContentType = inputContentTypes.get(0);
            batchJob.setValue(BatchJob.INPUT_DATA_CONTENT_TYPE,
              inputDataContentType);
          } else {
            dataAccessObject.delete(batchJob);
            throw new HttpMessageNotReadableException(
              "inputDataContentType can only have one value.");
          }
        }
      }
      if (resultDataContentType != null) {
        final String resultType = businessApplication.getResultContentType(resultDataContentType);
        if (resultType == null) {
          throw new HttpMessageNotReadableException("resultDataContentType="
            + resultDataContentType + " is not supported.");
        } else {
          batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE, resultType);
        }
      }

      if (StringUtils.hasText(notificationEmail)) {
        if (StringUtils.hasText(notificationUrl)) {
          throw new HttpMessageNotReadableException(
            "Both notificationUrl and notificationEmail cannot be specified. Enter a value in one or the other but not both.");
        } else {
          notificationUrl = "mailto:" + notificationEmail;
        }
      }
      if (StringUtils.hasText(notificationUrl)) {
        batchJob.setValue(BatchJob.NOTIFICATION_URL, notificationUrl);
      }

      final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
      for (final Attribute parameter : requestMetaData.getAttributes()) {
        final String parameterName = parameter.getName();
        String value = HttpServletUtils.getParameter(parameterName);
        final boolean jobParameter = businessApplication.isJobParameter(parameterName);
        final boolean requestParameter = businessApplication.isRequestParameter(parameterName);
        boolean hasValue = StringUtils.hasText(value);
        if (parameter.getType() == DataTypes.BOOLEAN) {
          if ("on".equals(value)) {
            value = "true";
          } else {
            value = "false";
          }
          hasValue = true;
        }
        if (hasValue) {
          if (jobParameter) {
            businessApplicationParameters.put(parameterName, value);
          } else if (requestParameter) {
            if (parameter.getType() != DataTypes.BOOLEAN
              || StringUtils.hasText(HttpServletUtils.getParameter(parameterName))) {
              throw new HttpMessageNotReadableException(
                "Parameter "
                  + parameterName
                  + " cannot be specified on a job. It can only be specified as a field in the input data.");
            }
          }
        } else {
          if (jobParameter && !requestParameter && parameter.isRequired()) {
            throw new HttpMessageNotReadableException("Parameter "
              + parameterName + " is required");
          }
        }
      }
      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_PARAMS,
        JsonMapIoFactory.toString(businessApplicationParameters));

      try {

        if (inputDataUrls == null) {
          inputDataUrls = new ArrayList<String>();
        } else {
          for (final Iterator<String> iterator = inputDataUrls.iterator(); iterator.hasNext();) {
            final String inputDataUrl = iterator.next();
            if (!StringUtils.hasText(inputDataUrl)) {
              iterator.remove();
            }
          }
        }
        if (inputDataFiles == null) {
          inputDataFiles = new ArrayList<MultipartFile>();
        } else {
          for (final Iterator<MultipartFile> iterator = inputDataFiles.iterator(); iterator.hasNext();) {
            final MultipartFile multipartFile = iterator.next();
            if (multipartFile.getSize() == 0) {
              iterator.remove();
            }
          }
        }

        if (inputDataUrls.isEmpty() == inputDataFiles.isEmpty()) {
          throw new HttpMessageNotReadableException(
            "Either inputData files or inputDataUrl(s) must be specified but not both");
        } else if (businessApplication.isPerRequestInputData()) {
          batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS,
            inputDataFiles.size() + inputDataUrls.size());
          batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.REQUESTS_CREATED);
          dataAccessObject.write(batchJob);
          int requestSequenceNumber = 0;
          if (inputDataUrls.isEmpty()) {
            for (final MultipartFile file : inputDataFiles) {
              final InputStream in = file.getInputStream();
              try {
                final org.springframework.core.io.Resource resource = new InputStreamResource(
                  "in", in, file.getSize());
                dataAccessObject.createBatchJobExecutionGroup(batchJobId,
                  ++requestSequenceNumber, inputDataContentType, resource);
              } finally {
                InvokeMethodAfterCommit.invoke(in, "close");
              }
            }
          } else {
            for (final String inputDataUrl : inputDataUrls) {
              dataAccessObject.createBatchJobExecutionGroup(batchJobId,
                ++requestSequenceNumber, inputDataContentType,
                inputDataUrl.trim());
            }
          }
          batchJobService.schedule(businessApplicationName, batchJobId);
        } else {
          createStructuredJob(batchJobId, batchJob, inputDataFiles,
            inputDataUrls);
        }
      } catch (final IOException e) {
        dataAccessObject.delete(batchJob);
        throw new HttpMessageNotReadableException(e.getMessage(), e);
      }
      HttpServletUtils.setPathVariable("batchJobId", batchJobId);
      batchJobUiBuilder.redirectPage("clientView");

      if (businessApplication.isInfoLogEnabled()) {
        AppLogUtil.info(log, "End\tJob submit multiple\tbatchJobId="
          + batchJobId, stopWatch);
      }
      final Map<String, Object> statistics = new HashMap<String, Object>();
      statistics.put("submittedJobsTime", stopWatch);
      statistics.put("submittedJobsCount", 1);
      InvokeMethodAfterCommit.invoke(batchJobService, "addStatistics",
        businessApplication, statistics);
    }
  }

  /**
   * <p>Create a new cloud job containing multiple requests to be processed by the
   * business application.</p>
   * 
   * <p>The job and request parameters for the business application must be passed
   * using the multipart/form-data encoding in the body of a HTTP POST request (e.g. a HTML form).</p>
   * 
   * <p>In addition to the standard parameters listed in the API each business
   * application has additional job and request parameters. The
   * <a href= "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsSingle">Get Business Applications Single</a>
   * resource should be consulted to get the full list of supported parameters. </p>
   * 
   * <h5>Structured Input Data</h5>
   * 
   * <p>For structured input data business applications the request parameters are specified
   * in the HTTP POST form.</p>
   * 
   * <h5>Opaque input data</h5>
   * 
   * <p>For opaque input data (e.g. JPEG image, ESRI Shapefile) the requests can be
   * specified as one inputData files or one inputDataUrl
   * parameter. It is not possible to mix inputData and inputDataUrl parameters
   * in the same cloud job.</p>
   * 
   * <p class="note">NOTE: The maximum size including all parameters and protocol overhead of a
   * multi-part request is 20MB. Therefore inputDataUrl should be used instead
   * of inputData where possible for opaque data.</p>
   * 
   * 
   * @param businessApplicationName The name of the business application.
   * @param inputDataContentType The MIME type of the input data specified by
   * an inputData or inputDataUrl parameter.
   * @param inputDataFile The multi-part file containing the input data.
   * @param inputDataUrl The http: URL to the file or resource containing input
   * data.
   * @param srid The coordinate system code of the projection for the input geometry.
   * @param resultDataContentType The MIME type of the result data specified to
   * be returned after running the request.
   * @param resultSrid The coordinate system code of the projection for the
   * result geometry.
   * @param resultNumAxis The number of coordinate axis in the result geometry
   * (e.g. 2 for 2D or 3 for 3D).
   * @param resultScaleFactorXy The scale factor to apply the x, y coordinates.
   * The scale factor is 1 / minimum unit. For example if the minimum unit was
   * 1mm (0.001) the scale factor is 1000 (1 / 0.001).
   * @param resultScaleFactorZ The scale factor to apply the z coordinate. The
   * scale factor is 1 / minimum unit. For example if the minimum unit was 1mm
   * (0.001) the scale factor is 1000 (1 / 0.001).
   * @param notificationUrl The http: URL to be notified when the job is
   * completed. A copy of the Job status will be posted to process running at
   * this URL.
   * @param notificationEmail The email address to send the job status to when
   * the job is completed.
   * 
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/single"
  }, method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createJobWithSingleRequest(
    @PathVariable final String businessApplicationName,
    @RequestParam(required = false) String inputDataContentType,
    @RequestParam(value = "inputData", required = false) final Object inputDataFile,
    @RequestParam(required = false) final String inputDataUrl,
    @RequestParam(required = false) final String srid,
    @RequestParam(required = false) String resultDataContentType,
    @RequestParam(required = false, defaultValue = "3005") final int resultSrid,
    @RequestParam(required = false, defaultValue = "2") final int resultNumAxis,
    @RequestParam(required = false, defaultValue = "-1") final int resultScaleFactorXy,
    @RequestParam(required = false, defaultValue = "-1") final int resultScaleFactorZ,
    @RequestParam(required = false) String notificationUrl, @RequestParam(
        required = false) final String notificationEmail) throws IOException {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, "clientSingle");
    if (businessApplication != null) {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplicationName);

      final DataObject batchJob = createBatchJob();
      final Long batchJobId = batchJob.getValue(BatchJob.BATCH_JOB_ID);

      final AppLog log = businessApplication.getLog();
      log.info("Start\tJob submit single\tbatchJobId=" + batchJobId);
      final String consumerKey = getConsumerKey();

      final String defaultInputDataContentType = businessApplication.getDefaultInputDataContentType();
      final String defaultResultDataContentType = businessApplication.getDefaultResultDataContentType();

      final Map<String, String> businessApplicationParameters = new HashMap<String, String>();
      addTestParameters(businessApplication, businessApplicationParameters);

      if (!StringUtils.hasText(inputDataContentType)) {
        inputDataContentType = defaultInputDataContentType;
      }
      final boolean perRequestInputData = businessApplication.isPerRequestInputData();
      org.springframework.core.io.Resource inputDataIn = null;
      if (perRequestInputData) {
        if (!businessApplication.isInputContentTypeSupported(inputDataContentType)
          && !businessApplication.isInputContentTypeSupported("*/*")) {
          throw new HttpMessageNotReadableException("inputDataContentType="
            + inputDataContentType + " is not supported.");
        }
        inputDataIn = getResource("inputData");
        final boolean hasInputDataIn = inputDataIn != null;
        final boolean hasInputDataUrl = StringUtils.hasText(inputDataUrl);
        if (hasInputDataIn == hasInputDataUrl) {
          throw new HttpMessageNotReadableException(
            "Either an inputData file or inputDataUrl parameter must be specified, but not both");
        }
      }
      if (!StringUtils.hasText(resultDataContentType)) {
        resultDataContentType = defaultResultDataContentType;
      }
      final String resultContentType = businessApplication.getResultContentType(resultDataContentType);
      if (resultContentType == null) {
        throw new HttpMessageNotReadableException("resultDataContentType="
          + resultDataContentType + " is not supported.");
      } else {
        resultDataContentType = resultContentType;
      }
      if (StringUtils.hasText(notificationEmail)) {
        if (StringUtils.hasText(notificationUrl)) {
          throw new HttpMessageNotReadableException(
            "Both notificationUrl and notificationEmail cannot be specified. Enter a value in one or the other but not both.");
        } else {
          notificationUrl = "mailto:" + notificationEmail;
        }
      }
      final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
      final DataObject inputData = new ArrayDataObject(requestMetaData);
      for (final Attribute attribute : requestMetaData.getAttributes()) {
        final String parameterName = attribute.getName();
        String value = HttpServletUtils.getParameter(parameterName);
        final boolean required = attribute.isRequired();
        boolean hasValue = value != null && value.trim().length() > 0;
        if (attribute.getType() == DataTypes.BOOLEAN) {
          if ("on".equals(value)) {
            value = "true";
          } else {
            value = "false";
          }
          hasValue = true;
        }
        if (hasValue) {
          if (businessApplication.isJobParameter(parameterName)) {
            businessApplicationParameters.put(parameterName, value);
          } else {
            try {
              batchJobService.setStructuredInputDataValue(srid, inputData,
                attribute, value, true);
            } catch (final IllegalArgumentException e) {
              throw new HttpMessageNotReadableException("Parameter "
                + parameterName + " cannot be set");
            }
          }
        } else if (required) {
          throw new HttpMessageNotReadableException("Parameter "
            + parameterName + " is required");
        }

      }
      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_NAME,
        businessApplicationName);
      batchJob.setValue(BatchJob.USER_ID, consumerKey);
      batchJob.setValue(BatchJob.NUM_SUBMITTED_GROUPS, 1);

      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_PARAMS,
        JsonMapIoFactory.toString(businessApplicationParameters));

      if (StringUtils.hasText(notificationUrl)) {
        batchJob.setValue(BatchJob.NOTIFICATION_URL, notificationUrl);
      }
      batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE,
        resultDataContentType);
      batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS, 1);
      batchJob.setValue(BatchJob.NUM_SCHEDULED_GROUPS, 0);
      batchJob.setValue(BatchJob.NUM_COMPLETED_REQUESTS, 0);
      batchJob.setValue(BatchJob.NUM_FAILED_REQUESTS, 0);
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      batchJob.setValue(BatchJob.LAST_SCHEDULED_TIMESTAMP, now);
      batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.REQUESTS_CREATED);
      batchJob.setValue(BatchJob.WHEN_STATUS_CHANGED, now);
      dataAccessObject.write(batchJob);
      if (perRequestInputData) {
        if (inputDataIn != null) {
          dataAccessObject.createBatchJobExecutionGroup(batchJobId, 1,
            inputDataContentType, inputDataIn);
        } else {
          dataAccessObject.createBatchJobExecutionGroup(batchJobId, 1,
            inputDataContentType, inputDataUrl);
        }
      } else {

        inputData.put("requestSequenceNumber", 1);
        final String inputDataString = JsonDataObjectIoFactory.toString(
          requestMetaData, Collections.singletonList(inputData));
        dataAccessObject.createBatchJobExecutionGroup(batchJobId, 1,
          inputDataString, 1);
      }

      batchJobService.schedule(businessApplicationName, batchJobId);

      HttpServletUtils.setPathVariable("batchJobId", batchJobId);

      AppLogUtil.infoAfterCommit(log, "End\tJob submit single\tbatchJobId="
        + batchJobId, stopWatch);
      final Map<String, Object> statistics = new HashMap<String, Object>();
      statistics.put("submittedJobsTime", stopWatch);
      statistics.put("submittedJobsCount", 1);

      statistics.put("preProcessScheduledJobsCount", 1);
      statistics.put("preProcessScheduledJobsTime", 0);

      statistics.put("preProcessedTime", 0);
      statistics.put("preProcessedJobsCount", 1);
      statistics.put("preProcessedRequestsCount", 1);

      InvokeMethodAfterCommit.invoke(batchJobService, "addStatistics",
        businessApplication, statistics);

      batchJobUiBuilder.redirectPage("clientView");
    }
  }

  private void createStructuredJob(final Long batchJobId,
    final DataObject batchJob, final List<MultipartFile> inputDataFiles,
    final List<String> inputDataUrls) throws IOException {
    if (!inputDataFiles.isEmpty()) {
      if (inputDataFiles.size() == 1) {
        final MultipartFile file = inputDataFiles.get(0);
        final InputStream in = file.getInputStream();
        try {
          final org.springframework.core.io.Resource resource = new InputStreamResource(
            "in", in, file.getSize());
          batchJob.setValue(BatchJob.STRUCTURED_INPUT_DATA, resource);
        } finally {
          InvokeMethodAfterCommit.invoke(in, "close");
        }

      } else {
        throw new HttpMessageNotReadableException(
          "inputData can only be specified once");
      }
    } else {
      if (inputDataUrls.size() == 1) {
        final String inputDataUrl = inputDataUrls.get(0);
        batchJob.setValue(BatchJob.STRUCTURED_INPUT_DATA_URL,
          inputDataUrl.trim());
      } else {
        throw new HttpMessageNotReadableException(
          "inputDataUrl must only be specified onces");
      }
    }
    dataAccessObject.write(batchJob);
    batchJobService.preProcess(batchJobId);
  }

  /**
   * <p>
   * Cancel the user's cloud job. This will mark the job as cancelled and remove all requests
   * and results from the job. The job will be removed after a few days.
   * </p>
   * <p>
   * This service should be invoked after the results from the cloud job are
   * downloaded. If this method is not called the cloud job will automatically
   * be deleted 7 days after the result download was started.
   * </p>
   * <p>
   * This method can also be used to cancel a cloud job before it was finished.
   * If a cloud job was submitted in error or no longer required use this method
   * to cancel the cloud job to help free resources on the system to process
   * other cloud jobs.
   * </p>
   * 
   * @param batchJobId The cloud job identifier.
   * @web.response.status 200
   * <p>
   * <b>OK</b>
   * </p>
   * <p>
   * If the cloud job was deleted an empty response will be returned.
   * </p>
   * @web.response.status 404
   * <p>
   * <b>Not Found</b>
   * </p>
   * <p>
   * If the cloud job does not exist, has been deleted, or was owned by another
   * user.
   * </p>
   */
  @RequestMapping(value = {
    "/ws/jobs/{batchJobId}"
  }, method = RequestMethod.DELETE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteJob(@PathVariable final long batchJobId) {
    final String consumerKey = getConsumerKey();
    final DataObject batchJob = dataAccessObject.getBatchJob(consumerKey,
      batchJobId);
    if (batchJob == null) {
      throw new PageNotFoundException("The cloud job " + batchJobId
        + " does not exist");
    } else {
      if (batchJobService.cancelBatchJob(batchJobId)) {
        throw new PageNotFoundException("The cloud job " + batchJobId
          + " does not exist");
      } else {
        final HttpServletResponse response = HttpServletUtils.getResponse();
        response.setStatus(HttpServletResponse.SC_OK);
      }
    }
  }

  /**
   * <p>Check that a user is authenticated. This can be used by JavaScript applications to force
   * a login page to be displayed. Returns a map with authenticated=true if the user
   * is authenticated. Response is undefined if the user is not authenticated as the authentication
   * mechanism will not allow access to the resource.</p>
   * 
   * @return The authenticated message.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/authenticated"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Map<String, ? extends Object> getAuthenticated() {
    final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
      "Authenticated");
    map.put("authenticated", true);
    return map;
  }

  private List<BusinessApplication> getAuthorizedBusinessApplications() {
    final EvaluationContext evaluationContext = getSecurityEvaluationContext();

    final List<BusinessApplication> businessApplications = batchJobService.getBusinessApplications();
    for (final Iterator<BusinessApplication> iterator = businessApplications.iterator(); iterator.hasNext();) {
      final BusinessApplication businessApplication = iterator.next();
      if (!hasPermission(businessApplication, evaluationContext)) {
        iterator.remove();
      }
    }
    return businessApplications;
  }

  private BusinessApplication getBusinessApplication(
    final String businessApplicationName, final String pageName) {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new PageNotFoundException("Business application "
        + businessApplicationName + " does not exist.");
    } else {
      checkPermission(businessApplication);
      return businessApplication;
    }
  }

  private PageInfo getBusinessApplicationPageInfo(
    final BusinessApplication businessApplication, final String pageName) {
    final String key = pageName + "PageInfo";
    PageInfo page = businessApplication.getProperty(key);
    if (page == null) {
      final String title = businessApplication.getTitle();
      if ("instant".equals(pageName)) {
        page = new PageInfo(title + " Instant", "", "get", "post");
      } else if ("single".equals(pageName)) {
        page = new PageInfo(title + " Create Single Request Job", "", "post");
      } else if ("multiple".equals(pageName)) {
        page = new PageInfo(title + " Create Multi-Request Job", "", "post");
      }

      page.setDescription(businessApplication.getDescription());
      page.addInputContentType(MediaType.MULTIPART_FORM_DATA);

      page.setAttribute("businessApplicationName",
        businessApplication.getName());
      page.setAttribute("businessApplicationTitle", title);
      page.setAttribute("businessApplicationDescription",
        businessApplication.getDescription());
      page.setAttribute("businessApplicationDescriptionUrl",
        businessApplication.getDescriptionUrl());

      final Set<String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
      page.setAttribute("inputDataContentTypes", inputDataContentTypes);

      page.setAttribute("perRequestInputData",
        businessApplication.isPerRequestInputData());

      page.setAttribute("parameters",
        getRequestAttributeList(businessApplication));

      final Set<String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      page.setAttribute("resultDataContentTypes", resultDataContentTypes);
      page.setAttribute("perRequestResultData",
        businessApplication.isPerRequestInputData());

      if (!businessApplication.isPerRequestResultData()) {
        page.setAttribute("resultFields",
          getResultAttributeList(businessApplication));
      }
      businessApplication.setProperty(key, page);
    }
    return page;
  }

  /**
   * <p>Get the list of links to the
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsResources">Get Business Applications Resources</a>
   * resource for each business application the user is authorized to access.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document. Each child resource supports following custom attributes.</a>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>businessApplicationName</td>
   *         <td>The name of the business application.</td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   * 
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/apps"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getBusinessApplications() {
    final List<BusinessApplication> applications = getAuthorizedBusinessApplications();
    HttpServletUtils.setAttribute("title", "Business Applications");
    if (HtmlUiBuilder.isDataTableCallback()) {
      return businessAppBuilder.createDataTableMap(applications, "clientList");
    } else if (MediaTypeUtil.isHtmlPage()) {
      final String url = HttpServletUtils.getFullUrl("/ws/#businessApplication_clientList");
      final HttpServletResponse response = HttpServletUtils.getResponse();
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final PageInfo page = createRootPageInfo("Business Appliations");

      for (final BusinessApplication app : applications) {
        if (app.isEnabled()) {
          final String name = app.getName();
          final String title = app.getTitle();
          final PageInfo appPage = addPage(page, name, title);
          appPage.setAttribute("businessApplicationName", name);
        }
      }
      return page;
    }
  }

  /**
   * <p>The instant resource has the two modes described below, the specification mode and the
   * execute instant request mode. If the <code>format</code> parameter is not included or the
   * <code>specification</code> parameter equals <code>true</code> then the specification mode
   * is enabled, otherwise the execute instant request mode is enabled.</p>
   *  
   * <h5>Specification</h5>
   * 
   * <p>Get the specification of the execute instant request mode of this service.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document with the following additional fields
   * which are the parameters to the execute instant request mode.</a>.</p>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>description</td>
   *         <td>The description of services offered by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationName</td>
   *         <td>The name of the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationTitle</td>
   *         <td>The display title of the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationDescription</td>
   *         <td>The description of services offered by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationDescriptionUrl</td>
   *         <td>A link to a web page describing more details about the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>parameters</td>
   *         <td><p>The array of parameters that can be passed to the service. Each parameter is an object
   *         containing the following fields.</p>
   *           <div class="simpleDataTable">
   *             <table class="data">
   *               <thead>
   *                 <tr>
   *                   <th>Attribute</th>
   *                   <th>Description</th>
   *                 </tr>
   *               </thead>
   *               <tbody>
   *                 <tr>
   *                   <td>name</td>
   *                   <td>The case sensitive name of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>type</td>
   *                   <td>The <a href="../../dataTypes.html">data type</a> of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>description</td>
   *                   <td>The description of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>descriptionUrl</td>
   *                   <td>The link to a more detailed description of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>jobParameter</td>
   *                   <td>Boolean flag indicating if the parameter can be specified globally on the job.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>requestParameter</td>
   *                   <td>Boolean flag indicating if the parameter can be specified on each request in the job.</td>
   *                 </tr>
   *               </tbody>
   *             </table>
   *           </div>
   *         </td>
   *       </tr>
   *       <tr>
   *         <td>inputDataContentTypes</td>
   *         <td>The array of MIME media types of input data accepted by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>perRequestInputData</td>
   *         <td>Boolean flag indicating that the business application accepts <a href="../../opaqueData.html">opaque data</a> (true) or <a href="../../structuredData.html">structured data</a> (false).</td>
   *       </tr>
   *       <tr>
   *         <td>resultDataContentTypes</td>
   *         <td>The array of MIME media types of result data generated by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>perRequestResultData</td>
   *         <td>Boolean flag indicating that the business application returns <a href="../../opaqueData.html">opaque data</a> (true) or <a href="../../structuredData.html">structured data</a> (false).</td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   *  
   * <h5>Execute Instant Request</h5>
   * 
   * <p>Execute a single instant request using the business application with the results
   * returned in the request.</p>
   * 
   * <p>The job and request parameters for the business application must be passed
   * using the query string parameters in a HTTP get request or
   * application/form-url-encoded, multipart/form-data encoding in the body of a HTTP POST request (e.g. a HTML form).</p>
   * 
   * <p>In addition to the standard parameters listed in the API each business
   * application has additional job and request parameters. Invoke the specification mode of this
   * resource should be consulted to get the full list of supported parameters. </p>

   * <p class="note">NOTE: The instant resource does not support opaque input data.</p>
   * 
   * @param businessApplicationName The name of the business application.
   * @param srid The coordinate system code of the projection for the input geometry.
   * @param format The MIME type of the result data specified to
   * be returned after running the request.
   * @param resultSrid The coordinate system code of the projection for the
   * result geometry.
   * @param resultNumAxis The number of coordinate axis in the result geometry
   * (e.g. 2 for 2D or 3 for 3D).
   * @param resultScaleFactorXy The scale factor to apply the x, y coordinates.
   * The scale factor is 1 / minimum unit. For example if the minimum unit was
   * 1mm (0.001) the scale factor is 1000 (1 / 0.001).
   * @param resultScaleFactorZ The scale factor to apply the z coordinate. The
   * scale factor is 1 / minimum unit. For example if the minimum unit was 1mm
   * (0.001) the scale factor is 1000 (1 / 0.001).
   * @return The result.
   * 
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   * @web.response.status 403 <p>If the user does not have permission for this resource on the business application.</p>
   * @web.response.status 404 <p>If the business application does not exist, or is not enabled.</p>
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/instant",
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Object getBusinessApplicationsInstant(
    @PathVariable final String businessApplicationName,
    @RequestParam(defaultValue = "false") final boolean specification,
    @RequestParam(required = false) final String srid,
    @RequestParam final String format,
    @RequestParam(required = false, defaultValue = "3005") final int resultSrid,
    @RequestParam(required = false, defaultValue = "2") final int resultNumAxis,
    @RequestParam(required = false, defaultValue = "-1") final int resultScaleFactorXy,
    @RequestParam(required = false, defaultValue = "-1") final int resultScaleFactorZ) {
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, "clientInstant");
    if (businessApplication == null) {
      return null;
    } else {
      final PluginAdaptor plugin = batchJobService.getBusinessApplicationPlugin(businessApplication);
      checkPermission(businessApplication.getInstantModeExpression(),
        "No instant mode permission for " + businessApplication.getName());
      if (format == null || specification) {
        if (MediaTypeUtil.isHtmlPage()) {
          final Map<String, Object> titleParameters = new HashMap<String, Object>();
          final String title = businessApplication.getTitle();
          titleParameters.put("businessApplicationTitle", title);
          final PageInfo page = createRootPageInfo(title + " Instant");
          setBusinessApplicationDescription(page, businessApplication);
          page.setPagesElement(getFormInstant(businessApplication));

          HttpServletUtils.setAttribute("title", page.getTitle());
          return page;
        } else {
          return getBusinessApplicationPageInfo(businessApplication, "instant");
        }
      } else {
        final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
        final DataObject requestParameters = new ArrayDataObject(
          requestMetaData);
        for (final Attribute attribute : requestMetaData.getAttributes()) {
          final String name = attribute.getName();
          String value = HttpServletUtils.getParameter(name);
          boolean hasValue = StringUtils.hasText(value);
          if (attribute.getType() == DataTypes.BOOLEAN) {
            if ("on".equals(value)) {
              value = "true";
            } else {
              value = "false";
            }
            hasValue = true;
          }
          if (hasValue) {
            if (value == null) {
              if (attribute.isRequired()) {
                throw new IllegalArgumentException("Parameter is required "
                  + name);
              }
            } else if (!businessApplication.isRequestAttributeValid(name, value)) {
              throw new IllegalArgumentException(
                "Parameter value is not valid " + name + " " + value);
            } else {
              try {
                batchJobService.setStructuredInputDataValue(srid,
                  requestParameters, attribute, value, true);
              } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(
                  "Parameter value is not valid " + name + " " + value, e);
              }
            }
          }
        }
        final Map<String, Object> parameters = new LinkedHashMap<>(
          requestParameters);
        addTestParameters(businessApplication, parameters);
        plugin.setParameters(parameters);
        plugin.execute();
        final List<Map<String, Object>> list = plugin.getResults();
        HttpServletUtils.setAttribute("contentDispositionFileName",
          businessApplicationName);
        final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
        for (final Entry<String, Object> entry : businessApplication.getProperties()
          .entrySet()) {
          final String name = entry.getKey();
          final Object value = entry.getValue();
          HttpServletUtils.setAttribute(name, value);
        }

        try {
          final HttpServletResponse response = HttpServletUtils.getResponse();

          final DataObjectWriterFactory writerFactory = IoFactoryRegistry.getInstance()
            .getFactoryByMediaType(DataObjectWriterFactory.class, format);
          if (writerFactory == null) {
            throw new HttpMessageNotWritableException("Unsupported format "
              + format);
          } else {
            final String contentType = writerFactory.getMediaType(format);
            response.setContentType(contentType);
            final String fileExtension = writerFactory.getFileExtension(format);
            final String fileName = businessApplicationName + "-instant."
              + fileExtension;
            response.setHeader("Content-Disposition", "attachment; filename="
              + fileName);
          }

          final OutputStreamResource resource = new OutputStreamResource(
            "result", response.getOutputStream());
          final GeometryFactory geometryFactory = GeometryFactory.getFactory(
            resultSrid, resultNumAxis, resultScaleFactorXy, resultScaleFactorZ);
          final Writer<DataObject> writer = batchJobService.createStructuredResultWriter(
            resource, businessApplication, resultMetaData, format, "Result",
            geometryFactory);
          final boolean hasMultipleResults = businessApplication.getResultListProperty() != null;
          if (!hasMultipleResults) {
            writer.setProperty(IoConstants.SINGLE_OBJECT_PROPERTY, true);
          }
          int i = 1;
          final Map<String, Object> defaultProperties = new HashMap<String, Object>(
            writer.getProperties());

          for (final Map<String, Object> structuredResultMap : list) {
            final DataObject structuredResult = DataObjectUtil.getObject(
              resultMetaData, structuredResultMap);

            @SuppressWarnings("unchecked")
            final Map<String, Object> properties = (Map<String, Object>)structuredResultMap.get("customizationProperties");
            if (properties != null && !properties.isEmpty()) {
              writer.setProperties(properties);
            }

            structuredResult.put("sequenceNumber", 1);
            structuredResult.put("resultNumber", i);
            writer.write(structuredResult);
            if (properties != null && !properties.isEmpty()) {
              writer.clearProperties();
              writer.setProperties(defaultProperties);
            }
            i++;

          }
          writer.close();
          return null;
        } catch (final IOException e) {
          return ExceptionUtil.throwUncheckedException(e);
        }
      }
    }
  }

  /**
   * <p>Get the list of links to the
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersJobsInfo">Get Users Jobs Info</a>
   * resource for each of the user's jobs for the business application.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document. Each child resource supports following custom attributes.</a>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>batchJobId</td>
   *         <td>The unique identifier of the cloud job.</td>
   *       </tr>
   *       <tr>
   *         <td>batchJobUrl</td>
   *         <td>The URL to the <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersJobsInfo">Get Users Jobs Info</a> resource without the file format extension.</td>
   *       </tr>
   *       <tr>
   *         <td>jobStatus</td>
   *         <td>The current status of the job.</td>
   *       </tr>
   *       <tr>
   *         <td>creationTimestamp</td>
   *         <td>The time when the job was created.</td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   * 
   * @param businessApplicationName The name of the business application.
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getBusinessApplicationsJobs(
    @PathVariable final String businessApplicationName) {
    final BusinessApplication businessApplication = batchJobUiBuilder.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new PageNotFoundException("Business application "
        + businessApplicationName + " does not exist.");
    } else {
      final String consumerKey = getConsumerKey();
      CloudProcessingFramework.checkPermission(businessApplication);
      if (HtmlUiBuilder.isDataTableCallback()) {
        final Map<String, Object> parameters = new HashMap<String, Object>();

        final Map<String, Object> filter = new HashMap<String, Object>();
        filter.put(BatchJob.USER_ID, consumerKey);
        filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
        parameters.put("filter", filter);

        return batchJobUiBuilder.createDataTableMap("clientAppList", parameters);
      } else if (MediaTypeUtil.isHtmlPage()) {
        batchJobUiBuilder.redirectToTab(BusinessApplication.class,
          "clientView", "clientAppList");
        return null;
      } else {
        final String title = businessApplication.getTitle();
        final PageInfo page = createRootPageInfo(title + " Batch Jobs");
        final List<DataObject> batchJobs = dataAccessObject.getBatchJobsForUserAndApplication(
          consumerKey, businessApplicationName);
        for (final DataObject job : batchJobs) {
          addBatchJobStatusLink(page, job);
        }
        final Object table = batchJobUiBuilder.createDataTableHandler(
          "clientList", batchJobs);
        if (table instanceof Element) {
          final Element element = (Element)table;
          page.setPagesElement(element);
        } else {
          return table;
        }
        return page;
      }
    }
  }

  /**
   * <p>Get the specification of the 
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.createJobWithMultipleRequests">Create Job With Multiple Requests</a> service.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document with the following additional fields
   * which are the parameters to the
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.createJobWithMultipleRequests">Create Job With Multiple Requests</a>
   * service.</a>.</p>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>description</td>
   *         <td>The description of services offered by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationName</td>
   *         <td>The name of the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationTitle</td>
   *         <td>The display title of the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationDescription</td>
   *         <td>The description of services offered by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationDescriptionUrl</td>
   *         <td>A link to a web page describing more details about the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>parameters</td>
   *         <td><p>The array of parameters that can be passed to the service. Each parameter is an object
   *         containing the following fields.</p>
   *           <div class="simpleDataTable">
   *             <table class="data">
   *               <thead>
   *                 <tr>
   *                   <th>Attribute</th>
   *                   <th>Description</th>
   *                 </tr>
   *               </thead>
   *               <tbody>
   *                 <tr>
   *                   <td>name</td>
   *                   <td>The case sensitive name of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>type</td>
   *                   <td>The <a href="../../dataTypes.html">data type</a> of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>description</td>
   *                   <td>The description of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>descriptionUrl</td>
   *                   <td>The link to a more detailed description of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>jobParameter</td>
   *                   <td>Boolean flag indicating if the parameter can be specified globally on the job.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>requestParameter</td>
   *                   <td>Boolean flag indicating if the parameter can be specified on each request in the job.</td>
   *                 </tr>
   *               </tbody>
   *             </table>
   *           </div>
   *         </td>
   *       </tr>
   *       <tr>
   *         <td>inputDataContentTypes</td>
   *         <td>The array of MIME media types of input data accepted by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>perRequestInputData</td>
   *         <td>Boolean flag indicating that the business application accepts <a href="../../opaqueData.html">opaque data</a> (true) or <a href="../../structuredData.html">structured data</a> (false).</td>
   *       </tr>
   *       <tr>
   *         <td>resultDataContentTypes</td>
   *         <td>The array of MIME media types of result data generated by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>perRequestResultData</td>
   *         <td>Boolean flag indicating that the business application returns <a href="../../opaqueData.html">opaque data</a> (true) or <a href="../../structuredData.html">structured data</a> (false).</td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   * 
   * @param businessApplicationName The name of the business application.
   * @return The resource.
   * 
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   * @web.response.status 403 <p>If the user does not have permission for this resource on the business application.</p>
   * @web.response.status 404 <p>If the business application does not exist, or is not enabled.</p>
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/multiple"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public PageInfo getBusinessApplicationsMultiple(
    @PathVariable final String businessApplicationName) {
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, "clientMultiple");
    if (businessApplication == null) {
      return null;
    } else {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Map<String, Object> titleParameters = new HashMap<String, Object>();

      if (MediaTypeUtil.isHtmlPage()) {
        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);
        final PageInfo page = createRootPageInfo(title
          + "Create Multi-Request job");
        setBusinessApplicationDescription(page, businessApplication);
        page.addInputContentType(MediaType.MULTIPART_FORM_DATA);

        page.setPagesElement(getFormMultiple(businessApplication));

        HttpServletUtils.setAttribute("title", page.getTitle());
        return page;
      } else {
        return getBusinessApplicationPageInfo(businessApplication, "multiple");
      }
    }
  }

  /**
   * <p>Get the resources for a business application. The resource contains links to the
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsInstant">instant</a>,
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsSingle">create single request job</a>, and
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsMultiple">create multi request jobs</a>
   * resources.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document.</p>
   * 
   * @param businessApplicationName The name of the business application.
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   * @web.response.status 403 <p>If the user does not have permission for this resource on the business application.</p>
   * @web.response.status 404 <p>If the business application does not exist, or is not enabled.</p>
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getBusinessApplicationsResources(
    @PathVariable final String businessApplicationName) {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new PageNotFoundException("Business application "
        + businessApplicationName + " does not exist.");
    } else {
      checkPermission(businessApplication);
      final Map<String, Object> titleParameters = new HashMap<String, Object>();
      final String title = businessApplication.getTitle();
      titleParameters.put("businessApplicationTitle", title);
      HttpServletUtils.setAttribute("title", title);

      if (MediaTypeUtil.isHtmlPage()) {
        HttpServletUtils.setAttribute("pageHeading", title);
        final ElementContainer container = new ElementContainer();
        final String description = businessApplication.getDescription();
        if (StringUtils.hasText(description)) {
          container.add(new RawContent("<p>" + description + "</p>"));
        }
        final String descriptionUrl = businessApplication.getDescriptionUrl();
        if (StringUtils.hasText(descriptionUrl)) {
          container.add(new RawContent("<p>Click <a href=\"" + descriptionUrl
            + "\">here</a> for a more detailed description of the service.</p>"));
        }

        final TabElementContainer tabs = new TabElementContainer();
        container.add(tabs);
        final Element specification = getElementSpecification(businessApplication);
        tabs.add("description", "Overview", specification);

        if (hasPermission(businessApplication.getInstantModeExpression())) {
          final Element instantForm = getFormInstant(businessApplication);
          tabs.add("instant", "Instant", instantForm);
        }
        if (hasPermission(businessApplication.getBatchModeExpression())) {
          final Element singleForm = getFormSingle(businessApplication);
          tabs.add("single", "Create Single Request Job", singleForm);

          final Element multipleForm = getFormMultiple(businessApplication);
          tabs.add("multiple", "Create Multi-Request Job", multipleForm);

          final Map<String, Object> parameters = new HashMap<String, Object>();
          parameters.put("serverSide", Boolean.TRUE);
          batchJobUiBuilder.addTabDataTable(tabs, BatchJob.BATCH_JOB,
            "clientAppList", parameters);
        }
        return container;
      } else {
        final PageInfo page = createRootPageInfo(title);

        if (hasPermission(businessApplication.getInstantModeExpression())) {
          addPage(page, "instant", "Instant");
        }
        if (hasPermission(businessApplication.getBatchModeExpression())) {

          addPage(page, "single", "Create Single Request Job", "post");
          addPage(page, "multiple", "Create Multi-Request Job", "post");
        }
        return page;
      }
    }
  }

  /**
   * <p>Get the specification of the 
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.createJobWithSingleRequest">Create Job With Single Request</a> service.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document with the following additional fields
   * which are the parameters to the <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.createJobWithSingleRequest">Create Job With Single Request</a> service.</a>.</p>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>description</td>
   *         <td>The description of services offered by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationName</td>
   *         <td>The name of the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationTitle</td>
   *         <td>The display title of the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationDescription</td>
   *         <td>The description of services offered by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationDescriptionUrl</td>
   *         <td>A link to a web page describing more details about the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>parameters</td>
   *         <td><p>The array of parameters that can be passed to the service. Each parameter is an object
   *         containing the following fields.</p>
   *           <div class="simpleDataTable">
   *             <table class="data">
   *               <thead>
   *                 <tr>
   *                   <th>Attribute</th>
   *                   <th>Description</th>
   *                 </tr>
   *               </thead>
   *               <tbody>
   *                 <tr>
   *                   <td>name</td>
   *                   <td>The case sensitive name of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>type</td>
   *                   <td>The <a href="../../dataTypes.html">data type</a> of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>description</td>
   *                   <td>The description of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>descriptionUrl</td>
   *                   <td>The link to a more detailed description of the parameter.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>jobParameter</td>
   *                   <td>Boolean flag indicating if the parameter can be specified globally on the job.</td>
   *                 </tr>
   *                 <tr>
   *                   <td>requestParameter</td>
   *                   <td>Boolean flag indicating if the parameter can be specified on each request in the job.</td>
   *                 </tr>
   *               </tbody>
   *             </table>
   *           </div>
   *         </td>
   *       </tr>
   *       <tr>
   *         <td>inputDataContentTypes</td>
   *         <td>The array of MIME media types of input data accepted by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>perRequestInputData</td>
   *         <td>Boolean flag indicating that the business application accepts <a href="../../opaqueData.html">opaque data</a> (true) or <a href="../../structuredData.html">structured data</a> (false).</td>
   *       </tr>
   *       <tr>
   *         <td>resultDataContentTypes</td>
   *         <td>The array of MIME media types of result data generated by the business application.</td>
   *       </tr>
   *       <tr>
   *         <td>perRequestResultData</td>
   *         <td>Boolean flag indicating that the business application returns <a href="../../opaqueData.html">opaque data</a> (true) or <a href="../../structuredData.html">structured data</a> (false).</td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   * 
   * @param businessApplicationName The name of the business application.
   * @return The resource.
   * 
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   * @web.response.status 403 <p>If the user does not have permission for this resource on the business application.</p>
   * @web.response.status 404 <p>If the business application does not exist, or is not enabled.</p>
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/single",
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public PageInfo getBusinessApplicationsSingle(
    @PathVariable final String businessApplicationName) {
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, "clientSingle");
    if (businessApplication == null) {
      return null;
    } else {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Map<String, Object> titleParameters = new HashMap<String, Object>();

      if (MediaTypeUtil.isHtmlPage()) {
        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);

        final PageInfo page = createRootPageInfo(title
          + "Create Single Request job");
        setBusinessApplicationDescription(page, businessApplication);

        page.setPagesElement(getFormSingle(businessApplication));
        HttpServletUtils.setAttribute("title", page.getTitle());
        return page;
      } else {
        return getBusinessApplicationPageInfo(businessApplication, "single");
      }
    }
  }

  private String getConsumerKey() {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    final String consumerKey = authentication.getName();
    return consumerKey;
  }

  private Element getElementSpecification(
    final BusinessApplication businessApplication) {
    ElementContainer container = businessApplication.getProperty("specificationElement");
    if (container == null) {
      container = new ElementContainer();
      container.add(new XmlTagElement(HtmlUtil.H1,
        businessApplication.getTitle() + " (" + businessApplication.getName()
          + ")"));

      final String description = businessApplication.getDescription();
      if (StringUtils.hasText(description)) {
        container.add(new RawContent("<p>" + description + "</p>"));
      }
      final String descriptionUrl = businessApplication.getDescriptionUrl();
      if (StringUtils.hasText(descriptionUrl)) {
        container.add(new RawContent("<p>Click <a href=\"" + descriptionUrl
          + "\">here</a> for a more detailed description of the service.</p>"));
      }

      container.add(new RawContent(new ClassPathResource(
        "ca/bc/gov/open/cpf/api/web/service/services.html")));
      if (hasPermission(businessApplication.getInstantModeExpression())) {
        container.add(new RawContent(new ClassPathResource(
          "ca/bc/gov/open/cpf/api/web/service/instantMode.html")));
      }
      if (hasPermission(businessApplication.getBatchModeExpression())) {
        container.add(new RawContent(new ClassPathResource(
          "ca/bc/gov/open/cpf/api/web/service/batchMode.html")));
      }

      addRawContent(container,
        "ca/bc/gov/open/cpf/api/web/service/inputData.html");
      final Set<String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
      container.add(new ListElement(HtmlUtil.UL, HtmlUtil.LI,
        inputDataContentTypes));

      if (businessApplication.isPerRequestInputData()) {
        addRawContent(container,
          "ca/bc/gov/open/cpf/api/web/service/opaqueInputData.html");
      } else {
        addRawContent(container,
          "ca/bc/gov/open/cpf/api/web/service/structuredInputData.html");
      }
      final DataObjectMetaData requestMetaData = businessApplication.getRequestMetaData();
      final List<Attribute> requestAttributes = requestMetaData.getAttributes();
      final List<KeySerializer> serializers = new ArrayList<KeySerializer>();
      serializers.add(new StringKeySerializer("name"));
      serializers.add(new BooleanImageKeySerializer("properties."
        + BusinessApplication.JOB_PARAMETER, "Job Parameter"));
      if (!businessApplication.isPerRequestInputData()) {
        serializers.add(new BooleanImageKeySerializer("properties."
          + BusinessApplication.REQUEST_PARAMETER, "Request Parameter"));
      }
      serializers.add(new StringKeySerializer("typeDescription", "Type"));
      serializers.add(new StringKeySerializer("description"));

      final Attribute inputDataContentType = new Attribute(
        "inputDataContentType",
        DataTypes.STRING,
        false,
        "The MIME type of the input data specified by an inputData or inputDataUrl parameter.");
      inputDataContentType.setProperty(BusinessApplication.JOB_PARAMETER, true);
      requestAttributes.add(0, inputDataContentType);

      final Attribute inputData = new Attribute("inputData",
        new SimpleDataType("File", File.class), false,
        "The multi-part file containing the input data.");
      inputData.setProperty(BusinessApplication.JOB_PARAMETER, true);
      requestAttributes.add(1, inputData);

      final Attribute inputDataUrl = new Attribute("inputDataUrl",
        DataTypes.STRING, false,
        "The http: URL to the file or resource containing input data.");
      inputDataUrl.setProperty(BusinessApplication.JOB_PARAMETER, true);
      requestAttributes.add(2, inputDataUrl);

      final Attribute notificationEmail = new Attribute("notificationEmail",
        DataTypes.STRING, false,
        "The email address to send the job status to when the job is completed.");
      notificationEmail.setProperty(BusinessApplication.JOB_PARAMETER, true);
      requestAttributes.add(notificationEmail);

      final Attribute notificationUrl = new Attribute(
        "notificationUrl",
        DataTypes.STRING,
        false,
        "The http: URL to be notified when the job is completed. A copy of the Job status will be posted to process running at this URL.");
      notificationUrl.setProperty(BusinessApplication.JOB_PARAMETER, true);
      requestAttributes.add(notificationUrl);

      final RowsTableSerializer requestModel = new KeySerializerTableSerializer(
        serializers, requestAttributes);
      final TableView requestAttributesTable = new TableView(requestModel,
        "objectList resultAttributes");
      container.add(requestAttributesTable);

      addRawContent(container,
        "ca/bc/gov/open/cpf/api/web/service/resultFiles.html");
      final Set<String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      container.add(new ListElement(HtmlUtil.UL, HtmlUtil.LI,
        resultDataContentTypes));
      if (businessApplication.isPerRequestResultData()) {
        addRawContent(container,
          "ca/bc/gov/open/cpf/api/web/service/opaqueResults.html");
      } else {
        container.add(new XmlTagElement(HtmlUtil.H2, "Result Fields"));
        final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
        final List<Attribute> resultAttributes = resultMetaData.getAttributes();
        final List<KeySerializer> resultSerializers = new ArrayList<KeySerializer>();
        resultSerializers.add(new StringKeySerializer("name"));
        resultSerializers.add(new StringKeySerializer("typeDescription", "Type"));
        resultSerializers.add(new StringKeySerializer("description"));
        final RowsTableSerializer resultModel = new KeySerializerTableSerializer(
          resultSerializers, resultAttributes);
        final TableView resultAttributesTable = new TableView(resultModel,
          "objectList resultAttributes");
        container.add(resultAttributesTable);
      }
      addRawContent(container,
        "ca/bc/gov/open/cpf/api/web/service/errorResults.html");

    }
    return container;
  }

  private Field getField(final Attribute attribute) {
    final String name = attribute.getName();
    final boolean required = attribute.isRequired();
    final DataType dataType = attribute.getType();
    final Map<Object, Object> allowedValues = attribute.getAllowedValues();
    final Object defaultValue = attribute.getDefaultValue();
    Field field;
    if (allowedValues.isEmpty()) {
      final Class<?> typeClass = dataType.getJavaClass();
      if (dataType.equals(DataTypes.BASE64_BINARY)) {
        field = new FileField(name, required);
      } else if (dataType.equals(DataTypes.LONG)) {
        field = new LongField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.INT)) {
        field = new IntegerField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.SHORT)) {
        field = new ShortField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.BYTE)) {
        field = new ByteField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.FLOAT)) {
        field = new FloatField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.DOUBLE)) {
        field = new DoubleField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.BOOLEAN)) {
        field = new CheckBoxField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.INTEGER)) {
        field = new BigIntegerField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.DECIMAL)) {
        field = new BigDecimalField(name, required, defaultValue);
      } else if (dataType.equals(DataTypes.ANY_URI)) {
        field = new UrlField(name, required, defaultValue);
      } else if (Date.class.isAssignableFrom(typeClass)) {
        field = new DateField(name, required, defaultValue);
      } else if (Timestamp.class.isAssignableFrom(typeClass)) {
        field = new TimestampField(name, required, defaultValue);
      } else if (java.util.Date.class.isAssignableFrom(typeClass)) {
        field = new DateTimeField(name, required, defaultValue);
      } else if (Geometry.class.isAssignableFrom(dataType.getJavaClass())) {
        field = new TextAreaField(name, 60, 10, required);
      } else if (URL.class.isAssignableFrom(dataType.getJavaClass())) {
        field = new UrlField(name, required, defaultValue);
      } else if (String.class.isAssignableFrom(typeClass)) {
        int length = attribute.getLength();
        if (length == -1) {
          length = 70;
        }
        field = new TextField(name, length, defaultValue, required);
      } else {
        throw new IllegalArgumentException("Values with class "
          + typeClass.getName() + " are not supported");
      }
    } else {
      field = new SelectField(name, defaultValue, required, allowedValues);
    }
    return field;
  }

  private Element getFormInstant(final BusinessApplication businessApplication) {
    final ElementContainer container = new ElementContainer();

    final String url = businessAppBuilder.getPageUrl("clientInstant");
    final Form form = new Form("instantForm", url);
    final ElementContainer fields = new ElementContainer(new TableBody());
    final boolean perRequestInputData = businessApplication.isPerRequestInputData();

    final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
    for (final Attribute attribute : requestMetaData.getAttributes()) {
      final String name = attribute.getName();
      if (!businessApplication.isCoreParameter(name)) {
        if (businessApplication.isJobParameter(name) || !perRequestInputData) {
          addFieldRow(fields, attribute);
        }
      }
    }
    if (perRequestInputData) {
      addInputDataFields(fields, businessApplication);
    }
    addGeometryFields(fields, businessApplication);
    addResultDataFields(fields, businessApplication, "format");
    addTestFields(fields, businessApplication);
    form.add(fields);
    form.add(new DivElementContainer("actionMenu", new SubmitField(
      "instantForm", "Create Job")));

    container.add(form);
    return container;
  }

  private Element getFormMultiple(final BusinessApplication businessApplication) {
    final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();

    final ElementContainer container = new ElementContainer();

    final String url = businessAppBuilder.getPageUrl("clientMultiple");
    final Form form = new Form("clientMultiple", url);
    form.setEncType(Form.MULTIPART_FORM_DATA);
    final ElementContainer fields = new ElementContainer(new TableBody());

    for (final Attribute attribute : requestMetaData.getAttributes()) {
      final String name = attribute.getName();
      if (!businessApplication.isCoreParameter(name)) {
        if (businessApplication.isJobParameter(name)) {
          addFieldRow(fields, attribute);
        }
      }
    }
    addMultiInputDataFields(fields, businessApplication);

    addGeometryFields(fields, businessApplication);

    addResultDataFields(fields, businessApplication, "resultDataContentType");

    addNotificationFields(fields);

    addTestFields(fields, businessApplication);

    form.add(fields);
    form.add(new DivElementContainer("actionMenu", new SubmitField(
      "clientMultiple", "Create Job")));

    container.add(form);
    return container;
  }

  private Element getFormSingle(final BusinessApplication businessApplication) {
    final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();

    final ElementContainer container = new ElementContainer();

    final String url = businessAppBuilder.getPageUrl("clientSingle");
    final Form form = new Form("createSingle", url);
    final ElementContainer fields = new ElementContainer(new TableBody());
    final boolean perRequestInputData = businessApplication.isPerRequestInputData();

    for (final Attribute attribute : requestMetaData.getAttributes()) {
      final String name = attribute.getName();
      if (!businessApplication.isCoreParameter(name)) {
        if (businessApplication.isJobParameter(name) || !perRequestInputData) {
          addFieldRow(fields, attribute);
        }
      }
    }
    addGeometryFields(fields, businessApplication);
    if (perRequestInputData) {
      addInputDataFields(fields, businessApplication);
    }
    addResultDataFields(fields, businessApplication, "resultDataContentType");

    addNotificationFields(fields);

    addTestFields(fields, businessApplication);

    form.add(fields);
    form.add(new DivElementContainer("actionMenu", new SubmitField(
      "createSingle", "Create Job")));

    container.add(form);
    return container;
  }

  public String getInputMediaType(final BusinessApplication application,
    String inputContentType) {
    if (StringUtils.hasText(inputContentType)) {
      if (!inputContentType.contains("/")) {
        inputContentType = application.getInputContentType(inputContentType);
        if (inputContentType == null) {
          return null;
        }
      }
      final MediaType mediaType = MediaType.valueOf(inputContentType);
      final Set<String> inputDataContentTypes = application.getInputDataContentTypes();
      for (final String contentType : inputDataContentTypes) {
        if (MediaType.valueOf(contentType).includes(mediaType)) {
          return contentType;
        }
      }
    }
    return null;
  }

  /**
   * <p>Get the list of links to the
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersJobsInfo">Get Jobs Info</a>
   * resource for each of the user's jobs.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document. Each child resource supports following custom attributes.</a>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>batchJobId</td>
   *         <td>The unique identifier of the cloud job.</td>
   *       </tr>
   *       <tr>
   *         <td>batchJobUrl</td>
   *         <td>The URL to the <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersJobsInfo">Get Users Jobs Info</a> resource without the file format extension.</td>
   *       </tr>
   *       <tr>
   *         <td>jobStatus</td>
   *         <td>The current status of the job.</td>
   *       </tr>
   *       <tr>
   *         <td>creationTimestamp</td>
   *         <td>The time when the job was created.</td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   * 
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getJobs() {
    final String consumerKey = getConsumerKey();
    if (HtmlUiBuilder.isDataTableCallback()) {
      final Map<String, Object> parameters = new HashMap<String, Object>();

      final Map<String, Object> filter = new HashMap<String, Object>();
      filter.put(BatchJob.USER_ID, consumerKey);
      parameters.put("filter", filter);

      return batchJobUiBuilder.createDataTableMap("clientList", parameters);
    } else if (MediaTypeUtil.isHtmlPage()) {
      HttpServletUtils.setAttribute("title", "Batch Jobs");
      final String url = HttpServletUtils.getFullUrl("/ws/#batchJob_clientList");
      final HttpServletResponse response = HttpServletUtils.getResponse();
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final PageInfo page = createRootPageInfo("Batch Jobs");
      final List<DataObject> batchJobs = dataAccessObject.getBatchJobsForUser(consumerKey);
      for (final DataObject job : batchJobs) {
        addBatchJobStatusLink(page, job);
      }
      return page;
    }
  }

  /**
   * <p>Get the details of a cloud job.</p>
   * 
  * 
   * <p>The method returns a BatchJob object with the following attributes.</a>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>id</td>
   *         <td>The unique identifier of the cloud job.</td>
   *       </tr>
   *       <tr>
   *         <td>businessApplicationName</td>
   *         <td>The name of the business application.</td>
   *       </tr>
   *       <tr>
   *         <td><b>&lt;parameter&gt;</b></td>
   *         <td>The the job parameters.</td>
   *       </tr>
   *       <tr>
   *         <td>resultSrid</td>
   *         <td>The coordinate system code of the projection for the result geometry.</td>
   *       </tr>
   *       <tr>
   *         <td>resultNumAxis</td>
   *         <td>The number of coordinate axis in the result geometry (e.g. 2 for 2D or 3 for 3D).</td>
   *       </tr>
   *       <tr>
   *         <td>resultScaleFactorXy</td>
   *         <td>The scale factor to apply the x, y coordinates. The scale factor is 1 / minimum unit. For example if the minimum unit was 1mm (0.001) the scale factor is 1000 (1 / 0.001).</td>
   *       </tr>
   *       <tr>
   *         <td>resultScaleFactorZ</td>
   *         <td>The scale factor to apply the z coordinate. The scale factor is 1 / minimum unit. For example if the minimum unit was 1mm (0.001) the scale factor is 1000 (1 / 0.001).</td>
   *       </tr>
   *       <tr>
   *         <td>resultDataContentType</td>
   *         <td>The MIME type of the result data specified to be returned after running the request.</td>
   *       </tr>
   *       <tr>
   *         <td>jobStatus</td>
   *         <td>The current status of the job.</td>
   *       </tr>
   *       <tr>
   *         <td>secondsToWaitForStatusCheck</td>
   *         <td>The number of seconds to wait before checking the status again</td>
   *       </tr>
   *       <tr>
   *         <td>numSubmittedRequests</td>
   *         <td>The number of requests submitted.</td>
   *       </tr>
   *       <tr>
   *         <td>numCompletedRequests</td>
   *         <td>The number of requests that completed execution successfully.</td>
   *       </tr>
   *       <tr>
   *         <td>numFailedRequests</td>
   *         <td>The number of requests that failed to execute.</td>
   *       </tr>
   *       <tr>
   *         <td>resultsUrl</td>
   *         <td></td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   * 
   * @param batchJobId The unique identifier of the cloud job. 
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/jobs/{batchJobId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getJobsInfo(@PathVariable final long batchJobId) {
    final String consumerKey = getConsumerKey();
    final DataObject batchJob = dataAccessObject.getBatchJob(consumerKey,
      batchJobId);
    if (batchJob == null) {
      throw new PageNotFoundException("Batch Job " + batchJobId
        + " does not exist.");
    } else {
      if (MediaTypeUtil.isHtmlPage()) {
        HttpServletUtils.setAttribute("title", "Batch Job " + batchJobId);
        final TabElementContainer tabs = new TabElementContainer();
        batchJobUiBuilder.addObjectViewPage(tabs, batchJob, "client");
        final String jobStatus = batchJob.getValue(BatchJob.JOB_STATUS);
        if (BatchJob.RESULTS_CREATED.equals(jobStatus)
          || BatchJob.DOWNLOAD_INITIATED.equals(jobStatus)) {
          final Map<String, Object> parameters = Collections.emptyMap();
          batchJobUiBuilder.addTabDataTable(tabs,
            BatchJobResult.BATCH_JOB_RESULT, "clientList", parameters);
          tabs.setSelectedIndex(1);
        }
        return tabs;
      } else {
        final String url = batchJobUiBuilder.getPageUrl("clientView");
        final Map<String, Object> batchJobMap = BatchJobService.toMap(batchJob,
          url, batchJobUiBuilder.getTimeUntilNextCheck(batchJob));
        return batchJobMap;
      }
    }
  }

  /**
   * <p>Get the contents of a user's job result file. The content type will be the content type
   * requested in the cloud job.</p>
   * 
   * @param batchJobId The unique identifier of the cloud job. 
   * @param resultId The unique identifier of the result file. 
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response.</p>
   */
  @RequestMapping(value = {
    "/ws/jobs/{batchJobId}/results/{resultId}"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void getJobsResult(@PathVariable final long batchJobId,
    @PathVariable final long resultId) throws IOException {
    final String consumerKey = getConsumerKey();

    final DataObject batchJob = dataAccessObject.getBatchJob(consumerKey,
      batchJobId);

    if (batchJob != null) {
      final DataObject batchJobResult = dataAccessObject.getBatchJobResult(resultId);
      if (EqualsRegistry.INSTANCE.equals(batchJobId,
        batchJobResult.getValue(BatchJobResult.BATCH_JOB_ID))) {
        dataAccessObject.setBatchJobDownloaded(batchJobId);
        final String resultDataUrl = batchJobResult.getValue(BatchJobResult.RESULT_DATA_URL);
        final HttpServletResponse response = HttpServletUtils.getResponse();
        if (resultDataUrl != null) {
          response.setStatus(HttpServletResponse.SC_SEE_OTHER);
          response.setHeader("Location", resultDataUrl);
        } else {
          try {
            final Blob resultData = batchJobResult.getValue(BatchJobResult.RESULT_DATA);
            final InputStream in = resultData.getBinaryStream();
            final String resultDataContentType = batchJobResult.getValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE);
            response.setContentType(resultDataContentType);

            long size = resultData.length();
            String jsonCallback = null;
            if (resultDataContentType.equals(MediaType.APPLICATION_JSON.toString())) {
              jsonCallback = HttpServletUtils.getParameter("callback");
              if (StringUtils.hasText(jsonCallback)) {
                size += 3 + jsonCallback.length();
              }
            }
            final DataObjectWriterFactory writerFactory = IoFactoryRegistry.getInstance()
              .getFactoryByMediaType(DataObjectWriterFactory.class,
                resultDataContentType);
            if (writerFactory != null) {
              final String fileExtension = writerFactory.getFileExtension(resultDataContentType);
              final String fileName = "job-" + batchJobId + "-result-"
                + resultId + "." + fileExtension;
              response.setHeader("Content-Disposition", "attachment; filename="
                + fileName + ";size=" + size);
            }
            final ServletOutputStream out = response.getOutputStream();
            if (StringUtils.hasText(jsonCallback)) {
              out.write(jsonCallback.getBytes());
              out.write("(".getBytes());
            }
            FileUtil.copy(in, out);
            if (StringUtils.hasText(jsonCallback)) {
              out.write(");".getBytes());
            }
            return;
          } catch (final SQLException e) {
            LoggerFactory.getLogger(getClass()).error(
              "Unable to get result data", e);
            throw new HttpMessageNotWritableException(
              "Unable to get result data", e);
          }
        }
      }
    }
    throw new PageNotFoundException("Batch Job result " + resultId
      + " does not exist.");
  }

  /**
   * <p>Get the list of links to the
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersJobsResult">Get Users Jobs Result</a>
   * resource for each of the results for a user's job.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document. Each child resource supports following custom attributes.</a>
   * 
   * <div class="simpleDataTable">
   *   <table class="data">
   *     <thead>
   *       <tr>
   *         <th>Attribute</th>
   *         <th>Description</th>
   *       </tr>
   *     </thead>
   *     <tbody>
   *       <tr>
   *         <td>batchJobResultType</td>
   *         <td>The type of result file structuredResultData, opaqueResultData, errorResultData.</td>
   *       </tr>
   *       <tr>
   *         <td>batchJobResultContentType</td>
   *         <td>The MIME type of the result file.</td>
   *       </tr>
   *     </tbody>
   *   </table>
   * </div>
   * 
   * @param batchJobId The unique identifier of the cloud job. 
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws/jobs/{batchJobId}/results"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getJobsResults(@PathVariable final long batchJobId) {
    final String consumerKey = getConsumerKey();

    final DataObject batchJob = dataAccessObject.getBatchJob(consumerKey,
      batchJobId);
    if (batchJob == null) {
      throw new PageNotFoundException("Batch Job " + batchJobId
        + " does not exist.");
    } else {
      final String title = "Batch Job " + batchJobId + " results";
      if (HtmlUiBuilder.isDataTableCallback()) {
        final Map<String, Object> parameters = new HashMap<String, Object>();

        final Map<String, Object> filter = new HashMap<String, Object>();
        filter.put(BatchJobResult.BATCH_JOB_ID, batchJobId);
        parameters.put("filter", filter);

        return batchJobResultUiBuilder.createDataTableMap("clientList",
          parameters);
      } else if (MediaTypeUtil.isHtmlPage()) {
        HttpServletUtils.setAttribute("title", title);
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("serverSide", false);
        final TabElementContainer tabs = new TabElementContainer();
        batchJobResultUiBuilder.addTabDataTable(tabs, BatchJob.BATCH_JOB,
          "clientList", parameters);
        return tabs;
      } else {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("batchJobId", batchJobId);
        final PageInfo page = createRootPageInfo(title);
        final List<DataObject> results = dataAccessObject.getBatchJobResults(batchJobId);
        if (batchJob.getValue(BatchJob.COMPLETED_TIMESTAMP) != null
          && !results.isEmpty()) {
          for (final DataObject batchJobResult : results) {
            final Number batchJobResultId = batchJobResult.getIdValue();
            parameters.put("batchJobResultId", batchJobResultId);
            final PageInfo resultPage = addPage(page, batchJobResultId,
              "Batch Job " + batchJobId + " result " + batchJobResultId);
            final String batchJobResultType = batchJobResult.getValue(BatchJobResult.BATCH_JOB_RESULT_TYPE);
            resultPage.setAttribute("batchJobResultType", batchJobResultType);
            resultPage.setAttribute("batchJobResultContentType",
              batchJobResult.getValue(BatchJobResult.RESULT_DATA_CONTENT_TYPE));
            if (batchJobResultType.equals(BatchJobResult.OPAQUE_RESULT_DATA)) {
              resultPage.setAttribute("batchJobExecutionGroupSequenceNumber",
                batchJobResult.getValue(BatchJobResult.SEQUENCE_NUMBER));
            }
          }
        }
        return page;
      }
    }
  }

  private List<Map<String, Object>> getRequestAttributeList(
    final BusinessApplication businessApplication) {
    final List<Map<String, Object>> parameters = new ArrayList<Map<String, Object>>();
    final boolean perRequestInputData = businessApplication.isPerRequestInputData();

    addParameter(
      parameters,
      "inputDataContentType",
      "string",
      null,
      "The MIME type of the input data specified by an inputData or inputDataUrl parameter.",
      true, false, perRequestInputData, Collections.emptyList());

    addParameter(parameters, "inputData", "file", null,
      "The multi-part file containing the input data.", true, false,
      perRequestInputData, Collections.emptyList());

    addParameter(parameters, "inputDataUrl", "string", null,
      "The http: URL to the file or resource containing input data.", true,
      false, perRequestInputData, Collections.emptyList());

    final DataObjectMetaData requestMetaData = businessApplication.getRequestMetaData();
    for (final Attribute attribute : requestMetaData.getAttributes()) {
      addParameter(parameters, attribute, perRequestInputData);
    }

    addParameter(parameters, "notificationEmail", "string", null,
      "The email address to send the job status to when the job is completed.",
      true, false, perRequestInputData, Collections.emptyList());

    addParameter(
      parameters,
      "notificationUrl",
      "string",
      null,
      "The http: URL to be notified when the job is completed. A copy of the Job status will be posted to process running at this URL.",
      true, false, perRequestInputData, Collections.emptyList());

    return parameters;
  }

  private org.springframework.core.io.Resource getResource(
    final String fieldName) {
    final HttpServletRequest request = HttpServletUtils.getRequest();
    if (request instanceof MultipartHttpServletRequest) {
      final MultipartHttpServletRequest multiPartRequest = (MultipartHttpServletRequest)request;
      final MultipartFile file = multiPartRequest.getFile(fieldName);
      if (file != null) {
        return new MultipartFileResource(file);
      }
    }
    final String value = HttpServletUtils.getParameter(fieldName);
    if (StringUtils.hasText(value)) {
      return new ByteArrayResource(fieldName, value);
    } else {
      return null;
    }
  }

  private List<Map<String, Object>> getResultAttributeList(
    final BusinessApplication businessApplication) {
    final List<Map<String, Object>> resultAttributes = new ArrayList<Map<String, Object>>();

    final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
    for (final Attribute attribute : resultMetaData.getAttributes()) {
      final String name = attribute.getName();
      final String typeDescription = attribute.getTypeDescription();
      final String description = attribute.getDescription();

      final Map<String, Object> resultAttribute = new LinkedHashMap<String, Object>();
      resultAttribute.put("name", name);
      resultAttribute.put("type", typeDescription);
      resultAttribute.put("description", description);
      resultAttribute.put("descriptionUrl",
        attribute.getProperty("descriptionUrl"));
      resultAttributes.add(resultAttribute);
    }
    return resultAttributes;
  }

  /**
   * <p>Get the root resource of the CPF web services. The resource contains links to the
   * <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplications">Get Business Applications</a>
   * and <a href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsers">Get Users</a> resources.</p>
   * 
   * <p>The method returns a <a href="../../resourceList.html">Resource Description</a> document.</a>
   * 
   * @return The resource.
   *
   * @web.response.status 200 <p>The resource will be returned in the body of the HTTP response in the requested format.</p>
   */
  @RequestMapping(value = {
    "/ws"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getRoot() {
    if (MediaTypeUtil.isHtmlPage()) {
      HttpServletUtils.setAttribute("title", "Cloud Processing Framework");

      final TabElementContainer tabs = new TabElementContainer();

      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("serverSide", Boolean.TRUE);
      businessAppBuilder.addTabDataTable(tabs, BatchJob.BATCH_JOB,
        "clientList", parameters);

      parameters.put("serverSide", Boolean.FALSE);
      businessAppBuilder.addTabDataTable(tabs, BusinessApplication.class,
        "clientList", parameters);

      return tabs;
    } else {
      final PageInfo page = createRootPageInfo("Cloud Processing Framework");
      addPage(page, "jobs", "Jobs");
      addPage(page, "apps", "Business Applications");
      return page;
    }
  }

  @Resource(name = "/CPF/CPF_BATCH_JOB_RESULTS-htmlbuilder")
  public void setBatchJobResultUiBuilder(
    final BatchJobResultUiBuilder batchJobResultUiBuilder) {
    this.batchJobResultUiBuilder = batchJobResultUiBuilder;
  }

  @Resource(name = "batchJobService")
  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }

  @Resource(name = "/CPF/CPF_BATCH_JOBS-htmlbuilder")
  public void setBatchJobUiBuilder(final BatchJobUiBuilder batchJobUiBuilder) {
    this.batchJobUiBuilder = batchJobUiBuilder;
  }

  @Resource(
      name = "ca.bc.gov.open.cpf.plugin.impl.BusinessApplication-htmlbuilder")
  public void setBusinessAppBuilder(
    final BusinessApplicationUiBuilder businessAppBuilder) {
    this.businessAppBuilder = businessAppBuilder;
  }

  private void setBusinessApplicationDescription(final PageInfo page,
    final BusinessApplication businessApplication) {
    String description = businessApplication.getDescription();
    String descriptionUrl = businessApplication.getDescriptionUrl();
    if (StringUtils.hasText(descriptionUrl)) {
      descriptionUrl = "<p>Click <a href=\"" + descriptionUrl
        + "\">here</a> for a more detailed description of the service.</p>";
    }
    if (StringUtils.hasText(description)) {
      description = "<p>" + description + "</p>";
      if (StringUtils.hasText(descriptionUrl)) {
        description += descriptionUrl;
      }
    } else {
      description = descriptionUrl;
    }
    page.setHtmlDescription(description);
    page.setAttribute("businessApplicationName", businessApplication.getName());
  }

  @Resource(name = "cpfDataAccessObject")
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

}
