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
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.io.FileUtil;
import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;

public class DatabasePreProcessGroup extends PreProcessGroup {
  private Transaction transaction;

  public DatabasePreProcessGroup(final JobPreProcessTask preProcess,
    final JobController jobController, final BusinessApplication businessApplication,
    final BatchJob batchJob, final Map<String, String> jobParameters,
    final int groupSequenceNumber) {
    super(preProcess, jobController, businessApplication, batchJob, jobParameters,
      groupSequenceNumber);
  }

  @Override
  public void cancel() {
    super.cancel();
    if (this.transaction != null) {
      this.transaction.setRollbackOnly();
      this.transaction.close();
    }
  }

  @Override
  public void commit() {
    try {
      super.commit();
    } catch (final Throwable e) {
      if (this.transaction != null) {
        this.transaction.setRollbackOnly(e);
      }
    } finally {
      if (this.transaction != null) {
        this.transaction.close();
      }
    }

  }

  @Override
  protected File newGroupFile() {
    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    this.transaction = dataAccessObject.newTransaction(Propagation.REQUIRES_NEW);
    return FileUtil.newTempFile(getBatchJobId() + "-group", ".tsv");
  }
}
