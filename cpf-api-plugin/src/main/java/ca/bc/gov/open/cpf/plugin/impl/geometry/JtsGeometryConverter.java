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
