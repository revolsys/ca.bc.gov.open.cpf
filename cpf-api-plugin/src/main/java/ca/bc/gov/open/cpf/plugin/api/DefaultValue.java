package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>The <code>DefaultValue</code> method annotation defines the default value to display on the form or use
 * if a value was not provided for a {@link JobParameter} or {@link RequestParameter}. The annotation
 * can only be defined on a <code>setXXX</code> method which has the {@link JobParameter} or {@link RequestParameter}.</p>
 *
 * <p>The default value is encoded as a string. The string values will be
 * converted to the data type of the parameter on the <code>setXXX</code> method.</p>
 *
 * <p>The default value is displayed with the parameter descriptions in the
 * business application specifications web services.</p>
 * 
 * <p>On the submit jobs form the default value is displayed in the text field or as selected value for
 * a select list created using {@link AllowedValues} on the form.</p>
 * 
 * <p>The following code fragment shows an example of using the API.</p>
 *
 * <figure><pre class="prettyprint language-java">&#064;DefaultValue(value = "10"})
&#064;JobParameter
&#064;RequestParameter
public void setMaxResults(final int maxResults) {
  this.maxResults = maxResults;
}</pre></figure>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface DefaultValue {
  /** The default value encoded as a string. The string value will be converted to the data type of the parameter. */
  String value();
}
