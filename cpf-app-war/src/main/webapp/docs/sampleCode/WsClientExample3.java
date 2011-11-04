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

import ca.bc.gov.open.cpf.client.api.BatchJobClient;
import com.revolsys.io.PrintUtil;


/**
 * List all the business application mapTileByLocation jobs for a specific user. 
 */
public class WsClientExample3 {

  private BatchJobClient batchJobClient;

  public static void main(final String[] args) {
    WsClientExample3 wsClient = new WsClientExample3();
    wsClient.execute();
  }

  public void execute() {
    String wsBaseUrl = "http://localhost:8080/ws/";
    String oAuthConsumerKey = "ae4a7382-85c0-46e0-986f-def6f7b9b12a";
    String oAuthConsumerSecret = "fd6417b5-722e-4264-9f3d-3effe819616b";
    batchJobClient = new BatchJobClient(wsBaseUrl, oAuthConsumerKey,
      oAuthConsumerSecret);

    System.out.println("\nExample 3\n\ngetting list of users jobs");
    List<String> jobList = null;
    try {
      jobList = batchJobClient.getUserJobs("mapTileByLocation");
      PrintUtil.print(jobList, System.out);
    } catch (IllegalStateException e) {
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
