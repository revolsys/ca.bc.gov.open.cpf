package ca.bc.gov.bsoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.util.StringUtils;

import ca.bc.gov.geomark.client.api.GeomarkClient;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.srm.app.soe.api.SOEOverlayShape;
import ca.bc.gov.srm.app.soe.api.SOEProcessRequest;
import ca.bc.gov.srm.app.soe.api.SOEReportException;
import ca.bc.gov.srm.app.soe.api.SOEReportRequest;
import ca.bc.gov.srm.app.soe.api.SOEReportResults;

import com.revolsys.io.FileUtil;
import com.revolsys.spring.resource.ClassPathResource;

@BusinessApplicationPlugin(name = "bsoe", title = "Batch Spatial Overlay",
    description = "The Batch Spatial Overlay allows pre-defined spatial queries to be run against the warehouse.",
    resultDataContentTypes = {
      "text/html", "text/xml"
    }, perRequestResultData = true, numRequestsPerWorker = 2, maxConcurrentRequests = 10, instantModePermission = "denyAll", logLevel = "ERROR")
public class BatchSOEPlugin {

  private static final GeomarkClient GEOMARK_CLIENT = new GeomarkClient();

  private static final String XSLT = new ClassPathResource("/GeoMarkToSoe.xsl",
    BatchSOEPlugin.class).contentsAsString();

  private AppLog appLog;

  private Float geomarkAdjacencyBuffer;

  private String geomarkName;

  private String geomarkURL;

  private String reportDatasourceName;

  private String reportDefinitionURL;

  private String reportTitle;

  private OutputStream resultData = null;

  private SecurityService securityService;

  private String soeReturnType = "xml";

  private String soeServiceURL;

  public void execute() {
    final boolean canAccess = this.securityService.canAccessResource("ReportUrl",
      this.reportDefinitionURL, "view");

    if (canAccess) {
      final SOEReportRequest request = new SOEReportRequest();
      try {
        request.setOverlayEngineURL(this.soeServiceURL);
        request.setLayerListURL(this.reportDefinitionURL);
        request.setCollectionElement("FEATURECOLLECTION");
        request.setFeatureElement("FEATURE");
        request.setGeometryElement("GEOMETRY");
        request.setReturnType(this.soeReturnType);
        request.setDatabaseName(this.reportDatasourceName);
        request.setReportName(this.reportTitle);
        request.setReportTitle(this.reportTitle);

        final String gmlGeometry = getGeomarkGeometry();
        final SOEOverlayShape shape = new SOEOverlayShape("", this.geomarkName, gmlGeometry);
        shape.setAdjBuffer(String.valueOf(this.geomarkAdjacencyBuffer));

        request.addOverlayShape(shape);

        this.appLog.debug("Sending SOE request to " + request.getOverlayEngineURL().toString());
        final SOEReportResults results = SOEProcessRequest.sendRequest(request);
        this.appLog.debug("SOE Error Code Returned=" + results.getErrorCode());

        if (results.getErrorCode() == 0) {
          try {
            this.resultData.write(results.getEncodedMsgContents().getBytes());
          } catch (final IOException e) {
            throw new RuntimeException("Error reading file from SOE", e);
          }
        } else {
          throw new RuntimeException("Error occured when calling SOE: " + results.getErrorCode()
            + " - " + results.getMsgContents());
        }
      } catch (final SOEReportException e) {
        throw new RuntimeException("Error occured when calling SOE:" + e.getMessage(), e);
      }
    } else {
      throw new SecurityException(
        "User does not have authorization to use the report url: " + this.reportDefinitionURL);
    }
  }

  private String getGeomarkGeometry() {
    this.appLog.debug("Get geomark " + this.geomarkURL);
    final InputStream in = GEOMARK_CLIENT.getGeomarkPartsStream(this.geomarkURL,
      "application/gml+xml", 3005);
    try {

      final TransformerFactory transFact = TransformerFactory.newInstance();

      final Source xsltSource = new StreamSource(new StringReader(XSLT));
      final Transformer trans = transFact.newTransformer(xsltSource);

      final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      final Source xmlSource = new StreamSource(reader);

      final StringWriter writer = new StringWriter();
      final Result result = new StreamResult(writer);

      trans.transform(xmlSource, result);

      return writer.toString();
    } catch (final TransformerException e) {
      throw new RuntimeException("Unable to transform " + this.geomarkURL, e);
    } finally {
      FileUtil.closeSilent(in);
    }
  }

  public String getSoeServiceURL() {
    return this.soeServiceURL;
  }

  public void setAppLog(final AppLog appLog) {
    this.appLog = appLog;
  }

  @RequestParameter(index = 6, description = "A buffer to apply to the geometry", units = "metres")
  public void setGeomarkAdjacencyBuffer(final Float geomarkAdjacencyBuffer) {
    this.geomarkAdjacencyBuffer = geomarkAdjacencyBuffer;
  }

  @RequestParameter(index = 4,
      description = "A description of the geometry to display on the report")
  @Required
  public void setGeomarkName(final String geomarkName) {
    this.geomarkName = StringUtils.trimWhitespace(geomarkName);
  }

  @RequestParameter(index = 5,
      description = "The URL to the geomark info page for the geometry used to filter the report")
  @Required
  public void setGeomarkURL(final String geomarkURL) {
    this.geomarkURL = StringUtils.trimWhitespace(geomarkURL);
  }

  @RequestParameter(index = 2,
      description = "The name of the data store to run the report against (e.g. WAREHOUSE)")
  @Required
  public void setReportDatasourceName(final String reportDatasourceName) {
    this.reportDatasourceName = StringUtils.trimWhitespace(reportDatasourceName);
  }

  @RequestParameter(index = 3, description = "The URL to the report definition to run")
  @Required
  public void setReportDefinitionURL(final String reportDefinitionURL) {
    this.reportDefinitionURL = StringUtils.trimWhitespace(reportDefinitionURL);
  }

  @RequestParameter(index = 1, description = "The tile to be displayed on the top of the report")
  @Required
  public void setReportTitle(final String reportTitle) {
    this.reportTitle = StringUtils.trimWhitespace(reportTitle);
  }

  public void setResultData(final OutputStream resultData) {
    this.resultData = resultData;
  }

  public void setResultDataContentType(final String format) {
    if (format != null) {
      if (format.contains("html")) {
        this.soeReturnType = "html";
      } else if (format.contains("xml")) {
        this.soeReturnType = "xml";
      }
    }
  }

  public void setSecurityService(final SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setSoeServiceURL(final String soeURL) {
    this.soeServiceURL = StringUtils.trimWhitespace(soeURL);
  }

}
