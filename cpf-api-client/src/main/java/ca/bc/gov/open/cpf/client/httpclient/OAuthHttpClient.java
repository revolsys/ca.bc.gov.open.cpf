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
package ca.bc.gov.open.cpf.client.httpclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthSchemeRegistry;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import com.revolsys.io.IoFactory;
import com.revolsys.io.Reader;
import com.revolsys.io.map.MapReaderFactory;
import com.revolsys.record.io.format.json.Json;
import com.revolsys.record.io.format.json.JsonParser;
import com.revolsys.spring.resource.InputStreamResource;
import com.revolsys.util.Exceptions;
import com.revolsys.util.UrlUtil;
import com.revolsys.util.WrappedException;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthCredentials;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.httpclient4.OAuthSchemeFactory;
import net.oauth.client.httpclient4.PreemptiveAuthorizer;

@SuppressWarnings("javadoc")
public class OAuthHttpClient extends DefaultHttpClient {
  public static HttpHost determineTarget(final HttpUriRequest request)
    throws ClientProtocolException {
    // A null target may be acceptable if there is a default target.
    // Otherwise, the null target is detected in the director.
    HttpHost target = null;

    final URI requestURI = request.getURI();
    if (requestURI.isAbsolute()) {
      target = URIUtils.extractHost(requestURI);
      if (target == null) {
        throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
      }
    }
    return target;
  }

  private final String consumerKey;

  private final String consumerSecret;

  private BasicHttpContext context;

  private final OAuthHttpClientPool pool;

  private String webServiceUrl;

  @SuppressWarnings("deprecation")
  public OAuthHttpClient(final OAuthHttpClientPool pool, final String webServiceUrl,
    final String consumerKey, final String consumerSecret) {
    this.pool = pool;
    this.consumerKey = consumerKey;
    this.consumerSecret = consumerSecret;
    try {
      final URL url = new URL(webServiceUrl);

      final String webServiceHost = url.getHost();
      final int webServicePort = url.getPort();
      final String wsPath = url.getPath().replaceAll("/+$", "");
      final String protocol = url.getProtocol();
      if (webServicePort == -1) {
        this.webServiceUrl = protocol + "://" + webServiceHost + wsPath;
      } else {
        this.webServiceUrl = protocol + "://" + webServiceHost + ":" + webServicePort + wsPath;
      }
      this.context = new BasicHttpContext();
      this.context.setAttribute(ClientContext.AUTH_SCHEME_PREF,
        Arrays.asList(OAuthSchemeFactory.SCHEME_NAME));

      final HttpParams parameters = getParams();
      parameters.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
      HttpConnectionParams.setConnectionTimeout(parameters, 5 * 60 * 1000);
      HttpConnectionParams.setSoTimeout(parameters, 5 * 60 * 1000);
      final AuthSchemeRegistry authSchemes = new AuthSchemeRegistry();
      setAuthSchemes(authSchemes);
      final OAuthSchemeFactory oauthSchemeFactory = new OAuthSchemeFactory();
      authSchemes.register("oauth", oauthSchemeFactory);
      final AuthScope authscope = new AuthScope(webServiceHost, webServicePort);
      final OAuthServiceProvider serviceProvider = new OAuthServiceProvider(null, null, null);
      final OAuthConsumer consumer = new OAuthConsumer(null, consumerKey, consumerSecret,
        serviceProvider);
      final OAuthAccessor accessor = new OAuthAccessor(consumer);
      final OAuthCredentials credentials = new OAuthCredentials(accessor);
      final CredentialsProvider credentialsProvider = getCredentialsProvider();
      credentialsProvider.setCredentials(authscope, credentials);
      final PreemptiveAuthorizer preemptiveAuthorizer = new PreemptiveAuthorizer();
      addRequestInterceptor(preemptiveAuthorizer, 0);

    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url " + webServiceUrl, e);
    }
  }

  public String appendOAuthToUrl(final String method, final String url) {
    final String oauthUrl = OAuthUrlUtil.addAuthenticationToUrl(method, url, this.consumerKey,
      this.consumerSecret);
    return oauthUrl;
  }

