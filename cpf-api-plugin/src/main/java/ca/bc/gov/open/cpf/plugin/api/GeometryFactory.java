package ca.bc.gov.open.cpf.plugin.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.impl.geometry.JtsWktWriter;

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
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * <p>The CPF provides an extended version of the <a href="http://tsusiatsoftware.net/jts/main.html"/>Java Topology Suite (JTS)</a> GeometryFactory to create JTS geometries. The
 * extended version includes support for coordinate system projection, precision model, and controls on the number of axis.</p>
 *
 * <p>The <code>GeometryFactory</code> does not provide a public constructor. <code>GeometryFactory</code> instances can
 * be obtained using the <code>getFactory</code> static methods described below.
 */
@SuppressWarnings("serial")
public class GeometryFactory extends
com.vividsolutions.jts.geom.GeometryFactory {

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

  public static String toWkt(final Geometry geometry) {
    return JtsWktWriter.toString(geometry);
  }

  public static String toWkt(final Geometry geometry, final boolean writeSrid) {
    return JtsWktWriter.toString(geometry, writeSrid);
  }

  /** The cached geometry factories. */
  private static Map<String, GeometryFactory> factories = new HashMap<String, GeometryFactory>();

  private final int axisCount;

  private final double scaleXy;

  private final double scaleZ;

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
    this.axisCount = axisCount;
    this.scaleXy = scaleXY;
    this.scaleZ = scaleZ;
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
   * <a href="http://postgis.net/docs/manual-2.0/using_postgis_dbmanagement.html#EWKB_EWKT">EWKT</a> encoded geometry.
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
  Polygon polygon = createGeometry(wkt);
  System.out.println(polygon);
  // POLYGON((1286630 561884,1143372 555809,1140228 667065,1280345 673006,1286630 561884))</pre>
   * </figure>
   * @param wkt The <a href="http://en.wikipedia.org/wiki/Well-known_text">WKT</a> or <a href="EWKT">http://postgis.net/docs/manual-2.0/using_postgis_dbmanagement.html#EWKB_EWKT</a> encoded geometry.</a>
   * @return The created geometry.
   */
  @SuppressWarnings("unchecked")
  public <T extends Geometry> T createGeometry(final String wkt) {
    return (T)parseGeometry(wkt, false);
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
   *   <li>{@link LineString}</li>
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
   *   <li>{@link Coordinate}</li>
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
    return this.axisCount;
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

  private int getNumAxis(final StringBuilder text) {
    skipWhitespace(text);
    final char c = text.charAt(0);
    switch (c) {
      case '(':
      case 'E':
        return 2;
      case 'M':
        text.delete(0, 1);
        return 4;
      case 'Z':
        if (text.charAt(1) == 'M') {
          text.delete(0, 2);
          return 4;
        } else {
          text.delete(0, 1);
          return 3;
        }
      default:
        throw new IllegalArgumentException(
          "Expecting Z, M, ZM, (, or EMPTY not: " + text);
    }
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
    return this.scaleXy;
  }

  public double getScaleZ() {
    return this.scaleZ;
  }

  public boolean hasM() {
    return getAxisCount() > 3;
  }

  private boolean hasText(final StringBuilder text, final String expected) {
    skipWhitespace(text);
    final int length = expected.length();
    final CharSequence subText = text.subSequence(0, length);
    if (subText.equals(expected)) {
      text.delete(0, length);
      return true;
    } else {
      return false;
    }
  }

  public boolean hasZ() {
    return getAxisCount() > 2;
  }

  private boolean isEmpty(final StringBuilder text) {
    if (hasText(text, "EMPTY")) {
      skipWhitespace(text);
      if (text.length() > 0) {
        throw new IllegalArgumentException(
          "Unexpected text at the end of an empty geometry: " + text);
      }
      return true;
    } else {
      return false;
    }
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

  private CoordinateSequence parseCoordinates(
    final GeometryFactory geometryFactory, final StringBuilder text,
    final int axisCount) {
    final int geometryFactoryNumAxis = getNumAxis();
    char c = text.charAt(0);
    if (c == '(') {
      text.delete(0, 1);
      final List<Double> coordinates = new ArrayList<Double>();
      int axisNum = 0;
      boolean finished = false;
      while (!finished) {
        final Double number = parseDouble(text);
        c = text.charAt(0);
        if (number == null) {
          if (c == ')') {
            finished = true;
          } else {
            throw new IllegalArgumentException(
              "Expecting end of coordinates ')' not" + text);
          }
        } else if (c == ',' || c == ')') {
          if (axisNum < axisCount) {
            if (axisNum < geometryFactoryNumAxis) {
              coordinates.add(number);
            }
            axisNum++;
            while (axisNum < geometryFactoryNumAxis) {
              coordinates.add(Double.NaN);
              axisNum++;
            }
            axisNum = 0;
          } else {
            throw new IllegalArgumentException(
              "Too many coordinates, vertex must have " + axisCount
              + " coordinates not " + (axisNum + 1));
          }
          if (c == ')') {
            finished = true;
          } else {
            text.delete(0, 1);
          }
        } else {
          if (axisNum < axisCount) {
            if (axisNum < geometryFactoryNumAxis) {
              coordinates.add(number);
            }
            axisNum++;
          } else {
            throw new IllegalArgumentException(
              "Too many coordinates, vertex must have " + axisCount
              + " coordinates not " + (axisNum + 1));

          }
        }
      }
      text.delete(0, 1);
      final double[] coords = new double[coordinates.size()];
      for (int i = 0; i < coords.length; i++) {
        coords[i] = coordinates.get(i);
      }
      return new PackedCoordinateSequence.Double(coords, geometryFactoryNumAxis);
    } else {
      throw new IllegalArgumentException(
        "Expecting start of coordinates '(' not: " + text);
    }
  }

  private Double parseDouble(final StringBuilder text) {
    skipWhitespace(text);
    int i = 0;
    for (; i < text.length(); i++) {
      final char c = text.charAt(i);
      if (Character.isWhitespace(c) || c == ',' || c == ')') {
        break;
      }
    }
    final String numberText = text.substring(0, i);
    text.delete(0, i);
    if (numberText.length() == 0) {
      return null;
    } else {
      return new Double(numberText);
    }

  }

  @SuppressWarnings("unchecked")
  private <T extends Geometry> T parseGeometry(final String value,
    final boolean useNumAxisFromGeometryFactory) {
    if (value != null && value.trim().length() > 0) {
      GeometryFactory geometryFactory = this;
      final int axisCount = getNumAxis();
      final double scaleXY = getScaleXY();
      final double scaleZ = getScaleZ();
      Geometry geometry;
      final StringBuilder text = new StringBuilder(value);
      if (hasText(text, "SRID=")) {
        final Integer srid = parseInteger(text);
        if (srid != null && srid != this.getSRID()) {
          geometryFactory = GeometryFactory.getFactory(srid, axisCount);
        }
        hasText(text, ";");
      }
      if (hasText(text, "POINT")) {
        geometry = parsePoint(geometryFactory, useNumAxisFromGeometryFactory,
          text);
      } else if (hasText(text, "LINESTRING")) {
        geometry = parseLineString(geometryFactory,
          useNumAxisFromGeometryFactory, text);
      } else if (hasText(text, "POLYGON")) {
        geometry = parsePolygon(geometryFactory, useNumAxisFromGeometryFactory,
          text);
      } else if (hasText(text, "MULTIPOINT")) {
        geometry = parseMultiPoint(geometryFactory,
          useNumAxisFromGeometryFactory, text);
      } else if (hasText(text, "MULTILINESTRING")) {
        geometry = parseMultiLineString(geometryFactory,
          useNumAxisFromGeometryFactory, text);
      } else if (hasText(text, "MULTIPOLYGON")) {
        geometry = parseMultiPolygon(geometryFactory,
          useNumAxisFromGeometryFactory, text);
      } else {
        throw new IllegalArgumentException("Unknown geometry type " + text);
      }
      if (this.getSRID() == 0) {
        final int srid = geometry.getSRID();
        if (useNumAxisFromGeometryFactory) {
          geometryFactory = GeometryFactory.getFactory(srid, axisCount,
            scaleXY, scaleZ);
          return (T)createGeometry(geometry);
        } else {
          return (T)geometry;
        }
      } else if (geometryFactory == this) {
        return (T)geometry;
      } else {
        return (T)this.createGeometry(geometry);
      }
    } else {
      return null;
    }
  }

  private Integer parseInteger(final StringBuilder text) {
    skipWhitespace(text);
    int i = 0;
    while (i < text.length() && Character.isDigit(text.charAt(i))) {
      i++;
    }
    if (!Character.isDigit(text.charAt(i))) {
      i--;
    }
    if (i < 0) {
      return null;
    } else {
      final String numberText = text.substring(0, i + 1);
      text.delete(0, i + 1);
      return Integer.valueOf(numberText);
    }
  }

  private LineString parseLineString(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuilder text) {
    int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != getNumAxis()) {
        final int srid = getSRID();
        final double scaleXY = getScaleXY();
        final double scaleZ = getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    } else {
      axisCount = getNumAxis();
    }
    if (isEmpty(text)) {
      return createLineString();
    } else {
      final CoordinateSequence points = parseCoordinates(geometryFactory, text,
        axisCount);
      return createLineString(points);
    }
  }

  private MultiLineString parseMultiLineString(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuilder text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != getNumAxis()) {
        final int srid = getSRID();
        final double scaleXY = getScaleXY();
        final double scaleZ = getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    }
    final List<CoordinateSequence> lines;
    if (isEmpty(text)) {
      lines = new ArrayList<CoordinateSequence>();
    } else {
      lines = parseParts(geometryFactory, text, axisCount);
    }
    return createMultiLineString(lines);
  }

  private MultiPoint parseMultiPoint(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuilder text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != getNumAxis()) {
        final int srid = getSRID();
        final double scaleXY = getScaleXY();
        final double scaleZ = getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    }

    final List<CoordinateSequence> pointsList;
    if (isEmpty(text)) {
      pointsList = new ArrayList<CoordinateSequence>();
    } else {
      pointsList = parseParts(geometryFactory, text, axisCount);
    }
    return createMultiPoint(pointsList);
  }

  private MultiPolygon parseMultiPolygon(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuilder text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != getNumAxis()) {
        final int srid = getSRID();
        final double scaleXY = getScaleXY();
        final double scaleZ = getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    }

    final List<List<CoordinateSequence>> polygons;
    if (isEmpty(text)) {
      polygons = new ArrayList<List<CoordinateSequence>>();
    } else {
      polygons = parsePartsList(geometryFactory, text, axisCount);
    }
    return multiPolygon(polygons);
  }

  private List<CoordinateSequence> parseParts(
    final GeometryFactory geometryFactory, final StringBuilder text,
    final int axisCount) {
    final List<CoordinateSequence> parts = new ArrayList<CoordinateSequence>();
    final char firstChar = text.charAt(0);
    switch (firstChar) {
      case '(':
        do {
          text.delete(0, 1);
          final CoordinateSequence coordinates = parseCoordinates(
            geometryFactory, text, axisCount);
          parts.add(coordinates);
        } while (text.charAt(0) == ',');
        if (text.charAt(0) == ')') {
          text.delete(0, 1);
        } else {
          throw new IllegalArgumentException("Expecting ) not" + text);
        }
        break;
      case ')':
        text.delete(0, 2);
        break;

      default:
        throw new IllegalArgumentException("Expecting ( not" + text);
    }
    return parts;
  }

  private List<List<CoordinateSequence>> parsePartsList(
    final GeometryFactory geometryFactory, final StringBuilder text,
    final int axisCount) {
    final List<List<CoordinateSequence>> partsList = new ArrayList<List<CoordinateSequence>>();
    final char firstChar = text.charAt(0);
    switch (firstChar) {
      case '(':
        do {
          text.delete(0, 1);
          final List<CoordinateSequence> parts = parseParts(geometryFactory,
            text, axisCount);
          partsList.add(parts);
        } while (text.charAt(0) == ',');
        if (text.charAt(0) == ')') {
          text.delete(0, 1);
        } else {
          throw new IllegalArgumentException("Expecting ) not" + text);
        }
        break;
      case ')':
        text.delete(0, 2);
        break;

      default:
        throw new IllegalArgumentException("Expecting ( not" + text);
    }
    return partsList;
  }

  private Point parsePoint(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuilder text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != getNumAxis()) {
        final int srid = getSRID();
        final double scaleXY = getScaleXY();
        final double scaleZ = getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    }
    if (isEmpty(text)) {
      return createPoint();
    } else {
      final CoordinateSequence points = parseCoordinates(geometryFactory, text,
        axisCount);
      if (points.size() > 1) {
        throw new IllegalArgumentException("Points may only have 1 vertex");
      }
      return createPoint(points);
    }
  }

  private Polygon parsePolygon(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuilder text) {
    int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != getNumAxis()) {
        final int srid = getSRID();
        final double scaleXY = getScaleXY();
        final double scaleZ = getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    } else {
      axisCount = getNumAxis();
    }

    final List<CoordinateSequence> parts;
    if (isEmpty(text)) {
      parts = new ArrayList<CoordinateSequence>();
    } else {
      parts = parseParts(geometryFactory, text, axisCount);
    }
    return createPolygon(parts);
  }

  private void skipWhitespace(final StringBuilder text) {
    for (int i = 0; i < text.length(); i++) {
      final char c = text.charAt(i);
      if (!Character.isWhitespace(c)) {
        if (i > 0) {
          text.delete(0, i);
        }
        return;
      }
    }
  }

}
