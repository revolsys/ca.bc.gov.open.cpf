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
package ca.bc.gov.open.cpf.plugins.maptile;

import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.plugin.api.AllowedValues;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.geometry.model.Polygon;
import com.revolsys.gis.grid.RectangularMapGrid;
import com.revolsys.gis.grid.RectangularMapGridFactory;
import com.revolsys.gis.grid.RectangularMapTile;

@BusinessApplicationPlugin(numRequestsPerWorker = 100, instantModePermission = "permitAll",
    description = "The Map Tile by Location service returns the map tile id and polygon boundary for the map tile specified by latitude/longitude location.")
public class MapTileByLocation {

  private double latitude;

  private double longitude;

  private String mapTileId;

  private Polygon mapTileBoundary;

  private String mapGridName;

  private short numBoundaryPoints = 50;

  public void execute() {
    final RectangularMapGrid grid = RectangularMapGridFactory.getGrid(this.mapGridName);
    if (grid == null) {
      throw new IllegalArgumentException("Grid not supported " + this.mapGridName);
    } else {
      final RectangularMapTile tile = grid.getTileByLocation(this.longitude, this.latitude);
      if (tile == null) {
        throw new IllegalArgumentException("tile not found");
      } else {
        this.mapTileId = tile.getFormattedName();
        this.mapTileBoundary = tile.getPolygon(this.numBoundaryPoints);
      }
    }
  }

  @ResultAttribute(index = 2)
  public double getLatitude() {
    return this.latitude;
  }

  @ResultAttribute(index = 3)
  public double getLongitude() {
    return this.longitude;
  }

  @ResultAttribute(index = 1)
  public String getMapGridName() {
    return this.mapGridName;
  }

  @ResultAttribute(index = 5, description = "The polygon boundary of the map tile.")
  public Polygon getMapTileBoundary() {
    return this.mapTileBoundary;
  }

  @ResultAttribute(index = 4, description = "The identifier of the map tile (e.g. 92G025).")
  public String getMapTileId() {
    return this.mapTileId;
  }

  @Required
  @RequestParameter(index = 2, minValue = "-180", maxValue = "180", units = "decimal degrees",
      description = "The latitude (decimal degrees) or the point to find the mapsheet for.")
  public void setLatitude(final double latitude) {
    this.latitude = latitude;
  }

  @Required
  @RequestParameter(index = 3, minValue = "-180", maxValue = "180", units = "decimal degrees",
      description = "The longitude (decimal degrees) or the point to find the mapsheet for.")
  public void setLongitude(final double longitude) {
    this.longitude = longitude;
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

  @JobParameter
  @RequestParameter(index = 4, minValue = "1", maxValue = "100",
      description = "The number of points to include on each edge of the polygon created for the map tile's bounding box.")
  @DefaultValue("20")
  public void setNumBoundaryPoints(final short numBoundaryPoints) {
    this.numBoundaryPoints = numBoundaryPoints;
  }
}
