<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
  <h1>Business Application by Version</h1>
  <p>
    The /ws/apps/{businessApplicationName}/{businessApplicationVersion} resource is the container resource for each version of a business application, with the resource name being the version of the business application.<br /> 
    For example the resource name for version 1.0.0 of the business application mapTileByLocation would be /ws/apps/mapTileByLocation/1.0.0.
  </p>
  <p>
    <b>URL:</b> /ws/apps/{businessApplicationName}/{businessApplicationVersion}<br />
    e.g. <a href="<c:out value="${baseUrl}"/>/ws/apps/mapTileByTileId/1.0.0" title="get version 1.0.0 of business application get map tile by tile id">/ws/apps/mapTileByTileId/1.0.0</a>
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource09.page">Business Application Job Submission</a></li>
  </ul>
  <h3>GET</h3>
  <p>
    The GET operation returns a <a href="apiWebServiceCapabilities.page">Web Service Capabilities document</a> describing the specified version of the web service resources for the named business application.
  </p>
  <p> 
    If the version is not currently supported the response will be a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5" target="_blank" title="Not Found" class="external">404 Not Found</a> will be returned, with a link to the /{businessApplicationName}/{businessApplicationVersion} resource for the most recent version. 
    The user can decide to follow the link to get the capabilities document of the most recent version, or stop further communication.
  </p>
  <table id="table-api">
    <tr>
      <th>Stereotype</th>
      <th>Attribute Name</th>
      <th>Type</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>&laquo;pathParameter&raquo;</td>
      <td>businessApplicationName</td>
      <td>String</td>
      <td>The businessApplicationName is the name of the business application.</td>
    </tr>
    <tr>
      <td>&laquo;pathParameter&raquo;</td>
      <td>businessApplicationVersion</td>
      <td>String</td>
      <td>The businessApplicationVersion is the version of the business application.</td>
    </tr>
  </table>
</div>
