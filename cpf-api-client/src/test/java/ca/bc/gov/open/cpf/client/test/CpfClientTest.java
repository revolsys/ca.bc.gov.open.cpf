/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import ca.bc.gov.open.cpf.client.api.Callback;
import ca.bc.gov.open.cpf.client.api.CpfClient;

import com.revolsys.spring.resource.ByteArrayResource;
import com.revolsys.spring.resource.FileSystemResource;
import com.revolsys.spring.resource.Resource;

@SuppressWarnings("javadoc")
public class CpfClientTest {
  private static final String url = "http://localhost/pub/cpf";

  private static final String consumerKey = "cpftest";

  private static final String consumerSecret = "cpftest";

  public static void main(final String[] args) throws IOException {
    testConstructor();
    testCloseConnection();

    testGetBusinessApplicationNames();
    testGetBusinessApplicationSingleSpecification();
    testGetBusinessApplicationInstantSpecification();
    testGetBusinessApplicationMultipleSpecification();

    testCloseBatchJob();

    // testCreateOpaqueResource();
    // testCreateOpaqueResourceCollection();
    // testCreateOpaqueUrl();
    // testCreateOpaqueUrlCollection();

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

  private static void testCloseBatchJob() {
    System.out.println("Close Batch Job");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92g025");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCloseConnection() {
    System.out.println("Close Connection");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      // Use the client
    } finally {
      client.closeConnection();
    }
  }

  @SuppressWarnings("unused")
  private static void testConstructor() {
    System.out.println("Constructor");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
  }

