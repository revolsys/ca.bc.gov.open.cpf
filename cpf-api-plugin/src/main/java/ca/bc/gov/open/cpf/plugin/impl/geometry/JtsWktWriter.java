/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.plugin.impl.geometry;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.revolsys.util.MathUtil;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class JtsWktWriter {

  private static int getDimension(final Geometry geometry) {
    int axisCount = 2;
    for (int i = 0; i < geometry.getNumGeometries(); i++) {
      int subNumAxis = 2;
      final Geometry subGeometry = geometry.getGeometryN(i);
      if (subGeometry instanceof Point) {
        final Point point = (Point)subGeometry;
        subNumAxis = point.getCoordinateSequence().getDimension();
      } else if (subGeometry instanceof LineString) {
        final LineString line = (LineString)subGeometry;
        subNumAxis = line.getCoordinateSequence().getDimension();
      } else if (subGeometry instanceof Polygon) {
        final Polygon polygon = (Polygon)subGeometry;
        final LineString ring = polygon.getExteriorRing();
        subNumAxis = ring.getCoordinateSequence().getDimension();
      }
      axisCount = Math.max(axisCount, subNumAxis);
    }
    return axisCount;
  }

  public static String toString(final Geometry geometry) {
    final StringWriter out = new StringWriter();
    final PrintWriter writer = new PrintWriter(out);
    write(writer, geometry);
    writer.flush();
    return out.toString();
  }

  public static String toString(final Geometry geometry, final boolean writeSrid) {
    final StringWriter out = new StringWriter();
    final PrintWriter writer = new PrintWriter(out);
    if (writeSrid) {
      final int srid = geometry.getSRID();
      if (srid > 0) {
        writer.print("SRID=");
        writer.print(srid);
        writer.print(';');
      }
    }
    write(writer, geometry);
    writer.flush();
    return out.toString();
  }

  public static void write(final PrintWriter out,
    final CoordinateSequence coordinates, final int axisCount) {
    out.print('(');
    write(out, coordinates, 0, axisCount);
    for (int i = 1; i < coordinates.size(); i++) {
      out.print(',');
      write(out, coordinates, i, axisCount);
    }
    out.print(')');
  }

  private static void write(final PrintWriter out,
    final CoordinateSequence coordinates, final int index, final int axisCount) {
    writeOrdinate(out, coordinates, index, 0);
    for (int j = 1; j < axisCount; j++) {
      out.print(' ');
      writeOrdinate(out, coordinates, index, j);
    }
  }

  public static void write(final PrintWriter out, final Geometry geometry) {
    if (geometry != null) {
      if (geometry instanceof Point) {
        final Point point = (Point)geometry;
        write(out, point);
      } else if (geometry instanceof MultiPoint) {
        final MultiPoint multiPoint = (MultiPoint)geometry;
        write(out, multiPoint);
      } else if (geometry instanceof LineString) {
        final LineString line = (LineString)geometry;
        write(out, line);
      } else if (geometry instanceof MultiLineString) {
        final MultiLineString multiLine = (MultiLineString)geometry;
        write(out, multiLine);
      } else if (geometry instanceof Polygon) {
        final Polygon polygon = (Polygon)geometry;
        write(out, polygon);
      } else if (geometry instanceof MultiPolygon) {
        final MultiPolygon multiPolygon = (MultiPolygon)geometry;
        write(out, multiPolygon);
      } else if (geometry instanceof GeometryCollection) {
        final GeometryCollection geometryCollection = (GeometryCollection)geometry;
        write(out, geometryCollection);
      } else {
        throw new IllegalArgumentException("Unknown geometry type"
          + geometry.getClass());
      }
    }
  }

  public static void write(final PrintWriter out, final Geometry geometry,
    final int axisCount) {
    if (geometry != null) {
      if (geometry instanceof Point) {
        final Point point = (Point)geometry;
        write(out, point, axisCount);
      } else if (geometry instanceof MultiPoint) {
        final MultiPoint multiPoint = (MultiPoint)geometry;
        write(out, multiPoint, axisCount);
      } else if (geometry instanceof LineString) {
        final LineString line = (LineString)geometry;
        write(out, line, axisCount);
      } else if (geometry instanceof MultiLineString) {
        final MultiLineString multiLine = (MultiLineString)geometry;
        write(out, multiLine, axisCount);
      } else if (geometry instanceof Polygon) {
        final Polygon polygon = (Polygon)geometry;
        write(out, polygon, axisCount);
      } else if (geometry instanceof MultiPolygon) {
        final MultiPolygon multiPolygon = (MultiPolygon)geometry;
        write(out, multiPolygon, axisCount);
      } else if (geometry instanceof GeometryCollection) {
        final GeometryCollection geometryCollection = (GeometryCollection)geometry;
        write(out, geometryCollection, axisCount);
      } else {
        throw new IllegalArgumentException("Unknown geometry type"
          + geometry.getClass());
      }
    }
  }

  public static void write(final PrintWriter out,
    final GeometryCollection multiGeometry) {
    final int axisCount = Math.min(getDimension(multiGeometry), 4);
    write(out, multiGeometry, axisCount);
  }

  private static void write(final PrintWriter out,
    final GeometryCollection multiGeometry, final int axisCount) {
    writeGeometryType(out, "MULTIGEOMETRY", axisCount);
    if (multiGeometry.isEmpty()) {
      out.print(" EMPTY");
    } else {
      out.print("(");
      Geometry geometry = multiGeometry.getGeometryN(0);
      write(out, geometry, axisCount);
      for (int i = 1; i < multiGeometry.getNumGeometries(); i++) {
        out.print(',');
        geometry = multiGeometry.getGeometryN(i);
        write(out, geometry, axisCount);
      }
      out.print(')');
    }
  }

  public static void write(final PrintWriter out, final LineString line) {
    final int axisCount = Math.min(getDimension(line), 4);
    write(out, line, axisCount);
  }

  private static void write(final PrintWriter out, final LineString line,
    final int axisCount) {
    writeGeometryType(out, "LINESTRING", axisCount);
    if (line.isEmpty()) {
      out.print(" EMPTY");
    } else {
      final CoordinateSequence coordinates = line.getCoordinateSequence();
      write(out, coordinates, axisCount);
    }
  }

  public static void write(final PrintWriter out,
    final MultiLineString multiLineString) {
    final int axisCount = Math.min(getDimension(multiLineString), 4);
    write(out, multiLineString, axisCount);
  }

  private static void write(final PrintWriter out,
    final MultiLineString multiLineString, final int axisCount) {
    writeGeometryType(out, "MULTILINESTRING", axisCount);
    if (multiLineString.isEmpty()) {
      out.print(" EMPTY");
    } else {
      out.print("(");
      LineString line = (LineString)multiLineString.getGeometryN(0);
      CoordinateSequence points = line.getCoordinateSequence();
      write(out, points, axisCount);
      for (int i = 1; i < multiLineString.getNumGeometries(); i++) {
        out.print(",");
        line = (LineString)multiLineString.getGeometryN(i);
        points = line.getCoordinateSequence();
        write(out, points, axisCount);
      }
      out.print(")");
    }
  }

  public static void write(final PrintWriter out, final MultiPoint multiPoint) {
    final int axisCount = Math.min(getDimension(multiPoint), 4);
    write(out, multiPoint, axisCount);
  }

  private static void write(final PrintWriter out, final MultiPoint multiPoint,
    final int axisCount) {
    writeGeometryType(out, "MULTIPOINT", axisCount);
    if (multiPoint.isEmpty()) {
      out.print(" EMPTY");
    } else {
      Point point = (Point)multiPoint.getGeometryN(0);
      CoordinateSequence coordinates = point.getCoordinateSequence();
      out.print("((");
      write(out, coordinates, 0, axisCount);
      for (int i = 1; i < multiPoint.getNumGeometries(); i++) {
        out.print("),(");
        point = (Point)multiPoint.getGeometryN(i);
        coordinates = point.getCoordinateSequence();
        write(out, coordinates, 0, axisCount);
      }
      out.print("))");
    }
  }

  public static void write(final PrintWriter out,
    final MultiPolygon multiPolygon) {
    final int axisCount = Math.min(getDimension(multiPolygon), 4);
    write(out, multiPolygon, axisCount);
  }

  private static void write(final PrintWriter out,
    final MultiPolygon multiPolygon, final int axisCount) {
    writeGeometryType(out, "MULTIPOLYGON", axisCount);
    if (multiPolygon.isEmpty()) {
      out.print(" EMPTY");
    } else {
      out.print("(");

      Polygon polygon = (Polygon)multiPolygon.getGeometryN(0);
      writePolygon(out, polygon, axisCount);
      for (int i = 1; i < multiPolygon.getNumGeometries(); i++) {
        out.print(",");
        polygon = (Polygon)multiPolygon.getGeometryN(i);
        writePolygon(out, polygon, axisCount);
      }
      out.print(")");
    }
  }

  public static void write(final PrintWriter out, final Point point) {
    final int axisCount = Math.min(getDimension(point), 4);
    write(out, point, axisCount);
  }

  private static void write(final PrintWriter out, final Point point,
    final int axisCount) {
    writeGeometryType(out, "POINT", axisCount);
    if (point.isEmpty()) {
      out.print(" EMPTY");
    } else {
      out.print("(");
      final CoordinateSequence coordinates = point.getCoordinateSequence();
      write(out, coordinates, 0, axisCount);
      out.print(')');
    }
  }

  public static void write(final PrintWriter out, final Polygon polygon) {
    final int axisCount = Math.min(getDimension(polygon), 4);
    write(out, polygon, axisCount);
  }

  private static void write(final PrintWriter out, final Polygon polygon,
    final int axisCount) {
    writeGeometryType(out, "POLYGON", axisCount);
    if (polygon.isEmpty()) {
      out.print(" EMPTY");
    } else {
      writePolygon(out, polygon, axisCount);
    }
  }

  private static void writeAxis(final PrintWriter out, final int axisCount) {
    if (axisCount > 3) {
      out.print(" ZM");
    } else if (axisCount > 2) {
      out.print(" Z");
    }
  }

  private static void writeGeometryType(final PrintWriter out,
    final String geometryType, final int axisCount) {
    out.print(geometryType);
    writeAxis(out, axisCount);
  }

  private static void writeOrdinate(final PrintWriter out,
    final CoordinateSequence coordinates, final int index,
    final int ordinateIndex) {
    if (ordinateIndex > coordinates.getDimension()) {
      out.print(0);
    } else {
      final double ordinate = coordinates.getOrdinate(index, ordinateIndex);
      if (Double.isNaN(ordinate)) {
        out.print(0);
      } else {
        out.print(MathUtil.toString(ordinate));
      }
    }
  }

  private static void writePolygon(final PrintWriter out,
    final Polygon polygon, final int axisCount) {
    out.print('(');
    final LineString shell = polygon.getExteriorRing();
    final CoordinateSequence coordinates = shell.getCoordinateSequence();
    write(out, coordinates, axisCount);
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      out.print(',');
      final LineString hole = polygon.getInteriorRingN(i);
      final CoordinateSequence holeCoordinates = hole.getCoordinateSequence();
      write(out, holeCoordinates, axisCount);
    }
    out.print(')');
  }
}
