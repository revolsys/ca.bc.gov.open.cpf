package net.oauth.signature.pem;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.oauth.signature.OAuthSignatureMethod;

/**
 * This class convert PEM into byte array. The begin marker is saved and it can
 * be used to determine the type of the PEM file.
 * 
 * @author zhang
 */
public class PEMReader {

  // Begin markers for all supported PEM files
  public static final String PRIVATE_PKCS1_MARKER = "-----BEGIN RSA PRIVATE KEY-----";

  public static final String PRIVATE_PKCS8_MARKER = "-----BEGIN PRIVATE KEY-----";

  public static final String CERTIFICATE_X509_MARKER = "-----BEGIN CERTIFICATE-----";

  public static final String PUBLIC_X509_MARKER = "-----BEGIN PUBLIC KEY-----";

  private static final String BEGIN_MARKER = "-----BEGIN ";

  private final InputStream stream;

  private byte[] derBytes;

  private String beginMarker;

  public PEMReader(final byte[] buffer) throws IOException {
    this(new ByteArrayInputStream(buffer));
  }

  public PEMReader(final InputStream inStream) throws IOException {
    stream = inStream;
    readFile();
  }

  public PEMReader(final String fileName) throws IOException {
    this(new FileInputStream(fileName));
  }

  public String getBeginMarker() {
    return beginMarker;
  }

  public byte[] getDerBytes() {
    return derBytes;
  }

  /**
   * Read the lines between BEGIN and END marker and convert the Base64 encoded
   * content into binary byte array.
   * 
   * @return DER encoded octet stream
   * @throws IOException
   */
  private byte[] readBytes(final BufferedReader reader, final String endMarker)
    throws IOException {
    String line = null;
    final StringBuffer buf = new StringBuffer();

    while ((line = reader.readLine()) != null) {
      if (line.indexOf(endMarker) != -1) {

        return OAuthSignatureMethod.decodeBase64(buf.toString());
      }

      buf.append(line.trim());
    }

    throw new IOException("Invalid PEM file: No end marker");
  }

  /**
   * Read the PEM file and save the DER encoded octet stream and begin marker.
   * 
   * @throws IOException
   */
  protected void readFile() throws IOException {

    String line;
    final BufferedReader reader = new BufferedReader(new InputStreamReader(
      stream));
    try {
      while ((line = reader.readLine()) != null) {
        if (line.indexOf(BEGIN_MARKER) != -1) {
          beginMarker = line.trim();
          final String endMarker = beginMarker.replace("BEGIN", "END");
          derBytes = readBytes(reader, endMarker);
          return;
        }
      }
      throw new IOException("Invalid PEM file: no begin marker");
    } finally {
      reader.close();
    }
  }
}
