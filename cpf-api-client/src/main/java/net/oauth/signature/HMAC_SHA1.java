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
package net.oauth.signature;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import net.oauth.OAuth;
import net.oauth.OAuthException;

/**
 * The HMAC-SHA1 signature method.
 *
 * @author John Kristian
 */
class HMAC_SHA1 extends OAuthSignatureMethod {

  /** ISO-8859-1 or US-ASCII would work, too. */
  private static final String ENCODING = OAuth.ENCODING;

  private static final String MAC_NAME = "HmacSHA1";

  private SecretKey key = null;

  private byte[] computeSignature(final String baseString)
    throws GeneralSecurityException, UnsupportedEncodingException {
    SecretKey key = null;
    synchronized (this) {
      if (this.key == null) {
        final String keyString = OAuth.percentEncode(getConsumerSecret()) + '&'
          + OAuth.percentEncode(getTokenSecret());
        final byte[] keyBytes = keyString.getBytes(ENCODING);
        this.key = new SecretKeySpec(keyBytes, MAC_NAME);
      }
      key = this.key;
    }
    final Mac mac = Mac.getInstance(MAC_NAME);
    mac.init(key);
    final byte[] text = baseString.getBytes(ENCODING);
    return mac.doFinal(text);
  }

  @Override
  protected String getSignature(final String baseString) throws OAuthException {
    try {
      final String signature = base64Encode(computeSignature(baseString));
      return signature;
    } catch (final GeneralSecurityException e) {
      throw new OAuthException(e);
    } catch (final UnsupportedEncodingException e) {
      throw new OAuthException(e);
    }
  }

  @Override
  protected boolean isValid(final String signature, final String baseString) throws OAuthException {
    try {
      final byte[] expected = computeSignature(baseString);
      final byte[] actual = decodeBase64(signature);
      return equals(expected, actual);
    } catch (final GeneralSecurityException e) {
      throw new OAuthException(e);
    } catch (final UnsupportedEncodingException e) {
      throw new OAuthException(e);
    }
  }

  @Override
  public void setConsumerSecret(final String consumerSecret) {
    synchronized (this) {
      this.key = null;
    }
    super.setConsumerSecret(consumerSecret);
  }

  @Override
  public void setTokenSecret(final String tokenSecret) {
    synchronized (this) {
      this.key = null;
    }
    super.setTokenSecret(tokenSecret);
  }

}
