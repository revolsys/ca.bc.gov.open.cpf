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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.open.cpf.client.api.BatchJobClient;
import com.revolsys.io.PrintUtil;

/**
 *  Submit a single request to a business application and display the results.
 *  This request will be processed in real time, rather than being queued for 
 *  processing with the batch job requests.  This would more traditionally be
 *  suitable for supporting the server side requirements of data mashups for 
 *  dynamic and interactive web pages and web applications.
 */
public class WsClientExample5 {

  private BatchJobClient batchJobClient;

  public static void main(final String[] args) {
    WsClientExample5 wsClient = new WsClientExample5();
    wsClient.execute();
  }

  public void execute() {
    String wsBaseUrl = "http://localhost:8080/ws/";
    String oAuthConsumerKey = "ae4a7382-85c0-46e0-986f-def6f7b9b12a";
    String oAuthConsumerSecret = "fd6417b5-722e-4264-9f3d-3effe819616b";
    batchJobClient = new BatchJobClient(wsBaseUrl, oAuthConsumerKey,
      oAuthConsumerSecret);

    String businessApplicationName = "mapTileByLocation";
    String businessApplicationVersion = "1.0.0";
    String resultContentType = "application/vnd.google-earth.kml+xml";
    
    Map<String, String> requestData = new HashMap<String, String>();
    requestData.put("mapGridName", "NTS 1:50 000");
    requestData.put("numBoundaryPoints", "50");
    requestData.put("latitude", "50.341111");
    requestData.put("longitude", "-122.4455561");
    
    try {
      InputStream resultInputStream = batchJobClient.submitSingleJobRequest(businessApplicationName, businessApplicationVersion, requestData, resultContentType);
      PrintUtil.print(resultInputStream, System.out);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    disconnect();
  }

  private void disconnect() {
    batchJobClient.closeConnection();
    batchJobClient = null;
  }

}