  @Override
  public void close() {
    this.pool.releaseClient(this);
  }

  public void deleteUrl(final String url) throws IOException, ClientProtocolException {
    final HttpDelete httpDelete = new HttpDelete(url);
    final HttpResponse response = execute(httpDelete, this.context);
    final HttpEntity entity = response.getEntity();
    EntityUtils.consume(entity);
  }

  public Map<String, Object> getJsonResource(final HttpResponse response) {
    try {
      final StatusLine statusLine = response.getStatusLine();
      final int httpStatusCode = statusLine.getStatusCode();
      final HttpEntity entity = response.getEntity();

      if (httpStatusCode == HttpStatus.SC_OK) {
        try (
          final InputStream in = entity.getContent()) {
          final Map<String, Object> map = JsonParser.read(in);
          return map;
        }
      } else {
        throw newException(entity, statusLine);
      }
    } catch (final IOException e) {
      throw new WrappedException(e);
    }
  }

  public Map<String, Object> getJsonResource(final HttpUriRequest request)
    throws IOException, ClientProtocolException {
    request.addHeader("Accept", "application/json");

    final ResponseHandler<Map<String, Object>> responseHandler = new FunctionResponseHandler<>(
      this::getJsonResource);

    final HttpHost target = determineTarget(request);
    final Map<String, Object> response = execute(target, request, responseHandler, this.context);

    return response;
  }

  public Map<String, Object> getJsonResource(final String url) {
    final HttpGet request = new HttpGet(url);
    try {
      return getJsonResource(request);
    } catch (final Throwable e) {
      request.abort();
      return Exceptions.throwUncheckedException(e);
    }
  }

  public Reader<Map<String, Object>> getMapReader(final String url) {
    final String fileName = UrlUtil.getFileName(url);
    return getMapReader(fileName, url);
  }

  public Reader<Map<String, Object>> getMapReader(final String fileName, final String url) {
    try {
      final HttpGet request = new HttpGet(url);
      final HttpResponse response = execute(request, this.context);
      final StatusLine statusLine = response.getStatusLine();
      final int httpStatusCode = statusLine.getStatusCode();
      final HttpEntity entity = response.getEntity();

      if (httpStatusCode == HttpStatus.SC_OK) {
        final InputStream in = entity.getContent();
        final Header contentTypeHeader = entity.getContentType();

        final String contentType = contentTypeHeader.getValue();
        final MapReaderFactory factory = IoFactory.factoryByMediaType(MapReaderFactory.class,
          contentType);
        if (factory == null) {
          throw new RuntimeException("Unable to read " + contentType);
        }

        final InputStreamResource resource = new InputStreamResource(fileName, in);
        return factory.newMapreader(resource);
      } else {
        throw newException(entity, statusLine);
      }
    } catch (final Throwable e) {
      return Exceptions.throwUncheckedException(e);
    }
  }

  public String getOAuthUrl(final String method, final String path) {
    String url = getUrl(path);
    url = OAuthUrlUtil.addAuthenticationToUrl(method, url, this.consumerKey, this.consumerSecret);
    return url;
  }

  public HttpResponse getResource(final String url) {
    try {
      final HttpGet httpGet = new HttpGet(url);
      final HttpResponse response = execute(httpGet, this.context);
      return response;
    } catch (final Exception e) {
      return (HttpResponse)Exceptions.throwUncheckedException(e);
    }
  }

  public String getUrl(final String path) {
    return this.webServiceUrl + path.replaceAll("/+", "/");
  }

  protected IOException newException(final HttpEntity entity, final StatusLine statusLine) {
    if (LoggerFactory.getLogger(OAuthHttpClient.class).isDebugEnabled()) {
      try {
        final String errorBody = EntityUtils.toString(entity);
        LoggerFactory.getLogger(OAuthHttpClient.class)
          .debug("Unable to get message from server: " + statusLine + "\n" + errorBody);
      } catch (final Throwable e) {
        LoggerFactory.getLogger(OAuthHttpClient.class)
          .error("Unable to get error message server: " + statusLine + "\n");
      }
    }
    return new IOException("Unable to get message from server: " + statusLine);
  }

