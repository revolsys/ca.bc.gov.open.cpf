package ca.bc.gov.open.cpf.api.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.util.UrlPathHelper;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobResult;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.web.builder.BatchJobUiBuilder;
import ca.bc.gov.open.cpf.api.web.builder.BusinessApplicationUiBuilder;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.api.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.api.log.ModuleLog;
import ca.bc.gov.open.cpf.plugin.api.module.Module;

import com.revolsys.gis.data.io.ListDataObjectReader;
import com.revolsys.gis.data.model.ArrayDataObject;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.DataObjectMetaDataImpl;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
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
import com.revolsys.ui.web.config.Page;
import com.revolsys.ui.web.controller.PathAliasController;
import com.revolsys.ui.web.rest.interceptor.MediaTypeUtil;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.ui.web.utils.MultipartFileResource;
import com.revolsys.util.CaseConverter;
import com.vividsolutions.jts.geom.Geometry;

@Controller
public class ClientWebService {

  private static final DateFormat DATETIME_FORMAT = DateFormat.getDateTimeInstance(
    DateFormat.DEFAULT, DateFormat.SHORT);

  public final PageInfo userAppJobsPage = new PageInfo(
    "{businessApplicationTitle} jobs for user {userId}");

  public final PageInfo userJobPage = new PageInfo("Job {batchJobId} Status");

  public final PageInfo userJobsPage = new PageInfo("Jobs for user {userId}");

  public static void checkPermission(
    final BusinessApplication businessApplication) {
    final EvaluationContext evaluationContext = getSecurityEvaluationContext();
    if (!hasPermission(businessApplication, evaluationContext)) {
      throw new AccessDeniedException("No permission for business application "
        + businessApplication.getName());
    }
  }

  public static PageInfo getPageInfo(final PageInfo pageInfo,
    final Map<String, Object> parameters) {
    final PageInfo page = pageInfo.clone();
    String title = page.getTitle();
    for (final Entry<String, ?> parameter : parameters.entrySet()) {
      final String name = "{" + parameter.getKey() + "}";
      final String value = parameter.getValue().toString();
      title = title.replace(name, value);
    }
    page.setTitle(title);
    page.setAttributes(parameters);
    return page;
  }

  public static EvaluationContext getSecurityEvaluationContext() {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    final MethodSecurityExpressionRoot root = new MethodSecurityExpressionRoot(
      authentication);
    final EvaluationContext evaluationContext = new StandardEvaluationContext(
      root);
    return evaluationContext;
  }

