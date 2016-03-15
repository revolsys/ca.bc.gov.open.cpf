/*
 * Copyright © 2008-2016, Province of British Columbia
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
package ca.bc.gov.mtws;

import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.plugin.api.AllowedValues;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.GeometryConfiguration;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.geometry.model.Polygon;
import com.revolsys.gis.grid.RectangularMapGrid;
import com.revolsys.gis.grid.RectangularMapGridFactory;
import com.revolsys.gis.grid.RectangularMapTile;

@BusinessApplicationPlugin(
    numRequestsPerWorker = 100,
    instantModePermission = "permitAll",
    description = "The Map Tile by Tile Id service returns the polygon boundary for the map tile specified by the tile name.")
public class MapTileByTileId {
  private static final Polygon DEFAULT_POLYGON = com.revolsys.geometry.model.GeometryFactory.wgs84()
    .polygon(
      com.revolsys.geometry.model.GeometryFactory.wgs84().linearRing(2, -121.0, 50, -120, 50, -120, 51,
        -121, 51, -121, 50));

  private String mapTileId = "92j";

  private Polygon mapTileBoundary = DEFAULT_POLYGON;

  private String mapGridName = "NTS 1:250 000";

  private short numBoundaryPoints = 20;

  public void execute() {
    final RectangularMapGrid grid = RectangularMapGridFactory.getGrid(this.mapGridName);
    if (grid == null) {
      throw new IllegalArgumentException("Grid not supported " + this.mapGridName);
    } else {
      final RectangularMapTile tile = grid.getTileByName(this.mapTileId);
      if (tile == null) {
        throw new IllegalArgumentException("tile not found");
      } else {
        this.mapTileId = tile.getFormattedName();
        this.mapTileBoundary = tile.getPolygon(this.numBoundaryPoints);
      }
    }
  }

  @ResultAttribute
  public String getMapGridName() {
    return this.mapGridName;
  }

  @ResultAttribute(description = "The polygon boundary of the map tile.")
  @GeometryConfiguration(srid = 4326, numAxis = 2)
  public Polygon getMapTileBoundary() {
    return this.mapTileBoundary;
  }

  @ResultAttribute
  public String getMapTileId() {
    return this.mapTileId;
  }

  @AllowedValues(value = {
    "NTS 1:1 000 000", "NTS 1:500 000", "NTS 1:250 000", "NTS 1:125 000", "NTS 1:50 000",
    "NTS 1:25 000", "BCGS 1:20 000", "BCGS 1:10 000", "BCGS 1:5 000", "BCGS 1:2 500",
    "BCGS 1:2 000", "BCGS 1:1250", "BCGS 1:1 000", "BCGS 1:500", "MTO"
  })
  @Required
  @JobParameter
  @RequestParameter(index = 1,
      description = "The name of the Map Grid the Map Tile ID is from (e.g. NTS 1:250 000).")
  @DefaultValue("NTS 1:250 000")
  public void setMapGridName(final String mapGridName) {
    this.mapGridName = StringUtils.trimWhitespace(mapGridName);
  }

  @Required
  @RequestParameter(index = 2, description = "The identifier of the map tile (e.g. 92G025).")
  public void setMapTileId(final String mapTileId) {
    this.mapTileId = StringUtils.trimWhitespace(mapTileId);
  }

  @JobParameter
  @RequestParameter(
      index = 3,
      minValue = "1",
      maxValue = "100",
      description = "The number of points to include on each edge of the polygon created for the map tile's bounding box.")
  @DefaultValue("20")
  public void setNumBoundaryPoints(final short numBoundaryPoints) {
    this.numBoundaryPoints = numBoundaryPoints;
  }

  // public void testExecute() {
  // final RectangularMapGrid grid =
  // RectangularMapGridFactory.getGrid("NTS 1:250 000");
  // final RectangularMapTile tile = grid.getTileByName("92j");
  // mapTileId = tile.getFormattedName();
  // mapTileBoundary = tile.getPolygon(numBoundaryPoints);
  // }
}
