package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>The Required method annotation defines a
 * {@link JobParameter} or {@link RequestParameter} as being required. If a value is not specified
 * for a required parameter the scheduler will exclude that request from being processed and an
 * error returned. The annotation can only be defined
 * on a <code>setXXX</code> method which has the {@link JobParameter} or {@link RequestParameter} annotations.</p>
 * 
 * <p>The following example shows the use of the annotation on a {@link JobParameter} method.</p>
 *
 * <figure><pre class="prettyprint language-java">&#064;Required
&#064;JobParameter
public void setAlgorithmName(final String algorithmName) {
  this.algorithmName = algorithmName;
}</pre></figure>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Required {
}
