package ca.bc.gov.open.cpf.plugin.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.revolsys.gis.model.coordinates.Coordinates;
import com.revolsys.gis.model.coordinates.list.CoordinatesList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * <p>The CPF provides an extended version of the <a href=http://tsusiatsoftware.net/jts/main.html"/>Java Topology Suite (JTS)</a> GeometryFactory to create JTS geometries. The
 * extended version includes support for coordinate system projection, precision model, and controls on the number of axis.</p>
 *
 * <p>The <code>GeometryFactory</code> does not provide a public constructor. <code>GeometryFactory</code> instances can
 * be obtained using the <code>getFactory</code> static methods described below.
 */
@SuppressWarnings("serial")
public class GeometryFactory extends com.revolsys.gis.cs.GeometryFactory {
  /** The cached geometry factories. */
  private static Map<String, GeometryFactory> factories = new HashMap<String, GeometryFactory>();

  /**
   * <p>Get a GeometryFactory with no coordinate system, 3D axis (x, y &amp; z) and a floating precision model.</p>
   * 
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory() {
    return getFactory(0, 3, 0, 0);
  }

  /**
   * <p>Get a GeometryFactory with no coordinate system, 3D axis (x, y &amp; z) and a fixed x, y & floating z precision models.</p>
   * 
   * @param scaleXy The scale factor used to round the x, y coordinates. The precision is 1 / scaleXy.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final double scaleXy) {
    return getFactory(0, 3, scaleXy, 0);
  }

  /**
   * Get the geometry factory used for an existing geometry.
   * 
   * @param geometry The geometry to get the factory from.
   * @return The geometry factory;
   */
  public static GeometryFactory getFactory(final Geometry geometry) {
    if (geometry == null) {
      return getFactory(0, 3, 0, 0);
    } else {
      final com.vividsolutions.jts.geom.GeometryFactory factory = geometry.getFactory();
      if (factory instanceof GeometryFactory) {
        return (GeometryFactory)factory;
      } else if (factory instanceof com.revolsys.gis.cs.GeometryFactory) {
        final com.revolsys.gis.cs.GeometryFactory rsGeometryFactory = (com.revolsys.gis.cs.GeometryFactory)factory;
        final int srid = rsGeometryFactory.getSRID();
        final int numAxis = rsGeometryFactory.getNumAxis();
        final double scaleXY = rsGeometryFactory.getScaleXY();
        final double scaleZ = rsGeometryFactory.getScaleZ();
        return getFactory(srid, numAxis, scaleXY, scaleZ);
      } else {
        final int crsId = geometry.getSRID();
        final PrecisionModel precisionModel = factory.getPrecisionModel();
        if (precisionModel.isFloating()) {
          return getFactory(crsId, 3, 0, 0);
        } else {
          final double scaleXy = precisionModel.getScale();
          return getFactory(crsId, 3, scaleXy, 0);
        }
      }
    }
  }

