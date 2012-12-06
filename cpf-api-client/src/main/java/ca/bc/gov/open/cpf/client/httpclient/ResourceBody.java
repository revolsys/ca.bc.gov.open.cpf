package ca.bc.gov.open.cpf.client.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.springframework.core.io.Resource;

public class ResourceBody extends AbstractContentBody {

  private final Resource resource;

  public ResourceBody(final Resource resource, final String contentType) {
    super(contentType);
    this.resource = resource;
  }

  @Override
  public String getCharset() {
    return null;
  }

  @Override
  public long getContentLength() {
    try {
      return resource.contentLength();
    } catch (final IOException e) {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public String getFilename() {
    try {
      return resource.getFilename();
    } catch (final IllegalStateException e) {
      return "unnamed";
    }
  }

  public InputStream getInputStream() throws IOException {
    return resource.getInputStream();
  }

  @Override
  public String getTransferEncoding() {
    return MIME.ENC_BINARY;
  }

  @Override
  public void writeTo(final OutputStream out) throws IOException {
    if (out == null) {
      throw new IllegalArgumentException("Output stream may not be null");
    }
    final InputStream in = getInputStream();
    try {
      final byte[] tmp = new byte[4096];
      int l;
      while ((l = in.read(tmp)) != -1) {
        out.write(tmp, 0, l);
      }
      out.flush();
    } finally {
      in.close();
    }
  }
}
