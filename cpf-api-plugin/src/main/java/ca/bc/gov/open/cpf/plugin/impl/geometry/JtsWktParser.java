package ca.bc.gov.open.cpf.plugin.impl.geometry;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.plugin.api.GeometryFactory;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;

public class JtsWktParser {

  public static boolean hasText(final StringBuffer text, final String expected) {
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

  public static Double parseDouble(final StringBuffer text) {
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

  public static Integer parseInteger(final StringBuffer text) {
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

  public static void skipWhitespace(final StringBuffer text) {
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

  private final GeometryFactory geometryFactory;

  public JtsWktParser() {
    this(GeometryFactory.getFactory());
  }

  public JtsWktParser(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  private int getNumAxis(final StringBuffer text) {
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

  private boolean isEmpty(final StringBuffer text) {
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

  private CoordinateSequence parseCoordinates(
    final GeometryFactory geometryFactory, final StringBuffer text,
    final int axisCount) {
    final int geometryFactoryNumAxis = geometryFactory.getNumAxis();
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

  public <T extends Geometry> T parseGeometry(final String value) {
    return parseGeometry(value, true);
  }

  @SuppressWarnings("unchecked")
  public <T extends Geometry> T parseGeometry(final String value,
    final boolean useNumAxisFromGeometryFactory) {
    if (StringUtils.hasLength(value)) {
      GeometryFactory geometryFactory = this.geometryFactory;
      final int axisCount = geometryFactory.getNumAxis();
      final double scaleXY = geometryFactory.getScaleXY();
      final double scaleZ = geometryFactory.getScaleZ();
      Geometry geometry;
      final StringBuffer text = new StringBuffer(value);
      if (hasText(text, "SRID=")) {
        final Integer srid = parseInteger(text);
        if (srid != null && srid != this.geometryFactory.getSRID()) {
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
      if (this.geometryFactory.getSRID() == 0) {
        final int srid = geometry.getSRID();
        if (useNumAxisFromGeometryFactory) {
          geometryFactory = GeometryFactory.getFactory(srid, axisCount,
            scaleXY, scaleZ);
          return (T)geometryFactory.createGeometry(geometry);
        } else {
          return (T)geometry;
        }
      } else if (geometryFactory == this.geometryFactory) {
        return (T)geometry;
      } else {
        return (T)this.geometryFactory.createGeometry(geometry);
      }
    } else {
      return null;
    }
  }

  private LineString parseLineString(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuffer text) {
    int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != geometryFactory.getNumAxis()) {
        final int srid = geometryFactory.getSRID();
        final double scaleXY = geometryFactory.getScaleXY();
        final double scaleZ = geometryFactory.getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    } else {
      axisCount = geometryFactory.getNumAxis();
    }
    if (isEmpty(text)) {
      return geometryFactory.createLineString();
    } else {
      final CoordinateSequence points = parseCoordinates(geometryFactory, text,
        axisCount);
      return geometryFactory.createLineString(points);
    }
  }

  private MultiLineString parseMultiLineString(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuffer text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != geometryFactory.getNumAxis()) {
        final int srid = geometryFactory.getSRID();
        final double scaleXY = geometryFactory.getScaleXY();
        final double scaleZ = geometryFactory.getScaleZ();
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
    return geometryFactory.createMultiLineString(lines);
  }

  private MultiPoint parseMultiPoint(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuffer text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != geometryFactory.getNumAxis()) {
        final int srid = geometryFactory.getSRID();
        final double scaleXY = geometryFactory.getScaleXY();
        final double scaleZ = geometryFactory.getScaleZ();
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
    return geometryFactory.createMultiPoint(pointsList);
  }

  private MultiPolygon parseMultiPolygon(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuffer text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != geometryFactory.getNumAxis()) {
        final int srid = geometryFactory.getSRID();
        final double scaleXY = geometryFactory.getScaleXY();
        final double scaleZ = geometryFactory.getScaleZ();
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
    return geometryFactory.multiPolygon(polygons);
  }

  private List<CoordinateSequence> parseParts(
    final GeometryFactory geometryFactory, final StringBuffer text,
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
    final GeometryFactory geometryFactory, final StringBuffer text,
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
    final boolean useNumAxisFromGeometryFactory, final StringBuffer text) {
    final int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != geometryFactory.getNumAxis()) {
        final int srid = geometryFactory.getSRID();
        final double scaleXY = geometryFactory.getScaleXY();
        final double scaleZ = geometryFactory.getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    }
    if (isEmpty(text)) {
      return geometryFactory.createPoint();
    } else {
      final CoordinateSequence points = parseCoordinates(geometryFactory, text,
        axisCount);
      if (points.size() > 1) {
        throw new IllegalArgumentException("Points may only have 1 vertex");
      }
      return geometryFactory.createPoint(points);
    }
  }

  private Polygon parsePolygon(GeometryFactory geometryFactory,
    final boolean useNumAxisFromGeometryFactory, final StringBuffer text) {
    int axisCount = getNumAxis(text);
    if (!useNumAxisFromGeometryFactory) {
      if (axisCount != geometryFactory.getNumAxis()) {
        final int srid = geometryFactory.getSRID();
        final double scaleXY = geometryFactory.getScaleXY();
        final double scaleZ = geometryFactory.getScaleZ();
        geometryFactory = GeometryFactory.getFactory(srid, axisCount, scaleXY,
          scaleZ);
      }
    } else {
      axisCount = geometryFactory.getNumAxis();
    }

    final List<CoordinateSequence> parts;
    if (isEmpty(text)) {
      parts = new ArrayList<CoordinateSequence>();
    } else {
      parts = parseParts(geometryFactory, text, axisCount);
    }
    return geometryFactory.createPolygon(parts);
  }

}
