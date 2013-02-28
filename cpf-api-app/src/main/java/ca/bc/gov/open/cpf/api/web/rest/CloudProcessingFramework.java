package ca.bc.gov.open.cpf.api.web.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.impl.log.ModuleLog;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.gis.data.io.DataObjectWriterFactory;
import com.revolsys.gis.data.io.ListDataObjectReader;
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
import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.io.json.JsonDataObjectIoFactory;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.spring.ByteArrayResource;
import com.revolsys.spring.InputStreamResource;
import com.revolsys.spring.InvokeMethodAfterCommit;
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
import com.revolsys.util.UrlUtil;
import com.vividsolutions.jts.geom.Geometry;

@Controller
public class CloudProcessingFramework {

  private static final DateFormat DATETIME_FORMAT = DateFormat.getDateTimeInstance(
    DateFormat.DEFAULT, DateFormat.SHORT);

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

  /** The batch job service used to interact with the database. */
  @Resource(name = "batchJobService")
  private BatchJobService batchJobService;

  @Resource(name = "/CPF/CPF_BATCH_JOBS-htmlbuilder")
  private BatchJobUiBuilder batchJobUiBuilder;

  @Resource(name = "/CPF/CPF_BATCH_JOB_RESULTS-htmlbuilder")
  private BatchJobResultUiBuilder batchJobResultUiBuilder;

  @Resource(
      name = "ca.bc.gov.open.cpf.plugin.impl.BusinessApplication-htmlbuilder")
  private BusinessApplicationUiBuilder businessAppBuilder;

  @Resource(name = "cpfDataAccessObject")
  private CpfDataAccessObject dataAccessObject;

  private final Map<String, RawContent> rawContent = new HashMap<String, RawContent>();

  public CloudProcessingFramework() {
  }

  public void addAppVersionPage(final PageInfo page,
    final BusinessApplication businessApplication,
    final String businessApplicationVersion) {
    final String title = businessApplication.getTitle();
    final PageInfo childPage = addPage(page, businessApplicationVersion, title
      + " v" + businessApplicationVersion);
    setBusinessApplicationDescription(childPage, businessApplication,
      businessApplicationVersion);
  }

  private void addBatchJobStatusLink(final PageInfo page,
    final String consumerKey, final DataObject job) {
    final Number batchJobId = job.getIdValue();
    final String batchJobUrl = HttpServletUtils.getFullUrl("/ws/users/"
      + consumerKey + "/jobs/" + batchJobId + "/");
    final Timestamp timestamp = job.getValue(BatchJob.WHEN_CREATED);
    final PageInfo childPage = addPage(page, batchJobUrl, "Batch Job "
      + batchJobId + " Status");

    childPage.setAttribute("consumerKey", consumerKey);
    childPage.setAttribute("batchJobId", batchJobId);
    childPage.setAttribute("batchJobUrl", batchJobUrl);
    childPage.setAttribute("jobStatus", job.getValue(BatchJob.JOB_STATUS));
    childPage.setAttribute("creationTimestamp",
      DATETIME_FORMAT.format(timestamp));
  }

  private void addFieldRow(final ElementContainer fields,
    final Attribute attribute) {
    final Field field = getField(attribute);
    final String name = attribute.getName();
    final String label = CaseConverter.toCapitalizedWords(name);
    final String instructions = attribute.getDescription();
    TableHeadingDecorator.addRow(fields, field, label, instructions);
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
    final String fieldName = "srid";
    addFieldRow(fields, requestMetaData, fieldName);
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
    final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
    final String defaultInputType = BusinessApplication.getDefaultMimeType(inputDataContentTypes);
    final SelectField inputDataContentType = new SelectField(
      "inputDataContentType", defaultInputType, true, inputDataContentTypes);

    TableHeadingDecorator.addRow(
      fields,
      inputDataContentType,
      "Input Data Content Type",
      "The MIME type of the input data specified by an inputData or inputDataUrl parameter.");

    final UrlField inputDataUrl = new UrlField("inputDataUrl", false);
    TableHeadingDecorator.addRow(fields, inputDataUrl, "Input Data URL",
      "The http: URL to the file or resource containing input data.");

    final FileField inputData = new FileField("inputData", false);
    TableHeadingDecorator.addRow(fields, inputData, "Input Data",
      "The multi-part file containing the input data.");
  }

  private void addMultiInputDataFields(final ElementContainer fields,
    final BusinessApplication businessApplication) {
    final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
    final String defaultInputType = BusinessApplication.getDefaultMimeType(inputDataContentTypes);
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
    final boolean jobParameter = attribute.getProperty(BusinessApplication.JOB_PARAMETER) == Boolean.TRUE;
    final boolean requestParameter = attribute.getProperty(BusinessApplication.REQUEST_PARAMETER) == Boolean.TRUE;
    final Collection<Object> allowedValues = attribute.getAllowedValues()
      .keySet();
    addParameter(parameters, name, typeDescription, description, jobParameter,
      requestParameter, perRequestInputData, allowedValues);
  }

  private void addParameter(final List<Map<String, Object>> parameters,
    final String name, final String typeDescription, final String description,
    final boolean jobParameter, final boolean requestParameter,
    final boolean perRequestInputData, final Collection<Object> allowedValues) {
    final Map<String, Object> parameter = new LinkedHashMap<String, Object>();
    parameter.put("name", name);
    parameter.put("type", typeDescription);
    parameter.put("description", description);
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
    final Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
    final String defaultValue = BusinessApplication.getDefaultMimeType(resultDataContentTypes);

    final SelectField resultDataContentType = new SelectField(fieldName,
      defaultValue, true, resultDataContentTypes);
    TableHeadingDecorator.addRow(
      container,
      resultDataContentType,
      "Result Data Content Type",
      "The MIME type of the result data specified to be returned after running the request");

  }

