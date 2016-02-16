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
package ca.bc.gov.open.cpf.api.scheduler;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.util.StopWatch;

import com.revolsys.collection.set.Sets;
import com.revolsys.identifier.Identifier;
import com.revolsys.io.PathName;
import com.revolsys.util.Dates;
import com.revolsys.util.JavaBeanUtil;

public class BusinessApplicationStatistics {
  public static final PathName APPLICATION_STATISTICS = PathName
    .newPathName("/CPF/CPF_APPLICATION_STATISTICS");

  public static final String BUSINESS_APPLICATION_NAME = "BUSINESS_APPLICATION_NAME";

  public static final String DAY = "day";

  public static final String DURATION_TYPE = "DURATION_TYPE";

  public static final String HOUR = "hour";

  public static final String MONTH = "month";

  public static final String START_TIMESTAMP = "START_TIMESTAMP";

  public static final String STATISTIC_VALUES = "STATISTIC_VALUES";

  public static final String YEAR = "year";

  public static final Set<String> MONTH_OR_YEAR = Sets.newHash(MONTH, YEAR);

  public static final List<String> DURATION_TYPES = Arrays.asList(HOUR, DAY, MONTH, YEAR);

  public static final List<String> STATISTIC_NAMES = Arrays.asList(
    "applicationExecutedFailedRequestsCount", "applicationExecutedGroupsCount",
    "applicationExecutedRequestsCount", "applicationExecutedTime", "completedFailedRequestsCount",
    "completedJobsCount", "completedRequestsCount", "completedTime", "executedGroupsCount",
    "executedRequestsCount", "executedTime", "postProcessedJobsCount", "postProcessedRequestsCount",
    "postProcessedTime", "preProcessedJobsCount", "preProcessedRequestsCount", "preProcessedTime",
    "submittedJobsCount", "submittedJobsTime");

  public static final String APPLICATION_STATISTIC_ID = "APPLICATION_STATISTIC_ID";

  public static String getId(final String durationType) {
    final Date date = new Date();
    return getId(durationType, date);
  }

  public static String getId(final String durationType, final Date date) {
    String pattern;
    if (durationType.equals(HOUR)) {
      pattern = "yyyy-MM-dd-HH";
    } else {
      if (durationType.equals(DAY)) {
        pattern = "yyyy-MM-dd";
      } else {
        if (durationType.equals(MONTH)) {
          pattern = "yyyy-MM";
        } else {
          if (durationType.equals(YEAR)) {
            pattern = "yyyy";
          } else {
            throw new IllegalArgumentException("Invalid duration type : " + durationType);
          }
        }
      }
    }
    return Dates.format(pattern, date);
  }

  public static BusinessApplicationStatistics newStatistics(final String businessApplicationName,
    final String durationType) {
    final String id = getId(durationType);
    return new BusinessApplicationStatistics(businessApplicationName, id);
  }

  private long applicationExecutedFailedRequestsCount;

  private long applicationExecutedGroupsCount;

  private long applicationExecutedRequestsCount;

  private long applicationExecutedTime;

  private final String businessApplicationName;

  private Identifier databaseId;

  private long completedFailedRequestsCount;

  private long completedJobsCount;

  private long completedRequestsCount;

  private long completedTime;

  private String durationType;

  private Date endTime;

  private long executedGroupsCount;

  private long executedRequestsCount;

  private long executedTime;

  private String id;

  private long postProcessedJobsCount;

  private long postProcessedRequestsCount;

  private long postProcessedTime;

  private long preProcessedJobsCount;

  private long preProcessedRequestsCount;

  private long preProcessedTime;

  private Date startTime;

  private long submittedJobsCount;

  private long submittedJobsTime;

  private final String dateString;

  private boolean modified;

