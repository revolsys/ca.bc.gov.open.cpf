<%@ page contentType="text/html; charset=UTF-8" session="false" pageEncoding="UTF-8"%><%@
  taglib
  uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><%@
  taglib
  uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<div class="jqueryTabs">
  <ul>
    <li><a href="#stats">Business Application Statistics</a></li>
  </ul>
  <div id="stats">
    <div class="objectView">
      <table class="data">
        <tbody>
          <tr>
            <th>Duration Type</th>
            <td><c:out value="${statisitcs.durationType}" /></td>
          </tr>
          <tr>
            <th>Start Time</th>
            <td><fmt:formatDate pattern="yyyy-MM-dd kk:mm:ss" value="${statisitcs.startTime}" /></td>
          </tr>
          <tr>
            <th>End Time</th>
            <td><fmt:formatDate pattern="yyyy-MM-dd kk:mm:ss" value="${statisitcs.endTime}" /></td>
          </tr>
          <tr>
            <th>Jobs Completed</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Jobs</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.completedJobsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.completedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.completedJobsCount}" /></td>
                    </tr>
                    <tr>
                      <th>Requests</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.completedRequestsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.completedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.completedRequestsCount}" />
                        <c:if test="${statisitcs.completedFailedRequestsCount != 0}">
                          <span style="color: red; font-weight: bold">(<c:out
                              value="${statisitcs.completedFailedRequestsCount}" />)
                          </span>
                        </c:if></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Jobs Submitted</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Jobs</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.submittedJobsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.submittedJobsTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.submittedJobsCount}" /></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Jobs Pre-Process Scheduled</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Jobs</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessScheduledJobsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessScheduledJobsTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessScheduledJobsCount}" /></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Jobs Pre-Processed</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Jobs</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessedJobsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessedJobsCount}" /></td>
                    </tr>
                    <tr>
                      <th>Requests</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessedRequestsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.preProcessedRequestsCount}" /></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Job Request Groups Scheduled</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Groups</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executeScheduledGroupsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executeScheduledTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executeScheduledGroupsCount}" /></td>
                    </tr>
                    <tr>
                      <th>Requests</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executeScheduledRequestsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executeScheduledTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executeScheduledRequestsCount}" /></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Job Request Groups Executed</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Groups</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executedGroupsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executedGroupsCount}" /></td>
                    </tr>
                    <tr>
                      <th>Requests</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executedRequestsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.executedRequestsCount}" /></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Job Request Groups Application Executed</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Groups</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.applicationExecutedGroupsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.applicationExecutedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.applicationExecutedGroupsCount}" /></td>
                    </tr>
                    <tr>
                      <th>Requests</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.applicationExecutedRequestsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.applicationExecutedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.applicationExecutedRequestsCount}" />
                        <c:if test="${statisitcs.applicationExecutedFailedRequestsCount != 0}">
                          <span style="color: red; font-weight: bold">(<c:out
                              value="${statisitcs.applicationExecutedFailedRequestsCount}" />)
                          </span>
                        </c:if></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Jobs Post-Process Scheduled</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Jobs</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessScheduledJobsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessScheduledJobsTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessScheduledJobsCount}" /></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
          <tr>
            <th>Jobs Post-Processed</th>
            <td><div class="simpleDataTable">
                <table class="data">
                  <thead>
                    <tr>
                      <th style="width: 100px">&nbsp;</th>
                      <th style="width: 100px">Avg Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Total Time (HH:MM:SS.SSS)</th>
                      <th style="width: 100px">Count</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <th>Jobs</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessedJobsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessedJobsCount}" /></td>
                    </tr>
                    <tr>
                      <th>Requests</th>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessedRequestsAverageTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessedTimeFormatted}" /></td>
                      <td style="text-align: right"><c:out
                          value="${statisitcs.postProcessedRequestsCount}" /></td>
                    </tr>
                  </tbody>
                </table></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>

