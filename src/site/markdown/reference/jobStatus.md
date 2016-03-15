## Job Status
The following status fields are returned with the job status.

<div class="table-responsive">
<table class="table table-striped tabled-bordered table-condensed">
  <thead>
    <tr>
      <th>Field Name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>id</td>
      <td>The id of this job, in the form of URL to the job.</td>
    </tr>
    <tr>
      <td>consumerKey</td>
      <td>The oAuth Consumer Key of the user who submitted the job.</td>
    </tr>
    <tr>
      <td>businessApplicationName</td>
      <td>The business application name associated with this job.</td>
    </tr>
    <tr>
      <td>businessApplicationVersion</td>
      <td>The version of the business application associated with this job.</td>
    </tr>
    <tr>
      <td><i>&lt;jobParameter&gt;</i></td>
      <td>Each business application specific job parameter will be included as a field.</td>
    </tr>
    <tr>
      <td>resultDataContentType</td>
      <td>The desired <a href="fileFormats.html">file format</a> type of the generated
      result data files.</td>
    </tr>
    <tr>
      <td>jobStatus</td>
      <td>The current processing status of this job.  A completed job will have a status of
      "resultsCreated", while a job that has yet to be queued for processing will have a 
      status of "submitted".</td>
    </tr>
    <tr>
      <td>secondsToWaitForStatusCheck</td>
      <td>The minimum number of seconds a client should wait before checking the status of
      a job again. 0 if the job processing has completed.</td> 
    </tr>
    <tr>
      <td>numSubmittedRequests</td>
      <td>Initially this will be set to the value passed in with the job.  Once the input
      request data has been validated, it will be updated to the actual number of requests in
      the Batch Job.</td>
    </tr>
    <tr>
      <td>numCompletedRequests</td>
      <td>The current number of Batch Job Requests successfully processed and returned by the
      Business Application.</td>
    </tr>
    <tr>
      <td>numFailedRequests</td>
      <td>The current number of Batch Job Requests that have failed processing by the Business
      Application.  If any input request data fails validation, then the job is not
      processed at all by the Business Application, and the number of failed requests will be
      set to the number of submitted requests.</td>
    </tr>
    <tr>
      <td>resultsUrl</td>
      <td>If the job has completed successfully, and result data files have been generated,
      then this URL will list the URL and details of each result file associated with this job.
      This field will not be present if the job processing has not completed.</td>
    </tr>
  </tbody>
</table>
</div>