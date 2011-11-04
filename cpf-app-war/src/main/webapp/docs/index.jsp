<div>
	<h1>What are the CPF Web Services?</h1>
	<p>The CPF Web Services gives you access to the business application
	data and information sources. These Web Services enables you to submit
	batches of requests for data from business application data and
	information sources. Once the batch job of requests has been submitted,
	the CPF Web Services gives you tools to check the processing status of
	your batch job of request, and to download the results file associated
	with the completed batch job.</p>
	
	<p>The CPF Web Services API is implemented as a RESTful web service.
	This allows the services to be invoked from a browser, or from a
	application written in any language, such as HTML/JavaScript on a web
	browser, or Java or C# from a client or from a server.</p>
	
	<p>Each Request in a Batch Job invokes a request to a Business
	Application. A Business Application is the component of the CPF Web
	Service that, upon receiving client request data, performs a specific
	function using that data, and returns the results. For example, there is
	a Business Application, MapTileByLocation, that, given a latitude and
	longitude reference point, and a particular map grid and scale, such as
	NTS 1:50,000, will return the Map File Identifier (e.g. NTS 0929G) of
	the map containing that geographical location.</p>
	
	<p>A full list of the Business Applications currently available can
	be found in the <a
		href="developerGuide/developerBusinessApplications.page">Application
	Reference</a> section of the <a href="developerGuide/index.page">Developer Guide</a>.
	</p>
	
	<h3>Documentation</h3>
	<p>The <a href="developerGuide/index.page">Developer Guide</a> describes each
	of the CPF Web Service resources, and
	explains how to use these resources to post information to the web
	services, and to invoke requests to obtain information from the web
	services. It also provides information and examples on how to develop
	simple HTTP forms, HTTP/JavaScript web browser applications and
	stand-alone applications that use the CPF Web Services API to obtain
	data results from the business application data sources.</p>
	
	<p>The <a href="pluginGuide/index.page">Business Application Plug-ins</a>
	section is aimed at application developers who wish to expand the
	capabilities of the CPF Web Services. By building a Business Application
	Plug-in, developers can make available different views of the business application
	data resources, or integrate access to third party data resources into
	the CPF Web Services. The Plug-ins section describes how a Business
	Application Plug-in interfaces with the Business Application Plug-in API
	to receive individual requests from the Web Services engine, to invoke
	an instance of the Business Application to process the request, and to
	send back the associated request results data.</p>
</div>