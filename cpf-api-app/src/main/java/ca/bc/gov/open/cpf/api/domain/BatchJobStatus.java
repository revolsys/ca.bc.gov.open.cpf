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
package ca.bc.gov.open.cpf.api.domain;

public interface BatchJobStatus {

  /**
   * The cancelled BatchJobStatus indicates that request was cancelled and all requests and
   * results have been removed.
   */
  String CANCELLED = "cancelled";

  /**
   * The creatingRequests BatchJobStatus indicates the objects are being created
   * by downloading the input data (if required), splitting the input data into
   * BusinessApplicationRequests, and validating the data for each
   * BusinessApplicationRequests.
   */
  String CREATING_REQUESTS = "creatingRequests";

  /**
   * The creatingResults BatchJobStatus indicates that the BatchJobResults are
   * in the process of being created.
   */
  String CREATING_RESULTS = "creatingResults";

  /**
   * The downloadInitiated BatchJobStatus indicates that the user initiated the
   * download of the BatchJobResults.
   */
  String DOWNLOAD_INITIATED = "downloadInitiated";

  /**
   * The processed BatchJobStatus indicates that all the
   * BusinessApplicationRequests of the BatchJob have been either successfully
   * completed or permanently failed. The BatchJob is ready for the
   * BatchJobResults to be created.
   */
  String PROCESSED = "processed";

  /**
   * The processing BatchJobStatus indicates that the BatchJob has been
   * scheduled for execution by the BusinessApplication and some of the
   * BusinessApplicationRequests may currently be executing.
   */
  String PROCESSING = "processing";

  /**
   * The requestsCreated BatchJobStatus indicates the objects have been created
   * by downloading the input data (if required), splitting the input data into
   * BusinessApplicationRequests, and validating the data for each
   * BusinessApplicationRequests.
   */
  String REQUESTS_CREATED = "requestsCreated";

  /**
   * The resultsCreated BatchJobStatus indicates that the BatchJobResults have
   * been created from the BusinessApplicationRequests for the BatchJob. The
   * file is ready to be downloaded.
   */
  String RESULTS_CREATED = "resultsCreated";

  /**
   * The submitted BatchJobStatus indicates that job has been accepted by the
   * application as is ready for the BusinessApplicationRequests be created by
   * downloading the input data (if required), splitting the input data into
   * tasks, and validating the data for each task.
   */
  String SUBMITTED = "submitted";

}
