/*
 Copyright 2009 Revolution Systems Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 
 $Author: chris.ogrady@revolsys.com $
 $Date: 2009-07-07 11:27:35 -0700 (Tue, 07 Jul 2009) $
 $Revision: 1959 $
*/
package examples;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ca.bc.gov.open.cpf.client.api.BatchJobClient;
import com.revolsys.io.MapReader;
import com.revolsys.io.PrintUtil;

/**
 * Submit a job for the business application mapTileByLocation using a URL as
 * the location of the batch job requests data. 
 * Wait for job processing to be completed. 
 * Get the results, extract the map names, remove duplicates, sort
 * the resulting list and display the short list of map names.
 */
public class WsClientExample1 {

  private BatchJobClient batchJobClient;

  public static void main(final String[] args) {
    WsClientExample1 wsClient = new WsClientExample1();
    wsClient.execute();
  }

  public void execute() {
    String wsBaseUrl = "http://localhost:8080/ws/";
    String oAuthConsumerKey = "ae4a7382-85c0-46e0-986f-def6f7b9b12a";
    String oAuthConsumerSecret = "fd6417b5-722e-4264-9f3d-3effe819616b";
    batchJobClient = new BatchJobClient(wsBaseUrl, oAuthConsumerKey,
      oAuthConsumerSecret);

    // Example 1: using URL job submission, get a sorted list of unique map
    // names associated with the map requests.
    System.out.println("\nExample 1\n\nsubmitting batch job by URL");
    Map<String, String> jobStatus;
    try {
      jobStatus = submitBatchJobByUrl();
      System.out.println("\njob status");
      PrintUtil.print(jobStatus, System.out);
      if (jobStatus == null) {
        System.out.println("\njob submission failed");
      } else if (!"resultGenerated".equals(jobStatus.get("jobStatus"))) {
        System.out.println("\njob has not completed");
        System.out.println("job status");
        PrintUtil.print(jobStatus, System.out);
      } else {
        List<Map<String, String>> jobResultFiles = batchJobClient.getJobResultsList(jobStatus.get("resultsUrl"));
        String jobResultUri = jobResultFiles.get(0).get("resourceUri");
        
        System.out.println("\nmap names");
        Set<String> listOfMapNames;
        listOfMapNames = processJobResults(jobResultUri);
        System.out.println(listOfMapNames.size() + " Maps");
        for (String mapName : listOfMapNames) {
          System.out.println(mapName);
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      e.printStackTrace();
    }

    disconnect();
  }

  private Map<String, String> submitBatchJobByUrl() {
    String businessApplicationName = "mapTileByLocation";
    String businessApplicationVersion = "1.0.0";
    int numRequests = 2880;
    String inputDataUrl = "http://webServicesBaseUrl/docs/sample-data/multiple-maps-by-location.csv";
    String inputDataContentType = "text/csv";
    String resultContentType = "text/csv";
    Map<String, String> parameters = new HashMap<String, String>();
    // parameters.put("mapGridName", "NTS 1:250 000");

    boolean waitForResponse = true;
    int maxWaitTime = -1;
    Map<String, String> jobStatus = null;
    try {
      jobStatus = batchJobClient.submitJob(
        businessApplicationName, businessApplicationVersion, numRequests, inputDataUrl,
        inputDataContentType, resultContentType, parameters, waitForResponse, maxWaitTime);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    return jobStatus;
  }

  private Set<String> processJobResults(final String jobResultsUri)
    throws IOException {
    Set<String> sortedListOfMaps = null;
    if (jobResultsUri != null) {
        // TODO handle results type for error results / data results
        MapReader resultsMapReader = batchJobClient.getResponseMapReader(jobResultsUri);
        sortedListOfMaps = sortedListOfUniqueMaps(resultsMapReader);
    }

    return sortedListOfMaps;
  }

  /**
   * Create a sorted list of maps, removing duplicate Map Names.
   * 
   * @param resultsMap
   */
  private Set<String> sortedListOfUniqueMaps(final MapReader resultsMapReader) {
    Set<String> mapSet = new TreeSet<String>();
    Iterator<Map<String, Object>> resultsIterator = resultsMapReader.iterator();
    while (resultsIterator.hasNext()) {
      Map<String, Object> result = (Map<String, Object>)resultsIterator.next();
      String mapDescription = (String)result.get("mapGridName") + "\tMap " + (String)result.get("mapTileId");
      mapSet.add(mapDescription);
    }
    return mapSet;
  }

  private void disconnect() {
    batchJobClient.closeConnection();
    batchJobClient = null;
  }

}
