package ca.bc.gov.open.cpf.plugin.impl;

import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.impl.security.MockSecurityService;

import com.revolsys.io.BaseCloseable;

public interface IBusinessApplicationPluginExecutor extends BaseCloseable {

  void addTestParameter(String name, Object value);

  @Override
  void close();

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts and returns
   * structured data.
   *
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param inputParameters The input parameters to the business application.
   * @return The result parameters from the business application.
   */
  Map<String, Object> execute(String businessApplicationName,
    Map<String, ? extends Object> inputParameters);

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts structured data
   * and returns opaque data.
   *
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param inputParameters The input parameters to the business application.
   * @param resultDataContentType The content type of the result data.
   * @param resultData The output stream that the plug-in can use to write the
   *          result data to.
   */
  void execute(String businessApplicationName, Map<String, ? extends Object> inputParameters,
    String resultDataContentType, OutputStream resultData);

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts opaque data and
   * returns structured data.
   *
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param jobParameters The global job parameters to the business application.
   * @param inputDataUrl The URL to the opaque input data.
   * @return The result parameters from the business application.
   */
  Map<String, Object> execute(String businessApplicationName,
    Map<String, ? extends Object> jobParameters, URL inputDataUrl);

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts and returns
   * opaque data.
   *
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param jobParameters The global job parameters to the business application.
   * @param inputDataUrl The URL to the opaque input data.
   * @param resultDataContentType The content type of the result data.
   * @param resultData The output stream that the plug-in can use to write the
   *          result data to.
   */
  void execute(String businessApplicationName, Map<String, ? extends Object> jobParameters,
    URL inputDataUrl, String resultDataContentType, OutputStream resultData);

  /**
   * Execute a {@link BusinessApplicationPlugin} which accepts and returns
   * structured data.
   *
   * @param businessApplicationName The name of the business application to
   *          execute.
   * @param inputParameters The input parameters to the business application.
   * @return The result parameters from the business application.
   */
  List<Map<String, Object>> executeList(String businessApplicationName,
    Map<String, Object> inputParameters);

  BusinessApplicationRegistry getBusinessApplicationRegistry();

  String getConsumerKey();

  MockSecurityService getSecurityService(String businessApplicationName);

  boolean hasResultsList(String businessApplicationName);

  void setConsumerKey(String consumerKey);

  void setTestModeEnabled(String businessApplicationName, boolean enabled);

  void setTestParameters(Map<String, Object> testParameters);

}
