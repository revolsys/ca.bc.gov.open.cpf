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
package ca.bc.gov.open.cpf.api.scheduler.spi.adminjobs;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;

import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.util.DateUtil;

/**
 * Periodically delete Batch Jobs older than the specified number of days, along
 * with all the Batch Jobs associated request and result data.
 */
public class RemoveOldBatchJobs {

  private BatchJobService batchJobService;

  private CpfDataAccessObject dataAccessObject;

  public void removeOldJobs() {
    try (
        Transaction transaction = this.dataAccessObject.createTransaction(Propagation.REQUIRES_NEW)) {
      try {
        final int dayInMilliseconds = 1000 * 60 * 60 * 24;

        final Calendar cal = new GregorianCalendar(); // local time
        final long timeNow = cal.getTimeInMillis();
        final long timeAtMidnightThisMorning = timeNow - timeNow
          % dayInMilliseconds;
        final long timeXDaysAgo = timeAtMidnightThisMorning
            - this.batchJobService.getDaysToKeepOldJobs() * dayInMilliseconds;
        final Timestamp keepUntilTimestamp = new Timestamp(timeXDaysAgo);
        cal.setTimeInMillis(timeXDaysAgo);
        cal.add(Calendar.MILLISECOND,
          -TimeZone.getDefault().getOffset(cal.getTimeInMillis()));

        int numberJobsDeleted = 0;
        final List<Long> batchJobIds = this.dataAccessObject.getOldBatchJobIds(keepUntilTimestamp);
        for (final Long batchJobId : batchJobIds) {
          try {
            this.batchJobService.deleteJob(batchJobId);
            numberJobsDeleted++;
          } catch (final Throwable t) {
            LoggerFactory.getLogger(getClass()).error(
              "Unable to delete Batch Job " + batchJobId, t);
          }
        }

        if (numberJobsDeleted > 0) {
          LoggerFactory.getLogger(getClass()).info(
            numberJobsDeleted + " old batch jobs deleted for jobs prior to "
                + DateUtil.format("yyyy-MMM-dd HH:mm:ss", cal.getTime()));
        }
      } catch (final Throwable e) {
        throw transaction.setRollbackOnly(e);
      }
    }
  }

  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
    this.dataAccessObject = batchJobService.getDataAccessObject();
  }

  /**
   * Set the number of days for which old jobs should be kept. Batch Jobs older
   * than this number of days will be periodically deleted, along with their
   * associated request and result data.
   *
   * @param daysToKeepOldJobs The Number of days for which old jobs should be
   *          kept.
   */
  public void setDaysToKeepOldJobs(final int daysToKeepOldJobs) {
    this.batchJobService.setDaysToKeepOldJobs(daysToKeepOldJobs);
  }

}
