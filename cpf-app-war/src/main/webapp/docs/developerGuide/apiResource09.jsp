<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
  <h1>Business Application Job Submission</h1>
  <p>
    The /ws/apps/{businessApplicationName}/{businessApplicationVersion}/jobs resource is used to submit new jobs to a business application.
  </p>
  <p>
    <b>URL:</b> /ws/apps/{businessApplicationName}/{businessApplicationVersion}/jobs<br />
    e.g. <a href="<c:out value="${baseUrl}"/>/ws/apps/mapTileByTileId/1.0.0/jobs" title="submit a batch job for business application get map tile by tile id">/ws/apps/mapTileByTileId/1.0.0/jobs</a>
  </p>
  <p>
    This URL also provides an HTML Form for manual job submission.
  </p>
  
  <h3>POST</h3>
  <p>
    The POST operation creates a new job to execute the business application requests asynchronously.
  </p>
  <p>
    If the job was accepted a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.2" target="_blank" title="201 - Created" class="external">201 Created</a> HTTP response with a link to the /ws/users/{userId}/jobs/{jobId} resource describing the created job will be returned.
  </p>
  <p> 
    If the businessApplicationVersion is not supported the response will be a <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5" target="_blank" title="404 - Not Found" class="external">404 Not Found</a> will be returned,  with a link to the /{businessApplicationName}/{businessApplicationVersion} resource for the most recent version.
  </p>
  <table id="table-api">
    <tr>
      <th>Stereotype</th>
      <th>Parameter Name</th>
      <th>Type</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>&laquo;pathParameter&raquo;</td>
      <td>businessApplicationName</td>
      <td>String</td>
      <td>The businessApplicationName is the name of the business application.</td>
    </tr>
    <tr>
      <td>&laquo;pathParameter&raquo;</td>
      <td>businessApplicationVersion</td>
      <td>String</td>
      <td>The businessApplicationVersion is the version of the business application.</td>
    </tr>
    <tr>
      <td>&laquo;formData&raquo;</td>
      <td>numRequests</td>
      <td>int</td>
      <td>The numRequests attribute is the number of requests the client application is submitting to be processed by the business application.</td>
    </tr>
    <tr>
      <td>&laquo;formData&raquo;</td>
      <td>inputDataContentType</td>
      <td>String</td>
      <td>The inputDataContentType is the MIME contentType of the external structured input data specified by the inputDataUrl.</td>
    </tr>
    <tr>
      <td>&laquo;formData&raquo;</td>
      <td>inputDataUrl</td>
      <td>String</td>
      <td>The inputDataUrl is the HTTP/HTTPS/FTP URL to the file containing the input data.</td>
    </tr>
    <tr>
      <td>&laquo;formData&raquo;</td>
      <td>inputData</td>
      <td>byte[]</td>
      <td>The inputData parameter is the file containing the input data for the business application requests provided as a file in the form data.</td>
    </tr>
    <tr>
      <td>&laquo;formData&raquo;</td>
      <td>resultDataContentType</td>
      <td>String</td>
      <td>The resultDataContentType parameter contains a list of MIME content types that the client application will accept the result data in. The content type selected will depend on those supported by the business application and the rules defined in the HTTP specification.</td>
    </tr>
    <tr>
      <td>&laquo;formData&raquo;</td>
      <td>{businessApplicationParameter Name}</td>
      <td>String</td>
      <td>The {businessApplication ParameterName} represents each parameter to pass to the business application with each request. The parameter names and values will be defined on a per business application basis.</td>
    </tr>
  </table>
</div>
