package ca.bc.gov.open.cpf.api.scheduler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.springframework.util.StopWatch;

import com.revolsys.util.JavaBeanUtil;

public class BusinessApplicationStatistics {
  public static final String APPLICATION_STATISTICS = "/CPF/CPF_APPLICATION_STATISTICS";

  public static final String BUSINESS_APPLICATION_NAME = "BUSINESS_APPLICATION_NAME";

  public static final String DAY = "day";

  private static final DateFormat DAY_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd");

  public static final String DURATION_TYPE = "DURATION_TYPE";

  public static final String HOUR = "hour";

  private static final DateFormat HOUR_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd-HH");

  public static final String MONTH = "month";

  private static final DateFormat MONTH_FORMAT = new SimpleDateFormat("yyyy-MM");

  public static final String START_TIMESTAMP = "START_TIMESTAMP";

  public static final String STATISTIC_VALUES = "STATISTIC_VALUES";

  public static final String YEAR = "year";

  private static final DateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");

  public static final List<String> DURATION_TYPES = Arrays.asList(HOUR, DAY,
    MONTH, YEAR);

  public static final List<String> STATISTIC_NAMES = Arrays.asList(
    "applicationExecutedFailedRequestsCount", "applicationExecutedGroupsCount",
    "applicationExecutedRequestsCount", "applicationExecutedTime",
    "completedFailedRequestsCount", "completedJobsCount",
    "completedRequestsCount", "completedTime", "executedGroupsCount",
    "executedRequestsCount", "executedTime", "executeScheduledGroupsCount",
    "executeScheduledRequestsCount", "executeScheduledTime",
    "postProcessedJobsCount", "postProcessedRequestsCount",
    "postProcessedTime", "postProcessScheduledJobsCount",
    "postProcessScheduledJobsTime", "preProcessedJobsCount",
    "preProcessedRequestsCount", "preProcessedTime",
    "preProcessScheduledJobsCount", "preProcessScheduledJobsTime",
    "submittedJobsCount", "submittedJobsTime");

  public static final String APPLICATION_STATISTIC_ID = "APPLICATION_STATISTIC_ID";

  public static BusinessApplicationStatistics createStatistics(
    final String businessApplicationName, final String durationType) {
    final String id = getId(durationType);
    return new BusinessApplicationStatistics(businessApplicationName, id);
  }

  public static String getId(final String durationType) {
    final Date date = new Date();
    return getId(durationType, date);
  }

  public static String getId(final String durationType, final Date date) {
    DateFormat dateFormat;
    if (durationType.equals(HOUR)) {
      dateFormat = HOUR_FORMAT;
    } else {
      if (durationType.equals(DAY)) {
        dateFormat = DAY_FORMAT;
      } else {
        if (durationType.equals(MONTH)) {
          dateFormat = MONTH_FORMAT;
        } else {
          if (durationType.equals(YEAR)) {
            dateFormat = YEAR_FORMAT;
          } else {
            throw new IllegalArgumentException("Invalid duration type : "
              + durationType);
          }
        }
      }
    }
    return dateFormat.format(date);
  }

  private long applicationExecutedFailedRequestsCount;

  private long applicationExecutedGroupsCount;

  private long applicationExecutedRequestsCount;

  private long applicationExecutedTime;

  private String businessApplicationName;

  private Integer databaseId;

  private long completedFailedRequestsCount;

  private long completedJobsCount;

  private long completedRequestsCount;

  private long completedTime;

  private String durationType;

  private Date endTime;

  private long executedGroupsCount;

  private long executedRequestsCount;

  private long executedTime;

  private long executeScheduledGroupsCount;

  private long executeScheduledRequestsCount;

  private long executeScheduledTime;

  private String id;

  private long postProcessedJobsCount;

  private long postProcessedRequestsCount;

  private long postProcessedTime;

  private long postProcessScheduledJobsCount;

  private long postProcessScheduledJobsTime;

  private long preProcessedJobsCount;

  private long preProcessedRequestsCount;

  private long preProcessedTime;

  private long preProcessScheduledJobsCount;

  private long preProcessScheduledJobsTime;

  private Date startTime;

  private long submittedJobsCount;

  private long submittedJobsTime;

  private String dateString;

  private boolean modified;

