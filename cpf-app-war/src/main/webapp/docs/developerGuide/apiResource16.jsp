<div>
  <h1>Results Files List for User Job</h1>
  <p>
    The /ws/users/{userId}/jobs/{jobId}/results resource is a container resource for the /ws/users/{userId}/jobs/{jobId}/results/{resultId} resource for a job.
  </p>
  <p>
    <b>URL:</b>/ws/users/{userID}/jobs/{jobId}/results<br />
    e.g./ws/users/ae4a7382-85c0-46e0-986f-def6f7b9b12a/jobs/5326/results
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource17.page">Result file for User Job</a> - results files could be data results or error results files.</li>
  </ul>
  <h3>GET</h3>
  <p>
    The GET operation returns the JobResults XML document for the jobId, which includes links to the /ws/users/{userId}/job/{jobId}/results/{resultId} (job result) resource for each result and error data file.
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
    <tr>
      <td>&laquo;pathParameter&raquo;</td>
      <td>jobID</td>
      <td>String</td>
      <td>The jobId is the unique identifier of the job.</td>
    </tr>
  </table>
</div>
