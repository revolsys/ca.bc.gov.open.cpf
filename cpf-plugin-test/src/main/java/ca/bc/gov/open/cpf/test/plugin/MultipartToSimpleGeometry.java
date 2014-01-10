package ca.bc.gov.open.cpf.test.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultList;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;

import com.vividsolutions.jts.geom.Geometry;

@BusinessApplicationPlugin(
    numRequestsPerWorker = 100,
    instantModePermission = "permitAll",
    description = "Converts any multi-part geometries into multiple records each with a single-part geometry. Also returns the length and area of the geometry.")
public class MultipartToSimpleGeometry {

  private Geometry geometry;

  private final List<GeometryResult> results = new ArrayList<GeometryResult>();

  private AppLog appLog;

  public void execute() {
    this.appLog.debug("Start");
    for (int i = 0; i < geometry.getNumGeometries(); i++) {
      final Geometry part = geometry.getGeometryN(i);
      final GeometryResult result = new GeometryResult(part);
      results.add(result);
    }
    this.appLog.debug("End");
  }

  public Map<String, Object> getCustomizationProperties() {
    final Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("kmlSnippet", "Length: $[length]");
    return properties;
  }

  @ResultList
  public List<GeometryResult> getResults() {
    return results;
  }

  public void setAppLog(final AppLog appLog) {
    this.appLog = appLog;
  }

  @Required
  @RequestParameter(
      description = "The multi-part geometry to split into it's component parts.")
  public void setGeometry(final Geometry geometry) {
    this.geometry = geometry;
  }

}
