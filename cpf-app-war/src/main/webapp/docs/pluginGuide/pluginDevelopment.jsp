<div>
  <h1>Business Application Plug-in Development</h1>
  
  <p>
    CPF Business Application Plug-ins interact with the Web Services engine using a supplied Java Plug-in API.  
    All Business Application Plug-ins must specify each request and response parameter in the Plug-in class.  
    Request parameters must have a supplied setter method, using Plug-in API Annotations to describe validation attributes of the request parameter.
    Response parameters must have a supplied getter method, using a Plug-in API Annotation to indicate that it is a response parameter.
  </p>
  <p>
    When the Plug-in is instantiated, the annotations result in the associated request parameters being set and validate accordingly.  
    The <em>execute()</em> method of the Plug-in is then called by the Plug-in API.  The <em>execute()</em> method must set the specified <em>ResponseField</em> parameters, which are then passed back, using the <em>ResponseField</em> getter methods, to the CPF Web Service engine for collation and delivery.
  </p>
  <p>
    It is recommended that you familiarize yourself with the <a href="pluginExamples.page">Plug-in Example</a> while reading the following Plug-in development API reference.
  </p>
  <p>
    The Plug-in must be compatible with Java release 5.0.  Additional information regarding Annotations can be found <a href="http://java.sun.com/j2se/1.5.0/docs/guide/language/annotations.html" title="Java Annotations">here</a>.
  </p>
</div>
