<div>
<h1>Plug-in API Reference</h1>

<h2>Request Data Annotations</h2>
<p>
	A setter method will need to be included in the Plug-in class for each possible parameter in the request, whether the parameter comes from the job or from an individual request.
  Annotations associated with the setter method will define the attributes and validation associated with the request parameter. 
  </p>
  
  <h3>@AllowedValues</h3>
  <p>
    The @AllowedValues annotation validates the request parameter against a list of possible values.  
    If the request parameter is not one of the supplied allowed values, then the request is not processed and an invlid parameter value error is returned.
  </p>
  
  <h3>@Required</h3>
  <p>
    The @Required annotation indicates that the specified parameter is mandatory.  
    If a value for the parameter is not present, then the request is not processed and an invlid parameter value error is returned.
  </p>
  
  <h3>@JobParameter</h3>
  <p>
    The @JobParameter annotation indicates that the specified parameter may be supplied at the Batch Job level.
    If this annotation is not indicated, then any job level value for this parameter will be ignored.
  </p>
  
  <h3>@RequestParameter</h3>
  <p>
    The @RequestParameter annotation indicates that the specified parameter may be supplied at the Batch Job Reqiest level.
    If this annotation is not indicated, then any request level value for this parameter will be ignored.  
    All request parameters must indicate at least one of the @JobParameter and @RequestParameter annotations, though both can also be indicated.
  </p>
  
  <h2>Response Data Annotations</h2>
  <p>
    A getter method will need to be include in the Plug-in class for each possilbe parameter to be returned in the response.
  </p>
  
  <h3>@ResponseField</h3>
  <p>
    The @ResponseField annotation indicates that parameter associated with the following getter method is to be passed back to the GeoBase Batch Web Services engine as a response data parameter.
  </p>
  
  <h2>The execute() method</h2>
  <p>
    The execute method must be a public method that has no arguments, and returns no values.  This method applies the validated request parameters against a user supplied algorithm to set the response parameters.  
    All thrown errors must be clearly defined.
  </p>
</div>
