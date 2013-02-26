package ca.bc.gov.open.cpf.client.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import ca.bc.gov.open.cpf.client.api.CpfClient;
import ca.bc.gov.open.cpf.client.api.Callback;

@SuppressWarnings("javadoc")
public class CpfClientTest {
  public static void main(String[] args) throws IOException {
    testConstructor();
    testCloseConnection();

    testGetBusinessApplicationNames();
    testGetBusinessApplicationVersions();
    testGetBusinessApplicationSingleSpecification();
    testGetBusinessApplicationInstantSpecification();
    testGetBusinessApplicationMultipleSpecification();

    testCloseBatchJob();

    testCreateOpaqueResource();
    testCreateOpaqueResourceCollection();
    testCreateOpaqueUrl();
    testCreateOpaqueUrlCollection();

    testCreateStructuredSingle();
    testCreateStructuredMultipleList();
    testCreateStructuredMultipleResource();
    testCreateStructuredMultipleUrl();

    testGetBatchJobStatus();
    testIsBatchJobCompleted();
    testGetBatchJobResultsError();
    testProcessBatchJobResultsError();
    testGetBatchJobResultsList();
    testGetBatchJobStructuredResults();
    testProcessBatchJobStructuredResults();

    testUserGetBatchJobIds();
    testUserAppGetBatchJobIds();
    testProcessBatchJobResultFile();
  }

  @SuppressWarnings("unused")
  private static void testConstructor() {
    System.out.println("Constructor");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
  }

