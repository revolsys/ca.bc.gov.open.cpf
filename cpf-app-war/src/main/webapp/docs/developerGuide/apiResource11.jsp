<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
  <h1>Resources for User</h1>
  <p>
    The /ws/users/{userId} resource is a container resource for the resources associated with user for the BPF.
  </p>
  <p>
    <b>URL:</b> /ws/users/{userID}<br />
    e.g. /ws/users/ae4a7382-85c0-46e0-986f-def6f7b9b12a
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource14.page">Jobs List for User</a></li>
    <li><a href="apiResource13.page">Business Application Jobs List for User</a></li>
  </ul>
  <h3>GET</h3>
  <p>
    The GET operation returns a <a href="apiWebServiceCapabilities.page">Web Service Capabilities Document</a> describing the following web service resources provided by the BPF.
  </p>
  <ul>
    <li>GET /ws/users/{userId}/jobs to get the list of jobs owned by a user</li>
    <li>GET /ws/users/{userId}/{businessApplicationName}/jobs to get the list of jobs owned by a user for a specific business application</li>
  </ul>
  <p>
    The other resources are not discoverable and are only available to a user if they have a URL to the resource returned by one of the other services.<br />
    For example the /ws/users/{userId}/jobs/{jobId} resource is returned when a job is created.
  </p>
  <p>
    If the userId is not valid a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5" target="_blank" title="404 - Not Found" class="external">404 Not Found</a> HTTP response will be returned.
  </p>
  <p>
    If the authUserId and userId do not represent the same user a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4" target="_blank" title="403 - Forbidden" class="external">403 Forbidden</a> HTTP response will be returned.
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
      <td>userId</td>
      <td>String</td>
      <td>The userId is the identifier user to get the resources for.</td>
    </tr>
  </table>
</div>
