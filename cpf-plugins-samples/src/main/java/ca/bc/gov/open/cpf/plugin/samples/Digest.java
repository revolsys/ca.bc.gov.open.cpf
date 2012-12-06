package ca.bc.gov.open.cpf.plugin.samples;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

import ca.bc.gov.open.cpf.plugin.api.AllowedValues;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

@BusinessApplicationPlugin(
    name = "Digest",
    version = "1.0.0",
    perRequestInputData = true,
    inputDataContentTypes = "*/*",
    instantModePermission = "denyAll")
public class Digest {

  private String algorithmName;

  private String digest;

  private URL inputDataUrl;

  public void execute() {
    try {
      final MessageDigest digester = MessageDigest.getInstance(algorithmName);
      final InputStream in = inputDataUrl.openStream();
      final byte[] buffer = new byte[4096];
      for (int count = in.read(buffer); count != -1; count = in.read(buffer)) {
        digester.update(buffer, 0, count);

      }
      final byte[] data = digester.digest();
      digest = new String(Hex.encodeHex(data));
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Cannot find digest algorithm "
        + algorithmName, e);
    } catch (final IOException e) {
      throw new IllegalArgumentException("Cannot read input data", e);
    }
  }

  @ResultAttribute
  public String getDigest() {
    return digest;
  }

  @AllowedValues(value = {
    "MD5", "SHA"
  })
  @Required
  @JobParameter
  public void setAlgorithmName(final String algorithmName) {
    this.algorithmName = algorithmName;
  }

  public void setInputDataUrl(final URL inputDataUrl) {
    this.inputDataUrl = inputDataUrl;
  }

}