  private static void testCreateOpaqueResource() {
    System.out.println("Opaque Input Resource");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      final Resource resource = new ByteArrayResource("Test string".getBytes());
      // Resource resource = new FileSystemResource(pathToFile);

      final String batchJobId = client.createJobWithOpaqueResourceRequests("Digest", parameters,
        "text/plain", "application/json", resource);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateOpaqueResourceCollection() {
    System.out.println("Opaque Input Resource Collection");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      final List<Resource> requests = new ArrayList<Resource>();
      requests.add(new ByteArrayResource("Test string".getBytes()));
      // requests.add(Resource resource = new FileSystemResource(pathToFile));

      final String batchJobId = client.createJobWithOpaqueResourceRequests("Digest", parameters,
        "text/plain", "application/json", requests);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateOpaqueUrl() {
    System.out.println("Opaque URL Resource");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      final String inputDataUrl = "http://localhost/pub/cpf/css/cpf.css";

      final String batchJobId = client.createJobWithOpaqueUrlRequests("Digest", parameters,
        "text/plain", "application/json", inputDataUrl);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateOpaqueUrlCollection() {
    System.out.println("Opaque Input URL Collection");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("algorithmName", "MD5");

      final List<String> inputDataUrls = new ArrayList<String>();
      inputDataUrls.add("http://localhost/pub/cpf/css/cpf.css");

      final String batchJobId = client.createJobWithOpaqueUrlRequests("Digest", parameters,
        "text/plain", "application/json", inputDataUrls);
      // Download the results of the job
      client.closeJob(batchJobId);
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateStructuredMultipleList() {
    System.out.println("Create Batch Job Structured Multiple List");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> jobParameters = new HashMap<String, Object>();
      jobParameters.put("mapGridName", "BCGS 1:20 000");

      final List<Map<String, ? extends Object>> requests = new ArrayList<Map<String, ? extends Object>>();
      requests.add(Collections.singletonMap("mapTileId", "92j025"));
      requests.add(Collections.singletonMap("mapTileId", "92j016"));

      final String batchJobId = client.createJobWithStructuredMultipleRequestsList(
        "MapTileByTileId", jobParameters, requests, "application/json");
      try {
        final List<Map<String, Object>> results = client.getJobStructuredResults(batchJobId, 20000);
        for (final Map<String, Object> result : results) {
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
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> jobParameters = new HashMap<String, Object>();
      jobParameters.put("mapGridName", "NTS 1:500 000");

      final int numRequests = 48;
      final Resource inputData = new FileSystemResource(
        "../cpf-war-app/src/main/webapp/docs/sample/NTS-500000-by-name.csv");

      final String batchJobId = client.createJobWithStructuredMultipleRequestsResource(
        "MapTileByTileId", jobParameters, numRequests, inputData, "text/csv", "application/json");
      try {
        final List<Map<String, Object>> results = client.getJobStructuredResults(batchJobId, 30000);
        for (final Map<String, Object> result : results) {
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
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> jobParameters = new HashMap<String, Object>();
      jobParameters.put("mapGridName", "NTS 1:500 000");

      final int numRequests = 48;

      final String inputDataUrl = "http://localhost/pub/cpf/docs/sample/NTS-500000-by-name.csv";
      final String batchJobId = client.createJobWithStructuredMultipleRequestsUrl("MapTileByTileId",
        jobParameters, numRequests, inputDataUrl, "text/csv", "application/json");
      try {
        final List<Map<String, Object>> results = client.getJobStructuredResults(batchJobId, 30000);
        for (final Map<String, Object> result : results) {
          System.out.println(result);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testCreateStructuredSingle() {
    System.out.println("Create Batch Job Structured Single");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j025");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final List<Map<String, Object>> results = client.getJobStructuredResults(batchJobId, 10000);
        for (final Map<String, Object> result : results) {
          System.out.println(result);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBatchJobResultsError() {
    System.out.println("Get Batch Job Results Error");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final List<Map<String, Object>> results = client.getJobErrorResults(batchJobId, 10000);
        for (final Map<String, Object> error : results) {
          System.out.println(error);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBatchJobResultsList() {
    System.out.println("Get Batch Job Results File List");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final List<Map<String, Object>> files = client.getJobResultFileList(batchJobId, 10000);
        for (final Map<String, Object> file : files) {
          System.out.println(file);
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
    final String url = "http://localhost/pub/cpf";
    final String consumerKey = "cpftest";
    final String consumerSecret = "cpftest";
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final Map<String, Object> status = client.getJobStatus(batchJobId);
        System.out.println(status);
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBatchJobStructuredResults() {
    System.out.println("Get Batch Job Structured Results");
    final String url = "http://localhost/pub/cpf";
    final String consumerKey = "cpftest";
    final String consumerSecret = "cpftest";
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j025");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final List<Map<String, Object>> results = client.getJobStructuredResults(batchJobId, 10000);
        for (final Map<String, Object> result : results) {
          System.out.println(result);
        }
      } finally {
        client.closeJob(batchJobId);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationInstantSpecification() {
    System.out.println("Get Business Application Specification");
    final String url = "http://localhost/pub/cpf";
    final String consumerKey = "cpftest";
    final String consumerSecret = "cpftest";
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> specification = client
        .getBusinessApplicationInstantSpecification("MapTileByTileId");
      System.out.println(specification);
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationMultipleSpecification() {
    System.out.println("Get Business Application Specification");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> specification = client
        .getBusinessApplicationMultipleSpecification("MapTileByTileId");
      System.out.println(specification);
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationNames() {
    System.out.println("Get Business Application Names");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final List<String> businessApplicationNames = client.getBusinessApplicationNames();
      for (final String businessApplicationName : businessApplicationNames) {
        System.out.println(businessApplicationName);
      }
    } finally {
      client.closeConnection();
    }
  }

  private static void testGetBusinessApplicationSingleSpecification() {
    System.out.println("Get Business Application Specification");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> specification = client
        .getBusinessApplicationSingleSpecification("MapTileByTileId");
      System.out.println(specification);
    } finally {
      client.closeConnection();
    }
  }

  private static void testIsBatchJobCompleted() {
    System.out.println("Is Batch Job Completed");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final boolean completed = client.isJobCompleted(batchJobId, 2000);
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

  private static void testProcessBatchJobResultFile() {
    System.out.println("Process Batch Job Results File");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j016");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final List<Map<String, Object>> files = client.getJobResultFileList(batchJobId, 10000);
        for (final Map<String, Object> file : files) {
          final String jobResultUrl = (String)file.get("resourceUri");
          client.processResultFile(jobResultUrl, new Callback<InputStream>() {

            @Override
            public void process(final InputStream in) {
              try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                  System.out.println(line);
                }
              } catch (final IOException e) {
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

  private static void testProcessBatchJobResultsError() {
    System.out.println("Process Batch Job Results Error");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final int numErrors = client.processJobErrorResults(batchJobId, 10000,
          new Callback<Map<String, Object>>() {
            @Override
            public void process(final Map<String, Object> error) {
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

  private static void testProcessBatchJobStructuredResults() {
    System.out.println("Process Batch Job Structured Results");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "92j025");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final int numResults = client.processJobStructuredResults(batchJobId, 10000,
          new Callback<Map<String, Object>>() {
            @Override
            public void process(final Map<String, Object> result) {
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

  private static void testUserAppGetBatchJobIds() {
    System.out.println("User Get App Batch Job Ids");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final List<String> batchJobIds = client.getUserJobIdUrls("MapTileByTileId");
        for (final String batchJobIdUrl : batchJobIds) {
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

  private static void testUserGetBatchJobIds() {
    System.out.println("User Get Batch Job Ids");
    final CpfClient client = new CpfClient(url, consumerKey, consumerSecret);
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("mapGridName", "BCGS 1:20 000");
      parameters.put("mapTileId", "INVALID");
      final String batchJobId = client.createJobWithStructuredSingleRequest("MapTileByTileId",
        parameters, "application/json");
      try {
        final List<String> batchJobIds = client.getUserJobIdUrls();
        for (final String batchJobIdUrl : batchJobIds) {
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
}
