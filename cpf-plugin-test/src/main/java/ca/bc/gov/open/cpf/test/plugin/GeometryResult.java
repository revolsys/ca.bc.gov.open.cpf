package ca.bc.gov.open.cpf.test.plugin;

import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.jts.geom.Geometry;

public class GeometryResult {

  private final Geometry geometry;

  public GeometryResult(final Geometry geometry) {
    this.geometry = geometry;
  }

  @ResultAttribute(description = "The area of the geometry")
  public double getArea() {
    return geometry.getArea();
  }

  public Map<String, Object> getCustomizationProperties() {
    final Map<String, Object> properties = new HashMap<String, Object>();
    final String styleId = geometry.getGeometryType();
    properties.put("kmlStyleUrl", "http://gov.bc.ca/kmlStyle.kml#" + styleId);
    properties.put("kmlWriteNulls", true);
    return properties;
  }

  @ResultAttribute(description = "The simple geometry.")
  public Geometry getGeometry() {
    return geometry;
  }

  @ResultAttribute(description = "The length of the geometry")
  public double getLength() {
    return geometry.getLength();
  }

}
