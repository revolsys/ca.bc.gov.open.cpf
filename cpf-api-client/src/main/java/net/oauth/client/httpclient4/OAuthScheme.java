/*
 * Copyright © 2008-2016, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oauth.client.httpclient4;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.RFC2617Scheme;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.message.BasicHeader;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthCredentials;
import net.oauth.OAuthMessage;

/**
 * @author Paul Austin
 * @author John Kristian
 */
class OAuthScheme extends RFC2617Scheme {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /** Whether the authentication process is complete (for the current context) */
  private boolean complete;

  private final String defaultRealm;

  OAuthScheme(final String defaultRealm) {
    this.defaultRealm = defaultRealm;
  }

  @Override
  public Header authenticate(final Credentials credentials, final HttpRequest request)
    throws AuthenticationException {
    String uri;
    String method;
    final HttpUriRequest uriRequest = getHttpUriRequest(request);
    if (uriRequest != null) {
      uri = uriRequest.getURI().toString();
      method = uriRequest.getMethod();
    } else {
      // Some requests don't include the server name in the URL.
      final RequestLine requestLine = request.getRequestLine();
      uri = requestLine.getUri();
      method = requestLine.getMethod();
    }
    try {
      final OAuthMessage message = new OAuthMessage(method, uri, null);
      final OAuthAccessor accessor = getAccessor(credentials);
      message.addRequiredParameters(accessor);
      final String authorization = message.getAuthorizationHeader(getRealm());
      return new BasicHeader("Authorization", authorization);
    } catch (final Exception e) {
      throw new AuthenticationException(null, e);
    }
  }

  private OAuthAccessor getAccessor(final Credentials credentials) {
    if (credentials instanceof OAuthCredentials) {
      return ((OAuthCredentials)credentials).getAccessor();
    }
    return new OAuthAccessor(new OAuthConsumer(null // callback URL
      , credentials.getUserPrincipal().getName() // consumer key
      , credentials.getPassword() // consumer secret
      , null)); // service provider
  }

  private HttpUriRequest getHttpUriRequest(HttpRequest request) {
    while (request instanceof RequestWrapper) {
      final HttpRequest original = ((RequestWrapper)request).getOriginal();
      if (original == request) {
        break;
      }
      request = original;
    }
    if (request instanceof HttpUriRequest) {
      return (HttpUriRequest)request;
    }
    return null;
  }

  @Override
  public String getRealm() {
    String realm = super.getRealm();
    if (realm == null) {
      realm = this.defaultRealm;
    }
    return realm;
  }

  @Override
  public String getSchemeName() {
    return OAuthSchemeFactory.SCHEME_NAME;
  }

  @Override
  public boolean isComplete() {
    return this.complete;
  }

  @Override
  public boolean isConnectionBased() {
    return false;
  }

  /**
   * Handle a challenge from an OAuth server.
   */
  @Override
  public void processChallenge(final Header challenge) throws MalformedChallengeException {
    super.processChallenge(challenge);
    this.complete = true;
  }

}
