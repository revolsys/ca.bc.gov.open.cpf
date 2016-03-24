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
package ca.bc.gov.open.cpf.plugins.digest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ca.bc.gov.open.cpf.plugin.api.AllowedValues;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.util.Hex;

@BusinessApplicationPlugin(perRequestInputData = true, numRequestsPerWorker = 1,
    instantModePermission = "denyAll",
    description = "The Map Tile by Location service returns the map tile id and polygon boundary for the map tile specified by latitude/longitude location.")
public class Digest {

  private String algorithmName;

  private URL inputDataUrl;

  private String digest;

  public void execute() {
    try {
      final MessageDigest digester = MessageDigest.getInstance("MD5");
      final InputStream in = this.inputDataUrl.openStream();
      final byte[] buffer = new byte[4096];
      for (int count = in.read(buffer); count != -1; count = in.read(buffer)) {
        digester.update(buffer, 0, count);
      }
      final byte[] data = digester.digest();
      this.digest = Hex.toHex(data);
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Cannot find digest algorithm " + this.algorithmName, e);
    } catch (final IOException e) {
      throw new RuntimeException("Cannot read input data", e);
    }
  }

  @ResultAttribute
  public String getAlgorithmName() {
    return this.algorithmName;
  }

  @ResultAttribute
  public String getDigest() {
    return this.digest;
  }

  @JobParameter
  @AllowedValues(value = {
    "MD5", "SHA-1", "SHA-256", "SHA-512"
  })
  public void setAlgorithmName(final String algorithmName) {
    this.algorithmName = algorithmName;
  }

  public void setInputDataContentType(final String inputDataContentType) {
  }

  public void setInputDataUrl(final URL inputDataUrl) {
    this.inputDataUrl = inputDataUrl;
  }
}
