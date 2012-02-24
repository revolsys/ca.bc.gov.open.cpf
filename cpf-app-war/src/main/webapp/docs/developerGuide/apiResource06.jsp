<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
  <h1>Business Applications List</h1>
  <p>
    The /ws/apps resource is the container resource for business applications, providing a list of all Business Applications names available to the web services.
  </p>
  <p>
    <b>URL:</b> <a href="<c:out value="${baseUrl}"/>/ws/apps" title="business applications list">/ws/apps</a>
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource08.page">Business Application by Version</a></li>
  </ul>
  <h3>GET</h3>
  <p>
    The GET operation returns a list of business applications names with a link to the /ws/apps/{businessApplicationName}/{businessApplicationVersion} resource for the most recent version of each business application.<br /> 
    e.g. <a href="<c:out value="${baseUrl}"/>/ws/apps/mapTileByLocation/1.0.0">/ws/apps/mapTileByLocation/1.0.0</a>
  </p>
</div>
