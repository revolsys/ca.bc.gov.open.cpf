<div>

	<h1>Business Application Plug-in Development Guide</h1>
	
	<p>Business Applications are the work horses of the CPF Web
	Services. The Business Application Plug-in receives data associated with
	a client's individual business application request, executes the
	business function associated with that Business Application, and returns
	either a result to the request, in the case of valid request data, or
	should there be a problem, an error code and description.</p>
	
	<p>The Business Application Plug-in interacts with the CPF
	Web Services using the <a href="pluginApi.page">CPF Plug-in API</a>.
	</p>
	
	<p>There are a number of Business Applications available, as
	described in the <a
		href="../developerGuide/businessApplications.page">Application
	Reference</a> section of the developers guide documentation. As demand
	requires other applications can be developed to make available different
	views of the CPF available data resources, or to integrate third party
	resources into the CPF Web Services.</p>
	
	<p>Business Application Plug-ins can consist in simple algorithms or
	database lookups, or can be perform complex and processor intensive
	tasks, such as image or numerical analysis. Business Application Plug-ins
	can be deployed locally, on remote servers or a server cloud. Business
	Applications themselves could invoke other Business Applications using
	the CPF Web Services.</p>
	
	<p>This guide provides overviews, a reference and examples to assist
	developers in implementing new Business Application Plug-ins for the
	CPF Web Services.</p>

</div>