  public BusinessApplicationStatistics(final String businessApplicationName,
    final String id) {
    String durationType;
    final String dateString = id;
    DateFormat dateFormat;
    final int length = id.length();
    if (length == 13) {
      dateFormat = HOUR_FORMAT;
      durationType = HOUR;
    } else {
      if (length == 10) {
        dateFormat = DAY_FORMAT;
        durationType = DAY;
      } else {
        if (length == 7) {
          dateFormat = MONTH_FORMAT;
          durationType = MONTH;
        } else {
          if (length == 4) {
            dateFormat = YEAR_FORMAT;
            durationType = YEAR;
          } else {
            throw new IllegalArgumentException("Invalid ID : " + id);
          }
        }
      }
    }
    try {
      final Date startTime = dateFormat.parse(dateString);
      this.businessApplicationName = businessApplicationName;
      this.durationType = durationType;
      final Calendar calendar = new GregorianCalendar();
      calendar.setTime(startTime);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      int incrementField;
      DateFormat dateFormat1;
      if (durationType.equals(HOUR)) {
        incrementField = Calendar.HOUR;
        dateFormat1 = HOUR_FORMAT;
      } else {
        calendar.set(Calendar.HOUR, 1);
        if (durationType.equals(DAY)) {
          incrementField = Calendar.DAY_OF_MONTH;
          dateFormat1 = DAY_FORMAT;
        } else {
          calendar.set(Calendar.DAY_OF_MONTH, 1);
          if (durationType.equals(MONTH)) {
            incrementField = Calendar.MONTH;
            dateFormat1 = MONTH_FORMAT;
          } else {
            calendar.set(Calendar.MONTH, 1);
            if (durationType.equals(YEAR)) {
              incrementField = Calendar.YEAR;
              dateFormat1 = YEAR_FORMAT;
            } else {
              throw new IllegalArgumentException("Invalid duration type : "
                + durationType);
            }
          }
        }
      }
      this.startTime = calendar.getTime();
      calendar.add(incrementField, 1);
      this.endTime = calendar.getTime();
      this.dateString = dateFormat1.format(startTime);
      this.id = dateString;
      this.modified = false;
    } catch (final ParseException e) {
      throw new IllegalArgumentException("Invalid date: " + dateString, e);
    }
  }

  public long addStatistic(final String statisticName, final Long value) {
    Long totalValue = JavaBeanUtil.getProperty(this, statisticName);
    if (value > 0) {
      totalValue += value;
      JavaBeanUtil.setProperty(this, statisticName, totalValue);
      modified = true;
    }
    return totalValue;
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
        } catch (IllegalStateException e) {
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
    return statistics.dateString.startsWith(dateString);
  }

  private String formatTime(final long time) {
    final long milliseconds = time % 1000;
    final long seconds = (time / 1000) % 60;
    final long minutes = (time / (60 * 1000)) % 60;
    final long hours = time / (60 * 60 * 1000);
    final StringBuffer s = new StringBuffer();
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
    return applicationExecutedFailedRequestsCount;
  }

  public long getApplicationExecutedGroupsAverageTime() {
    if (applicationExecutedGroupsCount == 0) {
      return 0;
    } else {
      return applicationExecutedTime / applicationExecutedGroupsCount;
    }
  }

  public String getApplicationExecutedGroupsAverageTimeFormatted() {
    return formatTime(getApplicationExecutedGroupsAverageTime());
  }

  public long getApplicationExecutedGroupsCount() {
    return applicationExecutedGroupsCount;
  }

  public long getApplicationExecutedRequestsAverageTime() {
    if (applicationExecutedRequestsCount == 0) {
      return 0;
    } else {
      return applicationExecutedTime / applicationExecutedRequestsCount;
    }
  }

  public String getApplicationExecutedRequestsAverageTimeFormatted() {
    return formatTime(getApplicationExecutedRequestsAverageTime());
  }

  public long getApplicationExecutedRequestsCount() {
    return applicationExecutedRequestsCount;
  }

  public long getApplicationExecutedTime() {
    return applicationExecutedTime;
  }

  public String getApplicationExecutedTimeFormatted() {
    return formatTime(getApplicationExecutedTime());
  }

  public String getBusinessApplicationName() {
    return businessApplicationName;
  }

  public long getCompletedFailedRequestsCount() {
    return completedFailedRequestsCount;
  }

  public long getCompletedJobsAverageTime() {
    if (completedJobsCount == 0) {
      return 0;
    } else {
      return completedTime / completedJobsCount;
    }
  }

  public String getCompletedJobsAverageTimeFormatted() {
    return formatTime(getCompletedJobsAverageTime());
  }

  public long getCompletedJobsCount() {
    return completedJobsCount;
  }

  public long getCompletedRequestsAverageTime() {
    if (completedRequestsCount == 0) {
      return 0;
    } else {
      return completedTime / completedRequestsCount;
    }
  }

  public String getCompletedRequestsAverageTimeFormatted() {
    return formatTime(getCompletedRequestsAverageTime());
  }

  public long getCompletedRequestsCount() {
    return completedRequestsCount;
  }

  public long getCompletedTime() {
    return completedTime;
  }

