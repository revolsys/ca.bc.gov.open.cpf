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
 
 $URL:$
 $Author:$
 $Date:$
 $Revision:$
 */
package ca.bc.gov.open.cpf.api.scheduler.spi.adminjobs;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;

/**
 * Periodically delete Batch Jobs older than the specified number of days, along
 * with all the Batch Jobs associated request and result data.
 */
public class RemoveOldBatchJobs {

  private static final Logger LOG = LoggerFactory.getLogger(RemoveOldBatchJobs.class);

  private CpfDataAccessObject dataAccessObject;

  /** The Number of days for which old jobs should be kept. */
  private int daysToKeepOldJobs = 7;

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public void removeOldJobs() {

    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    final int dayInMilliseconds = 1000 * 60 * 60 * 24;

    final Calendar cal = new GregorianCalendar(); // local time
    final long timeNow = cal.getTimeInMillis();
    final long timeAtMidnightThisMorning = timeNow
      - (timeNow % dayInMilliseconds);
    final long timeXDaysAgo = timeAtMidnightThisMorning
      - (daysToKeepOldJobs * dayInMilliseconds);
    final Timestamp keepUntilTimestamp = new Timestamp(timeXDaysAgo);
    cal.setTimeInMillis(timeXDaysAgo);
    cal.add(Calendar.MILLISECOND,
      -TimeZone.getDefault().getOffset(cal.getTimeInMillis()));

    int numberJobsDeleted = 0;
    final List<Long> batchJobIds = dataAccessObject.getOldBatchJobIds(keepUntilTimestamp);
    for (final Long batchJobId : batchJobIds) {
      try {
        dataAccessObject.deleteBatchJob(batchJobId);
        numberJobsDeleted++;
      } catch (final Throwable t) {
        LOG.error("Unable to delete Batch Job " + batchJobId, t);
      }
    }

    if (numberJobsDeleted > 0) {
      LOG.info(numberJobsDeleted + " old batch jobs deleted for jobs prior to "
        + sdf.format(cal.getTime()));
    }
  }

  @Resource(name = "cpfDataAccessObject")
  public void setDataAccessObject(final CpfDataAccessObject dataAccessObject) {
    this.dataAccessObject = dataAccessObject;
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
    this.daysToKeepOldJobs = daysToKeepOldJobs;
  }

}