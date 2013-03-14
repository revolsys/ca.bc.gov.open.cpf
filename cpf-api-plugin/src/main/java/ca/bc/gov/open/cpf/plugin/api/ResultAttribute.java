package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>The ResultAttribute annotation indicates that a getXXX method is a <a href="../../structuredData.html">structured
 * result data</a> attribute on a {@link BusinessApplicationPlugin} or {@link ResultList}
 * class.</p>
 * 
 * <p>Result attributes are implemented as Java bean properties on the plug-in or {@link ResultList} object.
 * The plug-in must implement a get or is property method for each result attribute. The
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
   * page and as field instructions on the create job forms. If the description
   * is not specified and there is a {@link RequestParameter} with the same name
   * that has a description then that will be used. This simplifies coding as it
   * removes the need to duplicate the description.
   */
  String description() default "";

  /** The index of the attribute in the output file. */
  int index() default -1;

  /**
   * The maximum length of the field including the scale. This is ignored for
   * fixed size data types such as boolean, byte, short, int, long, float and
   * double. The value -1 indicates no defined limit to the scale.
   */
  int length() default -1;

  /**
   * The number of decimal places for fixed precision numeric types.
   */
  int scale() default -1;
}
