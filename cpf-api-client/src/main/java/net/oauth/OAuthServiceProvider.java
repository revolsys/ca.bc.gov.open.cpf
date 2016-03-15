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
package net.oauth;

import java.io.Serializable;

/**
 * Properties of an OAuth Service Provider.
 *
 * @author John Kristian
 */
@SuppressWarnings("javadoc")
public class OAuthServiceProvider implements Serializable {

  private static final long serialVersionUID = 3306534392621038574L;

  public final String accessTokenURL;

  public final String requestTokenURL;

  public final String userAuthorizationURL;

  public OAuthServiceProvider(final String requestTokenURL, final String userAuthorizationURL,
    final String accessTokenURL) {
    this.requestTokenURL = requestTokenURL;
    this.userAuthorizationURL = userAuthorizationURL;
    this.accessTokenURL = accessTokenURL;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final OAuthServiceProvider other = (OAuthServiceProvider)obj;
    if (this.accessTokenURL == null) {
      if (other.accessTokenURL != null) {
        return false;
      }
    } else if (!this.accessTokenURL.equals(other.accessTokenURL)) {
      return false;
    }
    if (this.requestTokenURL == null) {
      if (other.requestTokenURL != null) {
        return false;
      }
    } else if (!this.requestTokenURL.equals(other.requestTokenURL)) {
      return false;
    }
    if (this.userAuthorizationURL == null) {
      if (other.userAuthorizationURL != null) {
        return false;
      }
    } else if (!this.userAuthorizationURL.equals(other.userAuthorizationURL)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.accessTokenURL == null ? 0 : this.accessTokenURL.hashCode());
    result = prime * result + (this.requestTokenURL == null ? 0 : this.requestTokenURL.hashCode());
    result = prime * result
      + (this.userAuthorizationURL == null ? 0 : this.userAuthorizationURL.hashCode());
    return result;
  }

}
