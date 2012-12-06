package ca.bc.gov.open.cpf.plugin.samples;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.GeometryConfiguration;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.gis.cs.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;

@BusinessApplicationPlugin(
    name = "GeometryProjection",
    version = "1.0.0",
    instantModePermission = "permitAll")
public class GeometryProjection {

  private Geometry geometry;

  private int resultSrid = 0;

  private int resultNumAxis = 2;

  private double resultScaleZ = 0;

  private double resultScaleXy = 0;

  public void execute() {
    final GeometryFactory geometryFactory = GeometryFactory.getFactory(
      resultSrid, resultNumAxis, resultScaleXy, resultScaleZ);
    geometry = geometryFactory.createGeometry(geometry);
  }

  @ResultAttribute
  @GeometryConfiguration(
      srid = 3005,
      numAxis = 3,
      scaleFactorXy = 1000,
      scaleFactorZ = 1,
      validate = true,
      primaryGeometry = true)
  public Geometry getGeometry() {
    return geometry;
  }

  @Required
  @RequestParameter(description = "The geometry to project")
  public void setGeometry(final Geometry geometry) {
    this.geometry = geometry;
  }

  public void setResultNumAxis(final int resultNumAxis) {
    this.resultNumAxis = resultNumAxis;
  }

  public void setResultScaleXy(final double resultScaleXy) {
    this.resultScaleXy = resultScaleXy;
  }

  public void setResultScaleZ(final double resultScaleZ) {
    this.resultScaleZ = resultScaleZ;
  }

  public void setResultSrid(final int resultSrid) {
    this.resultSrid = resultSrid;
  }

}
