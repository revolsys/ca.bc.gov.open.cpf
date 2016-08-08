/*
 * Copyright © 2008-2016, Province of British Columbia
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
package ca.bc.gov.open.cpf.api.scheduler;

import java.io.File;
import java.util.Map;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.web.controller.FileJobController;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.identifier.Identifier;

public class FilePreProcessGroup extends PreProcessGroup {
  public FilePreProcessGroup(final JobPreProcessTask preProcess,
    final FileJobController jobController, final BusinessApplication businessApplication,
    final BatchJob batchJob, final Map<String, String> jobParameters,
    final int groupSequenceNumber) {
    super(preProcess, jobController, businessApplication, batchJob, jobParameters,
      groupSequenceNumber);
  }

  @Override
  protected File newGroupFile() {
    final FileJobController jobController = (FileJobController)getJobController();
    final Identifier batchJobId = getBatchJobId();
    final int groupSequenceNumber = getGroupSequenceNumber();
    return jobController.getJobFile(batchJobId, JobController.GROUP_INPUTS, groupSequenceNumber);
  }
}
