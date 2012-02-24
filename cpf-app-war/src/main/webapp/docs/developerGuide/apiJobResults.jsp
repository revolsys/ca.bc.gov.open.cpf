<div>
  <h1>Api Reference</h1>
  <h2>JobResults</h2>
  <p>
    The JobResults is an XML document which provides links to the detailed results and error results for a Job. 
    If the job contained errors there will be a link to the {resultId} resource containing the XML JobError document. 
    If the job had at least one business application request then there will be one or more results with links to the {resultId} resource. 
    If the business application supports structured data there will be one link to the {resultId} resource containing with one record per result. 
    If the business application supports opaque result data there will be a link to the {resultId} resource for each business application request which completed successfully. 		
  </p>
  <table id="table-api">
    <tr>
      <th>Attribute Name</th>
      <th>Type</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>errorResult</td>
      <td>JobResult</td>
      <td>The errorResult is the JobResult containing the XML file with any errors in processing the job or any business application requests in the job.</td>
    </tr>
    <tr>
      <td>results</td>
      <td>JobResult</td>
      <td>The results is the list of JobResults created for the job.</td>
    </tr>
  </table>
</div>
