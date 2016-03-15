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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.revolsys.gis.grid.BcgsConstants;
import com.revolsys.gis.grid.GridUtil;
import com.revolsys.gis.grid.NtsConstants;
import com.revolsys.io.IoFactory;
import com.revolsys.io.map.MapWriter;
import com.revolsys.io.map.MapWriterFactory;

public class CreateTestFiles {
  public static void main(final String[] args) {
    new CreateTestFiles().run();
  }

  private final Map<String, MapWriter> writers = new HashMap<String, MapWriter>();

  private void createBcgs1000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 4; number++) {
      final double lon = parentLon - GridUtil.getNumberCol4(number) * BcgsConstants.WIDTH_1000;
      final double lat = parentLat + GridUtil.getNumberRow4(number) * BcgsConstants.HEIGHT_1000;

      final String tileName = parentTileName + "." + number;
      write("BCGS 1:1000", "csv", "text/csv", tileName, lon, lat);
      createBcgs500(tileName, lon, lat);
    }

  }

  private void createBcgs10000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 4; number++) {
      final double lon = parentLon - GridUtil.getNumberCol4(number) * BcgsConstants.WIDTH_10000;
      final double lat = parentLat + GridUtil.getNumberRow4(number) * BcgsConstants.HEIGHT_10000;

      final String tileName = parentTileName + "." + number;

      write("BCGS 1:10 000", "csv", "text/csv", tileName, lon, lat);
      createBcgs5000(tileName, lon, lat);
    }

  }

  private void createBcgs1250(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 4; number++) {
      final double lon = parentLon - GridUtil.getNumberCol4(number) * BcgsConstants.HEIGHT_1250;
      final double lat = parentLat + GridUtil.getNumberRow4(number) * BcgsConstants.WIDTH_1250;

      final String tileName = parentTileName + "." + number;
      write("BCGS 1:1250", "csv", "text/csv", tileName, lon, lat);
    }

  }

  private void createBcgs2000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 100; number++) {
      final double lon = parentLon - GridUtil.getNumberCol100(number) * BcgsConstants.WIDTH_2000;
      final double lat = parentLat + GridUtil.getNumberRow100(number) * BcgsConstants.HEIGHT_2000;

      final String tileName = parentTileName + "." + GridUtil.formatSheetNumber100(number);

      write("BCGS 1:2000", "csv", "text/csv", tileName, lon, lat);
      createBcgs1000(tileName, lon, lat);
    }

  }

  private void createBcgs20000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 100; number++) {
      final double lon = parentLon - GridUtil.getNumberCol100(number) * BcgsConstants.WIDTH_20000;
      final double lat = parentLat + GridUtil.getNumberRow100(number) * BcgsConstants.HEIGHT_20000;

      final String tileName = parentTileName + "." + GridUtil.formatSheetNumber100(number);

      write("BCGS 1:20 000", "csv", "text/csv", tileName, lon, lat);
      createBcgs10000(tileName, lon, lat);
      // createBcgs2000(tileName, lon, lat);
    }

  }

  private void createBcgs2500(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 4; number++) {
      final double lon = parentLon - GridUtil.getNumberCol4(number) * BcgsConstants.WIDTH_2500;
      final double lat = parentLat + GridUtil.getNumberRow4(number) * BcgsConstants.HEIGHT_2500;

      final String tileName = parentTileName + "." + number;

      write("BCGS 1:2500", "csv", "text/csv", tileName, lon, lat);
      createBcgs1250(tileName, lon, lat);
    }

  }

  private void createBcgs500(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 4; number++) {
      final double lon = parentLon - GridUtil.getNumberCol4(number) * BcgsConstants.WIDTH_500;
      final double lat = parentLat + GridUtil.getNumberRow4(number) * BcgsConstants.HEIGHT_500;

      final String tileName = parentTileName + "." + number;

      write("BCGS 1:500", "csv", "text/csv", tileName, lon, lat);
    }

  }

  private void createBcgs5000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 4; number++) {
      final double lon = parentLon - GridUtil.getNumberCol4(number) * BcgsConstants.WIDTH_5000;
      final double lat = parentLat + GridUtil.getNumberRow4(number) * BcgsConstants.HEIGHT_5000;

      final String tileName = parentTileName + "." + number;

      write("BCGS 1:5 000", "csv", "text/csv", tileName, lon, lat);
      // createBcgs2500(tileName, lon, lat);
    }

  }

  private void createNts1000000() {
    for (int i = 80; i <= 110; i += 10) {
      for (int j = 2; j <= 4; j++) {
        final double lat = 48 + (j - 2) * 4;
        final double lon = -112 - (i / 10 - 8) * 8;

        final String tileName = String.valueOf(i + j);
        write("NTS 1:1 000 000", "csv", "text/csv", tileName, lon, lat);
        createNts500000(tileName, lon, lat);
        createNts250000(tileName, lon, lat);
      }
    }

  }

  private void createNts125000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int v = 0; v <= 1; v++) {
      double lat = parentLat;
      char northSouth;
      if (v == 0) {
        northSouth = 'S';
      } else {
        lat += NtsConstants.HEIGHT_125000;
        northSouth = 'N';
      }
      for (int h = 0; h <= 1; h++) {
        double lon = parentLon;
        char eastWest;
        if (h == 1) {
          eastWest = 'W';
          lon -= NtsConstants.WIDTH_125000;
        } else {
          eastWest = 'E';
        }
        final String tileName = parentTileName + "/" + northSouth + "." + eastWest + ".";
        write("NTS 1:125 000", "csv", "text/csv", tileName, lon, lat);
      }
    }

  }

  private void createNts25000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (char letter8 = 'a'; letter8 <= 'h'; letter8++) {
      final double lat = parentLat + GridUtil.getLetter8Row(letter8) * NtsConstants.HEIGHT_25000;
      final double lon = parentLon - GridUtil.getLetter8Col(letter8) * NtsConstants.WIDTH_25000;

      final String tileName = parentTileName + letter8;
      write("NTS 1:25 000", "csv", "text/csv", tileName, lon, lat);
    }
  }

  private void createNts250000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (char letter = 'A'; letter <= 'P'; letter++) {
      final double lon = parentLon - GridUtil.getLetter16Col(letter) * NtsConstants.WIDTH_500000;
      final double lat = parentLat + GridUtil.getLetter16Row(letter) * NtsConstants.HEIGHT_500000;

      final String tileName = parentTileName + letter;
      write("NTS 1:250 000", "csv", "text/csv", tileName, lon, lat);
      createNts125000(tileName, lon, lat);
      createNts50000(tileName, lon, lat);
      createBcgs20000(tileName, lon, lat);
    }
  }

  private void createNts50000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int number = 1; number <= 16; number++) {
      final double lat = parentLat + GridUtil.getNumberRow16(number) * NtsConstants.HEIGHT_50000;
      final double lon = parentLon - GridUtil.getNumberCol16(number) * NtsConstants.WIDTH_50000;

      final String tileName = parentTileName + "/" + number;

      write("NTS 1:50 000", "csv", "text/csv", tileName, lon, lat);
      createNts25000(tileName, lon, lat);
    }

  }

  private void createNts500000(final String parentTileName, final double parentLon,
    final double parentLat) {
    for (int v = 0; v <= 1; v++) {
      double lat = parentLat;
      char northSouth;
      if (v == 0) {
        northSouth = 'S';
      } else {
        lat += NtsConstants.HEIGHT_500000;
        northSouth = 'N';
      }
      for (int h = 0; h <= 1; h++) {
        double lon = parentLon;
        char eastWest;
        if (h == 1) {
          eastWest = 'W';
          lon -= NtsConstants.WIDTH_500000;
        } else {
          eastWest = 'E';
        }
        final String tileName = parentTileName + northSouth + "." + eastWest + ".";
        write("NTS 1:500 000", "csv", "text/csv", tileName, parentLon, parentLat);
      }
    }
  }

  private MapWriter getWriter(final String gridName, final String key, final String type,
    final String mimeType) {
    final String name = gridName.replaceAll(" 1:", "-").replaceAll(" ", "") + "-" + key + "."
      + type;
    MapWriter writer = this.writers.get(name);
    if (writer == null) {
      try {
        final File file = new File("src/data/" + name);
        file.getParentFile().mkdirs();
        final FileOutputStream out = new FileOutputStream(file);
        final MapWriterFactory writerFactory = IoFactory.factoryByMediaType(MapWriterFactory.class,
          mimeType);
        writer = writerFactory.newMapWriter(out);
        this.writers.put(name, writer);
      } catch (final IOException e) {
        throw new RuntimeException(name, e);
      }
    }
    return writer;

  }

  private void run() {
    createNts1000000();
    for (final MapWriter writer : this.writers.values()) {
      writer.flush();
      writer.close();
    }
  }

  private void write(final String gridName, final String type, final String mimeType,
    final String tileName, final double lon, final double lat) {
    final Map<String, String> locationValues = new LinkedHashMap<String, String>();
    locationValues.put("mapGridName", gridName);
    locationValues.put("numBoundaryPoints", "20");
    locationValues.put("latitude", String.valueOf(lat));
    locationValues.put("longitude", String.valueOf(lon));
    write(gridName, type, mimeType, "by-location", locationValues);

    final Map<String, String> nameValues = new LinkedHashMap<String, String>();
    nameValues.put("mapGridName", gridName);
    nameValues.put("numBoundaryPoints", "20");
    nameValues.put("mapTileId", tileName);
    write(gridName, type, mimeType, "by-name", nameValues);

  }

  private void write(final String gridName, final String type, final String mimeType,
    final String key, final Map<String, String> values) {
    final MapWriter writer = getWriter(gridName, key, type, mimeType);
    writer.write(values);
  }
}
