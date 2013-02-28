package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>The AllowedValues method annotation defines the list of valid values for a
 * {@link JobParameter} or {@link RequestParameter}. The annotation can only be defined
 * on a setXXX method which has the {@link JobParameter} or {@link RequestParameter}.</p>
 *
 * <p>The list of allowed values is returned with the parameter descriptions in the
 * business application specifications web services.</p>
 * 
 * <p>The list of allowed values is used to create a select list field on the form. If the parameter
 * is not required the select field will include "-" to indicate the null (not selected) value.</p>
 *
 * <p>The list of allowed values are encoded as strings. The string values will be
 * converted to the data type of the parameter.</p>
 * 
 * <p>The following code fragment shows an example of using the API.</p>
 *
 * <pre class="prettyprint language-java">&#064;AllowedValues(value = {
  "MD5",
  "SHA"
})
&#064;JobParameter
&#064;RequestParameter
public void setAlgorithmName(final String algorithmName) {
  this.algorithmName = algorithmName;
}</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface AllowedValues {
  /** The list of allowed values encoded as strings. The string values will be converted to the data type of the parameter. */
  String[] value();
}