  public String getCompletedTimeFormatted() {
    return formatTime(getCompletedTime());
  }

  public Integer getDatabaseId() {
    return databaseId;
  }

  public String getDurationType() {
    return durationType;
  }

  public Date getEndTime() {
    return endTime;
  }

  public long getExecutedGroupsAverageTime() {
    if (executedGroupsCount == 0) {
      return 0;
    } else {
      return executedTime / executedGroupsCount;
    }
  }

  public String getExecutedGroupsAverageTimeFormatted() {
    return formatTime(getExecutedGroupsAverageTime());
  }

  public long getExecutedGroupsCount() {
    return executedGroupsCount;
  }

  public long getExecutedRequestsAverageTime() {
    if (executedRequestsCount == 0) {
      return 0;
    } else {
      return executedTime / executedRequestsCount;
    }
  }

  public String getExecutedRequestsAverageTimeFormatted() {
    return formatTime(getExecutedRequestsAverageTime());
  }

  public long getExecutedRequestsCount() {
    return executedRequestsCount;
  }

  public long getExecutedTime() {
    return executedTime;
  }

  public String getExecutedTimeFormatted() {
    return formatTime(getExecutedTime());
  }

  public long getExecuteScheduledGroupsAverageTime() {
    if (executeScheduledGroupsCount == 0) {
      return 0;
    } else {
      return executeScheduledTime / executeScheduledGroupsCount;
    }
  }

  public String getExecuteScheduledGroupsAverageTimeFormatted() {
    return formatTime(getExecuteScheduledGroupsAverageTime());
  }

  public long getExecuteScheduledGroupsCount() {
    return executeScheduledGroupsCount;
  }

  public long getExecuteScheduledRequestsAverageTime() {
    if (executeScheduledRequestsCount == 0) {
      return 0;
    } else {
      return executeScheduledTime / executeScheduledRequestsCount;
    }
  }

  public String getExecuteScheduledRequestsAverageTimeFormatted() {
    return formatTime(getExecuteScheduledRequestsAverageTime());
  }

  public long getExecuteScheduledRequestsCount() {
    return executeScheduledRequestsCount;
  }

  public long getExecuteScheduledTime() {
    return executeScheduledTime;
  }

  public String getExecuteScheduledTimeFormatted() {
    return formatTime(getExecuteScheduledTime());
  }

  public String getId() {
    return id;
  }

  public String getParentDurationType() {
    if (durationType.equals(HOUR)) {
      return DAY;
    } else if (durationType.equals(DAY)) {
      return MONTH;
    } else if (durationType.equals(MONTH)) {
      return YEAR;
    } else {
      return null;
    }
  }

  public String getParentId() {
    final int dashIndex = id.lastIndexOf('-');
    if (dashIndex == -1) {
      return null;
    } else {
      return id.substring(0, dashIndex);
    }
  }

  public long getPostProcessedJobsAverageTime() {
    if (postProcessedJobsCount == 0) {
      return 0;
    } else {
      return postProcessedTime / postProcessedJobsCount;
    }
  }

  public String getPostProcessedJobsAverageTimeFormatted() {
    return formatTime(getPostProcessedJobsAverageTime());
  }

  public long getPostProcessedJobsCount() {
    return postProcessedJobsCount;
  }

  public long getPostProcessedRequestsAverageTime() {
    if (postProcessedRequestsCount == 0) {
      return 0;
    } else {
      return postProcessedTime / postProcessedRequestsCount;
    }
  }

  public String getPostProcessedRequestsAverageTimeFormatted() {
    return formatTime(getPostProcessedRequestsAverageTime());
  }

  public long getPostProcessedRequestsCount() {
    return postProcessedRequestsCount;
  }

  public long getPostProcessedTime() {
    return postProcessedTime;
  }

  public String getPostProcessedTimeFormatted() {
    return formatTime(getPostProcessedTime());
  }

  public long getPostProcessScheduledJobsAverageTime() {
    if (postProcessScheduledJobsCount == 0) {
      return 0;
    } else {
      return postProcessScheduledJobsTime / postProcessScheduledJobsCount;
    }
  }

  public String getPostProcessScheduledJobsAverageTimeFormatted() {
    return formatTime(getPostProcessScheduledJobsAverageTime());
  }

  public long getPostProcessScheduledJobsCount() {
    return postProcessScheduledJobsCount;
  }

  public long getPostProcessScheduledJobsTime() {
    return postProcessScheduledJobsTime;
  }

  public String getPostProcessScheduledJobsTimeFormatted() {
    return formatTime(getPostProcessScheduledJobsTime());
  }

  public long getPreProcessedJobsAverageTime() {
    if (preProcessedJobsCount == 0) {
      return 0;
    } else {
      return preProcessedTime / preProcessedJobsCount;
    }
  }

