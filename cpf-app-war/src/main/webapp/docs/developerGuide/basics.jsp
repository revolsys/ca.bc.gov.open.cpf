<div>
  <h1>Getting Started</h1>
  <p>
    The CPF Web Services enable a client, through a browser or a client application, to request information from CPF data resources.
  </p>
  <h3>An Example</h3>
  <p>
    An engineering surveyor has thousands of longitude/latitude reference points of locations of road and bridge infrastructure requiring repairs.
    The surveyor wants to obtain a list of all of the <a href="http://maps.nrcan.gc.ca/topo_e.php" title="The National Topographic System of Canada" target="_blank" class="external">NTS</a> 1:50000 Map ID's associated with these reference points.
  </p>
  <p>
    The surveyor creates a Batch Job Request text file, each request line of the file representing a single request to get the <a href="http://maps.nrcan.gc.ca/topo_e.php" title="The National Topographic System of Canada" target="_blank">NTS</a> 1:50000 Map ID of the map containing a specific reference point.
    After <a href="security.page">registering/signing-on</a> to the CPF Batch Web Service, that Batch Job Request file is POSTed to the Web Service, either by using a simple HTTP POST form, an HTTP/JavaScript web application, or by using a custom developed client application.
  </p>
  <p>
    On submission, the Job ID of the POSTed job is returned.  Using this Job ID, another simple HTTP GET request returns the current processing status of the job, which will include whether or not the job processing has completed, how many individual requests have been processed so far, and an expected completion time for the processing of the entire batch job of requests. This Status method can be periodically polled until the Batch Job has completed processing. 
  </p>
  <p>
    Once the batch job processing has been completed, the job status request will also return a link to a list of job results, which will enable the surveyor to issue a final HTTP GET request to download the file of results, i.e. a file containing the <a href="http://maps.nrcan.gc.ca/topo_e.php" title="The National Topographic System of Canada" target="_blank">NTS</a> 1:50000 Map ID associated with each longitude/latitude reference point submitted in the original batch job of request.
  </p>
  <h3>Creating an Account</h3>
  <p>
    To get started, you must register to create your CPF Web Services Account.  
    Details on how to register can be found <a href="security.page">here</a>.
    Once you have signed on you can access the CPF Web Service Methods.  
  </p>
  <h3>Web Service Methods</h3>
  <p>
    The Web Service methods are accessed through HTTP URLs using standard HTTP GET, POST and DELETE verbs.  
    Typical Web Service Resources you may wish to access include those for 
    <ul>
      <li>
        POSTing a new Batch Job of Requests<br />
         e.g. <a href="<c:out value="${baseUrl}"/>/ws/apps/mapTileByLocation/1.0.0/jobs" target="_blank">/ws/apps/mapTileByLocation/1.0.0/jobs</a>
      </li>
      <li>
        GETting the processing status of a Batch Job<br />
        e.g./ws/users/cd780787-41ee-466a-afa5-7e16f68d7dd5/jobs/3452
      </li>
      <li>
        GETting the results of a complete Batch Job<br />
        e.g.//ws/users/cd780787-41ee-466a-afa5-7e16f68d7dd5/jobs/3452/results/5468
      </li>
      <li>
        GETting a list of all jobs for a specific user<br />
        e.g./ws/users/cd780787-41ee-466a-afa5-7e16f68d7dd5/jobs
      </li>
      <li>
        GETting a list of all the Business Applications<br />
        e.g. <a href="<c:out value="${baseUrl}"/>/ws/apps/" target="_blank">/ws/apps/</a>
      </li>
    </ul>
  </p>
  <h3>Accessing the Web Services</h3>
  <p>
    There are three ways of accessing the Web Services:
  </p>
  <ul>
    <li>
      <b>Static HTML Form</b> - Either using your own HTML forms, or using those provided through the Web Service URLs, this simple method allows you retrieve and submit batch job information using a browser with little or no development.
      This is the quickest way to become familiar with the CPF Web Services, and the quickest way to get started.
    </li>
    <li>
      <b>Custom Web Application</b> - Using Dynamic HTML and JavaScript, a more advanced web browser application can be developed that not only submits the job request, but then uses the returned Batch Job ID to poll the Job Status method until the Batch Job processing has completed, after which the applications can automatically download the Jobs results files.   
    </li>
    <li>
      <b>Custom Client Application</b> - A client application, written in Java or some other high level language, can run on a client or a server and handle all the authorization, job submission and results retrieval aspects of your business needs.
    </li>
  </ul>

  <p>
    It is recommended that you familiarize yourself with the <a href="codeDemos.page">Tutorials</a> and <a href="codeLibrary.page">Example Code</a> in this Developer Guide.
    The Tutorial steps you through a simple example of how to manually POST a file of Batch Job Requests using a supplied form to the CPF Web Services engine.  It then steps you through how to check on the status of the job's processing, and then how to download the Job Results File at the completion of Job processing.
  </p>
</div>
