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

import java.util.Arrays;
import java.util.List;

import com.revolsys.io.PathName;

/**
 * The BatchJobResult is a result file generated after the execution of a
 * BatchJob. For structured output data one file will be generated containing
 * all the results. For opaque output data one file will be created for each
 * BatchJobExecutionGroup. A BatchJob may also have one file containing any errors
 * generated.
 */
public interface BatchJobResult extends Common {

  public static final PathName BATCH_JOB_RESULT = PathName
    .newPathName("/CPF/CPF_BATCH_JOB_RESULTS");

  public static final String BATCH_JOB_ID = "BATCH_JOB_ID";

  public static final String RESULT_DATA_URL = "RESULT_DATA_URL";

  public static final String RESULT_DATA_CONTENT_TYPE = "RESULT_DATA_CONTENT_TYPE";

  /**
   * The structuredResultData BatchJobResultType represents a file which
   * contains the structured result data from all requests
   * for a BatchJob is a single file.
   */
  public static final String STRUCTURED_RESULT_DATA = "structuredResultData";

  /**
   * The opaqueResultData BatchJobResultType represents files containing opaque
   * result data for a single BatchJobExecutionGroup.
   */
  public static final String OPAQUE_RESULT_DATA = "opaqueResultData";

  /**
   * The errorResultData represents a file containing all of the errors from all
   * BusinessApplicationRequests or the BatchJob in a single file. The file will
   * be a text/csv file contain the requestSequenceNumber, errorCode, and
   * errorMessage.
   */
  public static final String ERROR_RESULT_DATA = "errorResultData";

  public static final String BATCH_JOB_RESULT_TYPE = "BATCH_JOB_RESULT_TYPE";

  public static final String DOWNLOAD_TIMESTAMP = "DOWNLOAD_TIMESTAMP";

  public static final String SEQUENCE_NUMBER = "SEQUENCE_NUMBER";

  public static final List<String> ALL_EXCEPT_BLOB = Arrays.asList(BATCH_JOB_RESULT_TYPE,
    DOWNLOAD_TIMESTAMP, SEQUENCE_NUMBER, RESULT_DATA_CONTENT_TYPE, RESULT_DATA_URL, WHO_CREATED,
    WHEN_CREATED, WHO_UPDATED, WHEN_UPDATED, BATCH_JOB_ID);

}
