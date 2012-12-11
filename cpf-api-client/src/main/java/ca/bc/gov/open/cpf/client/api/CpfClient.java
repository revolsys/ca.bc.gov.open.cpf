package ca.bc.gov.open.cpf.client.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import ca.bc.gov.open.cpf.client.httpclient.HttpMultipartPost;
import ca.bc.gov.open.cpf.client.httpclient.OAuthHttpClient;
import ca.bc.gov.open.cpf.client.httpclient.OAuthHttpClientPool;

import com.revolsys.io.IoFactoryRegistry;
import com.revolsys.io.ListReader;
import com.revolsys.io.MapWriter;
import com.revolsys.io.MapWriterFactory;
import com.revolsys.io.Reader;
import com.revolsys.spring.ByteArrayResource;
import com.revolsys.util.ObjectProcessor;

/**
 * The CpfClient Java Client provides an easy to use application programming
 * interface to the Cloud Processing Framework Web Services. The Java Client
 * simplifies the task of developing Java applications to, amongst other things,
 * submit batch jobs, process the associated batch job results, close jobs, and
 * list jobs for a user.
 */
public class CpfClient {
  /** OAuth consumer key */
  private String consumerKey = "";

  /** HttpClient using OAuth credentials */
  private OAuthHttpClientPool httpClientPool;

  /**
   * The CloudProcessingFrameworkClient constructor.
   * 
   * @param url The full URL of the Batch Job Web Services, including the
   *          domain, port number and path e.g. http://localhost:8080/ws/
   * @param consumerKey The oAuth Consumer Key (web services user name) to be
   *          used.
   * @param consumerSecret The oAuth Consumer Secret (password) authorising the
   *          consumerKey.
   */
  public CpfClient(final String url, final String consumerKey,
    final String consumerSecret) {
    this.httpClientPool = new OAuthHttpClientPool(url, consumerKey,
      consumerSecret, 1);
    this.consumerKey = consumerKey;
  }

  /**
   * Add the job parameters to the request.
   * 
   * @param request The request.
   * @param jobParameters The parameters.s
   */
  private void addJobParameters(final HttpMultipartPost request,
    final Map<String, Object> jobParameters) {
    if (jobParameters != null && !jobParameters.isEmpty()) {
      for (final String parameterName : jobParameters.keySet()) {
        final Object value = jobParameters.get(parameterName);
        request.addParameter(parameterName, value);
      }
    }
  }

  /**
   * Cleanup and shutdown the HTTP client and associated connections.
   */
  @PreDestroy
  public void closeConnection() {
    httpClientPool.close();
    this.httpClientPool = null;
    this.consumerKey = null;
  }

  /**
   * Close the Batch Job.
   * 
   * @param jobUrl The URL of the job to be closed.
   */
  public void closeJob(final String jobUrl) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      httpClient.deleteUrl(jobUrl);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to close job " + jobUrl, e);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Create a new cloud job on the CPF server for a business application that
   * accepts opaque input data. This method can be used for multiple requests in
   * the same job. The content of the opaque request data can be specified using
   * collection a spring framework {@link Resource} objects.
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @param jobParameters The global job parameters.
   * @param requests The collection of resources for the requests.
   * @param inputDataContentType The MIME content type used for all the
   *          requests.
   * @param resultContentType The MIME content type to return the result data
   *          using.
   * @return The cloud job id (URL) of the created job.
   */
  public String createJobWithOpaqueResourceMultipleRequests(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters,
    final Collection<Resource> requests, final String inputDataContentType,
    final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/multiple/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, jobParameters);

