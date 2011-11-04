<div>
  <h1>Code Library</h1>
  <p>
    The <a href="codeLibrary.page">Code Library</a> gives you sample code for a simple Java client application and a JavaScript/HTML browser client.
  </p>
  <a href="codeSampleHtmlForm.page" title="Simple HTML Form">
    Simple HTML Form
  </a>
  <p>
    This is the simplest way to interact with the Batch Web Services.  This form allows a user to submit multiple requests to a business application.
    The requests would then be processed in background.<br />
    <em>Code Notes: You will need to update the URL associated with the form tag, and to be signed-in to the web services for this form to work.</em>
    <br /><br />
    <a href="codeSampleHtmlForm.page" class="button">view code</a>
    <a href="../sampleCode/WsFormExample.html.txt" class="button" target="_blank">download code</a>
  </p>
  <!-- 
  <h2>JavaScript Client</h2>
  <p>
  </p>
  <h3>Using the JavaScript Client</h3>
  <p>
  </p>
   -->
  <h2>The Java Client</h2>
  <p>
	  The Java Client provides an easy to use application programming interface to the Batch Web Services.
	  The Java Client simplifies the task of developing Java applications to, amongst other things, submit batch jobs, process the associated batch job results, close jobs, and list jobs for a user.
	  Java code examples of different methods of using the Java Client can be found below.
  </p>
  <p>
    To use the Java Client, you must download the code from the source repository, and install and build it on your server or client platform.
  </p>
  <p>
	  Java Client Subversion Source Repository:
	  <br />
	  <a href="http://open.revolsys.com/svn/repos/com.revolsys.grid/trunk/com.revolsys.grid.client.api/src/main/java/com/revolsys/grid/client/api/" target="_blank" title="Java Client Subversion Source Repository" class="external">http://open.revolsys.com/svn/repos/com.revolsys.grid/trunk/com.revolsys.grid.client.api/src/main/java/com/revolsys/grid/client/api/</a>
	  <br /><br />
	  <a href="../javadocs/com/revolsys/grid/client/api/BatchJobClient.html" target="_blank" class="button">view javadoc</a>
  </p>
  <h3>Using the Java Client</h3>
  <p>
    Following are some Java code examples of case studies using the Java Client.
  </p>
  <ul>
    <li>
     <b>Example 1:</b> Submit a job for the business application mapTileByLocation using a URL as the location of the batch job requests data. 
     Wait for job processing to be completed, then Get the results, extract the map names, remove duplicates, sort the resulting list and display the short list of map names.
     <br />
     <em>
       Code Notes: The oAuth consumer key and consumer secret needs to be updated in the code to reflect your web services user name and password.  
       The wsBaseUrl parameter will need to be modified to reflect the Batch Web Services base URL.
     </em>
     <br /><br />
	  <a href="codeSampleJavaClient1.page" class="button">view code</a>
	  <a href="../sampleCode/WsClientExample1.java" class="button" target="_blank">download code</a>
	  <br /><br />
  </li>
    <li>
      <b>Example 2:</b> Submit a job, using a local data file for batch job requests data.
      Wait for the job to complete, timing out after 1 hour.  Get and display the job status.
      Close the job.
      <br />
      <em>
        Code Notes: The oAuth consumer key and consumer secret needs to be updated in the code to reflect your web services user name and password. 
        The wsBaseUrl parameter will need to be modified to reflect the Batch Web Services base URL.
        You will need to download the data file.  
        The inputDataFile parameter will need to bee modified to reflect the local path name of the input data file.
      </em>
      <br /><br />
      <a href="codeSampleJavaClient2.page" class="button">view code</a>
      <a href="../sampleCode/WsClientExample2.java" class="button" target="_blank">download code</a>
      <a href="../sampleData/NTS-250000-by-location.csv" class="button" target="_blank">download data</a>
      <br /><br />
    </li>
    <li>
      <b>Example 3:</b> List all the business application mapTileByLocation jobs for a specific user.
      <br />
      <em>
        Code Notes: The oAuth consumer key and consumer secret needs to be updated in the code to reflect your web services user name and password. 
        The wsBaseUrl parameter will need to be modified to reflect the Batch Web Services base URL.
      </em>
      <br /><br />
      <a href="codeSampleJavaClient3.page" class="button">view code</a>
      <a href="../sampleCode/WsClientExample3.java" class="button" target="_blank" class="external">download code</a>
      <br /><br />
    </li>
    <li>
      <b>Example 4:</b> Get the job status of an existing successfully processed job. 
      List the jobs result files. 
      Get a map reader of the results. 
      Using the map reader, display the results.
      <br />
      <em>
        Code Notes: The oAuth consumer key and consumer secret needs to be updated in the code to reflect your web services user name and password. 
        The wsBaseUrl parameter will need to be modified to reflect the Batch Web Services base URL.
        The jobId parameter needs to contain the job Id of a completed job.
      </em>
      <br /><br />
      <a href="codeSampleJavaClient4.page" class="button">view code</a>
      <a href="../sampleCode/WsClientExample4.java" class="button" target="_blank">download code</a>
      <br /><br />
    </li>
    <li>
      <b>Example 5:</b> Submit a single request to a business application and display the results.
      This request will be processed in real time, rather than being queued for processing with the batch job requests.  
      This would more traditionally be suitable for supporting the server side requirements of data mashups for 
      dynamic and interactive web pages and web applications.
      <br />
      <em>
        Code Notes: The oAuth consumer key and consumer secret needs to be updated in the code to reflect your web services user name and password. 
        The wsBaseUrl parameter will need to be modified to reflect the Batch Web Services base URL.
      </em>
      <br /><br />
      <a href="codeSampleJavaClient5.page" class="button">view code</a>
      <a href="../sampleCode/WsClientExample5.java" class="button" target="_blank">download code</a>
      <br /><br />
    </li>
  </ul>
</div>