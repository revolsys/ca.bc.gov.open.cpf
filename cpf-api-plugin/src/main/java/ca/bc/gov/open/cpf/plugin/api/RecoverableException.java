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
package ca.bc.gov.open.cpf.plugin.api;

/**
 * <p>A recoverable exception indicates that the cause of the exception can be
 * recovered from. If a plug-in throws subclasses exception the request will be sent
 * to another worker for execution.</p>
 *
 * <p>This could be used if the connection to the database was unavailable at that time or if
 * some other resource was temprarily unnavailble.</p>
 */
public class RecoverableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Construct a new <code>RecoverableException</code>.
   */
  public RecoverableException() {
  }

  /**
   * Construct a new <code>RecoverableException</code>.
   *
   * @param message The exception message.
   */
  public RecoverableException(final String message) {
    super(message);
  }

  /**
   * Construct a new <code>RecoverableException</code>.
   *
   * @param message The exception message.
   * @param cause The cause of the exception.
   */
  public RecoverableException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Construct a new <code>RecoverableException</code>.
   *
   * @param cause The cause of the exception.
   */
  public RecoverableException(final Throwable cause) {
    super(cause);
  }

}
