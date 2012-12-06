/*
 * Copyright 2007 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.oauth.signature;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

/**
 * A pair of algorithms for computing and verifying an OAuth digital signature.
 * <p>
 * Static methods of this class implement a registry of signature methods. It's
 * pre-populated with the standard OAuth algorithms. Appliations can replace
 * them or add new ones.
 * 
 * @author John Kristian
 */
public abstract class OAuthSignatureMethod {

  /** An efficiently sortable wrapper around a parameter. */
  private static class ComparableParameter implements
    Comparable<ComparableParameter> {

    private static String toString(final Object from) {
      return (from == null) ? null : from.toString();
    }

    final Entry<String,String> value;

    private final String key;

    ComparableParameter(final Entry<String,String> value) {
      this.value = value;
      final String n = toString(value.getKey());
      final String v = toString(value.getValue());
      this.key = OAuth.percentEncode(n) + ' ' + OAuth.percentEncode(v);
      // ' ' is used because it comes before any character
      // that can appear in a percentEncoded string.
    }

    @Override
    public int compareTo(final ComparableParameter that) {
      return this.key.compareTo(that.key);
    }

    @Override
    public String toString() {
      return key;
    }

  }

  public static final String _ACCESSOR = "-Accessor";

  /**
   * The character encoding used for base64. Arguably US-ASCII is more accurate,
   * but this one decodes all byte values unambiguously.
   */
  private static final String BASE64_ENCODING = "ISO-8859-1";

  private static final Base64 BASE64 = new Base64();

  private static final Map<String, Class<?>> NAME_TO_CLASS = new ConcurrentHashMap<String, Class<?>>();

  static {
    registerMethodClass("HMAC-SHA1", HMAC_SHA1.class);
    registerMethodClass("PLAINTEXT", PLAINTEXT.class);
    registerMethodClass("RSA-SHA1", RSA_SHA1.class);
    registerMethodClass("HMAC-SHA1" + _ACCESSOR, HMAC_SHA1.class);
    registerMethodClass("PLAINTEXT" + _ACCESSOR, PLAINTEXT.class);
  }

  public static String base64Encode(final byte[] b) {
    final byte[] b2 = BASE64.encode(b);
    try {
      return new String(b2, BASE64_ENCODING);
    } catch (final UnsupportedEncodingException e) {
      System.err.println(e + "");
    }
    return new String(b2);
  }

  public static byte[] decodeBase64(final String s) {
    byte[] b;
    try {
      b = s.getBytes(BASE64_ENCODING);
    } catch (final UnsupportedEncodingException e) {
      System.err.println(e + "");
      b = s.getBytes();
    }
    return BASE64.decode(b);
  }

  /**
   * Determine whether the given arrays contain the same sequence of bytes. The
   * implementation discourages a <a
   * href="http://codahale.com/a-lesson-in-timing-attacks/">timing attack</a>.
   */
  public static boolean equals(final byte[] a, final byte[] b) {
    if (a == null) {
      return b == null;
    } else if (b == null) {
      return false;
    } else if (b.length <= 0) {
      return a.length <= 0;
    }
    byte diff = (byte)((a.length == b.length) ? 0 : 1);
    int j = 0;
    for (int i = 0; i < a.length; ++i) {
      diff |= a[i] ^ b[j];
      j = (j + 1) % b.length;
    }
    return diff == 0;
  }

  /**
   * Determine whether the given strings contain the same sequence of
   * characters. The implementation discourages a <a
   * href="http://codahale.com/a-lesson-in-timing-attacks/">timing attack</a>.
   */
  public static boolean equals(final String x, final String y) {
    if (x == null) {
      return y == null;
    } else if (y == null) {
      return false;
    } else if (y.length() <= 0) {
      return x.length() <= 0;
    }
    final char[] a = x.toCharArray();
    final char[] b = y.toCharArray();
    char diff = (char)((a.length == b.length) ? 0 : 1);
    int j = 0;
    for (int i = 0; i < a.length; ++i) {
      diff |= a[i] ^ b[j];
      j = (j + 1) % b.length;
    }
    return diff == 0;
  }

  public static String getBaseString(final OAuthMessage message)
    throws IOException, URISyntaxException {
    List<Entry<String,String>> parameters;
    String url = message.URL;
    final int q = url.indexOf('?');
    if (q < 0) {
      parameters = message.getParameters();
    } else {
      // Combine the URL query string with the other parameters:
      parameters = new ArrayList<Map.Entry<String,String>>();
      parameters.addAll(OAuth.decodeForm(message.URL.substring(q + 1)));
      parameters.addAll(message.getParameters());
      url = url.substring(0, q);
    }
    return OAuth.percentEncode(message.method.toUpperCase()) + '&'
      + OAuth.percentEncode(normalizeUrl(url)) + '&'
      + OAuth.percentEncode(normalizeParameters(parameters));
  }

  /** Retrieve the original parameters from a sorted collection. */
  private static List<Entry<String,String>> getParameters(
    final Collection<ComparableParameter> parameters) {
    if (parameters == null) {
      return null;
    }
    final List<Entry<String,String>> list = new ArrayList<Entry<String,String>>(parameters.size());
    for (final ComparableParameter parameter : parameters) {
      list.add(parameter.value);
    }
    return list;
  }

