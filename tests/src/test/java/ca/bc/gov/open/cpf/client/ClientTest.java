package ca.bc.gov.open.cpf.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ca.bc.gov.open.cpf.client.api.CpfClient;

import com.revolsys.io.Reader;
import com.revolsys.spring.ByteArrayResource;
import com.revolsys.util.ObjectProcessor;

public class ClientTest {

  private final String[] BUSINESS_APPLICATION_NAMES = {
    "MapTileByTileId"
  };

  private CpfClient client;

  public static void main(String[] args) {
    new ClientTest().run();
  }

  private String url = "http://localhost/cpf/ws";

  public ClientTest() {
    client = new CpfClient(url, "cpftest", "cpftest");
  }

  public void run() {
    testCloseUserJobs();

    testGetBusinessApplicationNames();
    testGetBusinessApplicationVersions();
    testGetBusinessApplicationSpecification();

    testMapTileByIdSingle();
    testMapTileByIdMultiple();
    testMapTileByIdResourceMultiple();

    testDigestUrlSingle();
    testDigestResourceSingle();

    testDigestUrlMultiple();
    testDigestResourceMultiple();

    testGetUserJobIdUrls();
    testGetUserAppsJobIdUrls();

    testMapTileByIdSingleError();
    testMapTileByIdSingleDownloadResult();

    testCloseUserJobs();

  }

  private void testGetBusinessApplicationNames() {
    List<String> businessApplicationNames = client.getBusinessApplicationNames();
    for (String businessApplicationName : BUSINESS_APPLICATION_NAMES) {
      final boolean contains = businessApplicationNames.contains(businessApplicationName);
      Assert.assertTrue("Business application !contains("
        + businessApplicationName + "')", contains);
    }
  }

  private void testGetBusinessApplicationVersions() {
    String businessApplicationName = "MapTileByTileId";
    List<String> versions = client.getBusinessApplicationVersions(businessApplicationName);
    Assert.assertTrue("Business application versions returned",
      !versions.isEmpty());
  }

  private void testGetBusinessApplicationSpecification() {
    Map<String, Object> specification = client.getBusinessApplicationSpecification(
      "MapTileByTileId", "1.0.0");
    Assert.assertEquals("resourceUri", specification.get("resourceUri"), url
      + "/apps/MapTileByTileId/1.0.0/specification/");
    Assert.assertEquals("businessApplicationName",
      specification.get("businessApplicationName"), "MapTileByTileId");
    Assert.assertEquals("businessApplicationVersion",
      specification.get("businessApplicationVersion"), "1.0.0");
  }

