<div>
  <h1>Web Services Root</h1>
  <p>
    The Web Services Root is the root container resource of the CPF batch web services. 
  </p>
  <p>
    <b>URL:</b> <a href="<c:out value="${baseUrl}"/>/ws/" title="web services root">/ws/</a>
  </p>
  <p>
    The resource contains the following child resources.
  </p>
  <ul>
    <li><a href="apiResource10.page">Users</a></li>
    <li><a href="apiResource06.page">Business Applications List</a></li>
  </ul>
  
  <h3>GET</h3>
  <p>
    The GET operation returns a <a href="apiWebServiceCapabilities.page">Web Service Capabilities Document</a> describing the following web service resources provided by the BPF.
  </p>
  <ul>
    <li>GET /ws/users/{userId}/jobs to get the list of jobs owned by a user</li>
    <li>GET /ws/users/{userId}/{businessApplicationName}/jobs to get the list of jobs owned by a user for a specific business application</li>
    <li>POST /{businessApplicationName}/{businessApplicationVersion/jobs to submit a job to a business application for a user</li>
  </ul>
  <p>
    The other resources are not discoverable and are only available to a user if they have a URL to the resource returned by one of the other services. 
  </p>
  <p>
    For example the	/ws/users/{userId}/jobs/{jobId} resource is returned when a job is created.
  </p>
</div>
