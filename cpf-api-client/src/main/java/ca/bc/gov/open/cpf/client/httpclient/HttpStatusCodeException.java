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
package ca.bc.gov.open.cpf.client.httpclient;

public class HttpStatusCodeException extends RuntimeException {
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private final int statusCode;

  private final String statusMessage;

  public HttpStatusCodeException(final int statusCode, final String statusMessage) {
    super(statusCode + " " + statusMessage);
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public String getStatusMessage() {
    return this.statusMessage;
  }

}
