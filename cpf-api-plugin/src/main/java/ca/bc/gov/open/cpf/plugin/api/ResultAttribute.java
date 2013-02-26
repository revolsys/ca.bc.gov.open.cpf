package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>The ResultAttribute annotation indicates that a getXXX method is a structured
 * result attribute on a {@link BusinessApplicationPlugin} or {@link ResultList}
 * class. After execution of the plug-in the result attribute methods will be
 * invoked to get the Plug-ins that do not accept per request result data
 * perRequestResultData=false can return structured result attributes. Result
 * attributes are implemented as Java bean properties on the class. The plug-in
 * will implement a get or is property method for each result attribute. The
 * result attribute name is the name of the Java bean property. Parameters can
 * only use the following data types byte, short, int, long, float, double,
 * boolean, String, URL, JTS Point, LineString, Polygon, MultiPoint,
 * MultiLineString and MultiPolygon.. The result attributes will be converted by
 * the CPF to the correct representation in the output format or a string
 * representation of the value before being returned to the user. The
 * ca.bc.gov.open.cpf.plugin.api.ResultAttribute annotation marks a get or is
 * property method as being a result attribute. Result attribute annotations
 * also support the description, index, length and scale attributes. The
 * description is used as Input help text on the business application
 * description page. If the description is omitted and there was a job or
 * request parameter of the same name then the description will be taken from
 * the parameter. The index defines the order of the attributes in the result
 * file. The length is the size of the field (number of characters or digits).
 * The scale is the number of decimal places.</p>
 * 
 * <pre class="prettyprint"><code class="language-java">
 * &#064;BusinessApplicationPlugin
 * public class Square {
 *   private int square;
 * 
 *   &#064;ResultAttribute(index = 2, description = &quot;The square of a value&quot;)
 *   public int getSquare() {
 *     return square;
 *   }
 * }
 * </code></pre>
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