  /** The factory for signature methods. */
  public static OAuthSignatureMethod newMethod(
    final String name,
    final OAuthAccessor accessor) throws OAuthException {
    try {
      final Class<?> methodClass = NAME_TO_CLASS.get(name);
      if (methodClass != null) {
        final OAuthSignatureMethod method = (OAuthSignatureMethod)methodClass.newInstance();
        method.initialize(name, accessor);
        return method;
      }
      final OAuthProblemException problem = new OAuthProblemException(
        OAuth.Problems.SIGNATURE_METHOD_REJECTED);
      final String acceptable = OAuth.percentEncode(NAME_TO_CLASS.keySet());
      if (acceptable.length() > 0) {
        problem.setParameter("oauth_acceptable_signature_methods",
          acceptable.toString());
      }
      throw problem;
    } catch (final InstantiationException e) {
      throw new OAuthException(e);
    } catch (final IllegalAccessException e) {
      throw new OAuthException(e);
    }
  }

  public static OAuthSignatureMethod newSigner(
    final OAuthMessage message,
    final OAuthAccessor accessor) throws IOException, OAuthException {
    message.requireParameters(OAuth.OAUTH_SIGNATURE_METHOD);
    final OAuthSignatureMethod signer = newMethod(message.getSignatureMethod(),
      accessor);
    signer.setTokenSecret(accessor.tokenSecret);
    return signer;
  }

  protected static String normalizeParameters(
    final Collection<? extends Entry<String,String>> parameters) throws IOException {
    if (parameters == null) {
      return "";
    }
    final List<ComparableParameter> p = new ArrayList<ComparableParameter>(
      parameters.size());
    for (final Entry<String,String> parameter : parameters) {
      if (!"oauth_signature".equals(parameter.getKey())) {
        p.add(new ComparableParameter(parameter));
      }
    }
    Collections.sort(p);
    return OAuth.formEncode(getParameters(p));
  }

  protected static String normalizeUrl(final String url)
    throws URISyntaxException {
    final URI uri = new URI(url);
    final String scheme = uri.getScheme().toLowerCase();
    String authority = uri.getAuthority().toLowerCase();
    final boolean dropPort = (scheme.equals("http") && uri.getPort() == 80)
      || (scheme.equals("https") && uri.getPort() == 443);
    if (dropPort) {
      // find the last : in the authority
      final int index = authority.lastIndexOf(":");
      if (index >= 0) {
        authority = authority.substring(0, index);
      }
    }
    String path = uri.getRawPath();
    if (path == null || path.length() <= 0) {
      path = "/"; // conforms to RFC 2616 section 3.2.2
    }
    // we know that there is no query and no fragment here.
    return scheme + "://" + authority + path;
  }

  /**
   * Subsequently, newMethod(name) will attempt to instantiate the given class,
   * with no constructor parameters.
   */
  public static void registerMethodClass(final String name, final Class<?> clazz) {
    if (clazz == null) {
      unregisterMethod(name);
    } else {
      NAME_TO_CLASS.put(name, clazz);
    }
  }

  /**
   * Subsequently, newMethod(name) will fail.
   */
  public static void unregisterMethod(final String name) {
    NAME_TO_CLASS.remove(name);
  }

  private String consumerSecret;

  private String tokenSecret;

  protected String getConsumerSecret() {
    return consumerSecret;
  }

  protected String getSignature(final OAuthMessage message)
    throws OAuthException, IOException, URISyntaxException {
    final String baseString = getBaseString(message);
    final String signature = getSignature(baseString);
    // Logger log = Logger.getLogger(getClass().getName());
    // if (log.isLoggable(Level.FINE)) {
    // log.fine(signature + "=getSignature(" + baseString + ")");
    // }
    return signature;
  }

  /** Compute the signature for the given base string. */
  protected abstract String getSignature(String baseString)
    throws OAuthException;

  public String getTokenSecret() {
    return tokenSecret;
  }

  protected void initialize(final String name, final OAuthAccessor accessor)
    throws OAuthException {
    String secret = accessor.consumer.consumerSecret;
    if (name.endsWith(_ACCESSOR)) {
      // This code supports the 'Accessor Secret' extensions
      // described in http://oauth.pbwiki.com/AccessorSecret
      final String key = OAuthConsumer.ACCESSOR_SECRET;
      Object accessorSecret = accessor.getProperty(key);
      if (accessorSecret == null) {
        accessorSecret = accessor.consumer.getProperty(key);
      }
      if (accessorSecret != null) {
        secret = accessorSecret.toString();
      }
    }
    if (secret == null) {
      secret = "";
    }
    setConsumerSecret(secret);
  }

  /** Decide whether the signature is valid. */
  protected abstract boolean isValid(String signature, String baseString)
    throws OAuthException;

  protected void setConsumerSecret(final String consumerSecret) {
    this.consumerSecret = consumerSecret;
  }

  public void setTokenSecret(final String tokenSecret) {
    this.tokenSecret = tokenSecret;
  }

  /**
   * Add a signature to the message.
   * 
   * @throws URISyntaxException
   * @throws IOException
   */
  public void sign(final OAuthMessage message) throws OAuthException,
    IOException, URISyntaxException {
    message.addParameter(new OAuth.Parameter("oauth_signature",
      getSignature(message)));
  }

  /**
   * Check whether the message has a valid signature.
   * 
   * @throws URISyntaxException
   * @throws OAuthProblemException the signature is invalid
   */
  public void validate(final OAuthMessage message) throws IOException,
    OAuthException, URISyntaxException {
    message.requireParameters("oauth_signature");
    final String signature = message.getSignature();
    final String baseString = getBaseString(message);
    if (!isValid(signature, baseString)) {
      final OAuthProblemException problem = new OAuthProblemException(
        "signature_invalid");
      problem.setParameter("oauth_signature", signature);
      problem.setParameter("oauth_signature_base_string", baseString);
      problem.setParameter("oauth_signature_method",
        message.getSignatureMethod());
      throw problem;
    }
  }

}
