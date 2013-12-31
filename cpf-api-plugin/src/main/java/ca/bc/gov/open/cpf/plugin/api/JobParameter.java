package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>The <code>JobParameter</code> method annotation indicates that a <code>setXXX</code> method is
 * a parameter that will be applied to all requests in a cloud job on a {@link BusinessApplicationPlugin}
 * class.</p>
 * 
 * <p>Job parameters are implemented as Java bean properties on the plug-in class.
 * The plug-in must implement a <code>setXXX</code> property method for each job parameter. The
 * job parameter name is the name of the Java bean property. The parameter type can
 * only use the supported <a href="../../dataTypes.html">data types</a>. The job parameters will be converted by
 * the CPF from the input data to the correct Java type.</p>
 * 
 * <p>Before execution of the plug-in the job methods will be invoked to set the job parameter values.</p>
 * 
 * <p>A <code>JobParameter</code> method can also be marked as a {@link RequestParameter} if the
 * parameter can be specified either at the job or request level.</p>
 * 
 * <p>The following example shows the use of the annotation.</p>
 * 
 * <figure><pre class="prettyprint language-java">private String algorithmName;
 
&#064;JobParameter
public void setAlgorithmName(final String algorithmName) {
  this.algorithmName = algorithmName;
}</pre></figure>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface JobParameter {
  /**
   * The description of the job parameter to display on the plug-in overview
   * page and as instructions on the create job forms.
   */
  String description() default "";

  /**
   * The url to a page that describes the parameter in greater detail than is possible on the
   * form. If specified the name of the parameter will be a hyper-link to this URL. 
   */
  String descriptionUrl() default "";

  /** The index (position) of the job parameter in the input file form. */
  int index() default -1;

  /**
   * The maximum length of the job parameter including the scale. This is ignored for
   * fixed size data types such as boolean, byte, short, int, long, float and
   * double. The value -1 indicates no defined limit to the scale.
   */
  int length() default -1;

  /**
   * The number of decimal places for fixed precision numeric types.
   */
  int scale() default -1;

  /**
   * The units of measurement for numeric fields (e.g. metres, feet, degrees).
   * This will be displayed after the field on the form.
   */
  String units() default "";
}