  public BusinessApplicationStatistics(final String businessApplicationName, final String id) {
    String durationType;
    final String dateString = id;
    String pattern;
    final int length = id.length();
    if (length == 13) {
      pattern = "yyyy-MM-dd-HH";
      durationType = HOUR;
    } else {
      if (length == 10) {
        pattern = "yyyy-MM-dd";
        durationType = DAY;
      } else {
        if (length == 7) {
          pattern = "yyyy-MM";
          durationType = MONTH;
        } else {
          if (length == 4) {
            pattern = "yyyy";
            durationType = YEAR;
          } else {
            throw new IllegalArgumentException("Invalid ID : " + id);
          }
        }
      }
    }
    final Date startTime = Dates.getDate(pattern, dateString);
    this.businessApplicationName = businessApplicationName;
    this.durationType = durationType;
    final Calendar calendar = new GregorianCalendar();
    calendar.setTime(startTime);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    int incrementField;
    if (durationType.equals(HOUR)) {
      incrementField = Calendar.HOUR;
      pattern = "yyyy-MM-dd-HH";
    } else {
      calendar.set(Calendar.HOUR, 1);
      if (durationType.equals(DAY)) {
        incrementField = Calendar.DAY_OF_MONTH;
        pattern = "yyyy-MM-dd";
      } else {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        if (durationType.equals(MONTH)) {
          incrementField = Calendar.MONTH;
          pattern = "yyyy-MM";
        } else {
          calendar.set(Calendar.MONTH, 1);
          if (durationType.equals(YEAR)) {
            incrementField = Calendar.YEAR;
            pattern = "yyyy";
          } else {
            throw new IllegalArgumentException("Invalid duration type : " + durationType);
          }
        }
      }
    }
    this.startTime = calendar.getTime();
    calendar.add(incrementField, 1);
    this.endTime = calendar.getTime();
    this.dateString = Dates.format(pattern, startTime);
    this.id = dateString;
    this.modified = false;
  }

  public long addStatistic(final String statisticName, final Long value) {
    if (STATISTIC_NAMES.contains(statisticName)) {
      Long totalValue = JavaBeanUtil.getProperty(this, statisticName);
      if (value > 0) {
        totalValue += value;
        JavaBeanUtil.setProperty(this, statisticName, totalValue);
        this.modified = true;
      }
      return totalValue;
    } else {
      return 0;
    }
  }

  public void addStatistic(final String name, final Object value) {
    if (value instanceof Number) {
      final Number number = (Number)value;
      addStatistic(name, number.longValue());
    } else if (value instanceof StopWatch) {
      final StopWatch stopWatch = (StopWatch)value;
      try {
        try {
          if (stopWatch.isRunning()) {
            stopWatch.stop();
          }
        } catch (final IllegalStateException e) {
        }
        final long time = stopWatch.getTotalTimeMillis();
        addStatistic(name, time);
      } catch (final IllegalStateException e) {
      }
    }
  }

  public void addStatistics(final BusinessApplicationStatistics savedStatistics) {
    for (final String name : STATISTIC_NAMES) {
      final Long value = JavaBeanUtil.getProperty(savedStatistics, name);
      if (value > 0) {
        addStatistic(name, value);
      }
    }
  }

  public void addStatistics(final Map<String, ? extends Object> values) {
    for (final Entry<String, ? extends Object> entry : values.entrySet()) {
      final String name = entry.getKey();
      final Object value = entry.getValue();
      addStatistic(name, value);
    }
  }

  public boolean containsPeriod(final BusinessApplicationStatistics statistics) {
    return statistics.dateString.startsWith(this.dateString);
  }

  private String formatTime(final long time) {
    final long milliseconds = time % 1000;
    final long seconds = time / 1000 % 60;
    final long minutes = time / (60 * 1000) % 60;
    final long hours = time / (60 * 60 * 1000);
    final StringBuilder s = new StringBuilder();
    if (hours < 10) {
      s.append("0");
    }
    s.append(hours);
    s.append(":");
    if (minutes < 10) {
      s.append("0");
    }
    s.append(minutes);
    s.append(":");
    if (seconds < 10) {
      s.append("0");
    }
    s.append(seconds);
    s.append(".");
    if (milliseconds < 100) {
      s.append("0");
    }
    if (milliseconds < 10) {
      s.append("0");
    }
    s.append(milliseconds);
    return s.toString();
  }

  public long getApplicationExecutedFailedRequestsCount() {
    return this.applicationExecutedFailedRequestsCount;
  }

  public long getApplicationExecutedGroupsAverageTime() {
    if (this.applicationExecutedGroupsCount == 0) {
      return 0;
    } else {
      return this.applicationExecutedTime / this.applicationExecutedGroupsCount;
    }
  }

  public String getApplicationExecutedGroupsAverageTimeFormatted() {
    return formatTime(getApplicationExecutedGroupsAverageTime());
  }

  public long getApplicationExecutedGroupsCount() {
    return this.applicationExecutedGroupsCount;
  }

  public long getApplicationExecutedRequestsAverageTime() {
    if (this.applicationExecutedRequestsCount == 0) {
      return 0;
    } else {
      return this.applicationExecutedTime / this.applicationExecutedRequestsCount;
    }
  }

  public String getApplicationExecutedRequestsAverageTimeFormatted() {
    return formatTime(getApplicationExecutedRequestsAverageTime());
  }

  public long getApplicationExecutedRequestsCount() {
    return this.applicationExecutedRequestsCount;
  }

  public long getApplicationExecutedTime() {
    return this.applicationExecutedTime;
  }

