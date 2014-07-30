package ca.bc.gov.open.cpf.plugin.impl.geometry;

import ca.bc.gov.open.cpf.plugin.api.GeometryFactory;

import com.revolsys.converter.string.StringConverter;
import com.vividsolutions.jts.geom.Geometry;

public class JtsGeometryConverter implements StringConverter<Geometry> {

  @Override
  public Class<Geometry> getConvertedClass() {
    return Geometry.class;
  }

  @Override
  public boolean requiresQuotes() {
    return true;
  }

  @Override
  public Geometry toObject(final Object value) {
    if (value instanceof Geometry) {
      final Geometry geometry = (Geometry)value;
      return geometry;
    } else if (value == null) {
      return null;
    } else {
      return toObject(value.toString());
    }
  }

  @Override
  public Geometry toObject(final String wkt) {
    final GeometryFactory factory = GeometryFactory.getFactory();
    return factory.createGeometry(wkt);
  }

  @Override
  public String toString(final Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof Geometry) {
      final Geometry geometry = (Geometry)value;
      return JtsWktWriter.toString(geometry, true);
    } else {
      return value.toString();
    }
  }

}
