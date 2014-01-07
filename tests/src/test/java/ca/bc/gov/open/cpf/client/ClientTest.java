package ca.bc.gov.open.cpf.client;

import java.util.List;
import java.util.Map;

import org.junit.Assert;

import ca.bc.gov.open.cpf.client.api.CpfClient;

public class ClientTest {

  public static void main(final String[] args) {
    new ClientTest().run();
  }

  private final String[] BUSINESS_APPLICATION_NAMES = {
    "MapTileByTileId"
  };

  private final CpfClient client;

  private final String url = "http://localhost/pub/cpf";

  public ClientTest() {
    client = new CpfClient(url, "cpftest", "cpftest");
  }

  public void run() {
    // testCloseUserJobs();

    testGetBusinessApplicationNames();
    // testGetBusinessApplicationVersions();
    testGetBusinessApplicationSpecification();
    //
    // testMapTileByIdSingle();
    // testMapTileByIdMultiple();
    // testMapTileByIdResourceMultiple();
    //
    // testDigestUrlSingle();
    // testDigestResourceSingle();
    //
    // testDigestUrlMultiple();
    // testDigestResourceMultiple();
    //
    // testGetUserJobIdUrls();
    // testGetUserAppsJobIdUrls();
    //
    // testMapTileByIdSingleError();
    // testMapTileByIdSingleDownloadResult();

    // testCloseUserJobs();

  }

  // private void testCloseUserJobs() {
  // final List<String> jobIdUrls = client.getUserJobIdUrls();
  // for (final String jobIdUrl : jobIdUrls) {
  // client.closeJob(jobIdUrl);
  // }
  // Assert.assertEquals(0, client.getUserJobIdUrls().size());
  // }

  // private void testDigestResourceMultiple() {
  // final Map<String, Object> jobParameters = new HashMap<String, Object>();
  // jobParameters.put("algorithmName", "SHA");
  // final List<Resource> resources = new ArrayList<Resource>();
  // resources.add(new ByteArrayResource("t", "This is a test".getBytes()));
  // final String jobIdUrl =
  // client.createJobWithOpaqueResourceRequests("digest", "1.0.0",
  // jobParameters, inputDataContentType, resultContentType, requests)
  //
  //
  // "digest", "1.0.0", jobParameters, resources, "text/html",
  // "application/json");
  //
  // final Reader<Map<String, Object>> results = client.getJobStructuredResults(
  // jobIdUrl, 5000);
  // final List<Map<String, Object>> resultList = results.read();
  // Assert.assertEquals("Num Results", resultList.size(), 1);
  // final Map<String, Object> result = resultList.get(0);
  // Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
  // .toString(), "1");
  // Assert.assertNotNull("digest", result.get("digest"));
  // }
  //
  // private void testDigestResourceSingle() {
  // final Map<String, Object> jobParameters = new HashMap<String, Object>();
  // jobParameters.put("algorithmName", "SHA");
  // final String jobIdUrl = client.createJobWithOpaqueResourceSingleRequest(
  // "digest", "1.0.0", jobParameters, new ByteArrayResource("t",
  // "This is a test".getBytes()), "text/plain", "application/json");
  //
  // final Reader<Map<String, Object>> results = client.getJobStructuredResults(
  // jobIdUrl, 10000);
  // final List<Map<String, Object>> resultList = results.read();
  // Assert.assertEquals("Num Results", resultList.size(), 1);
  // final Map<String, Object> result = resultList.get(0);
  // Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
  // .toString(), "1");
  // Assert.assertNotNull("digest", result.get("digest"));
  // }
  //
  // private void testDigestUrlMultiple() {
  // final Map<String, Object> jobParameters = new HashMap<String, Object>();
  // jobParameters.put("algorithmName", "SHA");
  // final List<String> urls = new ArrayList<String>();
  // urls.add(url + "/");
  // final String jobIdUrl = client.createJobWithOpaqueUrlMultipleRequests(
  // "digest", "1.0.0", jobParameters, urls, "text/html", "application/json");
  //
  // final Reader<Map<String, Object>> results = client.getJobStructuredResults(
  // jobIdUrl, 5000);
  // final List<Map<String, Object>> resultList = results.read();
  // Assert.assertEquals("Num Results", resultList.size(), 1);
  // final Map<String, Object> result = resultList.get(0);
  // Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
  // .toString(), "1");
  // Assert.assertNotNull("digest", result.get("digest"));
  // }
  //
  // private void testDigestUrlSingle() {
  // final Map<String, Object> jobParameters = new HashMap<String, Object>();
  // jobParameters.put("algorithmName", "SHA");
  // final String jobIdUrl = client.createJobWithOpaqueUrlSingleRequest(
  // "digest", "1.0.0", jobParameters, url + "/", "text/html",
  // "application/json");
  //
  // final Reader<Map<String, Object>> results = client.getJobStructuredResults(
  // jobIdUrl, 5000);
  // final List<Map<String, Object>> resultList = results.read();
  // Assert.assertEquals("Num Results", resultList.size(), 1);
  // final Map<String, Object> result = resultList.get(0);
  // Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
  // .toString(), "1");
  // Assert.assertNotNull("digest", result.get("digest"));
  // }

