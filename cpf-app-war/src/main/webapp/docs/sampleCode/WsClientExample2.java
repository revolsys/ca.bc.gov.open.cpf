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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.open.cpf.client.api.BatchJobClient;
import com.revolsys.io.PrintUtil;

/**
 * Submit a job, using a local data file for batch job requests data.
 * Wait for the job to complete, timing out after 1 hour.  Get and display the job status.
 * Close the job.
 */
public class WsClientExample2 {

  private BatchJobClient batchJobClient;

  public static void main(final String[] args) {
    WsClientExample2 wsClient = new WsClientExample2();
    wsClient.execute();
  }

  public void execute() {
    String wsBaseUrl = "http://localhost:8080/ws/";
    String oAuthConsumerKey = "ae4a7382-85c0-46e0-986f-def6f7b9b12a";
    String oAuthConsumerSecret = "fd6417b5-722e-4264-9f3d-3effe819616b";
    batchJobClient = new BatchJobClient(wsBaseUrl, oAuthConsumerKey,
      oAuthConsumerSecret);

    System.out.println("\nExample 2\n\nsubmitting batch job by file");
    
    Map<String, String> jobStatus;
    try {
      jobStatus = submitBatchJobByFile();

      if (jobStatus == null) {
        System.out.println("\njob submission failed");
      } else {
        if (!"resultGenerated".equals(jobStatus.get("jobStatus"))) {
          System.out.println("\njob has been submitted");
        }
        System.out.println("\njob status");
        PrintUtil.print(jobStatus, System.out);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
    
    disconnect();
  }

  private Map<String, String> submitBatchJobByFile() {
    String businessApplicationName = "mapTileByLocation";
    String businessApplicationVersion = "1.0.0";
    int numberOfRequests = 192;
    String inputDataFile = "C://tmp//NTS-250000-by-location.csv";
    File inputFile = new File(inputDataFile);
    String inputDataContentType = "text/csv";
    String resultContentType = "text/csv";
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("mapGridName", "NTS 1:250 000");

    boolean waitForResponse = true;
    int maxWaitTime = 3600000;
    Map<String, String> jobStatus = null;

    try {
      jobStatus = batchJobClient.submitJob(businessApplicationName,
        businessApplicationVersion, numberOfRequests, inputFile, inputDataContentType,
        resultContentType, parameters, waitForResponse, maxWaitTime);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return jobStatus;
  }

  private void disconnect() {
    batchJobClient.closeConnection();
    batchJobClient = null;
  }

}