  public static boolean hasPermission(
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

  public static boolean hasPermission(final Expression expression) {
    final EvaluationContext evaluationContext = getSecurityEvaluationContext();
    final boolean permitted = ExpressionUtils.evaluateAsBoolean(expression,
      evaluationContext);
    return permitted;
  }

  public static boolean isHtmlPage(final HttpServletRequest request) {
    return MediaTypeUtil.isPreferedMediaType(request, MediaType.TEXT_HTML);
  }

  private final PageInfo appPage = new PageInfo("{businessApplicationTitle}");

  private final PageInfo appsPage = new PageInfo("Business Applications");

  private final PageInfo appVersionInstantPage = new PageInfo(
    "Submit a single instant response request to {businessApplicationTitle} v{businessApplicationVersion}",
    null, "get");

  private final PageInfo appVersionMultiplePage = new PageInfo(
    "Submit multiple requests to {businessApplicationTitle} v{businessApplicationVersion}",
    null, "post");

  private final PageInfo appVersionPage = new PageInfo(
    "{businessApplicationTitle} v{businessApplicationVersion}");

  private final PageInfo appVersionSinglePage = new PageInfo(
    "Submit a single request to {businessApplicationTitle} v{businessApplicationVersion}",
    null, "post");

  /** The batch job service used to interact with the database. */
  private BatchJobService batchJobService;

  private BatchJobUiBuilder batchJobUiBuilder;

  private BusinessApplicationUiBuilder businessAppBuilder;

  private CpfDataAccessObject dataAccessObject;

  private Map<String, RawContent> rawContent = new HashMap<String, RawContent>();

  private final PageInfo rootPage = new PageInfo("Web Services");

  private final UrlPathHelper urlPathHelper = new UrlPathHelper();

  private final PageInfo userAppPage = new PageInfo(
    "{businessApplicationTitle} for {userId}");

  private final PageInfo userAppsPage = new PageInfo(
    "Business applications for user {userId}");

  private final PageInfo userPage = new PageInfo("Resources for user {userId}");

  private final PageInfo usersPage = new PageInfo("Users");

  public ClientWebService() {
  }

  private void addAppVersionPage(final PageInfo appPage, final String version,
    final Map<String, Object> titleParameters) {
    titleParameters.put("businessApplicationVersion", version);
    final PageInfo appVersionPage = getPageInfo(this.appVersionPage,
      titleParameters);
    appPage.addPage(version + "/", appVersionPage);
  }

  public void addFieldRow(final ElementContainer fields,
    final Attribute attribute) {
    final Field field = getField(attribute);
    final String name = attribute.getName();
    final String label = CaseConverter.toCapitalizedWords(name);
    final String instructions = attribute.getDescription();
    TableHeadingDecorator.addRow(fields, field, label, instructions);
  }

  public void addFieldRow(final ElementContainer fields,
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

  public void addInputDataFields(final ElementContainer fields,
    final BusinessApplication businessApplication) {
    final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
    final String defaultInputType = BusinessApplication.getDefaultMimeType(inputDataContentTypes);
    final SelectField inputDataContentType = new SelectField(
      "inputDataContentType", defaultInputType, true, inputDataContentTypes);

    TableHeadingDecorator.addRow(
      fields,
      inputDataContentType,
      "Input Data Content Type",
      "The MIME type of the input data specified by an inputData or inputDataUrl parameter");

    final UrlField inputDataUrl = new UrlField("inputDataUrl", false);
    TableHeadingDecorator.addRow(fields, inputDataUrl, "Input Data URL",
      "The URL to the input data");

    final FileField inputData = new FileField("inputData", false);
    TableHeadingDecorator.addRow(fields, inputData, "Input Data",
      "The inline input data");
  }

  public void addMultiInputDataFields(final ElementContainer fields,
    final BusinessApplication businessApplication) {
    final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
    final String defaultInputType = BusinessApplication.getDefaultMimeType(inputDataContentTypes);
    final SelectField inputDataContentType = new SelectField(
      "inputDataContentType", defaultInputType, true, inputDataContentTypes);

    if (businessApplication.isPerRequestInputData()) {
      ElementContainer container = new ElementContainer();
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
    EmailAddressField emailField = new EmailAddressField("notificationEmail",
      false);
    TableHeadingDecorator.addRow(container, emailField, "Notification Email",
      "The email address to send the job status to when the job is completed.");
    UrlField urlField = new UrlField("notificationUrl", false);
    TableHeadingDecorator.addRow(
      container,
      urlField,
      "Notification URL",
      "The http: URL to be notified when the job is completed. A copy of the Job status will be posted to process running at this URL");
  }

  private void addRawContent(ElementContainer container, String resource) {
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

  public void checkPermission(final Expression expression,
    final String accessDeniedMessage) {
    final boolean permitted = hasPermission(expression);
    if (!permitted) {
      throw new AccessDeniedException(accessDeniedMessage);
    }
  }

  public DataObject createBatchJob(final HttpServletRequest request) {
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
        batchJob.setValue(BatchJob.STRUCTURED_INPUT_DATA_URL,
          inputDataUrls.get(0));
      } else {
        throw new HttpMessageNotReadableException(
          "inputDataUrl must only be specified onces");
      }
    }
    dataAccessObject.write(batchJob);
    batchJobService.preProcess(batchJobId);
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public PageInfo getApp(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("businessApplicationName") final String businessApplicationName)
    throws NoSuchRequestHandlingMethodException {

    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      checkPermission(businessApplication);
      final List<String> compatibleVersions = businessApplication.getCompatibleVersions();
      if (compatibleVersions.isEmpty() || isHtmlPage(request)) {
        HttpServletUtils.setPathVariable("businessApplicationVersion",
          businessApplication.getVersion());
        businessAppBuilder.redirectPage("clientView");

        return null;
      } else {

        final Map<String, Object> titleParameters = new HashMap<String, Object>();

        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);
        final PageInfo page = getPageInfo(this.appPage, titleParameters);
        setBusinessApplicationDescription(page, businessApplication);

        final String version = businessApplication.getVersion();
        addAppVersionPage(page, version, titleParameters);

        for (final String compatibleVersion : compatibleVersions) {
          addAppVersionPage(page, compatibleVersion, titleParameters);
        }

        request.setAttribute("title", page.getTitle());
        return page;
      }
    }
  }

  @RequestMapping(value = {
    "/ws/apps"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Object getApps(final HttpServletRequest request,
    final HttpServletResponse response) {
    final List<BusinessApplication> applications = getBusinessApplications();
    request.setAttribute("title", "Business Applications");
    if (HtmlUiBuilder.isDataTableCallback(request)) {
      List<BusinessApplication> businessApplications = getBusinessApplications();
      return businessAppBuilder.createDataTableMap(request,
        businessApplications, "clientList");
    } else if (isHtmlPage(request)) {
      String url = Page.getFullUrl("/ws/#businessApplication_clientList");
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final PageInfo appsPage = this.appsPage.clone();

      for (final BusinessApplication app : applications) {
        if (app.isEnabled()) {
          final String name = app.getName();
          final String title = app.getTitle();
          final String version = app.getVersion();
          final String path = name + "/" + version + "/";
          final Map<String, Object> titleParameters = new HashMap<String, Object>();
          titleParameters.put("businessApplicationTitle", title);
          titleParameters.put("businessApplicationVersion", version);
          final PageInfo appPage = getPageInfo(this.appVersionPage,
            titleParameters);
          appPage.setAttribute("businessApplicationName", name);
          appPage.setAttribute("businessApplicationVersion", version);
          appsPage.addPage(path, appPage);
        }
      }
      return appsPage;
    }
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Object getAppVersion(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("businessApplicationVersion") final String businessApplicationVersion)
    throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(
      businessApplicationName, businessApplicationVersion);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      checkPermission(businessApplication);
      final Map<String, Object> titleParameters = new HashMap<String, Object>();
      final String title = businessApplication.getTitle();
      titleParameters.put("businessApplicationTitle", title);
      titleParameters.put("businessApplicationVersion",
        businessApplicationVersion);
      if (businessApplication.isVersionSupported(businessApplicationVersion)) {
        request.setAttribute("title", businessApplication.getTitle());

        if (isHtmlPage(request)) {
          request.setAttribute("pageHeading", businessApplication.getTitle());
          ElementContainer container = new ElementContainer();
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
          final Element specification = getAppVersionSpecificationElement(
            request, businessApplication);
          tabs.add("description", "Overview", specification);

          if (hasPermission(businessApplication.getInstantModeExpression())) {
            final Element instantForm = getAppVersionSingleInstantForm(request,
              businessApplication);
            tabs.add("instant", "Instant", instantForm);
          }
          if (hasPermission(businessApplication.getBatchModeExpression())) {
            final Element singleForm = getAppVersionSingleForm(request,
              businessApplication);
            tabs.add("single", "Create Single Request Job", singleForm);

            final Element multipleForm = getAppVersionMultipleForm(request,
              businessApplication);
            tabs.add("multiple", "Create Multi-Request Job", multipleForm);

            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("serverSide", Boolean.TRUE);
            batchJobUiBuilder.addTabDataTable(tabs, BatchJob.BATCH_JOB,
              "clientAppList", parameters);
          }
          return container;
        } else {
          final PageInfo page = getPageInfo(this.appVersionPage,
            titleParameters);

          if (hasPermission(businessApplication.getInstantModeExpression())) {
            page.addPage("instant/",
              getPageInfo(appVersionInstantPage, titleParameters));
          }
          if (hasPermission(businessApplication.getBatchModeExpression())) {

            page.addPage("single/",
              getPageInfo(appVersionSinglePage, titleParameters));
            page.addPage("multiple/",
              getPageInfo(appVersionMultiplePage, titleParameters));
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

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/multiple"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public PageInfo getAppVersionMultiple(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("businessApplicationVersion") final String businessApplicationVersion)
    throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = getBusinessApplication(
      request, response, businessApplicationName, businessApplicationVersion,
      "clientMultiple");
    if (businessApplication == null) {
      return null;
    } else {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Map<String, Object> titleParameters = new HashMap<String, Object>();

      final String title = businessApplication.getTitle();
      titleParameters.put("businessApplicationTitle", title);
      titleParameters.put("businessApplicationVersion",
        businessApplicationVersion);
      final PageInfo page = getPageInfo(this.appVersionMultiplePage,
        titleParameters);
      setBusinessApplicationDescription(page, businessApplication);
      page.addInputContentType(MediaType.MULTIPART_FORM_DATA);

      page.setPagesElement(getAppVersionMultipleForm(request,
        businessApplication));

      request.setAttribute("title", page.getTitle());
      return page;
    }
  }

  public Element getAppVersionMultipleForm(final HttpServletRequest request,
    final BusinessApplication businessApplication)
    throws NoSuchRequestHandlingMethodException {
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

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/single"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public PageInfo getAppVersionSingle(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("businessApplicationVersion") final String businessApplicationVersion)
    throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = getBusinessApplication(
      request, response, businessApplicationName, businessApplicationVersion,
      "clientSingle");
    if (businessApplication == null) {
      return null;
    } else {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Map<String, Object> titleParameters = new HashMap<String, Object>();

      final String title = businessApplication.getTitle();
      titleParameters.put("businessApplicationTitle", title);
      titleParameters.put("businessApplicationVersion",
        businessApplicationVersion);
      final PageInfo page = getPageInfo(this.appVersionSinglePage,
        titleParameters);
      setBusinessApplicationDescription(page, businessApplication);

      page.setPagesElement(getAppVersionSingleForm(request, businessApplication));
      request.setAttribute("title", page.getTitle());
      return page;
    }
  }

  public Element getAppVersionSingleForm(final HttpServletRequest request,
    final BusinessApplication businessApplication) {
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

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/instant"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Object getAppVersionSingleInstant(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("businessApplicationVersion") final String businessApplicationVersion)
    throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = getBusinessApplication(
      request, response, businessApplicationName, businessApplicationVersion,
      "clientInstant");
    if (businessApplication == null) {
      return null;
    } else {
      final PluginAdaptor plugin = batchJobService.getBusinessApplicationPlugin(businessApplication);
      checkPermission(businessApplication.getInstantModeExpression(),
        "No instant mode permission for " + businessApplication.getName());
      final String format = request.getParameter("format");
      if (format == null) {
        final Map<String, Object> titleParameters = new HashMap<String, Object>();
        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);
        titleParameters.put("businessApplicationVersion",
          businessApplicationVersion);
        final PageInfo page = getPageInfo(this.appVersionInstantPage,
          titleParameters);
        setBusinessApplicationDescription(page, businessApplication);
        page.setPagesElement(getAppVersionSingleInstantForm(request,
          businessApplication));

        request.setAttribute("title", page.getTitle());
        return page;
      } else {
        final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
        final DataObject parameters = new ArrayDataObject(requestMetaData);
        for (final Attribute attribute : requestMetaData.getAttributes()) {
          final String name = attribute.getName();
           String value = request.getParameter(name);
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
                final String sridString = request.getParameter("srid");
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
        request.setAttribute("contentDispositionFileName",
          businessApplicationName);
        final DataObjectMetaData resultMetaData = businessApplication.getResultMetaData();
        final List<DataObject> results = DataObjectUtil.getObjects(
          resultMetaData, list);
        for (Entry<String, Object> entry : businessApplication.getProperties()
          .entrySet()) {
          String name = entry.getKey();
          Object value = entry.getValue();
          request.setAttribute(name, value);
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

  public Element getAppVersionSingleInstantForm(
    final HttpServletRequest request,
    final BusinessApplication businessApplication) {
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

  public Element getAppVersionSpecificationElement(
    final HttpServletRequest request,
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
      Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
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
      final RowsTableSerializer requestModel = new KeySerializerTableSerializer(
        serializers, requestAttributes);
      final TableView requestAttributesTable = new TableView(requestModel,
        "objectList resultAttributes");
      container.add(requestAttributesTable);

      addRawContent(container,
        "ca/bc/gov/open/cpf/api/web/service/resultFiles.html");
      Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
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

  @RequestMapping(value = {
    "/ws/authenticated"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Map<String, ? extends Object> getAuthenticated() {
    final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
      "Authenticated");
    map.put("authenticated", true);
    return map;
  }

  public BatchJobService getBatchJobService() {
    return batchJobService;
  }

  private BusinessApplication getBusinessApplication(
    final HttpServletRequest request, final HttpServletResponse response,
    final String businessApplicationName,
    final String businessApplicationVersion, String pageName)
    throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(
      businessApplicationName, businessApplicationVersion);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new NoSuchRequestHandlingMethodException(request);
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

  public List<BusinessApplication> getBusinessApplications() {
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

  public Field getField(final Attribute attribute) {
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

  public org.springframework.core.io.Resource getResource(
    final HttpServletRequest request, final String fieldName) {
    if (request instanceof MultipartHttpServletRequest) {
      final MultipartHttpServletRequest multiPartRequest = (MultipartHttpServletRequest)request;
      final MultipartFile file = multiPartRequest.getFile(fieldName);
      if (file != null) {
        return new MultipartFileResource(file);
      }
    }
    final String value = request.getParameter(fieldName);
    if (StringUtils.hasText(value)) {
      return new ByteArrayResource(fieldName, value);
    } else {
      return null;
    }
  }

  @RequestMapping(value = {
    "/ws/", "/ws/index"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Object getRoot(final HttpServletRequest request) {
    if (isHtmlPage(request)) {
      request.setAttribute("title", "Cloud Processing Framework");

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
      final PageInfo rootPage = this.rootPage.clone();
      String url = HttpServletUtils.getFullRequestUrl(request);
      url = url.replaceAll("/index([^/]+/?)?$", "/");
      rootPage.setUrl(url);

      return rootPage;
    }
  }

  @RequestMapping(value = {
    "/ws/users/{userId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public PageInfo getUser(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("userId") final String userId) {
    if (isHtmlPage(request)) {
      String url = Page.getFullUrl("/ws/");
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("userId", userId);
      final PageInfo userPage = getPageInfo(this.userPage, parameters);
      final PageInfo userJobsPage = getPageInfo(this.userJobsPage, parameters);
      final PageInfo userAppsPage = getPageInfo(this.userAppsPage, parameters);
      userPage.addPage("jobs/", userJobsPage);
      userPage.addPage("apps/", userAppsPage);
      request.setAttribute("title", userPage.getTitle());
      return userPage;
    }
  }

  @RequestMapping(value = {
    "/ws/users/{userId}/apps/{businessApplicationName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public PageInfo getUserApp(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("userId") final String userId)
    throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = batchJobService.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else if (isHtmlPage(request)) {
      HttpServletUtils.setPathVariable("businessApplicationVersion",
        businessApplication.getVersion());
      businessAppBuilder.redirectPage("clientView");
      return null;
    } else {
      checkPermission(businessApplication);
      final Map<String, Object> titleParameters = new HashMap<String, Object>();
      titleParameters.put("userId", userId);

      final String title = businessApplication.getTitle();
      titleParameters.put("businessApplicationTitle", title);
      final PageInfo userAppPage = getPageInfo(this.userAppPage,
        titleParameters);
      final PageInfo userAppJobsPage = getPageInfo(this.userAppJobsPage,
        titleParameters);
      userAppPage.addPage("jobs/", userAppJobsPage);
      request.setAttribute("title", userAppPage.getTitle());
      return userAppPage;
    }
  }

  @RequestMapping(value = {
    "/ws/users/{userId}/apps/{businessApplicationName}/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Object getUserAppJobs(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("userId") final String userId)
    throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = batchJobUiBuilder.getBusinessApplication(businessApplicationName);
    if (businessApplication == null || !businessApplication.isEnabled()) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      ClientWebService.checkPermission(businessApplication);
      if (HtmlUiBuilder.isDataTableCallback(request)) {

        final Map<String, Object> parameters = new HashMap<String, Object>();

        final Map<String, Object> filter = new HashMap<String, Object>();
        filter.put(BatchJob.USER_ID, userId);
        filter.put(BatchJob.BUSINESS_APPLICATION_NAME, businessApplicationName);
        parameters.put("filter", filter);

        return batchJobUiBuilder.createDataTableMap(request, "clientAppList",
          parameters);
      } else if (isHtmlPage(request)) {
        HttpServletUtils.setPathVariable("businessApplicationVersion",
          businessApplication.getVersion());
        batchJobUiBuilder.redirectToTab(BusinessApplication.class,
          "clientView", "clientAppList");
        return null;
      } else {
        final Map<String, Object> titleParameters = new HashMap<String, Object>();
        titleParameters.put("userId", userId);

        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);
        final PageInfo page = ClientWebService.getPageInfo(userAppJobsPage,
          titleParameters);
        final List<DataObject> batchJobs = dataAccessObject.getBatchJobsForUserAndApplication(
          userId, businessApplicationName);
        for (final DataObject job : batchJobs) {
          final Number batchJobId = job.getIdValue();
          titleParameters.put("batchJobId", batchJobId);
          String url = urlPathHelper.getOriginatingContextPath(request);
          url += "/ws/users/" + userId + "/jobs/" + batchJobId + "/";
          page.addPage(url,
            ClientWebService.getPageInfo(userJobPage, titleParameters));
        }
        request.setAttribute("title", page.getTitle());
        final Object table = batchJobUiBuilder.createDataTableHandler(request,
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

  @RequestMapping(value = {
    "/ws/users/{userId}/apps"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public PageInfo getUserApps(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("userId") final String userId) {
    if (isHtmlPage(request)) {
      String url = Page.getFullUrl("/ws/#businessApplication_clientList");
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final Map<String, Object> titleParameters = new HashMap<String, Object>();
      titleParameters.put("userId", userId);
      final PageInfo userAppsPage = getPageInfo(this.userAppsPage,
        titleParameters);
      for (final BusinessApplication businessApplication : getBusinessApplications()) {
        final String name = businessApplication.getName();
        final String path = name + "/";

        final String title = businessApplication.getTitle();
        titleParameters.put("businessApplicationTitle", title);
        final PageInfo appPage = getPageInfo(this.userAppPage, titleParameters);
        userAppsPage.addPage(path, appPage);
      }
      request.setAttribute("title", userAppsPage.getTitle());
      return userAppsPage;
    }
  }

  @RequestMapping(value = {
    "/ws/users/{userId}/jobs/{batchJobId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Object getUserJob(final HttpServletRequest request,
    @PathVariable("userId") final String userId,
    @PathVariable("batchJobId") final long batchJobId)
    throws NoSuchRequestHandlingMethodException {
    final DataObject batchJob = dataAccessObject.getBatchJob(userId, batchJobId);
    if (batchJob == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      if (isHtmlPage(request)) {
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
        String url = batchJobUiBuilder.getPageUrl("clientView");
        final Map<String, Object> batchJobMap = BatchJobService.toMap(batchJob,
          url, batchJobUiBuilder.getTimeUntilNextCheck(batchJob));
        return batchJobMap;
      }
    }
  }

  @RequestMapping(value = {
    "/ws/users/{userId}/jobs/{batchJobId}"
  }, method = RequestMethod.DELETE)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteUserJob(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("userId") final String userId,
    @PathVariable("batchJobId") final long batchJobId)
    throws NoSuchRequestHandlingMethodException {
    final DataObject batchJob = dataAccessObject.getBatchJob(userId, batchJobId);
    if (batchJob == null) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      if (dataAccessObject.deleteBatchJob(batchJobId) == 0) {
        throw new RuntimeException("Unable to delete job " + batchJobId);
      } else {
        response.setStatus(HttpServletResponse.SC_OK);
      }
      // TODO remove from scheduler
    }
  }

  @RequestMapping(value = {
    "/ws/users/{userId}/jobs"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public Object getUserJobs(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable("userId") final String userId) {

    if (HtmlUiBuilder.isDataTableCallback(request)) {
      final Map<String, Object> parameters = new HashMap<String, Object>();

      final Map<String, Object> filter = new HashMap<String, Object>();
      filter.put(BatchJob.USER_ID, userId);
      parameters.put("filter", filter);

      return batchJobUiBuilder.createDataTableMap(request, "clientList",
        parameters);
    } else if (isHtmlPage(request)) {
      String url = Page.getFullUrl("/ws/#batchJob_clientList");
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("userId", userId);
      final PageInfo page = ClientWebService.getPageInfo(userJobsPage,
        parameters);
      final List<DataObject> batchJobs = dataAccessObject.getBatchJobsForUser(userId);
      for (final DataObject job : batchJobs) {
        final Map<String, Object> jobParameters = new HashMap<String, Object>();
        jobParameters.put("userId", userId);
        final Number batchJobId = job.getIdValue();
        jobParameters.put("batchJobId", batchJobId);
        jobParameters.put("jobStatus", job.getValue(BatchJob.JOB_STATUS));
        final Timestamp timestamp = job.getValue(BatchJob.WHEN_CREATED);
        jobParameters.put("creationTimestamp",
          DATETIME_FORMAT.format(timestamp));
        final PageInfo jobPageInfo = ClientWebService.getPageInfo(userJobPage,
          jobParameters);
        page.addPage(batchJobId + "/", jobPageInfo);
      }
      return page;
    }
  }

  @RequestMapping(value = {
    "/ws/users"
  }, method = RequestMethod.GET)
  @ResponseBody
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public PageInfo getUsers(final HttpServletRequest request,
    HttpServletResponse response) {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    final String userId = authentication.getName();
    if (isHtmlPage(request)) {
      String url = Page.getFullUrl("/ws/");
      response.setStatus(HttpServletResponse.SC_SEE_OTHER);
      response.setHeader("Location", url);
      return null;
    } else {
      final PageInfo usersPage = this.usersPage.clone();
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("userId", userId);
      final PageInfo userPage = getPageInfo(this.userPage, parameters);
      usersPage.addPage(userId + "/", userPage);
      return usersPage;
    }
  }

  @PostConstruct
  public void init() {
    rootPage.addPage("users/", usersPage);
    rootPage.addPage("apps/", appsPage);
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/multiple"
  }, method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postAppVersionMultiple(final HttpServletRequest request,
    final HttpServletResponse response,
    @RequestHeader("Content-Type") final MediaType mediaType,
    @PathVariable final String businessApplicationName,
    @PathVariable final String businessApplicationVersion)
    throws NoSuchRequestHandlingMethodException, IOException {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    final BusinessApplication businessApplication = getBusinessApplication(
      request, response, businessApplicationName, businessApplicationVersion,
      "clientMultiple");
    if (businessApplication != null) {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplication.getName());
      final Module module = businessApplication.getModule();
      final String moduleName = module.getName();
      final SecurityContext securityContext = SecurityContextHolder.getContext();
      final Authentication authentication = securityContext.getAuthentication();

      final String userId = authentication.getName();

      final Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      final String defaultResultDataContentType = resultDataContentTypes.get(0);
      final Map<String, String> suppportedInputDataContentTypes = businessApplication.getInputDataContentTypes();
      final String defaultInputDataContentType = suppportedInputDataContentTypes.get(0);

      if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)) {
        final Map<String, String> businessApplicationParameters = new HashMap<String, String>();
        final Long batchJobId = dataAccessObject.createId(BatchJob.BATCH_JOB);

        final Map<String, Object> logData = new LinkedHashMap<String, Object>();
        logData.put("batchJobId", batchJobId);
        logData.put("businessApplicationName", businessApplicationName);
        if (businessApplication.isInfoLogEnabled()) {
          ModuleLog.info(moduleName, "Job submit multiple", "Start", logData);
        }

        final DataObject batchJob = createBatchJob(request);
        batchJob.setIdValue(batchJobId);
        batchJob.setValue(BatchJob.BUSINESS_APPLICATION_NAME,
          businessApplicationName);
        batchJob.setValue(BatchJob.BUSINESS_APPLICATION_VERSION,
          businessApplicationVersion);
        batchJob.setValue(BatchJob.USER_ID, userId);
        batchJob.setValue(BatchJob.NUM_SUBMITTED_REQUESTS, 0);

        batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE,
          defaultResultDataContentType);
        String inputDataContentType = defaultInputDataContentType;

        final String[] inputDataContentTypes = request.getParameterValues("inputDataContentType");
        if (inputDataContentTypes == null) {
        } else {
          for (final String contentType : inputDataContentTypes) {
            if (contentType != null) {
              if (suppportedInputDataContentTypes.containsKey(contentType)) {
                inputDataContentType = contentType;
              } else {
                throw new HttpMessageNotReadableException(
                  "inputDataContentType=" + inputDataContentType
                    + " is not supported.");
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
        final String resultDataContentType = request.getParameter("resultDataContentType");
        if (resultDataContentType != null) {
          if (resultDataContentTypes.containsKey(resultDataContentType)) {
            batchJob.setValue(BatchJob.RESULT_DATA_CONTENT_TYPE,
              resultDataContentType);
          } else {
            throw new HttpMessageNotReadableException("resultDataContentType="
              + resultDataContentType + " is not supported.");
          }
        }

        String notificationUrl = request.getParameter("notificationUrl");
        final String notificationEmail = request.getParameter("notificationEmail");
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
          String value = request.getParameter(parameterName);
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

          final MultipartHttpServletRequest multiPartRequest = (MultipartHttpServletRequest)request;
          final List<MultipartFile> inputDataFiles = multiPartRequest.getFiles("inputData");
          final String[] inputDataUrlsArray = request.getParameterValues("inputDataUrl");
          final List<String> inputDataUrls = new ArrayList<String>();
          if (inputDataUrlsArray != null) {
            for (final String inputDataUrl : inputDataUrlsArray) {
              if (inputDataUrl != null && inputDataUrl.trim().length() > 0) {
                inputDataUrls.add(inputDataUrl);
              }
            }
          }
          for (final Iterator<MultipartFile> iterator = inputDataFiles.iterator(); iterator.hasNext();) {
            final MultipartFile multipartFile = iterator.next();
            if (multipartFile.getSize() == 0) {
              iterator.remove();
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
                  ++requestSequenceNumber, inputDataContentType, inputDataUrl);
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
        HttpServletUtils.setPathVariable("userId", userId);
        HttpServletUtils.setPathVariable("batchJobId", batchJobId);
        batchJobUiBuilder.redirectPage("clientView");

        if (businessApplication.isInfoLogEnabled()) {
          ModuleLog.info(moduleName, "Job submit multiple", "End multiple",
            stopWatch, logData);
        }
        Map<String, Object> statistics = new HashMap<String, Object>();
        statistics.put("submittedJobsTime", stopWatch);
        statistics.put("submittedJobsCount", 1);
        InvokeMethodAfterCommit.invoke(batchJobService, "addStatistics",
          businessApplication, statistics);
      } else {
        response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
          "Content-type must be multipart/form-data");
      }
    }
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}/single"
  }, method = RequestMethod.POST)
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void postAppVersionSingle(
    final HttpServletRequest request,
    final HttpServletResponse response,
    @RequestHeader("Content-Type") final MediaType mediaType,
    @PathVariable("businessApplicationName") final String businessApplicationName,
    @PathVariable("businessApplicationVersion") final String businessApplicationVersion)
    throws NoSuchRequestHandlingMethodException, IOException {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    final BusinessApplication businessApplication = getBusinessApplication(
      request, response, businessApplicationName, businessApplicationVersion,
      "clientSingle");
    if (businessApplication != null) {
      checkPermission(businessApplication.getBatchModeExpression(),
        "No batch mode permission for " + businessApplicationName);
      final Module module = businessApplication.getModule();
      final String moduleName = module.getName();

      final Long batchJobId = dataAccessObject.createId(BatchJob.BATCH_JOB);
      final DataObject batchJob = createBatchJob(request);
      batchJob.setIdValue(batchJobId);

      final Map<String, Object> logData = new LinkedHashMap<String, Object>();
      logData.put("batchJobId", batchJobId);
      logData.put("businessApplicationName", businessApplicationName);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.info(moduleName, "Job submit single", "Start", logData);
      }
      final SecurityContext securityContext = SecurityContextHolder.getContext();
      final Authentication authentication = securityContext.getAuthentication();

      final String userId = authentication.getName();

      final Map<String, String> resultDataContentTypes = businessApplication.getResultDataContentTypes();
      final String defaultResultDataContentType = resultDataContentTypes.get(0);
      final Map<String, String> inputDataContentTypes = businessApplication.getInputDataContentTypes();
      final String defaultInputDataContentType = inputDataContentTypes.get(0);

      final Map<String, String> businessApplicationParameters = new HashMap<String, String>();

      String inputDataContentType = request.getParameter("inputDataContentType");
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
        inputDataIn = getResource(request, "inputData");
        inputDataUrl = request.getParameter("inputDataUrl");
        final boolean hasInputDataIn = inputDataIn != null;
        final boolean hasInputDataUrl = StringUtils.hasText(inputDataUrl);
        if (hasInputDataIn == hasInputDataUrl) {
          throw new HttpMessageNotReadableException(
            "Either an inputData file or inputDataUrl parameter must be specified, but not both");
        }
      }
      String resultDataContentType = request.getParameter("resultDataContentType");
      if (!StringUtils.hasText(resultDataContentType)) {
        resultDataContentType = defaultResultDataContentType;
      }
      if (!resultDataContentTypes.containsKey(resultDataContentType)) {
        throw new HttpMessageNotReadableException("resultDataContentType="
          + resultDataContentType + " is not supported.");
      }
      String notificationUrl = request.getParameter("notificationUrl");
      final String notificationEmail = request.getParameter("notificationEmail");
      if (StringUtils.hasText(notificationEmail)) {
        if (StringUtils.hasText(notificationUrl)) {
          throw new HttpMessageNotReadableException(
            "Both notificationUrl and notificationEmail cannot be specified. Enter a value in one or the other but not both.");
        } else {
          notificationUrl = "mailto:" + notificationEmail;
        }
      }
      final String srid = request.getParameter("srid");
      final DataObjectMetaDataImpl requestMetaData = businessApplication.getRequestMetaData();
      final DataObject inputData = new ArrayDataObject(requestMetaData);
      for (final Attribute attribute : requestMetaData.getAttributes()) {
        final String parameterName = attribute.getName();
        String value = request.getParameter(parameterName);
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
      batchJob.setValue(BatchJob.USER_ID, userId);
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

      HttpServletUtils.setPathVariable("userId", userId);
      HttpServletUtils.setPathVariable("batchJobId", batchJobId);
      batchJobUiBuilder.redirectPage("clientView");

      logData.put("batchJobId", batchJobId);
      if (businessApplication.isInfoLogEnabled()) {
        ModuleLog.infoAfterCommit(moduleName, "Job submit single", "End",
          stopWatch, logData);
      }
      Map<String, Object> statistics = new HashMap<String, Object>();
      statistics.put("submittedJobsTime", stopWatch);
      statistics.put("submittedJobsCount", 1);

      statistics.put("preProcessScheduledJobsCount", 1);
      statistics.put("preProcessScheduledJobsTime", 0);

      statistics.put("preProcessedTime", 0);
      statistics.put("preProcessedJobsCount", 1);
      statistics.put("preProcessedRequestsCount", 1);

      InvokeMethodAfterCommit.invoke(batchJobService, "addStatistics",
        businessApplication, statistics);
    }
  }

  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }

  @Resource(name = "/CPF/CPF_BATCH_JOBS-htmlbuilder")
  public void setBatchJobUiBuilder(final BatchJobUiBuilder batchJobUiBuilder) {
    this.batchJobUiBuilder = batchJobUiBuilder;
  }

  @Resource(
      name = "ca.bc.gov.open.cpf.plugin.api.BusinessApplication-htmlbuilder")
  public void setBusinessAppBuilder(
    final BusinessApplicationUiBuilder businessAppBuilder) {
    this.businessAppBuilder = businessAppBuilder;
  }

  public void setBusinessApplicationDescription(final PageInfo page,
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
  }

  @Resource(name = "cpfDataAccessObject")
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
  }

}