  private static void testCloseConnection() {
    System.out.println("Close Connection");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      // Use the client
    } finally {
      client.closeConnection();
    }
  }

  private static void testCloseBatchJob() {
    System.out.println("Close Batch Job");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92g025");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBatchJobResultsError() {
    System.out.println("Get Batch Job Results Error");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        List<Map<String, Object>> results = client.getJobErrorResults(
          batchJobId, 10000);
        for (Map<String, Object> error : results) {
          System.out.println(error);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testProcessBatchJobResultsError() {
    System.out.println("Process Batch Job Results Error");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        int numErrors = client.processJobErrorResults(batchJobId, 10000,
          new Callback<Map<String, Object>>() {
            public void process(Map<String, Object> error) {
              System.out.println(error);
            }
          });
        System.out.println(numErrors);
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBatchJobResultsList() {
    System.out.println("Get Batch Job Results File List");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        List<Map<String, Object>> files = client.getJobResultFileList(
          batchJobId, 10000);
        for (Map<String, Object> file : files) {
          System.out.println(file);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testProcessBatchJobResultFile() {
    System.out.println("Process Batch Job Results File");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j016");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        List<Map<String, Object>> files = client.getJobResultFileList(
          batchJobId, 10000);
        for (Map<String, Object> file : files) {
          String jobResultUrl = (String)file.get("resourceUri");
          client.processResultFile(jobResultUrl, new Callback<InputStream>() {

            @Override
            public void process(InputStream in) {
              try {
                BufferedReader reader = new BufferedReader(
                  new InputStreamReader(in));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                  System.out.println(line);
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          });
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBatchJobStructuredResults() {
    System.out.println("Get Batch Job Structured Results");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j025");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        List<Map<String, Object>> results = client.getJobStructuredResults(
          batchJobId, 10000);
        for (Map<String, Object> result : results) {
          System.out.println(result);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testProcessBatchJobStructuredResults() {
    System.out.println("Process Batch Job Structured Results");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j025");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        int numResults = client.processJobStructuredResults(batchJobId, 10000,
          new Callback<Map<String, Object>>() {
            public void process(Map<String, Object> result) {
              System.out.println(result);
            }
          });
        System.out.println(numResults);
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateStructuredSingle() {
    System.out.println("Create Batch Job Structured Single");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j025");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        List<Map<String, Object>> results = client.getJobStructuredResults(
          batchJobId, 10000);
        for (Map<String, Object> result : results) {
          System.out.println(result);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateStructuredMultipleList() {
    System.out.println("Create Batch Job Structured Multiple List");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> jobParameters = new HashMap<String, Object>();
      jobParameters.put("mapGridName", "BCGS 1:20 000");

      List<Map<String, ? extends Object>> requests = new ArrayList<Map<String, ? extends Object>>();
      requests.add(Collections.singletonMap("mapTileId", "92j025"));
      requests.add(Collections.singletonMap("mapTileId", "92j016"));

      String batchJobId = client.createJobWithStructuredMultipleRequestsList(
        "MapTileByTileId", "1.0.0", jobParameters, requests, "application/json");
      try {
        List<Map<String, Object>> results = client.getJobStructuredResults(
          batchJobId, 20000);
        for (Map<String, Object> result : results) {
          System.out.println(result);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateStructuredMultipleResource() {
    System.out.println("Create Batch Job Structured Multiple Resource");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> jobParameters = new HashMap<String, Object>();
      jobParameters.put("mapGridName", "NTS 1:500 000");

      int numRequests = 48;
      Resource inputData = new FileSystemResource(
        "../cpf-war-app/src/main/webapp/docs/sample/NTS-500000-by-name.csv");

      String batchJobId = client.createJobWithStructuredMultipleRequestsResource(
        "MapTileByTileId", "1.0.0", jobParameters, numRequests, inputData,
        "text/csv", "application/json");
      try {
        List<Map<String, Object>> results = client.getJobStructuredResults(
          batchJobId, 30000);
        for (Map<String, Object> result : results) {
          System.out.println(result);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateStructuredMultipleUrl() {
    System.out.println("Create Batch Job Structured Multiple URL");
  String url = "http://localhost/pub/cpf";
  String consumerKey = "cpftest";
  String consumerSecret = "cpftest";
  CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
  try {
    Map<String, Object> jobParameters = new HashMap<String, Object>();
    jobParameters.put("mapGridName", "NTS 1:500 000");

    int numRequests = 48;

    String inputDataUrl = "http://localhost/pub/cpf/docs/sample/NTS-500000-by-name.csv";
    String batchJobId = client.createJobWithStructuredMultipleRequestsUrl(
      "MapTileByTileId", "1.0.0", jobParameters, numRequests, inputDataUrl,
      "text/csv", "application/json");
    try {
      List<Map<String, Object>> results = client.getJobStructuredResults(
        batchJobId, 30000);
      for (Map<String, Object> result : results) {
        System.out.println(result);
      }
    } finally {
      client.closeJob(batchJobId);
    }
  } finally {
    client.closeConnection();
  }
  }

  private static void testGetBatchJobStatus() {
    System.out.println("Get Batch Job Status");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        Map<String, Object> status = client.getJobStatus(batchJobId);
        System.out.println(status);
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testIsBatchJobCompleted() {
    System.out.println("Is Batch Job Completed");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        boolean completed = client.isJobCompleted(batchJobId, 2000);
        if (completed) {
          System.out.println("Job Completed");
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testUserGetBatchJobIds() {
    System.out.println("User Get Batch Job Ids");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        List<String> batchJobIds = client.getUserJobIdUrls();
        for (String batchJobIdUrl : batchJobIds) {
          System.out.println(batchJobIdUrl);
        }
        if (!batchJobIds.contains(batchJobId)) {
          System.err.println("Missing job " + batchJobId);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testUserAppGetBatchJobIds() {
    System.out.println("User Get App Batch Job Ids");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      String batchJobId = client.createJobWithStructuredSingleRequest(
        "MapTileByTileId", "1.0.0", parameters, "application/json");
      try {
        List<String> batchJobIds = client.getUserJobIdUrls("MapTileByTileId");
        for (String batchJobIdUrl : batchJobIds) {
          System.out.println(batchJobIdUrl);
        }
        if (!batchJobIds.contains(batchJobId)) {
          System.err.println("Missing job " + batchJobId);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateOpaqueResource() {
    System.out.println("Opaque Input Resource");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      Resource resource = new ByteArrayResource("Test string".getBytes());
      // Resource resource = new FileSystemResource(pathToFile);

      String batchJobId = client.createJobWithOpaqueResourceRequests("Digest",
        "1.0.0", parameters, "text/plain", "application/json", resource);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateOpaqueResourceCollection() {
    System.out.println("Opaque Input Resource Collection");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      List<Resource> requests = new ArrayList<Resource>();
      requests.add(new ByteArrayResource("Test string".getBytes()));
      // requests.add(Resource resource = new FileSystemResource(pathToFile));

      String batchJobId = client.createJobWithOpaqueResourceRequests("Digest",
        "1.0.0", parameters, "text/plain", "application/json", requests);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateOpaqueUrl() {
    System.out.println("Opaque URL Resource");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      String inputDataUrl = "http://localhost/pub/cpf/css/cpf.css";

      String batchJobId = client.createJobWithOpaqueUrlRequests("Digest",
        "1.0.0", parameters, "text/plain", "application/json", inputDataUrl);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateOpaqueUrlCollection() {
    System.out.println("Opaque Input URL Collection");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      List<String> inputDataUrls = new ArrayList<String>();
      inputDataUrls.add("http://localhost/pub/cpf/css/cpf.css");

      String batchJobId = client.createJobWithOpaqueUrlRequests("Digest",
        "1.0.0", parameters, "text/plain", "application/json", inputDataUrls);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationNames() {
    System.out.println("Get Business Application Names");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      List<String> businessApplicationNames = client.getBusinessApplicationNames();
      for (String businessApplicationName : businessApplicationNames) {
        System.out.println(businessApplicationName);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationVersions() {
    System.out.println("Get Business Application Versions");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      List<String> versions = client.getBusinessApplicationVersions("MapTileByTileId");
      for (String version : versions) {
        System.out.println(version);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationMultipleSpecification() {
    System.out.println("Get Business Application Specification");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> specification = client.getBusinessApplicationMultipleSpecification(
        "MapTileByTileId", "1.0.0");
      System.out.println(specification);
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationSingleSpecification() {
    System.out.println("Get Business Application Specification");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> specification = client.getBusinessApplicationSingleSpecification(
        "MapTileByTileId", "1.0.0");
      System.out.println(specification);
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationInstantSpecification() {
    System.out.println("Get Business Application Specification");
    String url = "http://localhost/pub/cpf";
    String consumerKey = "cpftest";
    String consumerSecret = "cpftest";
    CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      Map<String, Object> specification = client.getBusinessApplicationInstantSpecification(
        "MapTileByTileId", "1.0.0");
      System.out.println(specification);
    } finally {
      client.closeConnection();
    }
  }
}
