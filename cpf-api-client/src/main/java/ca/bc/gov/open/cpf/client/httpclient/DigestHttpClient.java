/*
 * Copyright © 2008-2015, Province of British Columbia
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
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.revolsys.io.FileUtil;
import com.revolsys.parallel.ThreadInterruptedException;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.record.io.format.json.Json;
import com.revolsys.record.io.format.json.JsonParser;
import com.revolsys.util.ExceptionUtil;

public class DigestHttpClient {
  private static HttpHost determineTarget(final HttpUriRequest request) {
    HttpHost target = null;

    final URI requestURI = request.getURI();
    if (requestURI.isAbsolute()) {
      target = URIUtils.extractHost(requestURI);
      if (target == null) {
        throw new IllegalArgumentException("URI does not specify a valid host name: " + requestURI);
      }
    }
    return target;
  }

  private final String webServiceUrl;

  private final ResponseHandler<Map<String, Object>> jsonResponseHandler = new FunctionResponseHandler<>(
    this::getJsonResource);

  private final DefaultHttpClient httpClient;

  public DigestHttpClient(final String webServiceUrl, final String username, final String password,
    final int poolSize) {
    this.webServiceUrl = webServiceUrl;

    final ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();
    connectionManager.setDefaultMaxPerRoute(poolSize);
    connectionManager.setMaxTotal(poolSize);
    this.httpClient = new DefaultHttpClient(connectionManager);
    final HttpParams params = this.httpClient.getParams();

    final List<String> authPrefs = Collections.singletonList(AuthPolicy.DIGEST);
    params.setParameter(AuthPNames.TARGET_AUTH_PREF, authPrefs);

    final HttpGet request = new HttpGet(webServiceUrl);
    final HttpHost host = determineTarget(request);
    final String hostName = host.getHostName();
    final int port = host.getPort();

    final AuthScope authScope = new AuthScope(hostName, port);
    final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username,
      password);

    final CredentialsProvider credentialsProvider = this.httpClient.getCredentialsProvider();
    credentialsProvider.setCredentials(authScope, credentials);

  }

  public void closeResponse(final HttpResponse response) {
    try {
      final HttpEntity entity = response.getEntity();
      try (
        final InputStream content = entity.getContent()) {
      }
    } catch (final Throwable e) {
    }
  }

  protected HttpStatusCodeException createException(final HttpEntity entity,
    final StatusLine statusLine) {
    final Logger log = LoggerFactory.getLogger(getClass());
    if (log.isDebugEnabled()) {
      try {
        final String errorBody = EntityUtils.toString(entity);
        log.debug("Unable to get message from server: " + statusLine + "\n" + errorBody);
      } catch (final Throwable e) {
        log.error("Unable to get error message server: " + statusLine + "\n");
      }
    }
    return new HttpStatusCodeException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
  }

  protected <V> V execute(final HttpUriRequest request, final ResponseHandler<V> responseHandler) {
    final BasicHttpContext context = new BasicHttpContext();

    final HttpHost host = determineTarget(request);

    try {
      final V response = this.httpClient.execute(host, request, responseHandler, context);
      return response;
    } catch (final Throwable e) {
      if (ThreadUtil.isInterrupted()) {
        throw new ThreadInterruptedException();
      } else {
        return ExceptionUtil.throwUncheckedException(e);
      }
    }
  }

  public Map<String, Object> getJsonResource(final HttpResponse response) {
    final StatusLine statusLine = response.getStatusLine();
    final int httpStatusCode = statusLine.getStatusCode();
    final HttpEntity entity = response.getEntity();

    if (httpStatusCode == HttpStatus.SC_OK) {
      try {
        final InputStream in = entity.getContent();
        try {
          final Map<String, Object> map = JsonParser.read(in);
          return map;
        } finally {
          FileUtil.closeSilent(in);
        }
      } catch (final Throwable e) {
        return ExceptionUtil.throwUncheckedException(e);
      }
    } else {
      throw createException(entity, statusLine);
    }
  }

  public Map<String, Object> getJsonResource(final HttpUriRequest request) {
    request.addHeader("Accept", "application/json");

    final Map<String, Object> response = execute(request, this.jsonResponseHandler);

    return response;
  }

  public Map<String, Object> getJsonResource(final String url) {
    final HttpGet request = new HttpGet(url);
    try {
      return getJsonResource(request);
    } catch (final Throwable e) {
      request.abort();
      return ExceptionUtil.throwUncheckedException(e);
    }
  }

  public void getResource(final String url, final File file) {
    final HttpGet request = new HttpGet(url);
    execute(request, new ResponseHandler<Void>() {
      @Override
      public Void handleResponse(final HttpResponse response) {
        final StatusLine statusLine = response.getStatusLine();
        final int httpStatusCode = statusLine.getStatusCode();
        final HttpEntity entity = response.getEntity();

        if (httpStatusCode == HttpStatus.SC_OK) {
          try {
            final InputStream in = entity.getContent();
            try {
              FileUtil.copy(in, file);
            } finally {
              FileUtil.closeSilent(in);
            }
          } catch (final Throwable e) {
            ExceptionUtil.throwUncheckedException(e);
          }
          return null;
        } else {
          throw createException(entity, statusLine);
        }
      }
    });
  }

  public String getUrl(final String path) {
    return this.webServiceUrl + path;
  }

  public Map<String, Object> postJsonResource(final String url) {
    final HttpPost request = new HttpPost(url);
    return getJsonResource(request);
  }

  public Map<String, Object> postJsonResource(final String url,
    final Map<String, ? extends Object> message) {
    final HttpPost request = new HttpPost(url);
    request.addHeader("Content-type", "application/json");
    final String bodyString = Json.toString(message);
    try {
      final StringEntity bodyEntity = new StringEntity(bodyString, "UTF-8");
      request.setEntity(bodyEntity);
      return getJsonResource(request);
    } catch (final Throwable e) {
      return ExceptionUtil.throwUncheckedException(e);
    }
  }

  public HttpResponse postResource(final String url, final String contentType, final File file) {
    try {
      final HttpPost request = new HttpPost(url);
      final FileEntity entity = new FileEntity(file, contentType);
      request.setEntity(entity);

      final BasicHttpContext context = new BasicHttpContext();

      final HttpResponse response = this.httpClient.execute(request, context);
      return response;
    } catch (final Throwable e) {
      return ExceptionUtil.throwUncheckedException(e);
    }
  }

  public HttpResponse postResource(final String url, final String contentType,
    final InputStream in) {
    try {
      final HttpPost request = new HttpPost(url);
      final InputStreamEntity entity = new InputStreamEntity(in, ContentType.create(contentType));
      request.setEntity(entity);

      final BasicHttpContext context = new BasicHttpContext();

      final HttpResponse response = this.httpClient.execute(request, context);
      return response;
    } catch (final Throwable e) {
      return ExceptionUtil.throwUncheckedException(e);
    }
  }
}
