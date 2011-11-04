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
import java.util.List;
import java.util.Map;

import ca.bc.gov.open.cpf.client.api.BatchJobClient;
import com.revolsys.io.MapReader;
import com.revolsys.io.PrintUtil;

/**
 * Get the job status of an existing successfully processed job. 
 * List the jobs result files. 
 * Get a map reader of the results. 
 * Display the results.
 *
 */
public class WsClientExample4 {

  private BatchJobClient batchJobClient;

  public static void main(final String[] args) {
    WsClientExample4 wsClient = new WsClientExample4();
    wsClient.execute();
  }

  public void execute() {
    String wsBaseUrl = "http://localhost:8080/ws/";
    String oAuthConsumerKey = "ae4a7382-85c0-46e0-986f-def6f7b9b12a";
    String oAuthConsumerSecret = "fd6417b5-722e-4264-9f3d-3effe819616b";
    int jobId = 500;
    batchJobClient = new BatchJobClient(wsBaseUrl, oAuthConsumerKey,
      oAuthConsumerSecret);

    System.out.println("\nExample 4\n\ngetting job status");
    String jobUrl = wsBaseUrl + "users/" + oAuthConsumerKey + "/jobs/" + jobId;
    Map<String, String> jobStatus;
    try {
      System.out.println("\njob status");
      jobStatus = batchJobClient.getJobStatus(jobUrl);
      PrintUtil.print(jobStatus, System.out);

      System.out.println("\nresult files");
      List<Map<String, String>> resultsFileList = batchJobClient.getJobResultsList(jobStatus.get("resultsUrl"));
      for (Map<String, String> resultsFile : resultsFileList) {
        PrintUtil.print(resultsFile, System.out);
        System.out.println("\nresult file " + resultsFile.get("resourceUri"));
        MapReader responseMapReader = batchJobClient.getResponseMapReader(resultsFile.get("resourceUri"));
        batchJobClient.printMapReader(responseMapReader);
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    disconnect();
  }

  private void disconnect() {
    batchJobClient.closeConnection();
    batchJobClient = null;
  }

}
