package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface BusinessApplicationPlugin {
  String batchModePermission() default "permitAll";

  String[] compatibleVersions() default {};

  String descriptionUrl() default "";

  String description() default "";

  String[] inputDataContentTypes() default {};

  String instantModePermission() default "denyAll";

  String logLevel() default "ERROR";

  int maxConcurrentRequests() default 100;

  int maxRequestsPerJob() default Integer.MAX_VALUE;

  String name() default "";

  int numRequestsPerWorker() default 1;

  boolean perRequestInputData() default false;

  boolean perRequestResultData() default false;

  String[] resultDataContentTypes() default {};

  String title() default "";

  String version() default "";
}
