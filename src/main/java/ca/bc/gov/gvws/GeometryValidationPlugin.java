package ca.bc.gov.gvws;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.gis.cs.GeometryFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.precision.MinimumClearance;

/**
 * The GeometryValidationPlugin converts the geometry to the requested
 * coordinate system and precision model. It then checks to see if the geometry
 * {@link Geometry#isSimple()}, {@link Geometry#isValid()}, and calculates the
 * {@link MinimumClearance#getDistance()}.
 * 
 * @author Paul Austin <paul.austin@revolsys.com>
 */
@BusinessApplicationPlugin(name = "GeometryValidation", version = "1.0.0", numRequestsPerWorker = 100, instantModeSupported = true)
public class GeometryValidationPlugin {
  /** The geometry to validate. */
  private Geometry geometry;

  /** The flag indicating if the converted geometry was simple. */
  private boolean simple;

  /**
   * The number of coordinate axis to use for the converted geometry (2 for x, 7
   * and 3 for x, y, z).
   */
  private int resultNumAxis = 2;

  /** The scale factor used to round the x, y coordinates. */
  private double resultScaleXy;

  /** The scale factor used to round the z coordinate. */
  private double resultScaleZ;

  /** The EPSG coordinate system id to convert the geometry to. */
  private int resultSrid;

  /** The flag indicating if the converted geometry was valid. */
  private boolean valid;

  /** The minimum clearance required for the geometry to still be valid. */
  private double minimumClearance;

  /** The flag indicating if the geometry minimum clearance is robust. */
  private boolean isRobust = true;

  /**
   * Perform the geometry conversion and geometry validation.
   */
  public void execute() {
    final GeometryFactory geometryFactory = GeometryFactory.getFactory(
      resultSrid, resultNumAxis, resultScaleXy, resultScaleZ);
    geometry = geometryFactory.createGeometry(geometry);

    valid = geometry.isValid();
    simple = geometry.isSimple();

    minimumClearance = MinimumClearance.getDistance(geometry);
    if (minimumClearance >= 0.001) {
      minimumClearance = Math.ceil(minimumClearance * 1000.0) / 1000.0;
    }

    if (minimumClearance < 1 / resultScaleXy) {
      isRobust = false;
    }

  }

  /**
   * Get the geometry to validate that has been converted to the required
   * coordinate system and precision model.
   * 
   * @return The geometry to validate that has been converted to the required
   *         coordinate system and precision model.
   */
  @ResultAttribute
  public Geometry getGeometry() {
    return geometry;
  }

  /**
   * Get the minimum clearance required for the geometry to still be valid.
   * 
   * @return The minimum clearance required for the geometry to still be valid.
   */
  @ResultAttribute
  public double getMinimumClearance() {
    return minimumClearance;
  }

  /**
   * Get the flag indicating if the geometry minimum clearance is robust.
   * 
   * @return The flag indicating if the geometry minimum clearance is robust.
   */
  @ResultAttribute
  public boolean isRobust() {
    return isRobust;
  }

  /**
   * Get the flag indicating if the converted geometry was simple.
   * 
   * @return The flag indicating if the converted geometry was simple.
   */
  @ResultAttribute
  public boolean isSimple() {
    return simple;
  }

  /**
   * Get the flag indicating if the converted geometry was valid.
   * 
   * @return The flag indicating if the converted geometry was valid.
   */
  @ResultAttribute
  public boolean isValid() {
    return valid;
  }

  /**
   * Set the geometry to validate.
   * 
   * @param geometry The geometry to validate.
   */
  @Required
  @RequestParameter
  public void setGeometry(final Geometry geometry) {
    this.geometry = geometry;
  }

  /**
   * Get the number of coordinate axis to use for the converted geometry (2 for
   * x, 7 and 3 for x, y, z).
   * 
   * @param resultNumAxis The number of coordinate axis to use for the converted
   *          geometry (2 for x, 7 and 3 for x, y, z).
   */
  public void setResultNumAxis(final int resultNumAxis) {
    this.resultNumAxis = resultNumAxis;
  }

  /**
   * Get the scale factor used to round the x, y coordinates.
   * 
   * @param resultScaleXy The scale factor used to round the x, y coordinates.
   */
  public void setResultScaleXy(final double resultScaleXy) {
    this.resultScaleXy = resultScaleXy;
  }

  /**
   * Get the scale factor used to round the z coordinates.
   * 
   * @param resultScaleZ The scale factor used to round the z coordinates.
   */
  public void setResultScaleZ(final double resultScaleZ) {
    this.resultScaleZ = resultScaleZ;
  }

  /**
   * Set the EPSG coordinate system id to convert the geometry to.
   * 
   * @param resultSrid The EPSG coordinate system id to convert the geometry to.
   */
  public void setResultSrid(final int resultSrid) {
    this.resultSrid = resultSrid;
  }

}
