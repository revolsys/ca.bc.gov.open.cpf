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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

@SuppressWarnings("javadoc")
public class InvokeMethodResponseHandler<T> implements ResponseHandler<T> {

  /** The object to invoke the method on. */
  private Object object;

  /** The method to invoke. */
  private Method method;

  /**
   * Construct a new InvokeMethodResponseHandler.
   * 
   * @param methodName The name of the method to invoke.
   */
  public InvokeMethodResponseHandler(final Class<?> clazz,
    final String methodName) {
    try {
      method = clazz.getMethod(methodName, HttpResponse.class);
    } catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("Method could not be found " + clazz
        + "." + methodName, e);
    } catch (final Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Construct a new InvokeMethodResponseHandler with no parameters.
   * 
   * @param object The object to invoke the method on.
   * @param methodName The name of the method to invoke.
   */
  public InvokeMethodResponseHandler(final Object object,
    final String methodName) {
    this(object.getClass(), methodName);
    this.object = object;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T handleResponse(final HttpResponse response)
    throws ClientProtocolException, IOException {
    try {
      return (T)method.invoke(object, response);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        final IOException ioe = (IOException)cause;
        throw ioe;
      } else if (cause instanceof RuntimeException) {
        final RuntimeException re = (RuntimeException)cause;
        throw re;
      } else if (cause instanceof Error) {
        final Error t = (Error)cause;
        throw t;
      } else {
        throw new RuntimeException(cause);
      }

    } catch (final Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
