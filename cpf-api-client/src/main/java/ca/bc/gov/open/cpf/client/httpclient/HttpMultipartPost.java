package ca.bc.gov.open.cpf.client.httpclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.springframework.core.io.Resource;

/**
 * Utility for HTTP Post messages and their associated response messages.
 */

@SuppressWarnings("javadoc")
public class HttpMultipartPost {
  private HttpClient httpclient = null;

  /** HTTP Post method */
  private HttpPost httppost = new HttpPost();

  /** HTTP request entity sent with HTTP post message */
  private final MultipartEntity requestEntity = new MultipartEntity() {
    @Override
    public boolean isRepeatable() {
      return true;
    }
  };

  /** HTTP response entity received with HTTP response message */
  private HttpEntity responseEntity = null;

  /** HTTP response object returned from a post request */
  private HttpResponse response = null;

  /** userAgent set in header */
  private final String userAgent = "";

  private String url;

  /**
   * @param httpclient
   * @param url
   */
  public HttpMultipartPost(final HttpClient httpclient, final String url) {
    this(new HttpPost(url));
    this.url = url;
    this.httpclient = httpclient;
  }

  /**
   * @param httpclient
   * @param url
   */
  public HttpMultipartPost(final HttpClient httpclient, final URL url) {
    this(url.toString());
  }

  /**
   * Constructor for a previously instantiated post for which the headers have
   * been externally set.
   * 
   * @param httppost
   */
  public HttpMultipartPost(final HttpPost httppost) {
    httpclient = new DefaultHttpClient();
    this.httppost = httppost;

  }

  /**
   * @param urlString
   */
  public HttpMultipartPost(final String urlString) {
    this(new HttpPost(urlString));
    if (!"".equals(userAgent)) {
      httppost.setHeader("User-Agent", userAgent);
    }
    httppost.setHeader("Accept", "text/csv");
    if (httpclient == null) {
      httpclient = new DefaultHttpClient();
    }
  }

  /**
   * @param url
   */
  public HttpMultipartPost(final URL url) {
    this(url.toString());
  }

  /**
   * Add a file parameter.
   * 
   * @param parameterName
   * @param file
   */
  public void addParameter(final String parameterName, final File file) {
    requestEntity.addPart(parameterName, new FileBody(file));
  }

  /**
   * @param parameterName
   * @param parameterValue
   */
  public void addParameter(
    final String parameterName,
    final Object parameterValue) {
    if (parameterValue != null) {
      try {
        requestEntity.addPart(parameterName,
          new StringBody(parameterValue.toString()));
      } catch (final UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @param parameterName
   * @param parameterValue
   */
  public void addParameter(
    final String parameterName,
    final Resource resource,
    final String cotentType) {
    final ResourceBody body = new ResourceBody(resource, cotentType);
    requestEntity.addPart(parameterName, body);
  }

  /**
   * @param parameterName Parameter name
   * @param filename name of file associated with input stream
   * @param inputStream
   * @throws IOException
   */
  public void addParameter(
    final String parameterName,
    final String filename,
    final InputStream inputStream) throws IOException {
    requestEntity.addPart(parameterName, new InputStreamBody(inputStream,
      filename));
  }

  /**
   * Flush the response data stream.
   * 
   * @throws IOException
   */
  @SuppressWarnings("deprecation")
  public void close() throws IOException {
    if (responseEntity != null) {
      responseEntity.consumeContent();
    }
  }

  /**
   * Get the HTTP response message.
   * 
   * @return
   */
  public HttpResponse getResponse() {
    return response;
  }

  /**
   * Get input stream for response content
   * 
   * @throws IOException
   * @throws IllegalStateException
   * @throws IOException
   * @throws IllegalStateException
   */
  public InputStream getResponseContentStream() throws IOException {
    return responseEntity.getContent();
  }

  /**
   * Get response entity returned from HTTP response message.
   * 
   * @return
   */
  public HttpEntity getResponseEntity() {
    return responseEntity;
  }

  public String getUrl() {
    return url;
  }

  /**
   * Send the HTTP POST message.
   * 
   * @param context
   * @return HTTP status code.
   * @throws IOException
   */
  public int postRequest(final HttpContext context) throws IOException {
    response = null;
    int statusCode = HttpStatus.SC_BAD_REQUEST; // 400
    httppost.setEntity(requestEntity);

    try {
      response = httpclient.execute(httppost, context);
      statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < HttpStatus.SC_BAD_REQUEST) {
        responseEntity = response.getEntity();
      }
    } catch (final ClientProtocolException e) {
      e.printStackTrace();
    } catch (final IOException e) {
      e.printStackTrace();
    }
    return statusCode;
  }
}