  /**
   * <p>Get a GeometryFactory with the coordinate system, 3D axis (x, y &amp; z) and a floating precision models.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid) {
    return getFactory(srid, 3, 0, 0);
  }

  /**
   * <p>Get a GeometryFactory with the coordinate system, 2D axis (x &amp; y) and a fixed x, y precision model.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @param scaleXy The scale factor used to round the x, y coordinates. The precision is 1 / scaleXy.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final double scaleXy) {
    return getFactory(srid, 2, scaleXy, 0);
  }

  /**
   * <p>Get a GeometryFactory with no coordinate system, 3D axis (x, y &amp; z) and a fixed x, y &amp; floating z precision models.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @param scaleXy The scale factor used to round the x, y coordinates. The precision is 1 / scaleXy.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @param scaleZ The scale factor used to round the z coordinates. The precision is 1 / scaleZ.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid,
    final double scaleXy, final double scaleZ) {
    return getFactory(srid, 3, scaleXy, scaleZ);
  }

  /**
   * <p>Get a GeometryFactory with the coordinate system, number of axis and a floating precision model.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @param numAxis The number of coordinate axis. 2 for 2D x &amp; y coordinates. 3 for 3D x, y &amp; z coordinates.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final int numAxis) {
    return getFactory(srid, numAxis, 0, 0);
  }

  /**
   * <p>Get a GeometryFactory with the coordinate system, number of axis and a fixed x, y &amp; fixed z precision models.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @param numAxis The number of coordinate axis. 2 for 2D x &amp; y coordinates. 3 for 3D x, y &amp; z coordinates.
   * @param scaleXy The scale factor used to round the x, y coordinates. The precision is 1 / scaleXy.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @param scaleZ The scale factor used to round the z coordinates. The precision is 1 / scaleZ.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final int numAxis,
    final double scaleXy, final double scaleZ) {
    synchronized (factories) {
      final String key = srid + "-" + numAxis + "-" + scaleXy + "-" + scaleZ;
      GeometryFactory factory = factories.get(key);
      if (factory == null) {
        factory = new GeometryFactory(srid, numAxis, scaleXy, scaleZ);
        factories.put(key, factory);
      }
      return factory;
    }
  }

  /**
   * <p>Construct a GeometryFactory with the coordinate system, number of axis and a fixed x, y &amp; fixed z precision models.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @param numAxis The number of coordinate axis. 2 for 2D x &amp; y coordinates. 3 for 3D x, y &amp; z coordinates.
   * @param scaleXy The scale factor used to round the x, y coordinates. The precision is 1 / scaleXy.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @param scaleZ The scale factor used to round the z coordinates. The precision is 1 / scaleZ.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @return The geometry factory.
   */
  private GeometryFactory(final int crsId, final int numAxis,
    final double scaleXY, final double scaleZ) {
    super(crsId, numAxis, scaleXY, scaleZ);
  }

  /**
   * <p>Create a copy of an existing {@link Geometry}. If the geometry is in a different coordinate system
   * or precision model project the geometry to the coordinate system from this geometry factory
   * and apply the precision model.</p>
   * 
   * <p>The return type of this method will be auto-casted to the type of the variable the result is
   * assigned to. Use Geometry as the type if it is not possible to guarantee that the geometry is of a specific geometry type.</p>
   *
   * @param geometry The geometry.
   * @return The copied geometry.
   */
  @Override
  public <G extends Geometry> G copy(final G geometry) {
    return copy(geometry);
  }

  /**
   * <p>Create a {@link Geometry} from a <a href="http://en.wikipedia.org/wiki/Well-known_text">WKT</a> or
   * <a href="EWKT">http://postgis.net/docs/manual-2.0/using_postgis_dbmanagement.html#EWKB_EWKT</a> encoded geometry.
   * If the EWKT string includes a SRID the geometry will use read using that SRID and then
   * projected to the SRID of the geometry factory. If the SRID was not specified the geometry will
   * be assumed to be in the coordinate system of the geometry factory's SRID. The return type of
   * the WKT to geometry conversion will be auto-casted to the type of the variable the result is
   * assigned to. Use Geometry as the type if it is not possible to guarantee that the WKT is of a specific geometry type.</p>
   * 
   * <p>The following example shows a WGS84 EWKT polygon converted to a BC Albers polygon.</p>
   * 
   * <figure>
   *   <pre class="prettyprint language-java">GeometryFactory geometryFactory = GeometryFactory.getFactory(3005, 1.0);
  String wkt = "SRID=4326;POLYGON((-122 50,-124 50,-124 51,-122 51,-122 50))";
  Polygon polygon = geometryFactory.createGeometry(wkt);
  System.out.println(polygon);
  // POLYGON((1286630 561884,1143372 555809,1140228 667065,1280345 673006,1286630 561884))</pre>
   * </figure>
   * @param wkt The <a href="http://en.wikipedia.org/wiki/Well-known_text">WKT</a> or <a href="EWKT">http://postgis.net/docs/manual-2.0/using_postgis_dbmanagement.html#EWKB_EWKT</a> encoded geometry.</a>
   * @return The created geometry.
   */
  @Override
  public <T extends Geometry> T createGeometry(final String wkt) {
    return super.createGeometry(wkt);
  }

