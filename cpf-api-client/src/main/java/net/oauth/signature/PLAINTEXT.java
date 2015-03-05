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

import net.oauth.OAuth;
import net.oauth.OAuthException;

/**
 * The PLAINTEXT signature method.
 * 
 * @author John Kristian
 */
class PLAINTEXT extends OAuthSignatureMethod {

  private String signature = null;

  private synchronized String getSignature() {
    if (signature == null) {
      signature = OAuth.percentEncode(getConsumerSecret()) + '&'
        + OAuth.percentEncode(getTokenSecret());
    }
    return signature;
  }

  @Override
  public String getSignature(final String baseString) {
    return getSignature();
  }

  @Override
  protected boolean isValid(final String signature, final String baseString)
    throws OAuthException {
    return equals(getSignature(), signature);
  }

  @Override
  public void setConsumerSecret(final String consumerSecret) {
    synchronized (this) {
      signature = null;
    }
    super.setConsumerSecret(consumerSecret);
  }

  @Override
  public void setTokenSecret(final String tokenSecret) {
    synchronized (this) {
      signature = null;
    }
    super.setTokenSecret(tokenSecret);
  }

}
