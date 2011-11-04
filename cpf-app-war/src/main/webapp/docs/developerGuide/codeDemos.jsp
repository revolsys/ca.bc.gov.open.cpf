<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
	<h1>Tutorial</h1>
	<h2>Demo 1 - Static HTML Forms</h2>
	<p>
		This demonstration will have you submit a list of 192 latitude/longitude requests to the MapTileByLocation Business Application, 
	  which will return the NTS 1:25000 Map Tile name  associated with each latitude/longitude pair, 
	  and 20 lat/long points representing the boundary of each of these Map Tiles.
	</p>
	<ol class="demo">
		<li>
	  	Register and/or sign-on to your CPF account at <br />
	  	<a href="<c:out value="${baseUrl}"/>/account" title="registration for CPF Web Services" target="_blank" class="external">/account</a>.
	  </li>
	  <li>
	  	Download, save and view the the following request data file: <br />
	  	<a href="../sampleData/NTS-250000-by-location.csv" class="external">NTS-250000-by-location.csv</a>
	  </li>
	  <li>
	  	The first record of this file represents the data field names for the following data.  
	    Each subsequent record is a Business Application Request.
	  </li>
	  <li>
	  	Open a web browser tab at the following URL: <br />
	  	<a href="<c:out value="${baseUrl}"/>/ws/" target="_blank" class="external">/</a>
	  </li>
	  <li>
	  	Click through apps / mapTilebyLoacation/1.0.0  / jobs to get to the URL <br />
	  	<a href="<c:out value="${baseUrl}"/>/ws/apps/mapTileByLocation/1.0.0/jobs" target="_blank" class="external">/ws/apps/mapTileByLocation/1.0.0/jobs</a>
	  </li>
	  <li>
	  	You should now be presented with a POST form that will enable you to submit a Batch Job to the ampTileByLocation (ver 1.0.0) Business Application.
	  </li>
	  <li>
	  	Our test data has 192 request, so enter this value for Number Requests.  This helps the web services estimate and inform you of the time it expects to take for the job to complete.
	  </li>
	  <li>
	  	As our data is in text/csv format, leave input data type as is.
	  </li>
	  <li>
	  	Browse and select the data file you downloaded, and chose a Content Type for the format in which you wish to receive the results.
	  </li>
	  <li>
	  	Select NTS for the Map Grid Name.
	  </li>
	  <li>
	  	The remaining request fields are specified in the individual requests, so need not be set at the Job level.
	  </li>
	  <li>
	  	Now click on the submit button, and your job will be submitted to the CPF Web Services engine.
	  </li>
	  <li>
	  	A page will immediately return with a link to your new job.  
	    Each time you click on this link, you will receive an updated status of the progress of your job.
	  </li>
	  <li>
	  	Once the job has completed, you will see a link to the results associated with job.  
	    As these requests should process without error, there should only be a single results file.
	    Click on the results file to view the results.
	  </li>
	</ol>
</div>
