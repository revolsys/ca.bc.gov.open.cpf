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
package ca.bc.gov.open.cpf.api.web.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobRequestExecutionGroup;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.Worker;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.collection.range.RangeSet;
import com.revolsys.io.FileUtil;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.io.StringPrinter;
import com.revolsys.record.Record;
import com.revolsys.util.UrlUtil;

@Controller
public class InternalWebService {
  private BatchJobService batchJobService;

  private CpfDataAccessObject dataAccessObject;

  @Resource
  private CpfConfig cpfConfig;

  private JobController jobController;

  private void checkRunning() {
    if (!this.batchJobService.isRunning()) {
      throw new IllegalStateException("Application is not running");
    }
  }

  @PreDestroy
  public void close() {
    this.batchJobService = null;
    this.dataAccessObject = null;
    this.jobController = null;
  }

  @RequestMapping("/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/requests/{sequenceNumber}/inputData")
  public void getBatchJobExecutionGroupOpaqueInputData(
    @PathVariable("workerId") final String workerId, final HttpServletResponse response,
    @PathVariable("batchJobId") final Long batchJobId,
    @PathVariable("sequenceNumber") final long sequenceNumber)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    // final Record batchJobExecutionGroup =
    // this.dataAccessObject.getBatchJobExecutionGroup(
    // batchJobId, sequenceNumber);
    // if (batchJobExecutionGroup == null) {
    // throw new
    // NoSuchRequestHandlingMethodException(HttpServletUtils.getRequest());
    // } else {
    // final Record batchJob = this.dataAccessObject.getBatchJob(batchJobId);
    // if (batchJob != null) {
    // final String businessApplicationName =
    // batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
    // final BusinessApplication businessApplication =
    // this.batchJobService.getBusinessApplication(businessApplicationName);
    // if (businessApplication == null ||
    // !businessApplication.isPerRequestInputData()) {
    // throw new
    // NoSuchRequestHandlingMethodException(HttpServletUtils.getRequest());
    // } else {
    // // TODO final String inputDataContentType =
    // //
    // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE);
    // // final String inputDataUrl =
    // //
    // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_URL);
    // // if (inputDataUrl != null) {
    // // response.setStatus(HttpServletResponse.SC_SEE_OTHER);
    // // response.setHeader("Location", inputDataUrl);
    // // return;
    // // } else {
    // // final Blob inputData =
    // // batchJobExecutionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA);
    // // if (inputData == null) {
    // // throw new
    // // NoSuchRequestHandlingMethodException(HttpServletUtils.getRequest());
    // // } else {
    // // try {
    // // response.setContentType(inputDataContentType);
    // // final InputStream in = inputData.getBinaryStream();
    // // final OutputStream out = response.getOutputStream();
    // // FileUtil.copy(in, out);
    // // return;
    // // } catch (final SQLException e) {
    // // LoggerFactory.getLogger(InternalWebService.class).error(
    // // "Unable to load data from database", e);
    // // throw new
    // // HttpMessageNotWritableException("Unable to load data from database",
    // // e);
    // // } catch (final IOException e) {
    // // LoggerFactory.getLogger(InternalWebService.class).error(
    // // "Unable to write blob to request", e);
    // // throw new
    // // HttpMessageNotWritableException("Unable to write blob to request",
    // // e);
    // // }
    // // }
    // // }
    // }
    // }
    // }
  }

  @RequestMapping(value = "/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}")
  @ResponseBody
  public Map<String, Object> getBatchJobRequestExecutionGroup(final HttpServletRequest request,
    @PathVariable("workerId") final String workerId, @PathVariable("groupId") final String groupId)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final BatchJobRequestExecutionGroup group = this.batchJobService.getBatchJobRequestExecutionGroup(
      workerId, groupId);
    if (group == null || group.isCancelled()) {
      return Collections.emptyMap();
    } else {
      final Long batchJobId = group.getBatchJobId();
      final Map<String, Object> groupSpecification = new LinkedHashMap<String, Object>();
      final BusinessApplication businessApplication = group.getBusinessApplication();
      groupSpecification.put("workerId", workerId);
      groupSpecification.put("groupId", groupId);
      groupSpecification.put("consumerKey", group.getconsumerKey());
      groupSpecification.put("batchJobId", batchJobId);
      final Module module = businessApplication.getModule();
      if (module == null || !module.isStarted()) {
        this.batchJobService.rescheduleGroup(group);
      } else {
        final String moduleName = module.getName();
        groupSpecification.put("moduleName", moduleName);
        if (module.isRemoteable()) {
          groupSpecification.put("moduleTime", module.getStartedTime());
        }
        groupSpecification.put("businessApplicationName", businessApplication.getName());
        groupSpecification.put("applicationParameters", group.getBusinessApplicationParameterMap());
        if (businessApplication.isPerRequestResultData()) {
          groupSpecification.put("resultDataContentType", group.getResultDataContentType());
        }

        final int groupSequenceNumber = group.getSequenceNumber();
        if (businessApplication.isPerRequestInputData()) {
          final List<Map<String, Object>> requestParameterList = new ArrayList<Map<String, Object>>();
          groupSpecification.put("requests", requestParameterList);
          final Map<String, Object> requestParameters = new HashMap<String, Object>();
          requestParameters.put("sequenceNumber", groupSequenceNumber);
          // TODO requestParameters.put("inputDataContentType",
          // executionGroup.getValue(BatchJobExecutionGroup.INPUT_DATA_CONTENT_TYPE));
          requestParameterList.add(requestParameters);
        } else {
          final String structuredInputData = this.jobController.getGroupInputString(batchJobId,
            groupSequenceNumber);
          if (structuredInputData == null) {
            return Collections.emptyMap();
          } else if (structuredInputData.charAt(0) == '{') {
            groupSpecification.put("requests", new StringPrinter(structuredInputData));
          } else {
            groupSpecification.put("requests", structuredInputData);
          }
        }
      }
      return groupSpecification;
    }
  }

  @RequestMapping(value = {
    "/worker/modules/{moduleName}/{moduleTime}/urls/{urlId}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public void getModuleUrl(final HttpServletRequest request, final HttpServletResponse response,
    @PathVariable("moduleName") final String moduleName,
    @PathVariable("moduleTime") final Long moduleTime, @PathVariable("urlId") final int urlId)
    throws NoSuchRequestHandlingMethodException, IOException {
    checkRunning();
    final Module module = this.batchJobService.getModule(moduleName);
    if (module == null || !module.isStarted() || module.getStartedTime() != moduleTime) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final List<URL> jarUrls = module.getJarUrls();
      final URL url = jarUrls.get(urlId);
      final InputStream in = UrlUtil.getInputStream(url);
      try {
        final OutputStream out = response.getOutputStream();
        try {
          FileUtil.copy(in, out);
        } finally {
          FileUtil.closeSilent(out);
        }
      } finally {
        FileUtil.closeSilent(in);
      }

    }
  }

  @RequestMapping(value = {
    "/worker/modules/{moduleName}/{moduleTime}/urls"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getModuleUrls(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable("moduleName") final String moduleName,
    @PathVariable("moduleTime") final Long moduleTime) throws NoSuchRequestHandlingMethodException,
    IOException {
    checkRunning();
    final BusinessApplicationRegistry businessApplicationRegistry = this.batchJobService.getBusinessApplicationRegistry();
    final Module module = businessApplicationRegistry.getModule(moduleName);
    if (module == null || !module.isStarted() || module.getStartedTime() != moduleTime) {
      throw new NoSuchRequestHandlingMethodException(request);
    } else {
      final List<URL> jarUrls = module.getJarUrls();
      final String url = this.getWebServiceUrl() + "/worker/modules/" + moduleName + "/"
        + moduleTime + "/urls/";
      final List<String> webServiceJarUrls = new ArrayList<String>();
      for (int i = 0; i < jarUrls.size(); i++) {
        webServiceJarUrls.add(url + i);
      }
      final Map<String, Object> result = new NamedLinkedHashMap<String, Object>("ModuleUrls");
      result.put("jarUrls", webServiceJarUrls);
      return result;
    }
  }

  public String getWebServiceUrl() {
    return this.cpfConfig.getInternalWebServiceUrl();
  }

  @RequestMapping("/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/error")
  public void postBatchJobExecutionGroupError(@PathVariable("workerId") final String workerId,
    @PathVariable("batchJobId") final Long batchJobId,
    @PathVariable("groupId") final String groupId, final InputStream in) {
    checkRunning();
    final BatchJobRequestExecutionGroup group = this.batchJobService.getBatchJobRequestExecutionGroup(
      workerId, groupId);

    if (group != null) {
      synchronized (group) {
        if (!group.isCancelled()) {
          final int sequenceNumber = group.getSequenceNumber();
          this.jobController.setGroupError(batchJobId, sequenceNumber, in);
        }
      }
    }
  }

  @RequestMapping("/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/requests/{sequenceNumber}/resultData")
  @ResponseBody
  public Map<String, ? extends Object> postBatchJobExecutionGroupOpaqueOutputData(
    @PathVariable("workerId") final String workerId,
    @PathVariable("batchJobId") final Long batchJobId,
    @PathVariable("groupId") final String groupId,
    @PathVariable("sequenceNumber") final int sequenceNumber, final InputStream in)
    throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final BatchJobRequestExecutionGroup group = this.batchJobService.getBatchJobRequestExecutionGroup(
      workerId, groupId);

    if (group != null) {
      synchronized (group) {
        if (!group.isCancelled()) {
          final Record batchJob = this.dataAccessObject.getBatchJob(batchJobId);
          if (batchJob != null) {
            final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
            final BusinessApplication businessApplication = this.batchJobService.getBusinessApplication(businessApplicationName);
            if (businessApplication != null && businessApplication.isPerRequestResultData()) {
              final String resultDataContentType = batchJob.getValue(BatchJob.RESULT_DATA_CONTENT_TYPE);
              final File file = FileUtil.createTempFile("result", ".bin");

              try {
                FileUtil.copy(in, file);
                if (!group.isCancelled()) {
                  this.batchJobService.createBatchJobResultOpaque(batchJobId, sequenceNumber,
                    resultDataContentType, file);
                }
              } finally {
                FileUtil.closeSilent(in);
                FileUtil.deleteDirectory(file);
              }
            }
          }
        }
      }
    }
    final Map<String, Object> map = new NamedLinkedHashMap<String, Object>(
      "OpaqueOutputDataResults");
    return map;
  }

  @RequestMapping(value = "/worker/workers/{workerId}/jobs/{batchJobId}/groups/{groupId}/results",
      method = RequestMethod.POST)
  public void postBatchJobRequestExecutionGroupResults(
    @PathVariable("workerId") final String workerId,//
    @PathVariable("batchJobId") final String batchJobId,//
    @PathVariable("groupId") final String groupId, //
    @RequestParam(value = "completedRequestRange", defaultValue = "") final String completedRequestRange, //
    @RequestParam(value = "failedRequestRange", defaultValue = "") final String failedRequestRange, //
    @RequestParam(value = "groupExecutedTime", defaultValue = "0") final Long groupExecutedTime, //
    @RequestParam(value = "applicationExecutedTime", defaultValue = "0") final Long applicationExecutedTime, //
    final InputStream in) throws NoSuchRequestHandlingMethodException {
    checkRunning();
    final Worker worker = this.batchJobService.getWorker(workerId);
    if (worker != null) {
      final BatchJobRequestExecutionGroup group = worker.removeExecutingGroup(groupId);
      if (group != null && !group.isCancelled()) {
        synchronized (group) {
          final BatchJob batchJob = group.getBatchJob();
          if (!batchJob.isCompleted()) {
            final RangeSet completedRequests = batchJob.addCompletedRequests(completedRequestRange);
            final RangeSet failedRequests = batchJob.addFailedRequests(failedRequestRange);
            if (in != null) {
              this.batchJobService.updateBatchJobExecutionGroupFromResponse(worker, batchJob,
                group, in);
            }
            batchJob.removeGroup(group);
            final BusinessApplication businessApplication = group.getBusinessApplication();
            final String moduleName = businessApplication.getModuleName();
            final long executionTime = this.batchJobService.updateGroupStatistics(group,
              businessApplication, moduleName, applicationExecutedTime, groupExecutedTime,
              completedRequests.size(), failedRequests.size());
            final AppLog appLog = businessApplication.getLog();
            appLog.info("End\tGroup execution\tgroupId=" + groupId + "\tworkerId=" + workerId
              + "\ttime=" + executionTime / 1000.0);
          }
        }
      }
    }
  }

  @RequestMapping(value = "/worker/workers/{workerId}/jobs/groups/nextId",
      method = RequestMethod.POST)
  @ResponseBody
  public Map<String, Object> postNextBatchJobExecutionGroupId(
    @PathVariable("workerId") final String workerId,
    @RequestParam("workerStartTime") final long workerStartTime, //
    @RequestParam(value = "moduleName", required = false) final List<String> moduleNames,
    @RequestParam(value = "maxMessageId", required = false, defaultValue = "0") final int maxMessageId) {
    Map<String, Object> response = Collections.emptyMap();
    final BatchJobService batchJobService = this.batchJobService;
    if (batchJobService != null) {
      checkRunning();
      try {
        batchJobService.setWorkerConnectTime(workerId, workerStartTime);
        response = batchJobService.getNextBatchJobRequestExecutionGroup(workerId, maxMessageId,
          moduleNames);
      } catch (final Throwable e) {
        LoggerFactory.getLogger(InternalWebService.class).error(e.getMessage(), e);
        throw new HttpMessageNotWritableException("Unable to get execution group id", e);
      } finally {
        batchJobService.setWorkerConnectTime(workerId, workerStartTime);
      }
    }
    return response;
  }

  @Resource
  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
    this.dataAccessObject = batchJobService.getDataAccessObject();
    this.jobController = batchJobService.getJobController();
  }
}
