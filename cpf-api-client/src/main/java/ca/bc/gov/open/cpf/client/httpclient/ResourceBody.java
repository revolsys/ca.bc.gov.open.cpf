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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.AbstractContentBody;

import com.revolsys.spring.resource.Resource;

@SuppressWarnings("javadoc")
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
      return this.resource.contentLength();
    } catch (final IOException e) {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public String getFilename() {
    try {
      return this.resource.getFilename();
    } catch (final IllegalStateException e) {
      return "unnamed";
    }
  }

  public InputStream getInputStream() throws IOException {
    return this.resource.getInputStream();
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
