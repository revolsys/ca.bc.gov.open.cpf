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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultList;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;

import com.revolsys.jts.geom.Geometry;

@BusinessApplicationPlugin(
    numRequestsPerWorker = 100,
    instantModePermission = "permitAll",
    description = "Converts any multi-part geometries into multiple records each with a single-part geometry. Also returns the length and area of the geometry.")
public class MultipartToSimpleGeometry {

  private Geometry geometry;

  private final List<GeometryResult> results = new ArrayList<GeometryResult>();

  private AppLog appLog;

  public void execute() {
    this.appLog.debug("Start");
    for (int i = 0; i < geometry.getGeometryCount(); i++) {
      final Geometry part = geometry.getGeometry(i);
      final GeometryResult result = new GeometryResult(part);
      results.add(result);
    }
    this.appLog.debug("End");
  }

  public Map<String, Object> getCustomizationProperties() {
    final Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("kmlSnippet", "Length: $[length]");
    return properties;
  }

  @ResultList
  public List<GeometryResult> getResults() {
    return results;
  }

  public void setAppLog(final AppLog appLog) {
    this.appLog = appLog;
  }

  @Required
  @RequestParameter(
      description = "The multi-part geometry to split into it's component parts.")
  public void setGeometry(final Geometry geometry) {
    this.geometry = geometry;
  }

}