  private void testGetBusinessApplicationNames() {
    final List<String> businessApplicationNames = client.getBusinessApplicationNames();
    for (final String businessApplicationName : BUSINESS_APPLICATION_NAMES) {
      final boolean contains = businessApplicationNames.contains(businessApplicationName);
      Assert.assertTrue("Business application !contains("
        + businessApplicationName + "')", contains);
    }
  }

  private void testGetBusinessApplicationSpecification() {
    final Map<String, Object> specification = client.getBusinessApplicationSingleSpecification("MapTileByTileId");
    Assert.assertEquals("resourceUri", specification.get("resourceUri"), url
      + "/apps/MapTileByTileId/1.0.0/specification/");
    Assert.assertEquals("businessApplicationName",
      specification.get("businessApplicationName"), "MapTileByTileId");
    Assert.assertEquals("businessApplicationVersion",
      specification.get("businessApplicationVersion"), "1.0.0");
  }

  // private void testGetUserAppsJobIdUrls() {
  // final Map<String, Object> request = new HashMap<String, Object>();
  // request.put("mapGridName", "BCGS 1:20 000");
  // request.put("mapTileId", "92j016");
  // final String jobIdUrl = client.createJobWithStructuredSingleRequest(
  // "MapTileByTileId", "1.0.0", request, "application/json");
  //
  // final List<String> jobIdUrls = client.getUserJobIdUrls("MapTileByTileId");
  // final boolean contains = jobIdUrls.contains(jobIdUrl);
  // Assert.assertTrue("UserAppJobs contains " + jobIdUrl, contains);
  // }
  //
  // private void testGetUserJobIdUrls() {
  // final Map<String, Object> request = new HashMap<String, Object>();
  // request.put("mapGridName", "BCGS 1:20 000");
  // request.put("mapTileId", "92j016");
  // final String jobIdUrl = client.createJobWithStructuredSingleRequest(
  // "MapTileByTileId", "1.0.0", request, "application/json");
  //
  // final List<String> jobIdUrls = client.getUserJobIdUrls();
  // Assert.assertTrue("UserJobs contains " + jobIdUrl,
  // jobIdUrls.contains(jobIdUrl));
  // }
  //
  // private void testMapTileByIdMultiple() {
  // final Map<String, Object> jobParameters = new LinkedHashMap<String,
  // Object>();
  // jobParameters.put("mapGridName", "BCGS 1:20 000");
  // final List<Map<String, Object>> requests = new ArrayList<Map<String,
  // Object>>();
  // final Map<String, Object> requestParameters = new HashMap<String,
  // Object>();
  // requestParameters.put("mapTileId", "92j016");
  // requests.add(requestParameters);
  // final String jobIdUrl = client.createJobWithStructuredMultipleRequests(
  // "MapTileByTileId", "1.0.0", jobParameters, requests, "application/json");
  // final Reader<Map<String, Object>> results = client.getJobStructuredResults(
  // jobIdUrl, 5000);
  // final List<Map<String, Object>> resultList = results.read();
  // Assert.assertEquals("Num Results", resultList.size(), 1);
  // final Map<String, Object> result = resultList.get(0);
  // Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
  // .toString(), "1");
  // Assert.assertEquals("mapGridName", result.get("mapGridName"),
  // "BCGS 1:20 000");
  // Assert.assertEquals("mapTileId", result.get("mapTileId"), "92J.016");
  // }
  //
  // private void testMapTileByIdResourceMultiple() {
  // final Map<String, Object> jobParameters = new LinkedHashMap<String,
  // Object>();
  // jobParameters.put("mapGridName", "BCGS 1:20 000");
  // final Resource inputData = new ClassPathResource("/mapTile.csv");
  // final String jobIdUrl =
  // client.createJobWithStructuredResourceMultipleRequests(
  // "MapTileByTileId", "1.0.0", jobParameters, 1, inputData, "text/csv",
  // "application/json");
  // final Reader<Map<String, Object>> results = client.getJobStructuredResults(
  // jobIdUrl, 5000);
  // final List<Map<String, Object>> resultList = results.read();
  // Assert.assertEquals("Num Results", resultList.size(), 1);
  // final Map<String, Object> result = resultList.get(0);
  // Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
  // .toString(), "1");
  // Assert.assertEquals("mapGridName", result.get("mapGridName"),
  // "BCGS 1:20 000");
  // Assert.assertEquals("mapTileId", result.get("mapTileId"), "92J.016");
  // }
  //
  // private void testMapTileByIdSingle() {
  // final Map<String, Object> request = new HashMap<String, Object>();
  // request.put("mapGridName", "BCGS 1:20 000");
  // request.put("mapTileId", "92j016");
  // final String jobIdUrl = client.createJobWithStructuredSingleRequest(
  // "MapTileByTileId", "1.0.0", request, "application/json");
  //
  // final Reader<Map<String, Object>> results = client.getJobStructuredResults(
  // jobIdUrl, 5000);
  // final List<Map<String, Object>> resultList = results.read();
  // Assert.assertEquals("Num Results", resultList.size(), 1);
  // final Map<String, Object> result = resultList.get(0);
  // Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
  // .toString(), "1");
  // Assert.assertEquals("mapGridName", result.get("mapGridName"),
  // "BCGS 1:20 000");
  // Assert.assertEquals("mapTileId", result.get("mapTileId"), "92J.016");
  // }
  //
  // private void testMapTileByIdSingleDownloadResult() {
  // final Map<String, Object> request = new HashMap<String, Object>();
  // request.put("mapGridName", "BCGS 1:20 000");
  // request.put("mapTileId", "92j016");
  // final String jobIdUrl = client.createJobWithStructuredSingleRequest(
  // "MapTileByTileId", "1.0.0", request, "application/json");
  //
  // for (final Map<String, Object> resultFile : client.getJobResultFileList(
  // jobIdUrl, 5000)) {
  // final String url = (String)resultFile.get("resourceUri");
  // client.processResultFile(url, new ObjectProcessor<InputStream>() {
  // @Override
  // public void process(final InputStream in) {
  // final DataInputStream din = new DataInputStream(in);
  // try {
  // for (String line = din.readLine(); line != null; line = din.readLine()) {
  // System.out.println(line);
  // }
  // } catch (final IOException e) {
  // throw new RuntimeException("Unable to process results", e);
  // }
  // }
  // });
  // }
  // }
  //
  // private void testMapTileByIdSingleError() {
  // final Map<String, Object> request = new HashMap<String, Object>();
  // request.put("mapGridName", "BCGS 1:20 000");
  // request.put("mapTileId", "92j0");
  // final String jobIdUrl = client.createJobWithStructuredSingleRequest(
  // "MapTileByTileId", "1.0.0", request, "application/json");
  //
  // final Reader<Map<String, Object>> errors = client.getJobErrorResults(
  // jobIdUrl, 5000);
  // final List<Map<String, Object>> list = errors.read();
  // Assert.assertEquals(list.size(), 1);
  // for (final Map<String, Object> error : list) {
  // System.out.println(error.get("sequenceNumber"));
  // System.out.println(error.get("errorCode"));
  // System.out.println(error.get("errorMessage"));
  // }
  // }*/
}