  public String getApplicationExecutedTimeFormatted() {
    return formatTime(getApplicationExecutedTime());
  }

  public String getBusinessApplicationName() {
    return this.businessApplicationName;
  }

  public long getCompletedFailedRequestsCount() {
    return this.completedFailedRequestsCount;
  }

  public long getCompletedJobsAverageTime() {
    if (this.completedJobsCount == 0) {
      return 0;
    } else {
      return this.completedTime / this.completedJobsCount;
    }
  }

  public String getCompletedJobsAverageTimeFormatted() {
    return formatTime(getCompletedJobsAverageTime());
  }

  public long getCompletedJobsCount() {
    return this.completedJobsCount;
  }

  public long getCompletedRequestsAverageTime() {
    if (this.completedRequestsCount == 0) {
      return 0;
    } else {
      return this.completedTime / this.completedRequestsCount;
    }
  }

  public String getCompletedRequestsAverageTimeFormatted() {
    return formatTime(getCompletedRequestsAverageTime());
  }

  public long getCompletedRequestsCount() {
    return this.completedRequestsCount;
  }

  public long getCompletedTime() {
    return this.completedTime;
  }

  public String getCompletedTimeFormatted() {
    return formatTime(getCompletedTime());
  }

  public Identifier getDatabaseId() {
    return this.databaseId;
  }

  public String getDurationType() {
    return this.durationType;
  }

  public Date getEndTime() {
    return this.endTime;
  }

  public long getExecutedGroupsAverageTime() {
    if (this.executedGroupsCount == 0) {
      return 0;
    } else {
      return this.executedTime / this.executedGroupsCount;
    }
  }

  public String getExecutedGroupsAverageTimeFormatted() {
    return formatTime(getExecutedGroupsAverageTime());
  }

  public long getExecutedGroupsCount() {
    return this.executedGroupsCount;
  }

  public long getExecutedRequestsAverageTime() {
    if (this.executedRequestsCount == 0) {
      return 0;
    } else {
      return this.executedTime / this.executedRequestsCount;
    }
  }

  public String getExecutedRequestsAverageTimeFormatted() {
    return formatTime(getExecutedRequestsAverageTime());
  }

  public long getExecutedRequestsCount() {
    return this.executedRequestsCount;
  }

  public long getExecutedTime() {
    return this.executedTime;
  }

  public String getExecutedTimeFormatted() {
    return formatTime(getExecutedTime());
  }

  public String getId() {
    return this.id;
  }

  public String getParentDurationType() {
    if (this.durationType.equals(HOUR)) {
      return DAY;
    } else if (this.durationType.equals(DAY)) {
      return MONTH;
    } else if (this.durationType.equals(MONTH)) {
      return YEAR;
    } else {
      return null;
    }
  }

  public String getParentId() {
    final int dashIndex = this.id.lastIndexOf('-');
    if (dashIndex == -1) {
      return null;
    } else {
      return this.id.substring(0, dashIndex);
    }
  }

  public long getPostProcessedJobsAverageTime() {
    if (this.postProcessedJobsCount == 0) {
      return 0;
    } else {
      return this.postProcessedTime / this.postProcessedJobsCount;
    }
  }

  public String getPostProcessedJobsAverageTimeFormatted() {
    return formatTime(getPostProcessedJobsAverageTime());
  }

  public long getPostProcessedJobsCount() {
    return this.postProcessedJobsCount;
  }

  public long getPostProcessedRequestsAverageTime() {
    if (this.postProcessedRequestsCount == 0) {
      return 0;
    } else {
      return this.postProcessedTime / this.postProcessedRequestsCount;
    }
  }

  public String getPostProcessedRequestsAverageTimeFormatted() {
    return formatTime(getPostProcessedRequestsAverageTime());
  }

  public long getPostProcessedRequestsCount() {
    return this.postProcessedRequestsCount;
  }

  public long getPostProcessedTime() {
    return this.postProcessedTime;
  }

  public String getPostProcessedTimeFormatted() {
    return formatTime(getPostProcessedTime());
  }

  public long getPreProcessedJobsAverageTime() {
    if (this.preProcessedJobsCount == 0) {
      return 0;
    } else {
      return this.preProcessedTime / this.preProcessedJobsCount;
    }
  }

  public String getPreProcessedJobsAverageTimeFormatted() {
    return formatTime(getPreProcessedJobsAverageTime());
  }

  public long getPreProcessedJobsCount() {
    return this.preProcessedJobsCount;
  }

  public long getPreProcessedRequestsAverageTime() {
    if (this.preProcessedRequestsCount == 0) {
      return 0;
    } else {
      return this.preProcessedTime / this.preProcessedRequestsCount;
    }
  }

