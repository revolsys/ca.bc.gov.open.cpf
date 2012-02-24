<%@
  taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><div>
  <h1>Security and Registration</h1>
  <p>
    The CPF Web Services API is available to all who wish to use it.  
    Registration is, however, required.
    This enables us to ensure that the results data is delivered to the person who requested it.
    All users of the CPF Web Services API will need to include their user credentials for each request.
  </p>
  <p>
    Registration for the CPF Web Services uses <a href="http://openid.net/">OpenID</a>.  
    OpenID enables users to use their existing web accounts with OpenID supported web service providers, rather than having to sign up for and manage yet another web based user name and password.
    For example, if you have a yahoo email, you can use this account for your CPF Web Services registration.
  </p>
  <p>
    A current list of <a href="http://en.wikipedia.org/wiki/List_of_OpenID_providers" title="OpenID Poviders" target="_blank" class="external">OpenID Providers</a> can be found <a href="http://en.wikipedia.org/wiki/List_of_OpenID_providers" title="OpenID Poviders" target="_blank" class="external">here</a>.
    Note that while some OpenID providers allow you to use your screen name or email address to register access the OpenID registration, others require you to enter a URL specific to their OpenID service.
  </p>
  <p>
  Using OpenID, by signing in using an OpenID enabled account, you are automatically creating a CPF Web Services Account, if you do not have one already.  
  If at any point you are doubtful of the security of your credentials, then click on the "Reset Credentials" button, and a new OAuth Consumer Key and Secret will be generated for your OpenID account.
  </p>
  
  <h2>Registration</h2>
  <p>
    You may register an account for the CPF Web Services and/or signin <a href="<c:out value="${baseUrl}"/>/account" title="registration for CPF Web Services" target="_blank">here</a>:<br />
    <a href="<c:out value="${baseUrl}"/>/account" title="registration for CPF Web Services" target="_blank">/account</a>  
  </p>
  <p>
    Upon registration, OpenID will issue you with an <em>OAuth Consumer Key</em>, to be used as your CPF WS user ID, and an <em>OAuth Consumer Secret</em>, to be used as your CPF WS password.
    Keep these secure for later use.
  </p>
  <h2>Example Registration Screens</h2>
  
  <h4>Enter the URL of your OpenID Provider's website.</h4> 
  <div class="screenshot"><a href="../images/register1.gif" target="_blank" title="click to popup larger image"><img src="../images/register1.gif" /></a></div>
  
  <h4>Signin using the User ID and Password of yourOpenID Provider's account.</h4>
  <div class="screenshot"><a href="../images/register2.gif" target="_blank" title="click to popup larger image"><img src="../images/register2.gif" class="screenshot" /></a></div>
  
  <h4>Read and continue past the OpenID confirmation screen.</h4>
  <div class="screenshot"><a href="../images/register3.gif" target="_blank" title="click to popup larger image"><img src="../images/register3.gif" class="screenshot" /></a></div>
  
  <h4>Copy and securely store your OAuth Consumer Key and Oauth Consumer Secret.</h4>
  <div class="screenshot"><a href="../images/register4.gif" target="_blank" title="click to popup larger image"><img src="../images/register4.gif" class="screenshot" /></a></div>
  
  <h4>Signin to the Web Services using your new OAuth credentials.</h4>
  <div class="screenshot"><a href="../images/register5.gif" target="_blank" title="click to popup larger image"><img src="../images/register5.gif" class="screenshot" /></a></div>
 
  <h3>Logging Out</h3>
  <p>
    To log out of the CPF Web Services, return to the Account page, and click on the logout button on the side menu.
    If you have just registered your CPF Web Services account, be aware that logging out of the CPF Web Services does not log you out of your OpenID Provider account.
  </p>
  <p>
    If, for example, you registered using your Yahoo email account, then you will need to log out of your Yahoo mail separately.
    This will not be necessary if you signed into the CPF Web Services using an existing account you had previously created.
  </p>
  
  <h3>OAuth</h3> 
  <p>
    The CPF Web Services uses <a href="http://oauth.net/" title="An open protocol to allow secure API authorization" target="_blank" class="external">OAuth</a> for authentication.
    If you are accessing the Web Services using a browser, <a href="http://en.wikipedia.org/wiki/Digest_access_authentication" target="_blank" class="external">HTTP Digest access authentication</a> is used to validate your user name and password without sending your credentials in clear text.
    If you are accessing the Web Services using a client application, written in Java or some other language, then the preferred method is to use an OAuth signature for authentication.
  </p>
  <p>
    CPF is committed to ensuring your data privacy and security.  CPF's privacy policy can be found <a href="http://www.gov.bc.ca/com/privacy.html" title="privacy statement" target="_blank" class="external">here</a>.
  </p>
</div>
