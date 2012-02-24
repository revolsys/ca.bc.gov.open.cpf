<div>
  <h1>Specific Job for User</h1>
  <p>
    The /ws/users/{userId}/jobs/{jobId} resource represents a job owned by a user and is a container resource for the resources associated with the job.
  </p>
  <p>
    <b>URL:</b>/ws/users/{userID}/jobs/{jobId}<br />
    e.g./ws/users/ae4a7382-85c0-46e0-986f-def6f7b9b12a/jobs/5326
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource16.page" title="">Results Files List for User Job</a></li>
  </ul>
  <h3>GET</h3>
  <p>
    The GET operation returns a <a href="apiJobSpecification.page" title="Job specification XML document">Job Specification XML document</a> containing the high level description of the job. 
    If the job has been completed and results are available the document will include a link to the /ws/users/{userId}/jobs/{jobId}/results resource for the Job.
  </p>
  <p>
    If the userId is not valid a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5" target="_blank" title="404 - Not Found" class="external">404 Not Found</a> HTTP response will be returned.
  </p>
  <p>
    If the authUserId and userId do not represent the same user a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4" target="_blank" title="403 Forbidden" class="external">403 Forbidden</a> HTTP response will be returned.
  </p>
  <table>
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
  <h3>DELETE</h3>
  <p>
    The DELETE operation cancels and deletes the job. Further processing of the job will be cancelled and all input data and result data will be deleted from the job. 
    Any statistics will be summarized and added to the statistics for the current period.
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
