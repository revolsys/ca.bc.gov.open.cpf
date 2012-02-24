<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
  <h1>Business Application</h1>
  <p>
    The /ws/apps/{businessApplicationName} resource is the container resource for each business application, with the resource name being the name of the business application.<br />
    For example the resource name for the business application mapTileByLocation would be /ws/apps/mapTileByLocation.
  </p>
  <p>
    <b>URL:</b> /ws/apps/{businessApplicationName}<br />
    e.g. <a href="<c:out value="${baseUrl}"/>/ws/apps/mapTileByTileId" title="business application get map tile by tile id">/ws/apps/mapTileByTileId</a>
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource08.page">Business Application by Version</a> for each version compatible with the most recent version</li>
  </ul>
  <h3>GET</h3>
  <p>
    The GET operation returns a HTTP response with a link to the /ws/apps/{businessApplicationName}/{businessApplicationVersion} resource for the most recent version of the business application.<br /> 
    e.g. /ws/apps/mapTileByLocation/1.0.0
  </p>
  <p>
    If the business application name is not valid a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5" target="_blank" title="404 - Not Found" class="external">404 Not Found</a> HTTP response will be returned. 
  </p>
  <table id="table-api">
    <tr>
      <th>Stereotype</th>
      <th>Parameter Name</th>
      <th>Type</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>&laquo;pathParameter&raquo;</td>
      <td>businessApplicationName</td>
      <td>String</td>
      <td>The businessApplicationName is the name of the business application.</td>
    </tr>
  </table>
</div>