  private void testMapTileByIdMultiple() {
    Map<String, Object> jobParameters = new LinkedHashMap<String, Object>();
    jobParameters.put("mapGridName", "BCGS 1:20 000");
    List<Map<String, Object>> requests = new ArrayList<Map<String, Object>>();
    Map<String, Object> requestParameters = new HashMap<String, Object>();
    requestParameters.put("mapTileId", "92j016");
    requests.add(requestParameters);
    String jobIdUrl = client.createJobWithStructuredMultipleRequests(
      "MapTileByTileId", "1.0.0", jobParameters, requests, "application/json");
    final Reader<Map<String, Object>> results = client.getJobStructuredResults(
      jobIdUrl, 5000);
    final List<Map<String, Object>> resultList = results.read();
    Assert.assertEquals("Num Results", resultList.size(), 1);
    Map<String, Object> result = resultList.get(0);
    Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
      .toString(), "1");
    Assert.assertEquals("mapGridName", result.get("mapGridName"),
      "BCGS 1:20 000");
    Assert.assertEquals("mapTileId", result.get("mapTileId"), "92J.016");
  }

  private void testMapTileByIdResourceMultiple() {
    Map<String, Object> jobParameters = new LinkedHashMap<String, Object>();
    jobParameters.put("mapGridName", "BCGS 1:20 000");
    Resource inputData = new ClassPathResource("/mapTile.csv");
    String jobIdUrl = client.createJobWithStructuredResourceMultipleRequests(
      "MapTileByTileId", "1.0.0", jobParameters, 1, inputData, "text/csv",
      "application/json");
    final Reader<Map<String, Object>> results = client.getJobStructuredResults(
      jobIdUrl, 5000);
    final List<Map<String, Object>> resultList = results.read();
    Assert.assertEquals("Num Results", resultList.size(), 1);
    Map<String, Object> result = resultList.get(0);
    Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
      .toString(), "1");
    Assert.assertEquals("mapGridName", result.get("mapGridName"),
      "BCGS 1:20 000");
    Assert.assertEquals("mapTileId", result.get("mapTileId"), "92J.016");
  }

  private void testMapTileByIdSingle() {
    Map<String, Object> request = new HashMap<String, Object>();
    request.put("mapGridName", "BCGS 1:20 000");
    request.put("mapTileId", "92j016");
    String jobIdUrl = client.createJobWithStructuredSingleRequest(
      "MapTileByTileId", "1.0.0", request, "application/json");

    final Reader<Map<String, Object>> results = client.getJobStructuredResults(
      jobIdUrl, 5000);
    final List<Map<String, Object>> resultList = results.read();
    Assert.assertEquals("Num Results", resultList.size(), 1);
    Map<String, Object> result = resultList.get(0);
    Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
      .toString(), "1");
    Assert.assertEquals("mapGridName", result.get("mapGridName"),
      "BCGS 1:20 000");
    Assert.assertEquals("mapTileId", result.get("mapTileId"), "92J.016");
  }

  private void testMapTileByIdSingleError() {
    Map<String, Object> request = new HashMap<String, Object>();
    request.put("mapGridName", "BCGS 1:20 000");
    request.put("mapTileId", "92j0");
    String jobIdUrl = client.createJobWithStructuredSingleRequest(
      "MapTileByTileId", "1.0.0", request, "application/json");

    final Reader<Map<String, Object>> errors = client.getJobErrorResults(
      jobIdUrl, 5000);
    final List<Map<String, Object>> list = errors.read();
    Assert.assertEquals(list.size(), 1);
    for (Map<String, Object> error : list) {
      System.out.println(error.get("sequenceNumber"));
      System.out.println(error.get("errorCode"));
      System.out.println(error.get("errorMessage"));
    }
  }

  private void testMapTileByIdSingleDownloadResult() {
    Map<String, Object> request = new HashMap<String, Object>();
    request.put("mapGridName", "BCGS 1:20 000");
    request.put("mapTileId", "92j016");
    String jobIdUrl = client.createJobWithStructuredSingleRequest(
      "MapTileByTileId", "1.0.0", request, "application/json");

    for (Map<String, Object> resultFile : client.getJobResultFileList(jobIdUrl,
      5000)) {
      String url = (String)resultFile.get("resourceUri");
      client.processResultFile(url, new ObjectProcessor<InputStream>() {
        public void process(InputStream in) {
          DataInputStream din = new DataInputStream(in);
          try {
            for (String line = din.readLine(); line != null; line = din.readLine()) {
              System.out.println(line);
            }
          } catch (IOException e) {
            throw new RuntimeException("Unable to process results", e);
          }
        }
      });
    }
  }

  private void testDigestResourceSingle() {
    Map<String, Object> jobParameters = new HashMap<String, Object>();
    jobParameters.put("algorithmName", "SHA");
    String jobIdUrl = client.createJobWithOpaqueResourceSingleRequest("digest",
      "1.0.0", jobParameters,
      new ByteArrayResource("t", "This is a test".getBytes()), "text/plain",
      "application/json");

    final Reader<Map<String, Object>> results = client.getJobStructuredResults(
      jobIdUrl, 10000);
    final List<Map<String, Object>> resultList = results.read();
    Assert.assertEquals("Num Results", resultList.size(), 1);
    Map<String, Object> result = resultList.get(0);
    Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
      .toString(), "1");
    Assert.assertNotNull("digest", result.get("digest"));
  }

  private void testDigestUrlSingle() {
    Map<String, Object> jobParameters = new HashMap<String, Object>();
    jobParameters.put("algorithmName", "SHA");
    String jobIdUrl = client.createJobWithOpaqueUrlSingleRequest("digest",
      "1.0.0", jobParameters, url + "/", "text/html", "application/json");

    final Reader<Map<String, Object>> results = client.getJobStructuredResults(
      jobIdUrl, 5000);
    final List<Map<String, Object>> resultList = results.read();
    Assert.assertEquals("Num Results", resultList.size(), 1);
    Map<String, Object> result = resultList.get(0);
    Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
      .toString(), "1");
    Assert.assertNotNull("digest", result.get("digest"));
  }

  private void testDigestResourceMultiple() {
    Map<String, Object> jobParameters = new HashMap<String, Object>();
    jobParameters.put("algorithmName", "SHA");
    List<Resource> resources = new ArrayList<Resource>();
    resources.add(new ByteArrayResource("t", "This is a test".getBytes()));
    String jobIdUrl = client.createJobWithOpaqueResourceMultipleRequests(
      "digest", "1.0.0", jobParameters, resources, "text/html",
      "application/json");

    final Reader<Map<String, Object>> results = client.getJobStructuredResults(
      jobIdUrl, 5000);
    final List<Map<String, Object>> resultList = results.read();
    Assert.assertEquals("Num Results", resultList.size(), 1);
    Map<String, Object> result = resultList.get(0);
    Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
      .toString(), "1");
    Assert.assertNotNull("digest", result.get("digest"));
  }

  private void testDigestUrlMultiple() {
    Map<String, Object> jobParameters = new HashMap<String, Object>();
    jobParameters.put("algorithmName", "SHA");
    List<String> urls = new ArrayList<String>();
    urls.add(url + "/");
    String jobIdUrl = client.createJobWithOpaqueUrlMultipleRequests("digest",
      "1.0.0", jobParameters, urls, "text/html", "application/json");

    final Reader<Map<String, Object>> results = client.getJobStructuredResults(
      jobIdUrl, 5000);
    final List<Map<String, Object>> resultList = results.read();
    Assert.assertEquals("Num Results", resultList.size(), 1);
    Map<String, Object> result = resultList.get(0);
    Assert.assertEquals("sequenceNumber", result.get("sequenceNumber")
      .toString(), "1");
    Assert.assertNotNull("digest", result.get("digest"));
  }

  private void testGetUserJobIdUrls() {
    Map<String, Object> request = new HashMap<String, Object>();
    request.put("mapGridName", "BCGS 1:20 000");
    request.put("mapTileId", "92j016");
    String jobIdUrl = client.createJobWithStructuredSingleRequest(
      "MapTileByTileId", "1.0.0", request, "application/json");

    final List<String> jobIdUrls = client.getUserJobIdUrls();
    Assert.assertTrue("UserJobs contains " + jobIdUrl,
      jobIdUrls.contains(jobIdUrl));
  }

  private void testGetUserAppsJobIdUrls() {
    Map<String, Object> request = new HashMap<String, Object>();
    request.put("mapGridName", "BCGS 1:20 000");
    request.put("mapTileId", "92j016");
    String jobIdUrl = client.createJobWithStructuredSingleRequest(
      "MapTileByTileId", "1.0.0", request, "application/json");

    final List<String> jobIdUrls = client.getUserJobIdUrls("MapTileByTileId");
    final boolean contains = jobIdUrls.contains(jobIdUrl);
    Assert.assertTrue("UserAppJobs contains " + jobIdUrl, contains);
  }

  private void testCloseUserJobs() {
    List<String> jobIdUrls = client.getUserJobIdUrls();
    for (String jobIdUrl : jobIdUrls) {
      client.closeJob(jobIdUrl);
    }
    Assert.assertEquals(0, client.getUserJobIdUrls().size());
  }
}
