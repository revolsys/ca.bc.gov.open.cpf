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

@SuppressWarnings("javadoc")
public class HttpMultipartPost {
  private HttpClient httpclient = null;

  private HttpPost httppost = new HttpPost();

  private final MultipartEntity requestEntity = new MultipartEntity() {
    @Override
    public boolean isRepeatable() {
      return true;
    }
  };

  private HttpEntity responseEntity = null;

  private HttpResponse response = null;

  private final String userAgent = "";

  private String url;

  public HttpMultipartPost(final HttpClient httpclient, final String url) {
    this(new HttpPost(url));
    this.url = url;
    this.httpclient = httpclient;
  }

  public HttpMultipartPost(final HttpClient httpclient, final URL url) {
    this(url.toString());
  }

  public HttpMultipartPost(final HttpPost httppost) {
    httpclient = new DefaultHttpClient();
    this.httppost = httppost;

  }

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

  public HttpMultipartPost(final URL url) {
    this(url.toString());
  }

  public void addParameter(final String parameterName, final File file) {
    requestEntity.addPart(parameterName, new FileBody(file));
  }

  public void addParameter(final String parameterName,
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

  public void addParameter(final String parameterName, final Resource resource,
    final String cotentType) {
    final ResourceBody body = new ResourceBody(resource, cotentType);
    requestEntity.addPart(parameterName, body);
  }

  public void addParameter(final String parameterName, final String filename,
    final InputStream inputStream) throws IOException {
    requestEntity.addPart(parameterName, new InputStreamBody(inputStream,
      filename));
  }

  @SuppressWarnings("deprecation")
  public void close() throws IOException {
    if (responseEntity != null) {
      responseEntity.consumeContent();
    }
  }

  public HttpResponse getResponse() {
    return response;
  }

  public InputStream getResponseContentStream() throws IOException {
    return responseEntity.getContent();
  }

  public HttpEntity getResponseEntity() {
    return responseEntity;
  }

  public String getUrl() {
    return url;
  }

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
