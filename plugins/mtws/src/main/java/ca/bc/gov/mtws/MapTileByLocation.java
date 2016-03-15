package ca.bc.gov.mtws;

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

@BusinessApplicationPlugin(
    numRequestsPerWorker = 100,
    instantModePermission = "permitAll",
    description = "The Map Tile by Location service returns the map tile id and polygon boundary for the map tile specified by latitude/longitude location.")
public class MapTileByLocation {

  private double latitude;

  private double longitude;

  private String mapTileId;

  private Polygon mapTileBoundary;

  private String mapGridName;

  private short numBoundaryPoints = 50;

  public void execute() {
    final RectangularMapGrid grid = RectangularMapGridFactory.getGrid(mapGridName);
    if (grid == null) {
      throw new IllegalArgumentException("Grid not supported " + mapGridName);
    } else {
      final RectangularMapTile tile = grid.getTileByLocation(longitude,
        latitude);
      if (tile == null) {
        throw new IllegalArgumentException("tile not found");
      } else {
        mapTileId = tile.getFormattedName();
        mapTileBoundary = tile.getPolygon(numBoundaryPoints);
      }
    }
  }

  @ResultAttribute()
  public double getLatitude() {
    return latitude;
  }

  @ResultAttribute
  public double getLongitude() {
    return longitude;
  }

  @ResultAttribute
  public String getMapGridName() {
    return mapGridName;
  }

  @ResultAttribute(description = "The polygon boundary of the map tile.")
  public Polygon getMapTileBoundary() {
    return mapTileBoundary;
  }

  @ResultAttribute(
      description = "The identifier of the map tile (e.g. 92G025).")
  public String getMapTileId() {
    return mapTileId;
  }

  @Required
  @RequestParameter(
      index = 2,
      minValue = "-180",
      maxValue = "180",
      units = "decimal degrees",
      description = "The latitude (decimal degrees) or the point to find the mapsheet for.")
  public void setLatitude(final double latitude) {
    this.latitude = latitude;
  }

  @Required
  @RequestParameter(
      index = 3,
      minValue = "-180",
      maxValue = "180",
      units = "decimal degrees",
      description = "The longitude (decimal degrees) or the point to find the mapsheet for.")
  public void setLongitude(final double longitude) {
    this.longitude = longitude;
  }

  @AllowedValues(value = {
    "NTS 1:1 000 000", "NTS 1:500 000", "NTS 1:250 000", "NTS 1:125 000",
    "NTS 1:50 000", "NTS 1:25 000", "BCGS 1:20 000", "BCGS 1:10 000",
    "BCGS 1:5 000", "BCGS 1:2 500", "BCGS 1:2 000", "BCGS 1:1250",
    "BCGS 1:1 000", "BCGS 1:500", "MTO"
  })
  @Required
  @JobParameter
  @RequestParameter(
      index = 1,
      description = "The name of the Map Grid the Map Tile ID is from (e.g. NTS 1:250 000).")
  @DefaultValue("NTS 1:250 000")
  public void setMapGridName(final String mapGridName) {
    this.mapGridName = StringUtils.trimWhitespace(mapGridName);
  }

  @JobParameter
  @RequestParameter(
      index = 4,
      minValue = "1",
      maxValue = "100",
      description = "The number of points to include on each edge of the polygon created for the map tile's bounding box.")
  @DefaultValue("20")
  public void setNumBoundaryPoints(final short numBoundaryPoints) {
    this.numBoundaryPoints = numBoundaryPoints;
  }
}
