<div>
  <h1>Result File for User Job</h1>
  <p>
    The /ws/users/{userId}/jobs/{jobId}/results/{resultId} resource represents a result file for a job owned by a user.
  </p>
  <p>
    <b>URL:</b>/ws/users/{userID}/jobs/{jobId}/results/{resultId}<br />
    e.g./ws/users/ae4a7382-85c0-46e0-986f-def6f7b9b12a/jobs/5326/results/8245
  </p>
  <h3>GET</h3>
  <p>
    The GET operation returns the result file for a job.
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
    <tr>
      <td>&laquo;pathParameter&raquo;</td>
      <td>resultID</td>
      <td>String</td>
      <td>The resultId is the unique identifier for the result to be downloaded.</td>
    </tr>
  </table>
</div>