  public Map<String, Object> postJsonResource(final String url)
    throws ClientProtocolException, IOException {
    final HttpPost request = new HttpPost(url);
    return getJsonResource(request);
  }

  public Map<String, Object> postJsonResource(final String url,
    final Map<String, ? extends Object> message) throws ClientProtocolException, IOException {
    final HttpPost request = new HttpPost(url);
    request.addHeader("Content-type", "application/json");
    final String bodyString = Json.toString(message);
    final StringEntity bodyEntity = new StringEntity(bodyString, "UTF-8");
    request.setEntity(bodyEntity);
    return getJsonResource(request);
  }

  public int postResource(final HttpMultipartPost post) throws IOException {
    return post.postRequest(this.context);
  }

  public HttpResponse postResource(final String url) throws IOException, ClientProtocolException {
    try {
      final HttpPost httpPost = new HttpPost(url);
      final List<NameValuePair> parameters = Collections.emptyList();
      final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters);
      httpPost.setEntity(entity);
      final HttpResponse response = execute(httpPost, this.context);
      return response;
    } catch (final Throwable e) {
      return Exceptions.throwUncheckedException(e);
    }
  }

  public HttpResponse postResource(final String url, final String contentType, final File file)
    throws IOException, ClientProtocolException {
    try {
      final HttpPost httpPost = new HttpPost(url);
      final FileEntity entity = new FileEntity(file, contentType);
      httpPost.setEntity(entity);
      final HttpResponse response = execute(httpPost, this.context);
      return response;
    } catch (final Throwable e) {
      return Exceptions.throwUncheckedException(e);
    }
  }

  public String postResourceRedirect(final HttpMultipartPost request) {
    try {
      final int statusCode = postResource(request);
      final HttpResponse response = request.getResponse();
      try {
        if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
          final StatusLine statusLine = response.getStatusLine();
          final String statusMessage = statusLine.getReasonPhrase();
          throw new RuntimeException(statusCode + " " + statusMessage);
        } else if (statusCode == HttpStatus.SC_OK) {
          final HttpEntity entity = response.getEntity();
          try (
            final InputStream in = entity.getContent()) {

            final Map<String, Object> map = JsonParser.read(in);
            return (String)map.get("id");
          }
        } else {
          final Header[] header = response.getHeaders("Location");
          if (header.length > 0) {
            final String jobIdUrl = header[0].getValue();
            return jobIdUrl;
          } else {
            throw new RuntimeException("Unable to get location header for " + request.getUrl());
          }
        }
      } finally {
        request.close();
      }
    } catch (final IOException e) {
      throw new RuntimeException("Unable to send POST request " + request.getUrl(), e);
    }
  }

  @SuppressWarnings("deprecation")
  public String postResourceRedirect(final String url) {
    try {
      final HttpResponse response = postResource(url);
      try {
        final StatusLine statusLine = response.getStatusLine();
        final int statusCode = statusLine.getStatusCode();

        if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
          final String statusMessage = statusLine.getReasonPhrase();
          throw new RuntimeException(statusCode + " " + statusMessage);
        } else if (statusCode == HttpStatus.SC_OK) {
          final HttpEntity entity = response.getEntity();
          try (
            final InputStream in = entity.getContent()) {

            final Map<String, Object> map = JsonParser.read(in);
            return (String)map.get("id");
          }
        } else {
          final Header[] header = response.getHeaders("Location");
          if (header.length > 0) {
            final String jobIdUrl = header[0].getValue();
            return jobIdUrl;
          } else {
            throw new RuntimeException("Unable to get location header for " + url);
          }
        }
      } finally {
        response.getEntity().consumeContent();
      }
    } catch (final IOException e) {
      throw new RuntimeException("Unable to send POST request " + url, e);
    }
  }

}
