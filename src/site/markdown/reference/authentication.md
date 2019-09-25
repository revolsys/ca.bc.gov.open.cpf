## Authentication
The CPF web services can support multiple different types of authentication. Each type of 
authentication has a different root URL for the web services.

### 2 legged O-OAuth
By default the CPF uses the [2-legged OAuth 1.0](http://oauth.net/core/1.0a/)
authentication scheme. In this scheme each client application is given a consumer key (username) and 
consumer secret (signing key). The client application signs each request using the OAuth protocol.
The server repeats the signing process and compares the results and the timestamp &amp; N-Once to verify
that the request has not already been processed.

The OAuth authentication scheme is designed to be used by client applications written
in programming languages such as Java or .Net. A <a href="client/java-api">Java Client</a> is 
provided that implements the OAuth authentication schemes. Other programming languages will need to
find or develop their own OAuth API. Applications can request consumer key and secret for use in 
their application. A different consumer key and secret will be provided for each environment so the 
application must use configuration parameters for them when deploying an application.

The consumer secret must be kept secure within the application and not exposed to
the users of the application. The signing of requests should therefore be done on the server rather
than in JavaScript running on the client side in a web browser. The client application takes full
responsibility for any access to CPF using their consumer key and consumer secret. 

Assuming the application was deployed to localhost with SSL encryption enabled using the /cpf
context path. The following root URL [https://localhost/cpf](https://localhost/cpf) can be used in
a Java or other programming language client or server side application to access the CPF web services. 

### HTTP Digest

[HTTP Digest](https://tools.ietf.org/html/rfc2617#section-3) authentication can also be used. 

Assuming the application was deployed to localhost with SSL encryption enabled using the /cpf
context path. The following root URL [https://localhost/cpf](https://localhost/cpf) can be used in
a Java or other programming language client or server side application to access the CPF web services. 
