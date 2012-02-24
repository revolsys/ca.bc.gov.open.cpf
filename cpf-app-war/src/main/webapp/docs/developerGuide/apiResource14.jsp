<div>
  <h1>Jobs List for User</h1>
  <p>
    The /ws/users/{userId}/jobs resource is a container resource for the jobs owned by a user.
  </p>
  <p>
    <b>URL:</b> /ws/users/{userID}/jobs<br />
    e.g./ws/users/ae4a7382-85c0-46e0-986f-def6f7b9b12a/jobs
  </p>
  <p>
    The resource contains the following child resources. 
	  <ul>
	    <li>/ws/users/{userId}/jobs/{jobId}</li>
	  </ul>
  </p>
  <h3>GET</h3>
  <p>
    The GET operation returns a <a href="apiJobList.page" title="Job List XML document">Job List XML document</a> with a list of links to the /ws/users/{userId}/jobs/{jobId} resources owned by the user.
  </p>
  <p>
    If the userId is not valid a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5" target="_blank" title="404 - Not Found" class="external">404 Not Found</a> HTTP response will be returned.
  </p>
  <p>
    If the authUserId and userId do not represent the same user a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4" target="_blank" title="403 Forbidden" class="external">403 Forbidden</a> HTTP response will be returned.
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
