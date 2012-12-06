package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface RequestParameter {
  String description() default "";

  String descriptionUrl() default "";

  /** The index of the attribute in the input file/screen. */
  int index() default -1;

  int length() default -1;

  int scale() default -1;
}
