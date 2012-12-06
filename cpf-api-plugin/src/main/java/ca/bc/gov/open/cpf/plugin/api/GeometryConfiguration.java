package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The GeometryConfiguration annotation can be used on a class or on a get or
 * set method to indicate the configuration of the required coordinate system,
 * precision model, number of coordinate axis and if the geometries should be
 * validated.
 * </p>
 * <p>
 * The CPF uses this information to convert the input and result geometries to
 * the correct coordinate system or precision model.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {
  ElementType.METHOD, ElementType.TYPE
})
public @interface GeometryConfiguration {
  /**
   * The number or axis used in the geometry. For example a 2D geometry has x, y
   * coordinates, so the number of axis is 2. A 3D geometry has x, y, z so the
   * number of axis is 3. Currently only numAxis of 2 and 3 are supported.
   */
  public int numAxis() default 2;

  /**
   * The flag indicating that this bean property is the primary geometry. This
   * is required as some spatial file formats only support a single real
   * geometry field. Non-primary geometries will be written as an E-WKTR string
   * field. Only one geometry set method and one geometry get method can be
   * marked as primary (the input primary geometry parameter can be different
   * from the result primary geometry attribute.
   */
  public boolean primaryGeometry() default true;

  /**
   * The scale factor to apply the x, y coordinates. The scale factor is 1 /
   * minimum unit. For example if the minimum unit was 1mm (0.001) the scale
   * factor is 1000 (1 / 0.001). The default value 0 indicates a floating
   * precision model where the coordinates are not rounded.
   */
  public double scaleFactorXy() default 0;

  /**
   * The scale factor to apply the z coordinates. The scale factor is 1 /
   * minimum unit. For example if the minimum unit was 1mm (0.001) the scale
   * factor is 1000 (1 / 0.001). The default value 0 indicates a floating
   * precision model where the coordinates are not rounded.
   * 
   * @see #scaleFactorXy()
   */
  public double scaleFactorZ() default 0;

  /**
   * The srid of the coordinate system the geometry should be converted to. If
   * this attribute is ommited or 0 the coordinate system of the source geometry
   * will not be changed. Otherwise it will be projected to the requested
   * coordinate system.
   */
  public int srid() default 0;

  /**
   * Flag indicating if the geometry should be validated using the OGC isValid
   * predicate.
   */
  public boolean validate() default false;
}
