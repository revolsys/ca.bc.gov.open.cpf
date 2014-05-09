package ca.bc.gov.open.cpf.plugin.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.impl.geometry.JtsGeometryConverter;
import ca.bc.gov.open.cpf.plugin.impl.geometry.JtsWktParser;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.CoordinatesList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * <p>The CPF provides an extended version of the <a href=http://tsusiatsoftware.net/jts/main.html"/>Java Topology Suite (JTS)</a> GeometryFactory to create JTS geometries. The
 * extended version includes support for coordinate system projection, precision model, and controls on the number of axis.</p>
 *
 * <p>The <code>GeometryFactory</code> does not provide a public constructor. <code>GeometryFactory</code> instances can
 * be obtained using the <code>getFactory</code> static methods described below.
 */
@SuppressWarnings("serial")
public class GeometryFactory extends
  com.vividsolutions.jts.geom.GeometryFactory {
  /** The cached geometry factories. */
  private static Map<String, GeometryFactory> factories = new HashMap<String, GeometryFactory>();

  static {
    DataTypes.register("JtsGeometry", Geometry.class);
    DataTypes.register("JtsGeometryCollection", GeometryCollection.class);
    DataTypes.register("JtsPoint", Point.class);
    DataTypes.register("JtsMultiPoint", MultiPoint.class);
    DataTypes.register("JtsLineString", LineString.class);
    DataTypes.register("JtsLinearRing", LinearRing.class);
    DataTypes.register("JtsMultiLineString", MultiLineString.class);
    DataTypes.register("JtsPolygon", Polygon.class);
    DataTypes.register("JtsMultiPolygon", MultiPolygon.class);
    final JtsGeometryConverter converter = new JtsGeometryConverter();
    final StringConverterRegistry registry = StringConverterRegistry.getInstance();
    registry.addConverter(Geometry.class, converter);
    registry.addConverter(GeometryCollection.class, converter);
    registry.addConverter(Point.class, converter);
    registry.addConverter(MultiPoint.class, converter);
    registry.addConverter(LineString.class, converter);
    registry.addConverter(LinearRing.class, converter);
    registry.addConverter(MultiLineString.class, converter);
    registry.addConverter(Polygon.class, converter);
    registry.addConverter(MultiPolygon.class, converter);
  }

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
   * @param axisCount The number of coordinate axis. 2 for 2D x &amp; y coordinates. 3 for 3D x, y &amp; z coordinates.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final int axisCount) {
    return getFactory(srid, axisCount, 0, 0);
  }

  /**
   * <p>Get a GeometryFactory with the coordinate system, number of axis and a fixed x, y &amp; fixed z precision models.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @param axisCount The number of coordinate axis. 2 for 2D x &amp; y coordinates. 3 for 3D x, y &amp; z coordinates.
   * @param scaleXy The scale factor used to round the x, y coordinates. The precision is 1 / scaleXy.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @param scaleZ The scale factor used to round the z coordinates. The precision is 1 / scaleZ.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final int axisCount,
    final double scaleXy, final double scaleZ) {
    synchronized (factories) {
      final String key = srid + "-" + axisCount + "-" + scaleXy + "-" + scaleZ;
      GeometryFactory factory = factories.get(key);
      if (factory == null) {
        factory = new GeometryFactory(srid, axisCount, scaleXy, scaleZ);
        factories.put(key, factory);
      }
      return factory;
    }
  }

  private static PrecisionModel getPrecisionModel(final double scaleXY) {
    if (scaleXY <= 0) {
      return new PrecisionModel();
    } else {
      return new PrecisionModel(scaleXY);
    }
  }

  private final CoordinateSystem coordinateSystem;

  private final com.revolsys.jts.geom.GeometryFactory geometryFactory;

  private GeometryFactory(final CoordinateSystem coordinateSystem,
    final int axisCount, final double scaleXY, final double scaleZ) {
    super(getPrecisionModel(scaleXY), coordinateSystem.getId(),
      new PackedCoordinateSequenceFactory(
        PackedCoordinateSequenceFactory.DOUBLE, axisCount));
    this.coordinateSystem = coordinateSystem;
    this.geometryFactory = com.revolsys.jts.geom.GeometryFactory.getFactory(
      coordinateSystem.getId(), axisCount, scaleXY, scaleZ);
  }

  /**
   * <p>Construct a GeometryFactory with the coordinate system, number of axis and a fixed x, y &amp; fixed z precision models.</p>
   * 
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG coordinate system id</a>. 
   * @param axisCount The number of coordinate axis. 2 for 2D x &amp; y coordinates. 3 for 3D x, y &amp; z coordinates.
   * @param scaleXy The scale factor used to round the x, y coordinates. The precision is 1 / scaleXy.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @param scaleZ The scale factor used to round the z coordinates. The precision is 1 / scaleZ.
   * A scale factor of 1000 will give a precision of 1 / 1000 = 1mm for projected coordinate systems using metres.
   * @return The geometry factory.
   */
  private GeometryFactory(final int crsId, final int axisCount,
    final double scaleXY, final double scaleZ) {
    super(getPrecisionModel(scaleXY), crsId,
      new PackedCoordinateSequenceFactory(
        PackedCoordinateSequenceFactory.DOUBLE, axisCount));
    this.coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(crsId);
    this.geometryFactory = com.revolsys.jts.geom.GeometryFactory.getFactory(
      coordinateSystem.getId(), axisCount, scaleXY, scaleZ);
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
  @SuppressWarnings("unchecked")
  public <G extends Geometry> G copy(final G geometry) {
    return (G)createGeometry(geometry);
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
  @SuppressWarnings("unchecked")
  public <T extends Geometry> T createGeometry(final String wkt) {
    final JtsWktParser parser = new JtsWktParser(this);
    return (T)parser.parseGeometry(wkt);
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
  public LinearRing createLinearRing(final double... coordinates) {
    return super.createLinearRing(new PackedCoordinateSequence.Double(
      coordinates, getAxisCount()));
  }

  /**
   * <p>Create a {@link LineString} using the array of coordinates. The size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param coordinates The coordinates.
   * @return The created linestring.
   */
  public LineString createLineString(final double... coordinates) {
    final PackedCoordinateSequence.Double points = new PackedCoordinateSequence.Double(
      coordinates, getAxisCount());
    return super.createLineString(points);
  }

  /**
   * <p>Create a {@link MultiLineString} using the list of lines. The first ring in the list is the exterior ring and
   * the other rings are the interior rings. The rings in the list can be any of the following types.</p>
   * 
   * <ul>
   *   <li><code>double[]</code></li>
   *   <li>{@link Point}</li>
   *   <li>{@link Coordinates}</li>
   *   <li>{@link Coordinates}</li>
   *   <li>{@link CoordinatesList}</li>
   *   <li>{@link CoordinatesList}</li>
   * </ul>
   * 
   * <p>For a <code>double[]</code> the size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param lines The list of lines.
   * @return The created multi-linestring.
   */
  public MultiLineString createMultiLineString(final Collection<?> lines) {
    final LineString[] lineArray = getLineStringArray(lines);
    return createMultiLineString(lineArray);
  }

  /**
   * <p>Create a {@link MultiPoint} using the list of points. The points in the list can be any of the following types.</p>
   * 
   * <ul>
   *   <li><code>double[]</code></li>
   *   <li>{@link Point}</li>
   *   <li>{@link Coordinates}</li>
   *   <li>{@link Coordinates}</li>
   *   <li>{@link CoordinatesList}</li>
   *   <li>{@link CoordinatesList}</li>
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
    final Point[] pointArray = getPointArray(points);
    return createMultiPoint(pointArray);
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
  public Point createPoint(final double... coordinates) {
    final CoordinateSequence points = new PackedCoordinateSequence.Double(
      coordinates, getAxisCount());
    return super.createPoint(points);
  }

  public Point createPoint(final Object object) {
    CoordinateSequence coordinates;
    if (object instanceof Coordinate) {
      coordinates = new CoordinateArraySequence(new Coordinate[] {
        (Coordinate)object
      });
    } else if (object instanceof Point) {
      return copy((Point)object);
    } else if (object instanceof double[]) {
      coordinates = new PackedCoordinateSequence.Double((double[])object,
        getAxisCount());
    } else if (object instanceof CoordinateSequence) {
      final CoordinateSequence coordinatesList = (CoordinateSequence)object;
      coordinates = coordinatesList;
    } else {
      coordinates = null;
    }
    return createPoint(coordinates);
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
   *   <li>{@link CoordinatesList}</li>
   * </ul>
   * 
   * <p>For a <code>double[]</code> the size of the array should be a multiple
   * of the number of axis. For example a 2D geometry will have x1,y1...,xN,yN values and a 3D x1,y1,z1...,xN,yN,zN.
   * Geographic coordinates are always longitude, latitude and projected easting, northing.</p>
   * 
   * @param rings The list of rings.
   * @return The created polygon.
   */
  public Polygon createPolygon(final List<?> rings) {
    if (rings.size() == 0) {
      final CoordinateSequence nullPoints = getCoordinateSequenceFactory().create(
        0, getAxisCount());
      final LinearRing ring = createLinearRing(nullPoints);
      return createPolygon(ring, null);
    } else {
      final LinearRing exteriorRing = getLinearRing(rings, 0);
      final LinearRing[] interiorRings = new LinearRing[rings.size() - 1];
      for (int i = 1; i < rings.size(); i++) {
        interiorRings[i - 1] = getLinearRing(rings, i);
      }
      return createPolygon(exteriorRing, interiorRings);
    }
  }

  public int getAxisCount() {
    return geometryFactory.getAxisCount();
  }

  public CoordinateSystem getCoordinateSystem() {
    return coordinateSystem;
  }

  private LinearRing getLinearRing(final List<?> rings, final int index) {
    final Object ring = rings.get(index);
    if (ring instanceof LinearRing) {
      return (LinearRing)ring;
    } else if (ring instanceof CoordinateSequence) {
      final CoordinateSequence points = (CoordinateSequence)ring;
      return createLinearRing(points);
    } else if (ring instanceof LineString) {
      final LineString line = (LineString)ring;
      final CoordinateSequence points = line.getCoordinateSequence();
      return createLinearRing(points);
    } else if (ring instanceof double[]) {
      final double[] coordinates = (double[])ring;
      final CoordinateSequence points = new PackedCoordinateSequence.Double(
        coordinates, getAxisCount());
      return createLinearRing(points);
    } else {
      return null;
    }
  }

  private LineString[] getLineStringArray(final Collection<?> lines) {
    final List<LineString> lineStrings = new ArrayList<LineString>();
    for (final Object value : lines) {
      LineString lineString;
      if (value instanceof LineString) {
        lineString = (LineString)value;
      } else if (value instanceof CoordinateSequence) {
        final CoordinateSequence coordinates = (CoordinateSequence)value;
        lineString = createLineString(coordinates);
      } else if (value instanceof double[]) {
        final double[] points = (double[])value;
        lineString = createLineString(points);
      } else {
        lineString = null;
      }
      if (lineString != null) {
        lineStrings.add(lineString);
      }
    }
    return lineStrings.toArray(new LineString[lineStrings.size()]);
  }

  public int getNumAxis() {
    return getAxisCount();
  }

  private Point[] getPointArray(final Collection<?> pointsList) {
    final List<Point> points = new ArrayList<Point>();
    for (final Object object : pointsList) {
      final Point point = createPoint(object);
      if (point != null && !point.isEmpty()) {
        points.add(point);
      }
    }
    return points.toArray(new Point[points.size()]);
  }

  public Polygon[] getPolygonArray(final Collection<?> polygonList) {
    final List<Polygon> polygons = new ArrayList<Polygon>();
    for (final Object value : polygonList) {
      Polygon polygon;
      if (value instanceof Polygon) {
        polygon = (Polygon)value;
      } else if (value instanceof List) {
        final List<?> coordinateList = (List<?>)value;
        polygon = createPolygon(coordinateList);
      } else if (value instanceof CoordinateSequence) {
        final CoordinateSequence points = (CoordinateSequence)value;
        polygon = createPolygon(points);
      } else {
        polygon = null;
      }
      if (polygon != null) {
        polygons.add(polygon);
      }
    }
    return polygons.toArray(new Polygon[polygons.size()]);
  }

  public double getScaleXY() {
    return geometryFactory.getScaleXY();
  }

  public double getScaleZ() {
    return geometryFactory.getScaleZ();
  }

  public boolean hasM() {
    return getAxisCount() > 3;
  }

  public boolean hasZ() {
    return getAxisCount() > 2;
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
  public MultiPolygon multiPolygon(final Collection<?> polygons) {
    final Polygon[] polygonArray = getPolygonArray(polygons);
    return createMultiPolygon(polygonArray);
  }

}