      request.addParameter("numRequests", requests.size());
      request.addParameter("resultDataContentType", resultContentType);
      for (final Resource inputData : requests) {
        request.addParameter("inputData", inputData, inputDataContentType);
        request.addParameter("inputDataContentType", inputDataContentType);
      }
      request.addParameter("media", "application/json");
      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Create a new cloud job on the CPF server for a business application that
   * accepts opaque input data. This method can be used for a single request in
   * the same job. The content of the opaque request data can be specified using
   * a spring framework {@link Resoucrce} object.
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @param jobParameters The global job parameters.
   * @param request The resource for the requests.
   * @param inputDataContentType The MIME content type used for all the
   *          requests.
   * @param resultContentType The MIME content type to return the result data
   *          using.
   * @return The cloud job id (URL) of the created job.
   */
  public String createJobWithOpaqueResourceSingleRequest(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters, final Resource request,
    final String inputDataContentType, final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/single/");

      final HttpMultipartPost httpRequest = new HttpMultipartPost(httpClient,
        url);
      addJobParameters(httpRequest, jobParameters);

      httpRequest.addParameter("inputData", request, inputDataContentType);
      httpRequest.addParameter("inputDataContentType", inputDataContentType);
      httpRequest.addParameter("resultDataContentType", resultContentType);

      httpRequest.addParameter("media", "application/json");
      return httpClient.postResourceRedirect(httpRequest);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Create a new cloud job on the CPF server for a business application that
   * accepts opaque input data. This method can be used for multiple requests in
   * the same job. The content of the opaque request data can be specified using
   * collection of URLs.
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @param jobParameters The global job parameters.
   * @param requestUrls The collection of URLs for the requests.
   * @param inputDataContentType The MIME content type used for all the
   *          requests.
   * @param resultContentType The MIME content type to return the result data
   *          using.
   * @return The cloud job id (URL) of the created job.
   */
  public String createJobWithOpaqueUrlMultipleRequests(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters,
    final Collection<String> requestUrls, final String inputDataContentType,
    final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/multiple/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, jobParameters);

      request.addParameter("numRequests", requestUrls.size());
      request.addParameter("resultDataContentType", resultContentType);
      for (final String inputDataUrl : requestUrls) {
        request.addParameter("inputDataUrl", inputDataUrl);
        request.addParameter("inputDataContentType", inputDataContentType);
      }
      request.addParameter("media", "application/json");
      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Create a new cloud job on the CPF server for a business application that
   * accepts opaque input data. This method can be used for a single request in
   * the same job. The content of the opaque request data can be specified using
   * URL to the server to download the data from.
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @param jobParameters The global job parameters.
   * @param requests The collection of resources for the requests.
   * @param inputDataContentType The MIME content type used for all the
   *          requests.
   * @param resultContentType The MIME content type to return the result data
   *          using.
   * @return The cloud job id (URL) of the created job.
   */
  public String createJobWithOpaqueUrlSingleRequest(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters, final String inputDataUrl,
    final String inputDataContentType, final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/single/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, jobParameters);

      request.addParameter("inputDataUrl", inputDataUrl);
      request.addParameter("inputDataContentType", inputDataContentType);
      request.addParameter("resultDataContentType", resultContentType);

      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  public String createJobWithStructuredMultipleRequests(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters, final int numRequests,
    final String inputDataType, final Resource inputData,
    final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/multiple/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, jobParameters);

      request.addParameter("inputData", inputData, inputDataType);
      request.addParameter("inputDataContentType", inputDataType);
      request.addParameter("numRequests", numRequests);
      request.addParameter("resultDataContentType", resultContentType);
      request.addParameter("media", "application/json");

      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Submit a new Batch Job request using a Url to specify the location of the
   * Batch Job Request data.
   * 
   * @param businessApplicationName The name of the web services business
   *          application.
   * @param businessApplicationVersion The version of the web services business
   *          application.
   * @param numRequests The number of business application requests in the batch
   *          job.
   * @param inputDataUrl The full URL of the input data file of Batch Job
   *          Requests.
   * @param inputDataContentType MIME type of the input batch job request data.
   * @param resultContentType MIME type of the result data.
   * @param jobParameters A map of additional parameters specific to the
   *          requested Business Application associated with the batch job.
   * @param waitForResponse Set to true to have the method return only when the
   *          batch job has completed processing, or there is a problem
   *          processing the request.
   * @return A MapService of job status field name and value pairs.
   */
  public String createJobWithStructuredMultipleRequests(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters, final int numRequests,
    final String inputDataUrl, final String inputDataContentType,
    final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/multiple/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, jobParameters);

      request.addParameter("numRequests", numRequests);
      request.addParameter("inputDataUrl", inputDataUrl);
      request.addParameter("inputDataContentType", inputDataContentType);
      request.addParameter("resultContentType", resultContentType);

      request.addParameter("media", "application/json");
      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Submit a new Batch Job request, specifying a List of Maps as the source of
   * the Batch Job Request data.
   * 
   * @param businessApplicationName The name of the web services business
   *          application.
   * @param businessApplicationVersion The version of the web services business
   *          application.
   * @param jobParameters A map of additional parameters specific to the
   *          requested Business Application.
   * @param requests A list of data Maps of the requests.
   * @param resultContentType MIME type of the result data.
   * @param numRequests The number of business application requests in the batch
   *          job.
   * @param waitForResponse Set to true to have the method return only when the
   *          batch job has completed processing, or there is a problem
   *          processing the request. * @param waitTimeout The maximum time
   *          (milliseconds) to wait, after which the status of the incomplete
   *          job will be returned, -1 for indefinite.
   * @return A MapService of the job status field name and value pairs.
   */
  public String createJobWithStructuredMultipleRequests(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters,
    final List<Map<String, Object>> requests, final String resultContentType) {
    final String inputDataType = "application/json";
    final int numRequests = requests.size();

    final MapWriterFactory factory = IoFactoryRegistry.getInstance()
      .getFactoryByMediaType(MapWriterFactory.class, inputDataType);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final MapWriter mapWriter = factory.getWriter(out);

    for (final Map<String, Object> requestRecord : requests) {
      mapWriter.write(requestRecord);
    }
    mapWriter.close();
    final Resource inputData = new ByteArrayResource("data.json",
      out.toByteArray());

    return createJobWithStructuredMultipleRequests(businessApplicationName,
      businessApplicationVersion, jobParameters, numRequests, inputDataType,
      inputData, resultContentType);
  }

  /**
   * Create a new cloud job on the CPF server for a business application that
   * accepts structured input data. This method can be used for multiple
   * requests in the same job. The content of the structured request data can be
   * specified using a URL to the input data file.
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @param jobParameters The global job parameters.
   * @param inputDataUrl The URL containing the request input data.
   * @param inputDataContentType The MIME content type used for all the
   *          requests.
   * @param resultContentType The MIME content type to return the result data
   *          using.
   * @return The cloud job id (URL) of the created job.
   */
  public String createJobWithStructuredResourceMultipleRequests(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters, final int numRequests,
    final Resource inputData, final String inputDataContentType,
    final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/multiple/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, jobParameters);

      request.addParameter("numRequests", numRequests);
      request.addParameter("resultDataContentType", resultContentType);
      request.addParameter("inputData", inputData, inputDataContentType);
      request.addParameter("inputDataContentType", inputDataContentType);
      request.addParameter("media", "application/json");
      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  public String createJobWithStructuredSingleRequest(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> parameters, final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/single/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, parameters);

      request.addParameter("resultDataContentType", resultContentType);

      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Create a new cloud job on the CPF server for a business application that
   * accepts structured input data. This method can be used for multiple
   * requests in the same job. The content of the structured request data can be
   * specified using a URL to the input data file.
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @param jobParameters The global job parameters.
   * @param inputDataUrl The URL containing the request input data.
   * @param inputDataContentType The MIME content type used for all the
   *          requests.
   * @param resultContentType The MIME content type to return the result data
   *          using.
   * @return The cloud job id (URL) of the created job.
   */
  public String createJobWithStructuredUrlMultipleRequests(
    final String businessApplicationName,
    final String businessApplicationVersion,
    final Map<String, Object> jobParameters, final int numRequests,
    final String inputDataUrl, final String inputDataContentType,
    final String resultContentType) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/multiple/");

      final HttpMultipartPost request = new HttpMultipartPost(httpClient, url);
      addJobParameters(request, jobParameters);

      request.addParameter("numRequests", numRequests);
      request.addParameter("resultDataContentType", resultContentType);
      request.addParameter("inputDataUrl", inputDataUrl);
      request.addParameter("inputDataContentType", inputDataContentType);
      request.addParameter("media", "application/json");
      return httpClient.postResourceRedirect(request);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * <p>
   * Get the list of business application names available to the user on the CPF
   * server.
   * </p>
   * <p>
   * The following code fragment shows an example of using the API.
   * </p>
   * 
   * <pre style="border: 1px solid #666666; padding: 2px;">
   * String baseUrl = &quot;http://apps.gov.bc.ca/pub/cpf/ws&quot;;
   * String consumerKey = &quot;...&quot;;
   * String consumerSecret = &quot;...&quot;;
   * CpfClient client = new CpfClient(baseUrl, consumerKey, consumerSecret);
   * 
   * List&lt;String&gt; applicationNames = client.getBusinessApplicationNames();
   * System.out.println(applicationNames);
   * </pre>
   * 
   * @return The list of business application names.
   */
  public List<String> getBusinessApplicationNames() {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    final String url = httpClient.getUrl("/apps/");
    try {
      final Map<String, Object> result = httpClient.getJsonResource(url);
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> items = (List<Map<String, Object>>)result.get("resources");
      if (items != null && !items.isEmpty()) {
        final List<String> businessApplicationNames = new ArrayList<String>();
        for (final Map<String, Object> item : items) {
          final String businessApplicationName = (String)item.get("businessApplicationName");
          if (StringUtils.hasText(businessApplicationName)) {
            businessApplicationNames.add(businessApplicationName);
          }
        }
        return businessApplicationNames;
      }
      return Collections.emptyList();
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Get the specification of the business application. The specification
   * includes description of the job paramerters, input data type and
   * parameters, result data type and attributes.
   * 
   * @param businessApplicationName The name of the business application.
   * @param businessApplicationVersion The version of the business application.
   * @return The map containing the business application specification.
   */
  public Map<String, Object> getBusinessApplicationSpecification(
    final String businessApplicationName,
    final String businessApplicationVersion) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/" + businessApplicationVersion + "/specification/");
      final Map<String, Object> result = httpClient.getJsonResource(url);
      return result;
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Get the list of version numbers supported by the business application.
   * 
   * @param businessApplicationName The business application name.
   * @return The list of version numbers.
   */
  public List<String> getBusinessApplicationVersions(
    final String businessApplicationName) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl("/apps/" + businessApplicationName
        + "/");
      final Map<String, Object> result = httpClient.getJsonResource(url);
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> items = (List<Map<String, Object>>)result.get("resources");
      if (items != null && !items.isEmpty()) {
        final List<String> businessApplicationVersions = new ArrayList<String>();
        for (final Map<String, Object> item : items) {
          final String businessApplicationVersion = (String)item.get("businessApplicationVersion");
          if (StringUtils.hasText(businessApplicationVersion)) {
            businessApplicationVersions.add(businessApplicationVersion);
          }
        }
        return businessApplicationVersions;
      }
      return Collections.emptyList();
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Get the error records for a job. The results are returned using a
   * {@link Reader} object that can be used to interate over the list of error
   * records. Each error record is a map with fields sequenceNumber (to map back
   * to the nput request), errorCode and errorMessage.
   * 
   * @param jobIdUrl The job id URL.
   * @param maxWait The maximum number of milliseconds to wait for the job to be
   *          completed.
   * @return The reader maps containing the result fields.
   */
  public Reader<Map<String, Object>> getJobErrorResults(final String jobIdUrl,
    final long maxWait) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      for (final Map<String, Object> resultFile : getJobResultFileList(
        jobIdUrl, maxWait)) {
        final String resultType = (String)resultFile.get("batchJobResultType");
        if ("errorResultData".equals(resultType)) {
          final String resultUrl = (String)resultFile.get("resourceUri");
          return httpClient.getMapReader("error", resultUrl);
        }
      }
      return new ListReader<Map<String, Object>>();
    } finally {
      httpClientPool.releaseClient(httpClient);
    }

  }

  /**
   * Get the list if job id Urls for a user.
   * 
   * @param path The path to get the job id URLs from.
   * @return The list of job id URLs.
   */
  private List<String> getJobIdUrls(final String path) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final String url = httpClient.getUrl(path);
      final Map<String, Object> jobs = httpClient.getJsonResource(url);
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> resources = (List<Map<String, Object>>)jobs.get("resources");
      final List<String> jobIdUrls = new ArrayList<String>();
      if (resources != null) {
        for (final Map<String, Object> jobResource : resources) {
          final String jobIdUrl = (String)jobResource.get("resourceUri");
          if (jobIdUrl != null) {
            jobIdUrls.add(jobIdUrl);
          }
        }
      }
      return jobIdUrls;
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Get the list of result files for the job
   * 
   * @param jobIdUrl The job id URL.
   * @param maxWait The maximum number of milleseconds to wait for the job to be
   *          completed.
   * @return Ths list of maps that describe each of the result files.
   */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getJobResultFileList(final String jobIdUrl,
    final long maxWait) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      if (isJobCompleted(jobIdUrl, maxWait)) {
        final String resultsUrl = jobIdUrl + "results/";
        final Map<String, Object> jobResults = httpClient.getJsonResource(resultsUrl);
        return (List<Map<String, Object>>)jobResults.get("resources");
      } else {
        throw new IllegalStateException("Job results have not yet been created");
      }
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  /**
   * Get the status of the job including the URL to the results file list if it
   * has been defined.
   * 
   * <pre>
   * The following status fields are returned with the job
   * status: 
   * 
   * &lt;b&gt;id&lt;/b&gt;            - The id of this job, in the form of URL to the job. 
   * &lt;b&gt;userId&lt;/b&gt;        - The oAuth Consumer Key of the user who submitted 
   *                 the job.
   * &lt;b&gt;businessApplicationName&lt;/b&gt; - the business application name associated 
   *                 with this job. 
   * &lt;b&gt;businessApplicationVersion&lt;/b&gt; - the version of the business application 
   *                 associated with this job.
   * &lt;b&gt;jobStatus&lt;/b&gt;     - The current processing status of this job.  A 
   *                 completed job will have a status of &quot;resultsCreated&quot;, while
   *                 a job that has yet to be queued for processing will have a 
   *                 status of &quot;submitted&quot;.
   * &lt;b&gt;millisecondsUntilNextCheck&lt;/b&gt; - The number of remaining milliseconds 
   *                 estimated by the web services to complete processing of 
   *                 this job. 0 if the job processing has completed. 
   * &lt;b&gt;numSubmittedRequests&lt;/b&gt; - Initially this will be set to the value
   *                 passed in with the job.  Once the input request data has 
   *                 been validated, it will be updated to the actual number of
   *                 requests in the Batch Job. 
   * &lt;b&gt;numCompletedRequests&lt;/b&gt; - The current number of Batch Job Requests 
   *                 successfully processed and returned by the Business 
   *                 Application.
   * &lt;b&gt;numFailedRequests&lt;/b&gt; - The current number of Batch Job Requests that 
   *                 have failed processing by the Business Application.  If 
   *                 any input request data fails validation, then the batch job
   *                 is not processed at all by the Business Application, and
   *                 the number of failed requests will be set to the number of
   *                 submitted requests.
   * &lt;b&gt;resultDataContentType&lt;/b&gt; - The desired MIME type of the generated
   *                 result data files.  Supported MIME types include: text/csv,
   *                 application/json, application/xhtml+xml, text/xml, 
   *                 application/vnd.google-earth.kml+xml, 
   *                 application/vnd.sun.wadl+xml and text/uri-list.  Each
   *                 Business Application will specify which of these MIME types
   *                 are supported.
   * &lt;b&gt;resultsUrl&lt;/b&gt;    - If the job has completed successfully, and result data
   *                 files have been generated, then this URL will list the URL
   *                 and details of each result file associated with this job.
   *                 This field will not be present if the job processing has
   *                 not completed.
   * </pre>
   * 
   * @param jobUrl WS URL of the Batch Job.
   * @return jobStatusMap A map of the job status field name and value pairs.
   */
  public Map<String, Object> getJobStatus(final String jobUrl) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final Map<String, Object> jobStatusMap = httpClient.getJsonResource(jobUrl);
      return jobStatusMap;
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }

