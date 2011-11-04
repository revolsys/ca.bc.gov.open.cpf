<div>
  <h1>Map Tile by Id</h1>
  
  <table class="appDesc">
  <tr>
    <th style="width:30%;">Application Name</th>
    <td>mapTileById</td>
  </tr>
  <tr>
    <th>Description</th>
    <td>The Map Tile by Id business application will return the metadata for the map tile with the specified map tile id.</td>
  </tr>
  <tr>
    <th>Version</th>
    <td>1.0.0</td>
  </tr>
  <tr>
    <th>Compatible versions</th>
    <td>1.0.0</td>
  </tr>
  <tr>
    <th>Maximum requests per job</th>
    <td>10,000</td>
  </tr>
  <tr>
    <th>Average requests per second</th>
    <td>100 (times may vary)</td>
  </tr>
  <tr>
    <th>Input data content types</th>
    <td>
      text/csv<br />
      application/json<br />
      application/xhtml+xml<br />
      text/xml<br />
    </td>
  </tr>
  <tr>
    <th>Per request input data</th>
    <td>No</td>
  </tr>
  <tr>
    <th>Result data content types</th>
    <td>
      text/csv<br />
      application/json<br />
      application/xhtml+xml<br />
      text/xml<br />
    </td>
  </tr>
  <tr>
    <th>Per request result data</th>
    <td>No</td>
  </tr>
  </table>
  
  <h2>Request Parameters</h2>
  <table class="appDesc">
  <tr>
    <th>Parameter name</th>
    <th>Data type</th>
    <th>Description</th>
    <th>Required</th>
    <th>Job level</th>
    <th>Request level</th>
    <th>Values</th>
  </tr>
  <tr>
    <td>mapGridName</td>
    <td>String</td>
    <td>The mapGridName is the name of the map tile grid standard.</td>
    <td>Yes</td>
    <td>Yes</td>
    <td>Yes</td>
    <td>
      NTS<br />
      BCGS<br />
      MTO
  </td>
  </tr>
  <tr>
    <td>mapInverseScale</td>
    <td>String</td>
    <td>The mapInverse Scale is the scale of the specific map tile grid within the standard. For example BCGS 1:20k would be specified as 20000.</td>
    <td>No</td>
    <td>Yes</td>
    <td>Yes</td>
    <td>
      500000<br />
      250000<br />
      125000<br />
      50000<br />
      25000<br />
      20000<br />
      10000<br />
      5000<br />
      2500<br />
      1000<br />
      500
    </td>
  </tr>
  <tr>
    <td>numBoundaryPoints</td>
    <td>integer</td>
    <td>The numBoundary Points is number of points on the latitude boundary lines.</td>
    <td>No</td>
    <td>Yes</td>
    <td>Yes</td>
    <td></td>
  </tr>
  <tr>
    <td>mapTileId</td>
    <td>String</td>
    <td>The mapTileId is the map file identifier specific to the mapGridName and mapInverseScale.</td>
    <td>Yes</td>
    <td>No</td>
    <td>Yes</td>
    <td></td>
  </tr>
  </table>
  
  <h2>Result Data Fields</h2>
  <table class="appDesc">
  <tr>
    <th>Field name</th>
    <th>Data type</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>mapGridName</td>
    <td>String</td>
    <td>The mapGridName is the name of the map tile grid standard.</td>
  </tr>
  <tr>
    <td>mapInverseScale</td>
    <td>String</td>
    <td>The mapInverseScale is the scale of the specific map tile grid within the standard. For example BCGS 1:20k would be specified as 20000.</td>
  </tr>
  <tr>
    <td>latitude</td>
    <td>decimal</td>
    <td>The decimal degrees of latitude searched for.</td>
  </tr>
  <tr>
    <td>longitude</td>
    <td>decimal</td>
    <td>The decimal degrees of longitude searched for.</td>
  </tr>
  <tr>
    <td>mapTileId</td>
    <td>String</td>
    <td>The mapTileId is the map file identifier specific to the mapGridName and mapInverseScale.</td>
  </tr>
  <tr>
    <td>mapTileBoundary</td>
    <td>String</td>
    <td>The mapTileBoundary is the polygonal boundary of the map tile. The boundary will include the number of points as specified in numBoundaryPoints for each line of latitude.</td>
  </tr>
  </table>
</div>
