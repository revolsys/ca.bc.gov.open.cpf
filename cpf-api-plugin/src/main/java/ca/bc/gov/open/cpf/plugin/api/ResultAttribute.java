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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>The <code>ResultAttribute</code> method annotation indicates that a <code>getXXX</code> or <code>isXXX</code> method is a <a href="../../structuredData.html">structured
 * result data</a> attribute on a {@link BusinessApplicationPlugin} or {@link ResultList}
 * class that has perRequestResultData=false.</p>
 *
 * <p>Result attributes are implemented as Java bean properties on the plug-in class or {@link ResultList} class.
 * The plug-in must implement a <code>getXXX</code> or <code>isXXX</code> property method for each result attribute. The
 * result attribute name is the name of the Java bean property. The return type can
 * only use the supported <a href="../../dataTypes.html">data types</a>. The result attributes will be converted by
 * the CPF to the correct representation in the output format or a string
 * representation of the value before being returned to the user.</p>
 *
 * <p>After execution of the plug-in the result attribute methods will be invoked to get the result attribute values.</p>
 *
 * <p>The following example shows the use of the annotation on a {@link BusinessApplicationPlugin} class.</p>
 *
 * <figure><pre class="prettyprint language-java">&#064;BusinessApplicationPlugin
public class Square {
  private int square;

  &#064;ResultAttribute(index = 2, description = "The square of a value")
  public int getSquare() {
    return square;
  }
}</pre></figure>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface ResultAttribute {
  /**
   * The description of the result attribute to display on the plug-in overview
   * page and as instructions on the create job forms. If the description
   * is not specified and there is a {@link RequestParameter} with the same name
   * that has a description then that will be used. This simplifies coding as it
   * removes the need to duplicate the description.
   */
  String description() default "";

  /** The index  (position) of the attribute in the output file. */
  int index() default -1;

  /**
   * <p>The maximum length of the attribute including the scale, sign '-' and decimal point '.'.
   * This is ignored for fixed size data types such as boolean, byte, short, int, long.
   * The value -1 indicates the use of the default value for that data type.</p>
   *
   * <p class="note">NOTE: decimal types such as float, double and BigDecimal should have a
   * length specified if fixed size file formats such as shapefile are to be used.</p>
   */
  int length() default -1;

  /**
   * <p>The number of decimal places for fixed precision numeric types. This is ignored for
   * data types such as boolean, byte, short, int, long, string.
   * The value -1 indicates the use of the default value for that data type.</p>
   *
   * <p class="note">NOTE: decimal types such as float, double and BigDecimal should have a
   * scaled specified if fixed size file formats such as shapefile are to be used.</p>
   */
  int scale() default -1;
}
