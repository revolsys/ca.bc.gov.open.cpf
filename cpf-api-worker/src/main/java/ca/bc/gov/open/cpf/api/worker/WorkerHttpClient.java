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
package ca.bc.gov.open.cpf.api.worker;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import ca.bc.gov.open.cpf.client.httpclient.FunctionResponseHandler;
import ca.bc.gov.open.cpf.client.httpclient.HttpStatusCodeException;
import ca.bc.gov.open.cpf.plugin.impl.security.SignatureUtil;

import com.revolsys.collection.map.MapEx;
import com.revolsys.io.FileUtil;
import com.revolsys.logging.Logs;
import com.revolsys.parallel.ThreadInterruptedException;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.record.io.format.json.JsonParser;
import com.revolsys.util.Exceptions;
import com.revolsys.util.Property;
import com.revolsys.util.UrlUtil;

@SuppressWarnings("deprecation")
public class WorkerHttpClient {
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

  public static MapEx jsonToMap(final HttpResponse response) {
    final StatusLine statusLine = response.getStatusLine();
    final int httpStatusCode = statusLine.getStatusCode();
    final HttpEntity entity = response.getEntity();

    if (httpStatusCode == HttpStatus.SC_OK) {
      try {
        try (
          final InputStream in = entity.getContent()) {
          final MapEx map = JsonParser.read(in);
          return map;
        }
      } catch (final Throwable e) {
        return Exceptions.throwUncheckedException(e);
      }
    } else {
      throw newException(entity, statusLine);
    }
  }

  public static HttpStatusCodeException newException(final HttpEntity entity,
    final StatusLine statusLine) {
    final Logger log = Logs.logger(WorkerHttpClient.class);
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

  private final DefaultHttpClient httpClient;

  private final ResponseHandler<MapEx> jsonResponseHandler = new FunctionResponseHandler<>(
    WorkerHttpClient::jsonToMap);

  private final String webServiceUrl;

  private final String webServiceContextPath;

  private final String username;

  private final String password;

  public WorkerHttpClient(final String webServiceUrl, final String username, final String password,
    final int poolSize) {
    this.webServiceUrl = webServiceUrl;
    this.username = username;
    this.password = password;
    final URL url = UrlUtil.getUrl(webServiceUrl);
    this.webServiceContextPath = url.getPath();

    final ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();
    connectionManager.setDefaultMaxPerRoute(poolSize);
    connectionManager.setMaxTotal(poolSize);
    this.httpClient = new DefaultHttpClient(connectionManager);
  }

  public void close() {
    this.httpClient.close();
  }

  public CloseableHttpResponse execute(final HttpUriRequest request) {
    final BasicHttpContext context = new BasicHttpContext();

    final HttpHost host = determineTarget(request);

    try {
      return this.httpClient.execute(host, request, context);
    } catch (final Throwable e) {
      if (ThreadUtil.isInterrupted()) {
        throw new ThreadInterruptedException();
      } else {
        return Exceptions.throwUncheckedException(e);
      }
    }
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
        return Exceptions.throwUncheckedException(e);
      }
    }
  }

  public CloseableHttpResponse execute(final String path) {
    final String url = getUrl(path, null);
    final HttpGet request = new HttpGet(url);
    return execute(request);
  }

  private MapEx getJsonResource(final HttpUriRequest request) {
    request.addHeader("Accept", "application/json");

    final MapEx response = execute(request, this.jsonResponseHandler);

    return response;
  }

  public Map<String, Object> getJsonResource(final String path) {
    final String url = getUrl(path, null);
    final HttpGet request = new HttpGet(url);
    try {
      return getJsonResource(request);
    } catch (final Throwable e) {
      request.abort();
      return Exceptions.throwUncheckedException(e);
    }
  }

  public void getResource(final String path, final File file) {
    final String url = getUrl(path, null);
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
            Exceptions.throwUncheckedException(e);
          }
          return null;
        } else {
          throw newException(entity, statusLine);
        }
      }
    });
  }

  public String getUrl(final String path, final Map<String, ? extends Object> parameters) {
    final String fullPath = this.webServiceContextPath + path;
    final long time = System.currentTimeMillis();
    final String signature = SignatureUtil.sign(this.password, fullPath, time);
    final StringBuilder url = new StringBuilder(this.webServiceUrl);
    url.append(path);
    url.append("?workerUsername=");
    url.append(SignatureUtil.urlEncode(this.username));
    url.append("&workerSignature=");
    url.append(SignatureUtil.urlEncode(signature));
    url.append("&workerTime=");
    url.append(SignatureUtil.urlEncode(time));
    if (Property.hasValue(parameters)) {
      url.append('&');
      UrlUtil.appendQuery(url, parameters);
    }
    return url.toString();
  }

  public MapEx postGetJsonResource(final String path,
    final Map<String, ? extends Object> parameters) {
    final String url = getUrl(path, parameters);
    final HttpPost request = new HttpPost(url);
    return getJsonResource(request);
  }

  public HttpResponse postResource(final String path, final String contentType, final File file) {
    final String url = getUrl(path, null);
    try {
      final HttpPost request = new HttpPost(url);
      final FileEntity entity = new FileEntity(file, contentType);
      request.setEntity(entity);

      final BasicHttpContext context = new BasicHttpContext();

      final HttpResponse response = this.httpClient.execute(request, context);
      return response;
    } catch (final Throwable e) {
      return Exceptions.throwUncheckedException(e);
    }
  }

  public HttpResponse postResource(final String path, final String contentType,
    final InputStream in) {
    return postResource(path, contentType, in, null);
  }

  public HttpResponse postResource(final String path, final String contentType,
    final InputStream in, final Map<String, Object> parameters) {
    final String url = getUrl(path, parameters);
    try {
      final HttpPost request = new HttpPost(url);
      final InputStreamEntity entity = new InputStreamEntity(in, ContentType.create(contentType));
      request.setEntity(entity);

      final BasicHttpContext context = new BasicHttpContext();

      final HttpResponse response = this.httpClient.execute(request, context);
      return response;
    } catch (final Throwable e) {
      return Exceptions.throwUncheckedException(e);
    }
  }
}