  /**
   * <p>Create a {@link LinearRing} using the array of coordinates. The ring must form a closed loop.
   * The size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param coordinates The coordinates.
   * @return The created linear ring.
   */
  @Override
  public LinearRing createLinearRing(final double... coordinates) {
    return super.createLinearRing(coordinates);
  }

  /**
   * <p>Create a {@link LineString} using the array of coordinates. The size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param coordinates The coordinates.
   * @return The created linestring.
   */
  @Override
  public LineString createLineString(final double... coordinates) {
    return super.createLineString(coordinates);
  }

  /**
   * <p>Create a {@link MultiLineString} using the list of lines. The first ring in the list is the exterior ring and
   * the other rings are the interior rings. The rings in the list can be any of the following types.</p>
   * 
   * <ul>
   *   <li><code>double[]</code></li>
   *   <li>{@link Point}</li>
   *   <li>{@link Coordinate}</li>
   *   <li>{@link Coordinates}</li>
   *   <li>{@link CoordinatesList}</li>
   *   <li>{@link CoordinateSequence}</li>
   * </ul>
   * 
   * <p>For a <code>double[]</code> the size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param lines The list of lines.
   * @return The created multi-linestring.
   */
  @Override
  public MultiLineString createMultiLineString(final List<?> lines) {
    return super.createMultiLineString(lines);
  }

  /**
   * <p>Create a {@link MultiPoint} using the list of points. The points in the list can be any of the following types.</p>
   * 
   * <ul>
   *   <li><code>double[]</code></li>
   *   <li>{@link Point}</li>
   *   <li>{@link Coordinate}</li>
   *   <li>{@link Coordinates}</li>
   *   <li>{@link CoordinatesList}</li>
   *   <li>{@link CoordinateSequence}</li>
   * </ul>
   * 
   * <p>For a <code>double[]</code> the size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param points The list of points.
   * @return The created multi-point.
   */
  public MultiPoint createMultiPoint(final List<?> points) {
    return super.createMultiPoint(points);
  }

  /**
   * <p>Create a {@link MultiPolygon} using the list of points. The points in the list can be any of the following types.</p>
   * 
   * <ul>
   *   <li>{@link Polygon}</li>
   *   <li>{@link List} see {@link GeometryFactory#createPolygon(List)}</li>
   * </ul>
   * 
   * <p>For a <code>double[]</code> the size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param polygons The list of polygons.
   * @return The created multi-polygon.
   */
  @Override
  public MultiPolygon createMultiPolygon(final List<?> polygons) {
    return super.createMultiPolygon(polygons);
  }

  /**
   * <p>Create a point using the array of coordinates. The size of the array should be the same as the
   * number of axis used on this geometry factory. If the size is less then additional axis will be
   * set to 0. If greater then those values will be ignored. For example a 2D geometry will have x,y values and a 3D x,y,z.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param coordinates The coordinates.
   * @return The created point.
   */
  @Override
  public Point createPoint(final double... coordinates) {
    return super.createPoint(coordinates);
  }

  /**
   * <p>Create a polygon using the list of rings. The first ring in the list is the exterior ring and
   * the other rings are the interior rings. The rings in the list can be any of the following types.</p>
   * 
   * <ul>
   *   <li><code>double[]</code></li>
   *   <li>{@link LineString}</li>
   *   <li>{@link LinearRing}</li>
   *   <li>{@link CoordinatesList}</li>
   *   <li>{@link CoordinateSequence}</li>
   * </ul>
   * 
   * <p>For a <code>double[]</code> the size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param rings The list of rings.
   * @return The created polygon.
   */
  @Override
  public Polygon createPolygon(final List<?> rings) {
    return super.createPolygon(rings);
  }
}
