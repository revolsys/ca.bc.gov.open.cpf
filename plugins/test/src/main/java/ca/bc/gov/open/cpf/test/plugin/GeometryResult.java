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
package ca.bc.gov.open.cpf.test.plugin;

import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.geometry.model.Geometry;

public class GeometryResult {

  private final Geometry geometry;

  public GeometryResult(final Geometry geometry) {
    this.geometry = geometry;
  }

  @ResultAttribute(description = "The area of the geometry")
  public double getArea() {
    return this.geometry.getArea();
  }

  public Map<String, Object> getCustomizationProperties() {
    final Map<String, Object> properties = new HashMap<>();
    final String styleId = this.geometry.getGeometryType();
    properties.put("kmlStyleUrl", "http://gov.bc.ca/kmlStyle.kml#" + styleId);
    properties.put("kmlWriteNulls", true);
    return properties;
  }

  @ResultAttribute(description = "The simple geometry.")
  public Geometry getGeometry() {
    return this.geometry;
  }

  @ResultAttribute(description = "The length of the geometry")
  public double getLength() {
    return this.geometry.getLength();
  }

}