  private void checkPermission(final Expression expression,
    final String accessDeniedMessage) {
    final boolean permitted = hasPermission(expression);
    if (!permitted) {
      throw new AccessDeniedException(accessDeniedMessage);
    }
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
    batchJob.setValue(BatchJob.NUM_EXECUTING_REQUESTS, 0);
    batchJob.setValue(BatchJob.NUM_COMPLETED_REQUESTS, 0);
    batchJob.setValue(BatchJob.NUM_FAILED_REQUESTS, 0);

    return batchJob;
  }

  /**
   * <p>
   * Create a new batch job containing multiple requests to be processed by the
   * business application.
   * <p>
   * In addition to the standard parameters listed in the API each business
   * application has additional job and request parameters. The <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsMultiple"
   * >Get Business Applications Multiple</a> resource should be consulted to get
   * the full list of supported parameters.
   * </p>
   * <p>
   * The maximum size including all parameters and protocol overhead of a
   * multi-part request is 20MB. Therefore inputDataUrl should be used instead
   * of inputData where possible.
   * </p>
   * <h5>Structured Input Data</h5>
   * <p>
   * For structured input data business applications the requests are specified
   * using either a single inputData file or a single inputDataUrl can be
   * specified. The file must be in the <a href="../../fileFormat.html">file
   * format</a> specified by a single inputDataContentType. The contents of the
   * file must contain one record for each request to be processed in the batch
   * job. The fields of each record must contain the request parameters
   * supported by the business application. The names of the parameters are case
   * sensitive.
   * </p>
   * <h5>Opaque input data</h5>
   * <p>
   * For opaque input data (e.g. JPEG image, ESRI Shapefile) the requests can be
   * specified as one or more inputData files or one or more inputDataUrl
   * parameters. It is not possible to mix inputData and inputDataUrl parameters
   * in the same batch job. If all the requests have the same content type a
   * single inputDataContentType can be specified. Otherwise an
   * inputDataContentType must be specified for each inputData or inputDataUrl
   * in the same order.
   * </p>
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @param inputDataContentTypes The MIME type of the input data specified by
   * an inputData or inputDataUrl parameter.
   * @param inputDataFiles The multi-part file containing the input data.
   * @param inputDataUrls The http: URL to the file or resource containing input
   * data.
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
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/multiple"
  }, method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createJobWithMultipleRequests(
    @PathVariable final String businessApplicationName,
    @PathVariable final String businessApplicationVersion,
    @RequestParam(value = "inputDataContentType", required = false) final String[] inputDataContentTypes,
    @RequestParam(value = "inputData", required = false) List<MultipartFile> inputDataFiles,
    @RequestParam(value = "inputDataUrl", required = false) List<String> inputDataUrls,
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
      businessApplicationName, businessApplicationVersion, "clientMultiple");
    if (businessApplication != null) {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Module module = businessApplication.getModule();
      final String moduleName = module.getName();
      final String consumerKey = getConsumerKey();

      final Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      final String defaultResultDataContentType = resultDataContentTypes.get(0);
      final Map<String, String> suppportedInputDataContentTypes = businessApplication.getInputDataContentTypes();
      final String defaultInputDataContentType = suppportedInputDataContentTypes.get(0);

      final Map<String, String> businessApplicationParameters = new HashMap<String, String>();

      final DataObject batchJob = createBatchJob();
      final Long batchJobId = batchJob.getValue(BatchJob.BATCH_JOB_ID);

      final Map<String, Object> logData = new LinkedHashMap<String, Object>();
      logData.put("batchJobId", batchJobId);
      logData.put("businessApplicationName", businessApplicationName);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.info(moduleName, "Job submit multiple", "Start", logData);
      }

      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_NAME,
        businessApplicationName);
      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_VERSION,
        businessApplicationVersion);
      batchJob.setValue(BatchJob.USER_ID, consumerKey);
      batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS, 0);

      batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE,
        defaultResultDataContentType);
      String inputDataContentType = defaultInputDataContentType;

      if (inputDataContentTypes == null) {
      } else {
        for (final String inputContentType : inputDataContentTypes) {
          if (inputContentType != null) {
            final MediaType mediaType = MediaType.valueOf(inputContentType);
            boolean found = false;
            for (final String contentType : suppportedInputDataContentTypes.keySet()) {
              if (MediaType.valueOf(contentType).includes(mediaType)) {
                inputDataContentType = inputContentType;
                found = true;
              }
            }
            if (!found) {
              throw new HttpMessageNotReadableException("inputDataContentType="
                + inputDataContentType + " is not supported.");
            }
          }
        }
        if (!businessApplication.isPerRequestInputData()) {
          if (inputDataContentTypes.length == 1) {
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
        if (resultDataContentTypes.containsKey(resultDataContentType)) {
          batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE,
            resultDataContentType);
        } else {
          throw new HttpMessageNotReadableException("resultDataContentType="
            + resultDataContentType + " is not supported.");
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
            throw new HttpMessageNotReadableException(
              "Parameter "
                + parameterName
                + " cannot be specified on a job. It can only be specified as a field in the input data.");
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
                dataAccessObject.createBatchJobRequest(batchJobId,
                  ++requestSequenceNumber, inputDataContentType, resource);
              } finally {
                InvokeMethodAfterCommit.invoke(in, "close");
              }
            }
          } else {
            for (final String inputDataUrl : inputDataUrls) {
              dataAccessObject.createBatchJobRequest(batchJobId,
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
      HttpServletUtils.setPathVariable("consumerKey", consumerKey);
      HttpServletUtils.setPathVariable("batchJobId", batchJobId);
      batchJobUiBuilder.redirectPage("clientView");

      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.info(moduleName, "Job submit multiple", "End multiple",
          stopWatch, logData);
      }
      final Map<String, Object> statistics = new HashMap<String, Object>();
      statistics.put("submittedJobsTime", stopWatch);
      statistics.put("submittedJobsCount", 1);
      InvokeMethodAfterCommit.invoke(batchJobService, "addStatistics",
        businessApplication, statistics);
    }
  }

  /**
   * <p>
   * Create a new Batch Job for the version of the business application
   * containing a single request.
   * <p>
   * <p>
   * The job and request parameters for the business application must be passed
   * in using a multipart/mime-data or application/x-www-form-urlencoded
   * encoding in the body of a HTTP POST request.
   * </p>
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   */
  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/single"
  }, method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createJobWithSingleRequest(
    @PathVariable final String businessApplicationName,
    @PathVariable final String businessApplicationVersion) throws IOException {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, businessApplicationVersion, "clientSingle");
    if (businessApplication != null) {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplicationName);
      final Module module = businessApplication.getModule();
      final String moduleName = module.getName();

      final DataObject batchJob = createBatchJob();
      final Long batchJobId = batchJob.getValue(BatchJob.BATCH_JOB_ID);

      final Map<String, Object> logData = new LinkedHashMap<String, Object>();
      logData.put("batchJobId", batchJobId);
      logData.put("businessApplicationName", businessApplicationName);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.info(moduleName, "Job submit single", "Start", logData);
      }
      final String consumerKey = getConsumerKey();

      final Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      final String defaultResultDataContentType = resultDataContentTypes.get(0);
      final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
      final String defaultInputDataContentType = inputDataContentTypes.get(0);

      final Map<String, String> businessApplicationParameters = new HashMap<String, String>();

      String inputDataContentType = HttpServletUtils.getParameter("inputDataContentType");
      if (!StringUtils.hasText(inputDataContentType)) {
        inputDataContentType = defaultInputDataContentType;
      }
      final boolean perRequestInputData = businessApplication.isPerRequestInputData();
      org.springframework.core.io.Resource inputDataIn = null;
      String inputDataUrl = null;
      if (perRequestInputData) {
        if (!inputDataContentTypes.containsKey(inputDataContentType)
          && !inputDataContentTypes.containsKey("*/*")) {
          throw new HttpMessageNotReadableException("inputDataContentType="
            + inputDataContentType + " is not supported.");
        }
        inputDataIn = getResource("inputData");
        inputDataUrl = HttpServletUtils.getParameter("inputDataUrl");
        final boolean hasInputDataIn = inputDataIn != null;
        final boolean hasInputDataUrl = StringUtils.hasText(inputDataUrl);
        if (hasInputDataIn == hasInputDataUrl) {
          throw new HttpMessageNotReadableException(
            "Either an inputData file or inputDataUrl parameter must be specified, but not both");
        }
      }
      String resultDataContentType = HttpServletUtils.getParameter("resultDataContentType");
      if (!StringUtils.hasText(resultDataContentType)) {
        resultDataContentType = defaultResultDataContentType;
      }
      if (!resultDataContentTypes.containsKey(resultDataContentType)) {
        throw new HttpMessageNotReadableException("resultDataContentType="
          + resultDataContentType + " is not supported.");
      }
      String notificationUrl = HttpServletUtils.getParameter("notificationUrl");
      final String notificationEmail = HttpServletUtils.getParameter("notificationEmail");
      if (StringUtils.hasText(notificationEmail)) {
        if (StringUtils.hasText(notificationUrl)) {
          throw new HttpMessageNotReadableException(
            "Both notificationUrl and notificationEmail cannot be specified. Enter a value in one or the other but not both.");
        } else {
          notificationUrl = "mailto:" + notificationEmail;
        }
      }
      final String srid = HttpServletUtils.getParameter("srid");
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
                attribute, value);
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
      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_VERSION,
        businessApplicationVersion);
      batchJob.setValue(BatchJob.USER_ID, consumerKey);
      batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS, 0);

      batchJob.setValue(BatchJob.BUSINESS_APPLICATION_PARAMS,
        JsonMapIoFactory.toString(businessApplicationParameters));

      if (StringUtils.hasText(notificationUrl)) {
        batchJob.setValue(BatchJob.NOTIFICATION_URL, notificationUrl);
      }
      batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE,
        resultDataContentType);
      batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS, 1);
      batchJob.setValue(BatchJob.NUM_EXECUTING_REQUESTS, 0);
      batchJob.setValue(BatchJob.NUM_COMPLETED_REQUESTS, 0);
      batchJob.setValue(BatchJob.NUM_FAILED_REQUESTS, 0);
      final Timestamp now = new Timestamp(System.currentTimeMillis());
      batchJob.setValue(BatchJob.LAST_SCHEDULED_TIMESTAMP, now);
      batchJob.setValue(BatchJob.JOB_STATUS, BatchJob.REQUESTS_CREATED);
      batchJob.setValue(BatchJob.WHEN_STATUS_CHANGED, now);
      dataAccessObject.write(batchJob);
      if (perRequestInputData) {
        if (inputDataIn != null) {
          dataAccessObject.createBatchJobRequest(batchJobId, 1,
            inputDataContentType, inputDataIn);
        } else {
          dataAccessObject.createBatchJobRequest(batchJobId, 1,
            inputDataContentType, inputDataUrl);
        }
      } else {
        final String inputDataString = JsonDataObjectIoFactory.toString(inputData);
        dataAccessObject.createBatchJobRequest(batchJobId, 1, inputDataString);
      }

      batchJobService.schedule(businessApplicationName, batchJobId);

      HttpServletUtils.setPathVariable("consumerKey", consumerKey);
      HttpServletUtils.setPathVariable("batchJobId", batchJobId);

      logData.put("batchJobId", batchJobId);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.infoAfterCommit(moduleName, "Job submit single", "End",
          stopWatch, logData);
      }
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
   * Delete or cancel a user's batch job.
   * </p>
   * <p>
   * This service should be invoked after the results from the batch job are
   * downloaded. If this method is not called the batch job will automatically
   * be deleted 7 days after the result download was started.
   * </p>
   * <p>
   * This method can also be used to cancel a batch job before it was finished.
   * If a batch job was submitted in error or no longer required use this method
   * to cancel the batch job to help free resources on the system to process
   * other batch jobs.
   * </p>
   * 
   * @param consumerKey The consumerKey of the user accessing the service. Users
   * can only access their own jobs.
   * @param batchJobId The batch job identifier.
   * @web.response.status 200
   * <p>
   * <b>OK</b>
   * </p>
   * <p>
   * If the batch job was deleted an empty response will be returned.
   * </p>
   * @web.response.status 404
   * <p>
   * <b>Not Found</b>
   * </p>
   * <p>
   * If the batch job does not exist, has been deleted, or was owned by another
   * user.
   * </p>
   */
  @RequestMapping(value = {
    "/ws/users/{consumerKey}/jobs/{batchJobId}"
  }, method = RequestMethod.DELETE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteJob(@PathVariable final String consumerKey,
    @PathVariable final long batchJobId) {
    final DataObject batchJob = dataAccessObject.getBatchJob(consumerKey,
      batchJobId);
    if (batchJob == null) {
      throw new PageNotFoundException("The batch job " + batchJobId
        + " does not exist");
    } else {
      if (dataAccessObject.deleteBatchJob(batchJobId) == 0) {
        throw new PageNotFoundException("The batch job " + batchJobId
          + " does not exist");
      } else {
        final HttpServletResponse response = HttpServletUtils.getResponse();
        response.setStatus(HttpServletResponse.SC_OK);
      }
      batchJobService.cancelBatchJob(batchJobId);
    }
  }

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
    final String businessApplicationName,
    final String businessApplicationVersion, final String pageName) {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(
      businessApplicationName, businessApplicationVersion);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new PageNotFoundException("Business application "
        + businessApplicationName + "v" + businessApplicationVersion
        + " does not exist.");
    } else if (!businessApplication.isVersionSupported(businessApplicationVersion)) {
      HttpServletUtils.setPathVariable("businessApplicationVersion",
        businessApplication.getVersion());
      businessAppBuilder.redirectPage(pageName);
      return null;
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
      page.setAttribute("businessApplicationVersion",
        businessApplication.getVersion());
      page.setAttribute("businessApplicationTitle", title);
      page.setAttribute("businessApplicationDescription",
        businessApplication.getDescription());
      page.setAttribute("businessApplicationDescriptionUrl",
        businessApplication.getDescriptionUrl());

      final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
      page.setAttribute("inputDataContentTypes", inputDataContentTypes.keySet());

      page.setAttribute("perRequestInputData",
        businessApplication.isPerRequestInputData());

      page.setAttribute("parameters",
        getRequestAttributeList(businessApplication));

      final Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      page.setAttribute("resultDataContentTypes",
        resultDataContentTypes.keySet());
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
          final String version = app.getVersion();
          final String path = name + "/" + version;
          final PageInfo appPage = addPage(page, path, title + " v" + version);
          appPage.setAttribute("businessApplicationName", name);
          appPage.setAttribute("businessApplicationVersion", version);
        }
      }
      return page;
    }
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/instant"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Object getBusinessApplicationsInstant(
    @PathVariable final String businessApplicationName,
    @PathVariable final String businessApplicationVersion) {
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, businessApplicationVersion, "clientInstant");
    if (businessApplication == null) {
      return null;
    } else {
      final PluginAdaptor plugin = batchJobService.getBusinessApplicationPlugin(businessApplication);
      checkPermission(businessApplication.getInstantModeExpression(),
        "No instant mode permission for " + businessApplication.getName());
      final String format = HttpServletUtils.getParameter("format");
      if (format == null
        || "true".equals(HttpServletUtils.getParameter("specification"))) {
        if (MediaTypeUtil.isHtmlPage()) {
          final Map<String, Object> titleParameters = new HashMap<String, Object>();
          final String title = businessApplication.getTitle();
          titleParameters.put("businessApplicationTitle", title);
          titleParameters.put("businessApplicationVersion",
            businessApplicationVersion);
          final PageInfo page = createRootPageInfo(title + " Instant");
          setBusinessApplicationDescription(page, businessApplication,
            businessApplicationVersion);
          page.setPagesElement(getFormInstant(businessApplication));

          HttpServletUtils.setAttribute("title", page.getTitle());
          return page;
        } else {
          return getBusinessApplicationPageInfo(businessApplication, "instant");
        }
      } else {
        final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
        final DataObject parameters = new ArrayDataObject(requestMetaData);
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
                final String sridString = HttpServletUtils.getParameter("srid");
                batchJobService.setStructuredInputDataValue(sridString,
                  parameters, attribute, value);
              } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(
                  "Parameter value is not valid " + name + " " + value, e);
              }
            }
          }
        }
        plugin.setParameters(parameters);
        plugin.execute();
        final List<Map<String, Object>> list = plugin.getResults();
        HttpServletUtils.setAttribute("contentDispositionFileName",
          businessApplicationName);
        final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
        final List<DataObject> results = DataObjectUtil.getObjects(
          resultMetaData, list);
        for (final Entry<String, Object> entry : businessApplication.getProperties()
          .entrySet()) {
          final String name = entry.getKey();
          final Object value = entry.getValue();
          HttpServletUtils.setAttribute(name, value);
        }

        if (businessApplication.getResultListProperty() == null) {
          return results.get(0);
        } else {
          int i = 1;
          for (final DataObject result : results) {
            result.setValue("sequenceNumber", 1);
            result.setValue("resultNumber", i);
            i++;
          }
          return new ListDataObjectReader(resultMetaData, results);
        }
      }
    }
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/multiple"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public PageInfo getBusinessApplicationsMultiple(
    @PathVariable final String businessApplicationName,
    @PathVariable final String businessApplicationVersion) {
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, businessApplicationVersion, "clientMultiple");
    if (businessApplication == null) {
      return null;
    } else {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Map<String, Object> titleParameters = new HashMap<String, Object>();

      if (MediaTypeUtil.isHtmlPage()) {
        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);
        titleParameters.put("businessApplicationVersion",
          businessApplicationVersion);
        final PageInfo page = createRootPageInfo(title
          + "Create Multi-Request job");
        setBusinessApplicationDescription(page, businessApplication,
          businessApplicationVersion);
        page.addInputContentType(MediaType.MULTIPART_FORM_DATA);

        page.setPagesElement(getFormMultiple(businessApplication));

        HttpServletUtils.setAttribute("title", page.getTitle());
        return page;
      } else {
        return getBusinessApplicationPageInfo(businessApplication, "multiple");
      }
    }
  }

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
      final List<String> compatibleVersions = businessApplication.getCompatibleVersions();
      final String version = businessApplication.getVersion();
      if (MediaTypeUtil.isHtmlPage()) {
        HttpServletUtils.setPathVariable("businessApplicationVersion", version);
        return businessAppBuilder.redirectPage("clientView");
      } else {
        final String title = businessApplication.getTitle();
        final PageInfo page = createRootPageInfo(title);
        setBusinessApplicationDescription(page, businessApplication, null);

        addAppVersionPage(page, businessApplication, version);

        for (final String compatibleVersion : compatibleVersions) {
          addAppVersionPage(page, businessApplication, compatibleVersion);
        }
        return page;
      }
    }
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/single",
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public PageInfo getBusinessApplicationsSingle(
    @PathVariable final String businessApplicationName,
    @PathVariable final String businessApplicationVersion) {
    final BusinessApplication businessApplication = getBusinessApplication(
      businessApplicationName, businessApplicationVersion, "clientSingle");
    if (businessApplication == null) {
      return null;
    } else {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Map<String, Object> titleParameters = new HashMap<String, Object>();

      if (MediaTypeUtil.isHtmlPage()) {
        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);
        titleParameters.put("businessApplicationVersion",
          businessApplicationVersion);

        final PageInfo page = createRootPageInfo(title
          + "Create Single Request job");
        setBusinessApplicationDescription(page, businessApplication,
          businessApplicationVersion);

        page.setPagesElement(getFormSingle(businessApplication));
        HttpServletUtils.setAttribute("title", page.getTitle());
        return page;
      } else {
        return getBusinessApplicationPageInfo(businessApplication, "single");
      }
    }
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getBusinessApplicationsVersion(
    @PathVariable final String businessApplicationName,
    @PathVariable final String businessApplicationVersion) {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(
      businessApplicationName, businessApplicationVersion);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new PageNotFoundException("Business application "
        + businessApplicationName + " does not exist.");
    } else {
      checkPermission(businessApplication);
      final Map<String, Object> titleParameters = new HashMap<String, Object>();
      final String title = businessApplication.getTitle();
      titleParameters.put("businessApplicationTitle", title);
      titleParameters.put("businessApplicationVersion",
        businessApplicationVersion);
      if (businessApplication.isVersionSupported(businessApplicationVersion)) {
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
            container.add(new RawContent(
              "<p>Click <a href=\""
                + descriptionUrl
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
          final PageInfo page = createRootPageInfo(title + " v"
            + businessApplicationVersion);

          if (hasPermission(businessApplication.getInstantModeExpression())) {
            addPage(page, "instant", "Instant");
          }
          if (hasPermission(businessApplication.getBatchModeExpression())) {

            addPage(page, "single", "Create Single Request Job", "post");
            addPage(page, "multiple", "Create Multi-Request Job", "post");
          }
          return page;
        }
      } else {
        HttpServletUtils.setPathVariable("businessApplicationVersion",
          businessApplication.getVersion());
        businessAppBuilder.redirectPage("clientView");
        return null;
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
          + " v" + businessApplication.getVersion() + ")"));

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
      final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
      container.add(new ListElement(HtmlUtil.UL, HtmlUtil.LI,
        inputDataContentTypes.keySet()));

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
      final Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      container.add(new ListElement(HtmlUtil.UL, HtmlUtil.LI,
        resultDataContentTypes.keySet()));
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
      // TODO all data types
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
      } else if (Geometry.class.isAssignableFrom(dataType.getJavaClass())) {
        field = new TextAreaField(name, 60, 10, required);
      } else {
        int length = attribute.getLength();
        if (length == -1) {
          length = 70;
        }
        field = new TextField(name, length, defaultValue, required);
      }
    } else {
      field = new SelectField(name, defaultValue, required, allowedValues);
    }
    return field;
  }

  public Element getFormInstant(final BusinessApplication businessApplication) {
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

    addFieldRow(fields, requestMetaData, "resultDataContentType");

    addNotificationFields(fields);

    form.add(fields);
    form.add(new DivElementContainer("actionMenu", new SubmitField(
      "clientMultiple", "Create Job")));

    container.add(form);
    return container;
  }

  public Element getFormSingle(final BusinessApplication businessApplication) {
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
    addFieldRow(fields, requestMetaData, "resultDataContentType");
    addNotificationFields(fields);

    form.add(fields);
    form.add(new DivElementContainer("actionMenu", new SubmitField(
      "createSingle", "Create Job")));

    container.add(form);
    return container;
  }

  private List<Map<String, Object>> getRequestAttributeList(
    final BusinessApplication businessApplication) {
    final List<Map<String, Object>> parameters = new ArrayList<Map<String, Object>>();
    final boolean perRequestInputData = businessApplication.isPerRequestInputData();

    addParameter(
      parameters,
      "inputDataContentType",
      "string",
      "The MIME type of the input data specified by an inputData or inputDataUrl parameter.",
      true, false, perRequestInputData, Collections.emptyList());

    addParameter(parameters, "inputData", "file",
      "The multi-part file containing the input data.", true, false,
      perRequestInputData, Collections.emptyList());

    addParameter(parameters, "inputDataUrl", "string",
      "The http: URL to the file or resource containing input data.", true,
      false, perRequestInputData, Collections.emptyList());

    final DataObjectMetaData requestMetaData = businessApplication.getRequestMetaData();
    for (final Attribute attribute : requestMetaData.getAttributes()) {
      addParameter(parameters, attribute, perRequestInputData);
    }

    addParameter(parameters, "notificationEmail", "string",
      "The email address to send the job status to when the job is completed.",
      true, false, perRequestInputData, Collections.emptyList());

    addParameter(
      parameters,
      "notificationUrl",
      "string",
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
      resultAttributes.add(resultAttribute);
    }
    return resultAttributes;
  }

  /**
   * <p>
   * This resource is the root of the CPF web service resources.
   * <p>
   * <h5>Web Browser HTML</h5>
   * <p>
   * A HTML page containing two tabs; he user's batch jobs and authorized
   * business applications.
   * </p>
   * <h5>REST API</h5>
   * <p>
   * A <a href="../resourceDescription.html">Resource Description</a> of this
   * resource with resource links to the following resources.
   * </p>
   * <ul>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplications"
   * >business applications</a></li>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersResources"
   * >user</a></li>
   * </ul>
   * 
   * @return The resource.
   * @web.response.code 200
   * <p>
   * The resource will be returned in the body of the HTTP response in the
   * requested format.
   * </p>
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
      final String consumerKey = getConsumerKey();
      final PageInfo page = createRootPageInfo("Cloud Processing Framework");
      addPage(page, "users/" + consumerKey, "User " + consumerKey);
      addPage(page, "apps", "Business Applications");
      return page;
    }
  }

  /**
   * <p>
   * The users is a containing resource for the CPF users. This resource does
   * not display a list of the users as a user can only access their own
   * resources.
   * </p>
   * <h5>Web Browser HTML</h5>
   * <p>
   * Redirects to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource.
   * </p>
   * <h5>REST API</h5>
   * <p>
   * Redirects to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersResources"
   * >user</a> resource.
   * </p>
   * 
   * @web.response.status 302
   * <p>
   * In a web browser redirect to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource.
   * </p>
   * @web.response.status 302
   * <p>
   * Using the REST API redirect to the <a
   * href="#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUser"
   * >user</a> resource.
   * </p>
   */
  @RequestMapping(value = {
    "/ws/users"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void getUsers() {
    if (MediaTypeUtil.isHtmlPage()) {
      final String url = HttpServletUtils.getFullUrl("/ws/");
      HttpServletUtils.sendRedirect(url);
    } else {
      final String consumerKey = getConsumerKey();
      sendRedirectWithExtension("/" + consumerKey);
    }
  }

  /**
   * <p>
   * The list of the business applications for a user.
   * <p>
   * <h5>Web Browser HTML</h5>
   * <p>
   * Redirects to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource displaying the Business Applications tab.
   * </p>
   * <h5>REST API</h5>
   * <p>
   * A <a href="../resourceDescription.html">Resource Description</a> of this
   * resource with resource links to the following resources.
   * </p>
   * <ul>
   * <li>Resource link for each <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersBusinessApplicationsResources"
   * >business application</a></li>
   * </ul>
   * 
   * @return The resource.
   * @web.response.code 200
   * <p>
   * The resource will be returned in the body of the HTTP response in the
   * requested format.
   * </p>
   * @web.response.status 302
   * <p>
   * In a web browser redirect to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource business application tab.
   * </p>
   */
  @RequestMapping(value = {
    "/ws/users/{consumerKey}/apps"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public PageInfo getUsersBusinessApplications(
    @PathVariable final String consumerKey) {
    if (MediaTypeUtil.isHtmlPage()) {
      final String url = HttpServletUtils.getFullUrl("/ws/#businessApplication_clientList");
      return HttpServletUtils.sendRedirect(url);
    } else {
      final PageInfo page = createRootPageInfo("Business Applications");
      for (final BusinessApplication businessApplication : getAuthorizedBusinessApplications()) {
        final String name = businessApplication.getName();
        final String title = businessApplication.getTitle();
        addPage(page, name, title);
      }
      return page;
    }
  }

  /**
   * <p>
   * The list of the batch jobs for a user.
   * <p>
   * <h5>Web Browser HTML</h5>
   * <p>
   * Redirects to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource displaying the Batch Jobs tab.
   * </p>
   * <h5>REST API</h5>
   * <p>
   * A <a href="../resourceDescription.html">Resource Description</a> of this
   * resource with resource links to the following resources.
   * </p>
   * <ul>
   * <li>Resource link for each <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersBatchJobsInfo"
   * >batch job</a></li>
   * </ul>
   * 
   * @return The resource.
   * @web.response.code 200
   * <p>
   * The resource will be returned in the body of the HTTP response in the
   * requested format.
   * </p>
   * @web.response.status 302
   * <p>
   * In a web browser redirect to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource Batch Jobs tab.
   * </p>
   */
  @RequestMapping(value = {
    "/ws/users/{consumerKey}/apps/{businessApplicationName}/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getUsersBusinessApplicationsJobs(
    @PathVariable final String businessApplicationName,
    @PathVariable final String consumerKey) {
    final BusinessApplication businessApplication = batchJobUiBuilder.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new PageNotFoundException("Business application "
        + businessApplicationName + " does not exist.");
    } else {
      CloudProcessingFramework.checkPermission(businessApplication);
      if (HtmlUiBuilder.isDataTableCallback()) {
        final Map<String, Object> parameters = new HashMap<String, Object>();

        final Map<String, Object> filter = new HashMap<String, Object>();
        filter.put(BatchJob.USER_ID, consumerKey);
        filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
        parameters.put("filter", filter);

        return batchJobUiBuilder.createDataTableMap("clientAppList", parameters);
      } else if (MediaTypeUtil.isHtmlPage()) {
        HttpServletUtils.setPathVariable("businessApplicationVersion",
          businessApplication.getVersion());
        batchJobUiBuilder.redirectToTab(BusinessApplication.class,
          "clientView", "clientAppList");
        return null;
      } else {
        final String title = businessApplication.getTitle();
        final PageInfo page = createRootPageInfo(title + " Batch Jobs for "
          + consumerKey);
        final List<DataObject> batchJobs = dataAccessObject.getBatchJobsForUserAndApplication(
          consumerKey, businessApplicationName);
        for (final DataObject job : batchJobs) {
          addBatchJobStatusLink(page, consumerKey, job);
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
   * <p>
   * The resources available to a user for a business application.
   * <p>
   * <h5>Web Browser HTML</h5>
   * <p>
   * Redirects to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsResources"
   * >business application</a> resource.
   * </p>
   * <h5>REST API</h5>
   * <p>
   * A <a href="../resourceDescription.html">Resource Description</a> of this
   * resource with resource links to the following resources.
   * </p>
   * <ul>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsInstant"
   * >instant</a></li>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsSingle"
   * >create single request job</a></li>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsMultiple"
   * >create multi request jobs</a></li>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersBusinessApplicationsJobs"
   * >batch jobs</a></li>
   * </ul>
   * 
   * @return The resource.
   * @web.response.code 200
   * <p>
   * The resource will be returned in the body of the HTTP response in the
   * requested format.
   * </p>
   * @web.response.status 302
   * <p>
   * In a web browser redirect to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getBusinessApplicationsResources"
   * >business application</a> resource overview tab.
   * </p>
   */
  @RequestMapping(value = {
    "/ws/users/{consumerKey}/apps/{businessApplicationName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public PageInfo getUsersBusinessApplicationsResources(
    @PathVariable final String businessApplicationName,
    @PathVariable final String consumerKey) {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new PageNotFoundException("Business application "
        + businessApplicationName + " does not exist.");
    } else if (MediaTypeUtil.isHtmlPage()) {
      HttpServletUtils.setPathVariable("businessApplicationVersion",
        businessApplication.getVersion());
      businessAppBuilder.redirectPage("clientView");
      return null;
    } else {
      checkPermission(businessApplication);
      final String title = businessApplication.getTitle();
      final PageInfo page = createRootPageInfo(title);
      if (hasPermission(businessApplication.getInstantModeExpression())) {
        addPage(page, "../../../../instant", "Instant");
      }
      if (hasPermission(businessApplication.getBatchModeExpression())) {

        addPage(page, "../../../../single", "Create Single Request Job", "post");
        addPage(page, "../../../../multiple", "Create Multi-Request Job",
          "post");
      }
      addPage(page, "jobs", "Batch Jobs");
      return page;
    }
  }

  @RequestMapping(value = {
    "/ws/users/{consumerKey}/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getUsersJobs(@PathVariable final String consumerKey) {
    if (HtmlUiBuilder.isDataTableCallback()) {
      final Map<String, Object> parameters = new HashMap<String, Object>();

      final Map<String, Object> filter = new HashMap<String, Object>();
      filter.put(BatchJob.USER_ID, consumerKey);
      parameters.put("filter", filter);

      return batchJobUiBuilder.createDataTableMap("clientList", parameters);
    } else if (MediaTypeUtil.isHtmlPage()) {
      final String url = HttpServletUtils.getFullUrl("/ws/#batchJob_clientList");
      final HttpServletResponse response = HttpServletUtils.getResponse();
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("consumerKey", consumerKey);
      final PageInfo page = createRootPageInfo("Batch Jobs for " + consumerKey);
      final List<DataObject> batchJobs = dataAccessObject.getBatchJobsForUser(consumerKey);
      for (final DataObject job : batchJobs) {
        addBatchJobStatusLink(page, consumerKey, job);
      }
      return page;
    }
  }

  @RequestMapping(value = {
    "/ws/users/{consumerKey}/jobs/{batchJobId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getUsersJobsInfo(@PathVariable final String consumerKey,
    @PathVariable final long batchJobId) {
    final DataObject batchJob = dataAccessObject.getBatchJob(consumerKey,
      batchJobId);
    if (batchJob == null) {
      throw new PageNotFoundException("Batch Job " + batchJobId
        + " does not exist.");
    } else {
      if (MediaTypeUtil.isHtmlPage()) {
        final TabElementContainer tabs = new TabElementContainer();
        batchJobUiBuilder.addObjectViewPage(tabs, batchJob, "client");
        if (batchJob.getValue(BatchJob.COMPLETED_TIMESTAMP) != null) {
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

  @RequestMapping(value = {
    "/ws/users/{consumerKey}/jobs/{batchJobId}/results/{resultId}"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void getUsersJobsResult(@PathVariable final String consumerKey,
    @PathVariable final long batchJobId, @PathVariable final long resultId)
    throws IOException {
    final DataObject batchJob = dataAccessObject.getBatchJob(batchJobId);

    if (batchJob != null) {
      if (consumerKey.equals(batchJob.getValue(BatchJob.USER_ID))) {
        final DataObject batchJobResult = dataAccessObject.getBatchJobResult(resultId);
        if (EqualsRegistry.INSTANCE.equals(batchJobId,
          batchJobResult.getValue(BatchJobResult.BATCH_JOB_ID))) {
          if (!BatchJob.MARKED_FOR_DELETION.equals(batchJob.getValue(BatchJob.JOB_STATUS))) {
            dataAccessObject.setBatchJobStatus(batchJob,
              BatchJob.DOWNLOAD_INITIATED);
            if (batchJobResult.getValue(BatchJobResult.DOWNLOAD_TIMESTAMP) == null) {
              final Timestamp timestamp = new Timestamp(
                System.currentTimeMillis());
              batchJobResult.setValue(BatchJobResult.DOWNLOAD_TIMESTAMP,
                timestamp);
            }
          }
        }
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

  @RequestMapping(value = {
    "/ws/users/{consumerKey}/jobs/{batchJobId}/results"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object getUsersJobsResults(@PathVariable final String consumerKey,
    @PathVariable final long batchJobId) {
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
        parameters.put("consumerKey", consumerKey);
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
              resultPage.setAttribute("batchJobRequestSequenceNumber",
                batchJobResult.getValue(BatchJobResult.REQUEST_SEQUENCE_NUMBER));
            }
          }
        }
        return page;
      }
    }
  }

  /**
   * <p>
   * The list of the top-level resources for a user.
   * <p>
   * <h5>Web Browser HTML</h5>
   * <p>
   * Redirects to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource.
   * </p>
   * <h5>REST API</h5>
   * <p>
   * A <a href="../resourceDescription.html">Resource Description</a> of this
   * resource with resource links to the following resources.
   * </p>
   * <ul>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersJobs"
   * >batch jobs</a></li>
   * <li><a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getUsersBusinessApplications"
   * >business applications</a></li>
   * </ul>
   * 
   * @return The resource.
   * @web.response.code 200
   * <p>
   * The resource will be returned in the body of the HTTP response in the
   * requested format.
   * </p>
   * @web.response.status 302
   * <p>
   * In a web browser redirect to the <a href=
   * "#ca.bc.gov.open.cpf.api.web.rest.CloudProcessingFramework.getRoot"
   * >root</a> resource.
   * </p>
   */
  @RequestMapping(value = {
    "/ws/users/{consumerKey}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public PageInfo getUsersResources(@PathVariable final String consumerKey) {
    if (MediaTypeUtil.isHtmlPage()) {
      final String url = HttpServletUtils.getFullUrl("/ws/");
      return HttpServletUtils.sendRedirect(url);
    } else {
      final PageInfo page = createRootPageInfo("User " + consumerKey);
      page.setAttribute("userId", consumerKey);
      addPage(page, "jobs", "Batch Jobs for user " + consumerKey);
      addPage(page, "apps", "Business Applications for user " + consumerKey);
      HttpServletUtils.setAttribute("title", page.getTitle());
      return page;
    }
  }


  public Void sendRedirectWithExtension(final String path) {
    String url = MediaTypeUtil.getUrlWithExtension(path);
    final String callback = HttpServletUtils.getParameter("callback");
    if (StringUtils.hasText(callback)) {
      url = UrlUtil.getUrl(url, Collections.singletonMap("callback", callback));
    }
    HttpServletUtils.sendRedirect(url);
    return null;
  }

  private void setBusinessApplicationDescription(final PageInfo page,
    final BusinessApplication businessApplication, final String version) {
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
    page.setAttribute("businessApplicationVersion", version);
  }

}
