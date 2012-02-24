<div>
  <h1>Api Reference</h1>
  <h2>JobResult</h2>
  <p>
    The JobResult describes the metadata for a job result {resultId} resource, including a link to the resource.
  </p>
  <table id="table-api">
    <tr>
      <th>Attribute Name</th>
      <th>Type</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>resultId</td>
      <td>String</td>
      <td>The id is the unique identifier of the job result</td>
    </tr>
    <tr>
      <td>sequenceNumber</td>
      <td>String</td>
      <td>The sequenceNumber is the incremental sequence number allocated based on the order of the request in the input data. This is only used if the result data is in one file per business application request.</td>
    </tr>
    <tr>
      <td>jobResultUrl</td>
      <td>String</td>
      <td>The jobResultUrl is the URL to the resource to download the result from.</td>
    </tr>
    <tr>
      <td>contentType</td>
      <td>String</td>
      <td>The contentType is the MIME content type of the result file.</td>
    </tr>
    <tr>
      <td>size</td>
      <td>long</td>
      <td>The size is the size of the result file in bytes.</td>
    </tr>
  </table>
</div>
