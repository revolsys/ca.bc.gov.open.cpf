<div>
  <h1>Web Services API Reference</h1>
  <p>
    The Application Programming Interface for the CPF Web Services describes in detail the data associated with requests and responses for each of the available Web Service Resources.
  </p>
  
  <h2>File Formats</h2>
  <p>
    Batch Job Requests Files are sent to CPF Web Services, and Results and Error Files can be downloaded from the Web Services.  
    There are a number of different file formats associated with these files.
  </p>
  <p>
    The file formats available for Batch Job Request Files and Results Files are dependent upon the Business Application being invoked.
    See the specific Business Application details in the <a href="businessApplications.page">Business Applications Reference</a> for available file formats.
   </p>
   <p>
    Following are the file formats and associated <a href="http://en.wikipedia.org/wiki/MIME" title="Multipupose Internet Mail Extentions (MIME)" target="_blank" class="external">MIME types</a> used by the CPF Web Services:
    <ul>
      <li>
        <a href="http://en.wikipedia.org/wiki/Comma-separated_values" title="Comma Separated Values" target="_blank" class="external">Comma Separated Values</a> - text/csv 
      </li>
      <li>
        <a href="http://www.json.org/" title="JavaScript Object Notation" target="_blank" class="external">JavaScript Object Notation (JSON)</a> - application/json
      </li>
      <li>
        <a href="http://www.w3.org/TR/xhtml1/" title="The Extensible HyperText Markup Language (XHTML)" target="_blank" class="external">Extensible HyperText Markup Language (XHTML)</a> - application/xhtml+xml
      </li>
      <li>
        <a href="http://www.w3.org/XML/" title="The Extensible Markup Language (XML)" target="_blank" class="external">Extensible Markup Language (XML)</a> - text/xml
      </li>
      <li>
        <a href="http://en.wikipedia.org/wiki/Web_Application_Description_Language" title="Web Application Description Language (WADL)" target="_blank" class="external">Web Application Description Language (WADL)</a> - application/vnd.sun.wadl+xml
      </li>
    </ul>
   </p> 
     
  <h2>Error Files</h2>
  <p>
    All Error Files will be returned as text/csv files.  
    These Error Files will include a record for each request that failed.  
    Each error record will include the sequence number of the associated request, and error code, and a readable error message.
  </p>
  
  <h2>Web Service Methods</h2>
  <p>
    The following Web Service Methods can be invoked from an HTML form, web application or custom client application:
  </p>
  <ul id="apiRef">
    <li><a href="apiWebServices.page" title="Web Services Root - /ws">Web Services Root</a> - root container resource of the CPF Web Services</li>
    <li><a href="apiResource06.page" title="Business Applications Lis - /ws/apps">Business Applications List</a> - list all the available business applications</li>
    <li><a href="apiResource07.page" title="Business Application - /ws/apps/{businessApplicationName}">Business Application</a> - resource container for each business application</li>
    <li><a href="apiResource08.page" title="Business Application by Version - /ws/apps/{businessApplicationName}/{businessApplicationVersion}">Business Application by Version</a> - container resource for a specific version of a business application</li>
    <li><a href="apiResource09.page" title="Business Application Job Submission - /ws/apps/{businessApplicationName}/{businessApplicationVersion}/jobs">Business Application Job Submission</a> - resource used to submit new business application jobs</li>
    <li><a href="apiResource10.page" title="Userd - /ws/users">Users</a> - resource container for users of the system</li>
    <li><a href="apiResource11.page" title="Resources for User - /ws/users/{userId}">Resources for User</a> - resource container for a specific user</li>
    <li><a href="apiResource12.page" title="Business Application for User - /ws/users/{userId}/{businessApplicationName}">Business Application for User</a> - container for a specific business application of a user</li>
    <li><a href="apiResource13.page" title="Business Application Jobs List for User - /ws/users/{userId}/{businessApplicationName}/jobs">Business Application Jobs for User</a> - list the jobs owned by a user for a specific business application</li>
    <li><a href="apiResource14.page" title="Jobs List for User - /ws/users/{userId}/jobs">Jobs List for User</a> - list all the jobs owned by a user</li>
    <li><a href="apiResource15.page" title="Specific Job for User - /ws/users/{userId}/jobs/{jobId}">Specific Job for User</a> - allows a user to delete a specific job, or show its details</li>
    <li><a href="apiResource16.page" title="Result Files List for User Job - /ws/users/{userId}/jobs/{jobId}/results">Result Files List for User Job</a> - list the results and error results for a specific job for a user</li>
    <li><a href="apiResource17.page" title="Result File for User Job - /ws/users/{userId}/jobs/{jobId}/results/{resultId}">Result File for User Job</a> - the result or error result file for a specific job for a user</li>
  </ul>
</div>
