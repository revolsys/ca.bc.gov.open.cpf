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

  /**
   * A URL to a web page providing additional documentation for the business
   * application. This is displayed as a link on the application description
   * page.
   */
  String descriptionUrl() default "";

  /**
   * A description of the plugin that is displayed on the business application
   * list page. Keep to a few sentences, use the descriptionUrl to link to a
   * page containing more information.
   */
  String description() default "";

  /**
   * The array of MIME media types of input data accepted by the plug-in. For
   * perRequestInputData=false omit this value to use all the supported
   * structured data MIME media types. For perRequestInputData=true set this to
   * the MIME media types of the input data supported (e.g. image/jpeg).
   */
  String[] inputDataContentTypes() default {};

  /** */
  String instantModePermission() default "denyAll";

  /**
   * The level of logging to include for requests processed by the plug-in. The
   * default ERROR will only include any errors, INFO will include additional
   * logging and DEBUG will include the most. Enabling INFO or DEBUG will
   * increase the time it takes to process a request so should not be enabled in
   * production unless an issue is being investigated.
   */
  String logLevel() default "ERROR";

  /**
   * The maximum number of concurrent execution groups that will be scheduled
   * for this business application. If an application has a limit to the number
   * of database connections it can use this value can be used to limit the
   * number of requests that will be executed by the workers at one time.
   */
  int maxConcurrentRequests() default 100;

  /** The maximum number of requests that a user can submit in a batch job. */
  int maxRequestsPerJob() default Integer.MAX_VALUE;

  /**
   * The name of the plug-in in (lower|Upper)CaseCamelNotation (e.g.
   * fibonacciSequence or FibonacciSequence) . Must be a valid Java identifier
   * (no spaces or special characters). This is used in the web service URL for
   * the business application and in the batch jobs in the database. The name
   * should not be changed between releases. If the name changes manual changes
   * will be required to the data in the database and any exteral applications
   * that use the plug-in.
   */
  String name() default "";

  /** */
  int numRequestsPerWorker() default 1;

  /** */
  boolean perRequestInputData() default false;

  /** */
  boolean perRequestResultData() default false;

  /**
   * The array of MIME media types of output data returned by the plug-in. For
   * perRequestOutputData=false omit this value to use all the supported
   * structured data MIME media types. For perRequestOutputData=true set this to
   * the MIME media types of the output data supported (e.g. image/jpeg).
   */
  String[] resultDataContentTypes() default {};

  /**
   * The display title displayed on the web site for the plug-in (e.g. Fibonacci
   * Sequence).
   */
  String title() default "";

  /** */
  String version() default "";
}
