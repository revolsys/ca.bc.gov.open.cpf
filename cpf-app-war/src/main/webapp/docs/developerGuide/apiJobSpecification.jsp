<div>
  <h1>Job Specification XML Document</h1>
  <p>
    The Job Specification document is an XML formatted summary definition of a job, which defines the parameters used to submit the job, the current status, link to the {jobId} resource used to get the Job Specification, and when the job is completed a link to the results resource to get the List of Job Results for a job.
  </p>
  <table id="table-api">
    <tr>
      <th>Attribute Name</th>
      <th>Type</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>jobId</td>
      <td>String</td>
      <td>The jobId is the unique identifier for the job.</td>
    </tr>
    <tr>
      <td>jobUrl</td>
      <td>/ws/users/{userId}/jobs/{jobId}</td>
      <td>The jobUrl is the URL to the resource which returns the JobSpecification for the Job.</td>
    </tr>
    <tr>
      <td>businessApplicationName</td>
      <td>String</td>
      <td>The businessApplicationName is the name of the business application the job was submitted to.</td>
    </tr>
    <tr>
      <td>businessApplicationVersion</td>
      <td>String</td>
      <td>The businessApplicationVersion is the version of the business application the job was submitted to.</td>
    </tr>
    <tr>
      <td>businessApplicationParameters</td>
      <td>Map</td>
      <td>The businessApplicationParameters contains a map of the parameters passed to the business application for a job.</td>
    </tr>
    <tr>
      <td>status</td>
      <td>String</td>
      <td>The stats is the current status of the job.</td>
    </tr>
    <tr>
      <td>numRequests</td>
      <td>int</td>
      <td>The numRequests is the number of requests submitted by the client application for this job.</td>
    </tr>
    <tr>
      <td>numCompletedRequests</td>
      <td>int</td>
      <td>The numCompletedRequests is the number of requests which have been processed and completed successfully.</td>
    </tr>
    <tr>
      <td>numFailedRequests</td>
      <td>int</td>
      <td>The numFailedRequests is the number of requests which permanently failed to complete.</td>
    </tr>
    <tr>
      <td>submissionTimestamp</td>
      <td>Timestamp</td>
      <td>The submissionTimestamp is the timestamp when the job was submitted.</td>
    </tr>
    <tr>
      <td>estimatedCompletionTimestamp</td>
      <td>Timestamp</td>
      <td>The estimatedCompletionTimestamp is the estimated time when the job will be completed.</td>
    </tr>
    <tr>
      <td>completionTimestamp</td>
      <td>Timestamp</td>
      <td>completionTimestamp </td>
    </tr>
    <tr>
      <td>jobResultsUrl</td>
      <td>/ws/users/{userId}/jobs/{jobId}/results</td>
      <td>The jobResultsUrl is the URL to the job results resource which returns the list of results for the job.</td>
    </tr>
  </table>
</div>