  public String getPreProcessedRequestsAverageTimeFormatted() {
    return formatTime(getPreProcessedRequestsAverageTime());
  }

  public long getPreProcessedRequestsCount() {
    return this.preProcessedRequestsCount;
  }

  public long getPreProcessedTime() {
    return this.preProcessedTime;
  }

  public String getPreProcessedTimeFormatted() {
    return formatTime(getPreProcessedTime());
  }

  public Date getStartTime() {
    return this.startTime;
  }

  public long getSubmittedJobsAverageTime() {
    if (this.submittedJobsCount == 0) {
      return 0;
    } else {
      return this.submittedJobsTime / this.submittedJobsCount;
    }
  }

  public String getSubmittedJobsAverageTimeFormatted() {
    return formatTime(getSubmittedJobsAverageTime());
  }

  public long getSubmittedJobsCount() {
    return this.submittedJobsCount;
  }

  public long getSubmittedJobsTime() {
    return this.submittedJobsTime;
  }

  public String getSubmittedJobsTimeFormatted() {
    return formatTime(getSubmittedJobsTime());
  }

  public boolean isModified() {
    return this.modified;
  }

  public void setApplicationExecutedFailedRequestsCount(
    final long applicationExecutedFailedRequestsCount) {
    this.applicationExecutedFailedRequestsCount = applicationExecutedFailedRequestsCount;
  }

  public void setApplicationExecutedGroupsCount(final long applicationExecutedGroupsCount) {
    this.applicationExecutedGroupsCount = applicationExecutedGroupsCount;
  }

  public void setApplicationExecutedRequestsCount(final long applicationExecutedRequestsCount) {
    this.applicationExecutedRequestsCount = applicationExecutedRequestsCount;
  }

  public void setApplicationExecutedTime(final long applicationExecutedTime) {
    this.applicationExecutedTime = applicationExecutedTime;
  }

  public void setCompletedFailedRequestsCount(final long completedFailedRequestsCount) {
    this.completedFailedRequestsCount = completedFailedRequestsCount;
  }

  public void setCompletedJobsCount(final long completedJobsCount) {
    this.completedJobsCount = completedJobsCount;
  }

  public void setCompletedRequestsCount(final long completedRequestsCount) {
    this.completedRequestsCount = completedRequestsCount;
  }

  public void setCompletedTime(final long completedTime) {
    this.completedTime = completedTime;
  }

  public void setDatabaseId(final Identifier databaseId) {
    this.databaseId = databaseId;
  }

  public void setDurationType(final String durationType) {
    this.durationType = durationType;
  }

  public void setEndTime(final Date endTime) {
    this.endTime = endTime;
  }

  public void setExecutedGroupsCount(final long executedGroupsCount) {
    this.executedGroupsCount = executedGroupsCount;
  }

  public void setExecutedRequestsCount(final long executedRequestsCount) {
    this.executedRequestsCount = executedRequestsCount;
  }

  public void setExecutedTime(final long executedTime) {
    this.executedTime = executedTime;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setModified(final boolean modified) {
    this.modified = modified;
  }

  public void setPostProcessedJobsCount(final long postProcessedJobsCount) {
    this.postProcessedJobsCount = postProcessedJobsCount;
  }

  public void setPostProcessedRequestsCount(final long postProcessedRequestsCount) {
    this.postProcessedRequestsCount = postProcessedRequestsCount;
  }

  public void setPostProcessedTime(final long postProcessedTime) {
    this.postProcessedTime = postProcessedTime;
  }

  public void setPreProcessedJobsCount(final long preProcessedJobsCount) {
    this.preProcessedJobsCount = preProcessedJobsCount;
  }

  public void setPreProcessedRequestsCount(final long preProcessedRequestsCount) {
    this.preProcessedRequestsCount = preProcessedRequestsCount;
  }

  public void setPreProcessedTime(final long preProcessedTime) {
    this.preProcessedTime = preProcessedTime;
  }

  public void setStartTime(final Date startTime) {
    this.startTime = startTime;
  }

  public void setSubmittedJobsCount(final long submittedJobsCount) {
    this.submittedJobsCount = submittedJobsCount;
  }

  public void setSubmittedJobsTime(final long submittedJobsTime) {
    this.submittedJobsTime = submittedJobsTime;
  }

  public Map<String, Long> toMap() {
    final Map<String, Long> statistics = new TreeMap<String, Long>();
    for (final String name : STATISTIC_NAMES) {
      final Long value = JavaBeanUtil.getProperty(this, name);
      if (value > 0) {
        statistics.put(name, value);
      }
    }
    return statistics;
  }

  @Override
  public String toString() {
    return this.id + ": " + toMap();
  }

}
