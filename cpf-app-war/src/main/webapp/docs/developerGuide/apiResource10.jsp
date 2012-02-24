<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
  <h1>Users</h1>
  <p>
    The /ws/users resource is the container resource for the users of the system.
  </p>
  <p>
    <b>URL:</b> <a href="<c:out value="${baseUrl}"/>/ws/users" title="users">/ws/users</a>
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource11.page">Resources for User</a></li>
  </ul>
  <h3>GET</h3>
  <p>
    The GET operation returns a <a href="apiWebServiceCapabilities.page">Web Service Capabilities Document</a> describing the following web service resources provided by the BPF.
    <ul>
      <li>GET /ws/users/{userId}/jobs to get the list of jobs owned by a user</li>
      <li>GET /ws/users/{userId}/{businessApplicationName}/jobs to get the list of jobs owned by a user for a specific business application</li>
    </ul>
  </p>
  <p>
    The other resources are not discoverable, and are only available to a user if they have a URL to the resource returned by one of the other services.<br />
    For example the /ws/users/{userId}/jobs/{jobId} resource is returned when a job is created.
  </p>
</div>