  public Reader<Map<String, Object>> getJobStructuredResults(
    final String jobIdUrl, final long maxWait) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      for (final Map<String, Object> resultFile : getJobResultFileList(
        jobIdUrl, maxWait)) {
        final String resultType = (String)resultFile.get("batchJobResultType");
        if ("structuredResultData".equals(resultType)) {
          final String resultUrl = (String)resultFile.get("resourceUri");
          return httpClient.getMapReader(resultUrl);
        }
      }
      throw new IllegalStateException("Cannot find structured result file for "
        + jobIdUrl);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }

  }

  /**
   * Get the list of all job id URLs the user created.
   * 
   * @return The list of job id URLs.
   */
  public List<String> getUserJobIdUrls() {
    final String path = "/users/" + consumerKey + "/jobs/";
    return getJobIdUrls(path);
  }

  /**
   * Get the list of all job id URLs the user created for the business
   * application.
   * 
   * @param businessApplicationName The name of the business application.
   * @return The list of job id URLs.
   */
  public List<String> getUserJobIdUrls(final String businessApplicationName) {
    final String path = "/users/" + consumerKey + "/apps/"
      + businessApplicationName + "/jobs/";
    return getJobIdUrls(path);
  }

  /**
   * Check to see if the job has been completed. If the job has not been
   * completed within the maxWait number of milliseconds false will be returned.
   * 
   * @param jobIdUrl The job id URL.
   * @param maxWait The maximum number of milliseconds to wait.
   * @return True if the job was completed, false otherwise.
   */
  public boolean isJobCompleted(final String jobIdUrl, final long maxWait) {
    if (maxWait > 0) {
      final long startTime = System.currentTimeMillis();
      final long maxEnd = startTime + maxWait;
      long currentTime = startTime;
      while (currentTime < maxEnd) {
        final Map<String, Object> jobStatusMap = getJobStatus(jobIdUrl);
        final String jobStatus = (String)jobStatusMap.get("jobStatus");
        if ("resultsCreated".equals(jobStatus)) {
          return true;
        }
        long sleepTime = ((Number)jobStatusMap.get("secondsToWaitForStatusCheck")).intValue();
        sleepTime = Math.min(sleepTime, maxEnd - System.currentTimeMillis());
        if (sleepTime > 0) {
          synchronized (this) {
            try {
              wait(sleepTime);
            } catch (final InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        }
        currentTime = System.currentTimeMillis();
      }
    }
    final Map<String, Object> jobStatusMap = getJobStatus(jobIdUrl);
    final String jobStatus = (String)jobStatusMap.get("jobStatus");
    return "resultsCreated".equals(jobStatus);
  }

  /**
   * Download the result file for the job and process the returned input stream
   * using the result processor. This method ensures that the input stream is
   * closed correctly after being processed.
   * 
   * @param jobResultUrl The URL to the job result file.
   * @param resultProcessor The processor to process the input stream returned
   *          from the web service.
   */
  public void processResultFile(final String jobResultUrl,
    final ObjectProcessor<InputStream> resultProcessor) {
    OAuthHttpClient httpClient = httpClientPool.getClient();
    try {
      final HttpResponse response = httpClient.getResource(jobResultUrl);
      final HttpEntity entity = response.getEntity();
      try {
        final StatusLine statusLine = response.getStatusLine();
        final int httpStatusCode = statusLine.getStatusCode();
        final String httpStatusMessage = statusLine.getReasonPhrase();

        if (httpStatusCode >= HttpStatus.SC_BAD_REQUEST) {
          throw new RuntimeException(httpStatusCode + " " + httpStatusMessage);
        }
        final InputStream in = entity.getContent();
        resultProcessor.process(in);
      } finally {
        EntityUtils.consume(entity);
      }

    } catch (final IOException e) {
      throw new RuntimeException("Unable to get file " + jobResultUrl, e);
    } finally {
      httpClientPool.releaseClient(httpClient);
    }
  }
}
