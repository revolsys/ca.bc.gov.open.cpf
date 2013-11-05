/*
 Copyright 2009 Revolution Systems Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 
 $URL: https://secure.revolsys.com/svn/open.revolsys.com/ca.bc.gov.open.cpf/trunk/ca.bc.gov.open.cpf.api/src/main/java/ca/bc/gov/open/cpf/security/oauth/OAuthProcessingFilter.java $
 $Author: paul.austin@revolsys.com $
 $Date: 2010-01-06 14:31:17 -0800 (Wed, 06 Jan 2010) $
 $Revision: 2169 $
 */
package ca.bc.gov.open.cpf.api.security.oauth;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.SimpleOAuthValidator;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

public class OAuthProcessingFilter extends GenericFilterBean implements
  InitializingBean {
  /**
   * Extract the parts of the given request that are relevant to OAuth.
   * Parameters include OAuth Authorization headers and the usual request
   * parameters in the query string and/or form encoded body. The header
   * parameters come first, followed by the rest in the order they came from
   * request.getParameterMap().
   * 
   * @param URL the official URL of this service; that is the URL a legitimate
   *          client would use to compute the digital signature. If this
   *          parameter is null, this method will try to reconstruct the URL
   *          from the HTTP request; which may be wrong in some cases.
   */
  public static OAuthMessage getMessage(final HttpServletRequest request,
    String URL) {
    if (URL == null) {
      URL = request.getRequestURL().toString();
    }
    final int q = URL.indexOf('?');
    if (q >= 0) {
      URL = URL.substring(0, q);
      // The query string parameters will be included in
      // the result from getParameters(request).
    }
    return new HttpRequestMessage(request, URL);
  }

  /** Reconstruct the requested URL, complete with query string (if any). */
  public static String getRequestURL(final HttpServletRequest request) {
    final StringBuffer url = request.getRequestURL();
    final String queryString = request.getQueryString();
    if (queryString != null) {
      url.append("?").append(queryString);
    }
    return url.toString();
  }

  private AuthenticationEntryPoint authenticationEntryPoint;

  private UserDetailsService consumerDetailsService;

  private boolean ignoreFailure = false;

  private final SimpleOAuthValidator oauthMessageValidator = new SimpleOAuthValidator();

  @Override
  public void afterPropertiesSet() {
    if (!isIgnoreFailure()) {
      Assert.notNull(this.authenticationEntryPoint,
        "An AuthenticationEntryPoint is required");
    }
  }

  @Override
  public void doFilter(final ServletRequest request,
    final ServletResponse response, final FilterChain chain)
    throws IOException, ServletException {
    doFilterHttp((HttpServletRequest)request, (HttpServletResponse)response,
      chain);
  }

  public void doFilterHttp(final HttpServletRequest request,
    final HttpServletResponse response, final FilterChain chain)
    throws IOException, ServletException {

    String requestUrl;
    requestUrl = getRequestURL(request);
    final OAuthMessage message = getMessage(request, requestUrl);
    final String consumerKey = message.getConsumerKey();
    final SecurityContext context = SecurityContextHolder.getContext();
    try {
      final UserDetails consumerDetails = getConsumerDetails(consumerKey);
      if (consumerDetails != null) {
        final OAuthAccessor accessor = getOAuthAccessor(consumerDetails);

        oauthMessageValidator.validateMessage(message, accessor);
        final Collection<GrantedAuthority> authorities = consumerDetails.getAuthorities();
        final Authentication authResult = new OAuthAuthenticationToken(
          consumerKey, authorities);

        authResult.setAuthenticated(true);
        if (logger.isDebugEnabled()) {
          logger.debug("Authentication success: " + authResult.toString());
        }

        context.setAuthentication(authResult);
        onSuccessfulAuthentication(request, response, authResult);

      }
    } catch (final Exception e) {
      final AuthenticationException authE = new BadCredentialsException(
        "Signature validation failed", e);
      LoggerFactory.getLogger(OAuthProcessingFilter.class).debug("Authentication request for user: " + consumerKey + " failed",
        e);

      context.setAuthentication(null);
      onUnsuccessfulAuthentication(request, response, authE);
      if (!ignoreFailure) {
        authenticationEntryPoint.commence(request, response, authE);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  protected AuthenticationEntryPoint getAuthenticationEntryPoint() {
    return authenticationEntryPoint;
  }

  private UserDetails getConsumerDetails(final String consumerKey) {
    if (consumerKey == null) {
      return null;
    } else {
      final UserDetails consumerDetails = consumerDetailsService.loadUserByUsername(consumerKey);
      return consumerDetails;
    }
  }

  public UserDetailsService getConsumerDetailsService() {
    return consumerDetailsService;
  }

  private OAuthAccessor getOAuthAccessor(final UserDetails consumerDetails) {
    final String consumerKey = consumerDetails.getUsername();
    final String consumerSecret = consumerDetails.getPassword();
    final OAuthServiceProvider serviceProvider = new OAuthServiceProvider("",
      "", "");
    final OAuthConsumer consumer = new OAuthConsumer("", consumerKey,
      consumerSecret, serviceProvider);
    return new OAuthAccessor(consumer);
  }

  protected boolean isIgnoreFailure() {
    return ignoreFailure;
  }

  protected void onSuccessfulAuthentication(final HttpServletRequest request,
    final HttpServletResponse response, final Authentication authResult)
    throws IOException {
  }

  protected void onUnsuccessfulAuthentication(final HttpServletRequest request,
    final HttpServletResponse response, final AuthenticationException failed)
    throws IOException {
  }

  public void setAuthenticationEntryPoint(
    final AuthenticationEntryPoint authenticationEntryPoint) {
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  public void setConsumerDetailsService(
    final UserDetailsService consumerDetailsService) {
    this.consumerDetailsService = consumerDetailsService;
  }

  public void setIgnoreFailure(final boolean ignoreFailure) {
    this.ignoreFailure = ignoreFailure;
  }

}