  public String getPreProcessedJobsAverageTimeFormatted() {
    return formatTime(getPreProcessedJobsAverageTime());
  }

  public long getPreProcessedJobsCount() {
    return preProcessedJobsCount;
  }

  public long getPreProcessedRequestsAverageTime() {
    if (preProcessedRequestsCount == 0) {
      return 0;
    } else {
      return preProcessedTime / preProcessedRequestsCount;
    }
  }

  public String getPreProcessedRequestsAverageTimeFormatted() {
    return formatTime(getPreProcessedRequestsAverageTime());
  }

  public long getPreProcessedRequestsCount() {
    return preProcessedRequestsCount;
  }

  public long getPreProcessedTime() {
    return preProcessedTime;
  }

  public String getPreProcessedTimeFormatted() {
    return formatTime(getPreProcessedTime());
  }

  public long getPreProcessScheduledJobsAverageTime() {
    if (preProcessScheduledJobsCount == 0) {
      return 0;
    } else {
      return preProcessScheduledJobsTime / preProcessScheduledJobsCount;
    }
  }

  public String getPreProcessScheduledJobsAverageTimeFormatted() {
    return formatTime(getPreProcessScheduledJobsAverageTime());
  }

  public long getPreProcessScheduledJobsCount() {
    return preProcessScheduledJobsCount;
  }

  public long getPreProcessScheduledJobsTime() {
    return preProcessScheduledJobsTime;
  }

  public String getPreProcessScheduledJobsTimeFormatted() {
    return formatTime(getPreProcessScheduledJobsTime());
  }

  public Date getStartTime() {
    return startTime;
  }

  public long getSubmittedJobsAverageTime() {
    if (submittedJobsCount == 0) {
      return 0;
    } else {
      return submittedJobsTime / submittedJobsCount;
    }
  }

  public String getSubmittedJobsAverageTimeFormatted() {
    return formatTime(getSubmittedJobsAverageTime());
  }

  public long getSubmittedJobsCount() {
    return submittedJobsCount;
  }

  public long getSubmittedJobsTime() {
    return submittedJobsTime;
  }

  public String getSubmittedJobsTimeFormatted() {
    return formatTime(getSubmittedJobsTime());
  }

  public boolean isModified() {
    return modified;
  }

  public void setApplicationExecutedFailedRequestsCount(
    final long applicationExecutedFailedRequestsCount) {
    this.applicationExecutedFailedRequestsCount = applicationExecutedFailedRequestsCount;
  }

  public void setApplicationExecutedGroupsCount(
    final long applicationExecutedGroupsCount) {
    this.applicationExecutedGroupsCount = applicationExecutedGroupsCount;
  }

  public void setApplicationExecutedRequestsCount(
    final long applicationExecutedRequestsCount) {
    this.applicationExecutedRequestsCount = applicationExecutedRequestsCount;
  }

  public void setApplicationExecutedTime(final long applicationExecutedTime) {
    this.applicationExecutedTime = applicationExecutedTime;
  }

  public void setCompletedFailedRequestsCount(
    final long completedFailedRequestsCount) {
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

  public void setDatabaseId(final Integer databaseId) {
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

  public void setExecuteScheduledGroupsCount(
    final long executeScheduledGroupsCount) {
    this.executeScheduledGroupsCount = executeScheduledGroupsCount;
  }

  public void setExecuteScheduledRequestsCount(
    final long executeScheduledRequestsCount) {
    this.executeScheduledRequestsCount = executeScheduledRequestsCount;
  }

  public void setExecuteScheduledTime(final long executeScheduledTime) {
    this.executeScheduledTime = executeScheduledTime;
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

  public void setPostProcessedRequestsCount(
    final long postProcessedRequestsCount) {
    this.postProcessedRequestsCount = postProcessedRequestsCount;
  }

  public void setPostProcessedTime(final long postProcessedTime) {
    this.postProcessedTime = postProcessedTime;
  }

  public void setPostProcessScheduledJobsCount(
    final long postProcessScheduledJobsCount) {
    this.postProcessScheduledJobsCount = postProcessScheduledJobsCount;
  }

  public void setPostProcessScheduledJobsTime(
    final long postProcessScheduledJobsTime) {
    this.postProcessScheduledJobsTime = postProcessScheduledJobsTime;
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

  public void setPreProcessScheduledJobsCount(
    final long preProcessScheduledJobsCount) {
    this.preProcessScheduledJobsCount = preProcessScheduledJobsCount;
  }

  public void setPreProcessScheduledJobsTime(
    final long preProcessScheduledJobsTime) {
    this.preProcessScheduledJobsTime = preProcessScheduledJobsTime;
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
    return id + ": " + toMap();
  }

}
