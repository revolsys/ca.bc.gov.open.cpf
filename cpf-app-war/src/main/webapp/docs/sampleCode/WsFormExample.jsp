<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
    <form method="post" enctype="multipart/form-data" action="<c:out value="${baseUrl}"/>/ws/apps/mapTileByLocation/1.0.0/jobs">
      <dl>
        <dt>
          <label for="authUserId">User ID</label>
        </dt>
        <dd>
          <input name="authUserId" type="text" size="50" maxlength="255" />
        </dd>
        <dt>
          <label for="numRequests">Num Requests</label>
        </dt>
        <dd>
          <input name="numRequests" type="text" size="50" maxlength="255" />
        </dd>
        <dt>
          <label for="inputDataContentType">Input Data Content Type</label>
        </dt>
        <dd>
          <select name="inputDataContentType">
            <option>text/csv</option>
            <option>application/json</option>
          </select>
        </dd>
        <dt>
          <label for="inputDataUrl">Input Data Url</label>
        </dt>
        <dd>
          <input name="inputDataUrl" type="text" size="50" maxlength="255" />
        </dd>
        <dt>
          <label for="inputData">Input Data</label>
        </dt>
        <dd>
          <input name="inputData" type="file" />
        </dd>
        <dt>
          <label for="resultDataContentType">Result Data Content Type</label>
        </dt>
        <dd>
          <select name="resultDataContentType">
            <option>text/csv</option>
            <option>application/json</option>
            <option>application/xhtml+xml</option>
            <option>text/xml</option>
          </select>
        </dd>
        <dt>
          <label for="mapGridName">Map Grid Name</label>
        </dt>
        <dd>
          <select name="mapGridName">
            <option value="">-</option>
            <option>NTS</option>
            <option>BCGS</option>
            <option>MTO</option>
          </select>
        </dd>
        <dt>
          <label for="mapInverseScale">Map Inverse Scale</label>
        </dt>
        <dd>
          <select name="mapInverseScale">
            <option value="">-</option>
            <option>1000000</option>
            <option>500000</option>
            <option>250000</option>
            <option>125000</option>
            <option>50000</option>
            <option>25000</option>
            <option>20000</option>
            <option>10000</option>
            <option>5000</option>
            <option>2500</option>
            <option>2000</option>
            <option>1250</option>
            <option>1000</option>
            <option>500</option>
          </select>
        </dd>
        <dt>
          <label for="numBoundaryPoints">Num Boundary Points</label>
        </dt>
        <dd>
          <input name="numBoundaryPoints" type="text" size="50" maxlength="255" />
        </dd>
      </dl>
      <input name="Submit" type="submit" value="submit" />
    </form>
</div>