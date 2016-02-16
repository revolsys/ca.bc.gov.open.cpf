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
 * <p>The <code>AllowedValues</code> method annotation defines the list of valid values for a
 * {@link JobParameter} or {@link RequestParameter}. The annotation can only be defined
 * on a <code>setXXX</code> method which has either the {@link JobParameter} or {@link RequestParameter} annotations.</p>
 *
 * <p>The list of allowed values are encoded as strings. The string values will be
 * converted to the data type of the parameter on the <code>setXXX</code> method.</p>
 *
 * <p>The list of allowed values is returned with the parameter descriptions in the
 * business application specifications web services.</p>
 *
 * <p>The list of allowed values is used to Construct a new select list field on the job submission form. If the parameter
 * is not required the select field will include "-" to indicate the null (not selected) value.</p>
 *
 * <p>The following code fragment shows an example of using the API.</p>
 *
 * <figure><pre class="prettyprint language-java">&#064;AllowedValues(value = {
  "MD5",
  "SHA"
})
&#064;JobParameter
&#064;RequestParameter
public void setAlgorithmName(final String algorithmName) {
  this.algorithmName = algorithmName;
}</pre></figure>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface AllowedValues {
  /**
   * The list of allowed values encoded as strings. The string values will be converted to the data type of the parameter.
   *
   * @return The list of allowed values.
   */
  String[]value();
}
