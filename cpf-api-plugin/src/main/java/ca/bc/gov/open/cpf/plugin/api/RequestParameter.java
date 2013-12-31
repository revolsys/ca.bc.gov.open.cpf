package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>The <code>RequestParameter</code> method annotation indicates that a <code>setXXX</code> method is
 * a <a href="../../structuredData.html">structured input data</a> parameter on a {@link BusinessApplicationPlugin}
 * class that has {@link BusinessApplicationPlugin#perRequestInputData()}=false.</p>
 * 
 * <p>Request parameters are implemented as Java bean properties on the plug-in class.
 * The plug-in must implement a <code>setXXX</code> property method for each request parameter. The
 * request parameter name is the name of the Java bean property. The parameter type can
 * only use the supported <a href="../../dataTypes.html">data types</a>. The request parameters will be converted by
 * the CPF from the input data to the correct Java type.</p>
 * 
 * <p>Before execution of the plug-in the request methods will be invoked to set the request parameter values.</p>
 * 
 * <p>A <code>RequestParameter</code> method can also be marked as a {@link JobParameter} if the
 * parameter can be specified either at the job or request level.</p>
 * 
 * <p>The following example shows the use of the annotation.</p>
 * 
 * <figure><pre class="prettyprint language-java">private String algorithmName;
 
&#064;RequestParameter
public void setAlgorithmName(final String algorithmName) {
  this.algorithmName = algorithmName;
}</pre></figure>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface RequestParameter {
  /**
   * The description of the request parameter to display on the plug-in overview
   * page and as instructions on the create job forms.
   */
  String description() default "";

  /**
   * The url to a page that describes the parameter in greater detail than is possible on the
   * form. If specified the name of the parameter will be a hyper-link to this URL. 
   */
  String descriptionUrl() default "";

  /** The index (position) of the parameter in the input file form. */
  int index() default -1;

  /**
   * The maximum length of the parameter including the scale. This is ignored for
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
