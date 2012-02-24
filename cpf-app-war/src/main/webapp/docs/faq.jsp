<div>
  <h1>Frequently Asked Questions</h1>
  <p class="q">
    What is meant by RESTful Web Services?
  </p>
  <p class="a">
    Representational state transfer (REST) is a stateless client-server architectural concept whereby web services can be viewed as URL addressable resources, thus presenting the user with a clealy defined web architecture of the available services.
    The user interacts with the CPF Web Services using a small set of URLs and the HTTP GET, POST and DELETE verbs.
    <br />
    Additional reading on RESTful Web Services can be found <a href="http://en.wikipedia.org/wiki/Representational_State_Transfer#RESTful_web_services" target="_blank" class="external">here</a>. 
  </p>
  
  <p class="q">
    What is OAuth and how is it used for authentication?
  </p>
  <p class="a">
    OAuth is an open protocol used to authorize access to resources on the web without having to share your user name and password.  
    The CPF Web Services uses OAuth signatures to validate that the user requesting web service resources is who they say they are.
    CPF uses OpenID to establish an initial account identity.  
    Once the account is established, an OAuth Consumer Key and Secret is generated and used from thereon forward for access to the Web Services.
  </p>
  
  <p class="q">
    How do I find the URL for my OpenID registration?
  </p>
  <p class="a">
    Each 'OpenID provider' defines the method of authentication to be used.  
    Some organizations identify themselves in your Account, or Screen, name, and accept this in place of the URL.
    Others require a specific URL or a URL format that includes your account ID.
    Buttons for the more commonly used OpenID providers have been provided.  
    A good starting point for finding the URL requirements for the other OpenID providers you wish to use to create your CPF Web Services Account would be <a href="http://en.wikipedia.org/wiki/List_of_OpenID_providers#Password-based_providers" title="List of OpenID providers" target="_blank" class="external">here</a>.
    The official list of OpenID Providers can be found <a href="http://openiddirectory.com/openid-providers-c-1.html" title="The OpenID Directory" target="_blank" class="external">here</a>.
  </p>
  
  <p class="q">
    What is a Business Application?
  </p>
  <p class="a">
    A Business Application is a module of the Web Service Application that is able to receive a single client request for specific information to be collated, or for some other algorithm to be carried out on the client request data.
    Each Batch Job of client Requests must be directed at one specific Business Application, and the data sent in the job and each request must conform to that Business Application's data requirements.
    The Business Application then passes this back to the Web Services engine, which collates it with the other client requests before generating a formatted result file for the client to download.  
  
  <p class="q">
    What Java release is used for developing Business Application Plug-ins?
  </p>
  <p class="a">
    As the Plug-in API uses Annotations, the Plug-in must be compatible with Java release 5.0.
  </p>
</div>
