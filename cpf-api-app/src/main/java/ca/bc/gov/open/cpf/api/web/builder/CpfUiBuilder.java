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
package ca.bc.gov.open.cpf.api.web.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.domain.BatchJobStatus;
import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.BusinessApplicationStatistics;
import ca.bc.gov.open.cpf.api.web.controller.JobController;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;

import com.revolsys.identifier.Identifier;
import com.revolsys.io.PathName;
import com.revolsys.record.Record;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.spring.security.MethodSecurityExpressionRoot;
import com.revolsys.ui.html.builder.RecordHtmlUiBuilder;
import com.revolsys.ui.web.utils.HttpServletUtils;

public class CpfUiBuilder extends RecordHtmlUiBuilder {

  public static final String ADMIN = "ROLE_ADMIN";

  public static void checkAdminOrAnyModuleAdmin() {
    final boolean permitted = hasAnyRole(ADMIN) || hasRoleRegex("ROLE_ADMIN_MODULE_.*");
    if (!permitted) {
      throw new AccessDeniedException("Permission denied");
    }
  }

  public static void checkAdminOrAnyModuleAdmin(final String moduleName) {
    final boolean permitted = hasAnyRole(ADMIN)
      || hasRoleRegex("ROLE_ADMIN_MODULE_" + moduleName + ".*");
    if (!permitted) {
      throw new AccessDeniedException("Permission denied");
    }
  }

  public static void checkAdminOrAnyModuleAdminExceptSecurity() {
    final boolean permitted = hasAnyRole(ADMIN) || hasRoleRegex("ROLE_ADMIN_MODULE_.*_ADMIN");
    if (!permitted) {
      throw new AccessDeniedException("Permission denied");
    }
  }

  public static void checkAdminOrModuleAdmin(final String moduleName) {
    final boolean permitted = hasAnyRole(ADMIN, "ROLE_ADMIN_MODULE_" + moduleName + "_ADMIN");
    if (!permitted) {
      throw new AccessDeniedException("Permission denied");
    }
  }

  public static void checkHasAnyRole(final String... roleNames) {
    final boolean permitted = hasAnyRole(roleNames);
    if (!permitted) {
      throw new AccessDeniedException("Permission denied");
    }
  }

  public static void checkPermission(final Expression expression) {
    final boolean permitted = hasPermission(expression);
    if (!permitted) {
      throw new AccessDeniedException("Permission denied");
    }
  }

  public static void checkPermission(final Expression expression,
    final String accessDeniedMessage) {
    final boolean permitted = hasPermission(expression);
    if (!permitted) {
      throw new AccessDeniedException(accessDeniedMessage);
    }
  }

  public static Expression createExpression(final String permission) {
    return new SpelExpressionParser().parseExpression(permission);
  }

  protected static Collection<GrantedAuthority> getGrantedAuthorities() {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    @SuppressWarnings("unchecked")
    final Collection<GrantedAuthority> grantedAuthorities = (Collection<GrantedAuthority>)authentication
      .getAuthorities();
    return grantedAuthorities;
  }

  public static EvaluationContext getSecurityEvaluationContext() {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    final MethodSecurityExpressionRoot root = new MethodSecurityExpressionRoot(authentication);
    final EvaluationContext evaluationContext = new StandardEvaluationContext(root);
    return evaluationContext;
  }

  public static boolean hasAnyRole(final Collection<String> roleNames) {
    final Collection<GrantedAuthority> grantedAuthorities = getGrantedAuthorities();
    for (final GrantedAuthority grantedAuthority : grantedAuthorities) {
      final String authority = grantedAuthority.getAuthority();
      if (roleNames.contains(authority)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasAnyRole(final String... roleNames) {
    return hasAnyRole(Arrays.asList(roleNames));
  }

  public static boolean hasPermission(final Expression expression) {
    final EvaluationContext evaluationContext = getSecurityEvaluationContext();
    final boolean permitted = ExpressionUtils.evaluateAsBoolean(expression, evaluationContext);
    return permitted;
  }

  public static boolean hasRole(final String roleName) {
    final Collection<GrantedAuthority> grantedAuthorities = getGrantedAuthorities();
    for (final GrantedAuthority grantedAuthority : grantedAuthorities) {
      final String authority = grantedAuthority.getAuthority();
      if (authority.equals(roleName)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasRoleRegex(final String regex) {
    final Pattern pattern = Pattern.compile(regex);
    final Collection<GrantedAuthority> grantedAuthorities = getGrantedAuthorities();
    for (final GrantedAuthority grantedAuthority : grantedAuthorities) {
      final String authority = grantedAuthority.getAuthority();
      final Matcher matcher = pattern.matcher(authority);
      final boolean matches = matcher.matches();
      if (matches) {
        return true;
      }
    }
    return false;
  }

  private BatchJobService batchJobService;

  private BusinessApplicationRegistry businessApplicationRegistry;

  private CpfDataAccessObject dataAccessObject;

  /** The minimum time in milliseconds between status checks. */
  private int minTimeUntilNextCheck = 10;

  public CpfUiBuilder() {
  }

  public CpfUiBuilder(final String typePath, final PathName tableName, final String idPropertyName,
    final String title, final String pluralTitle) {
    super(typePath, tableName, idPropertyName, title, pluralTitle);
  }

  public CpfUiBuilder(final String typePath, final String title) {
    super(typePath, title);
  }

  public CpfUiBuilder(final String typePath, final String title, final String pluralTitle) {
    super(typePath, title, pluralTitle);
  }

  @PreDestroy
  public void close() {
    this.batchJobService = null;
    this.businessApplicationRegistry = null;
    this.dataAccessObject = null;
  }

  public BatchJob getBatchJob(final String businessApplicationName, final Object batchJobId)
    throws NoSuchRequestHandlingMethodException {
    final BatchJob batchJob = this.dataAccessObject.getBatchJob(Identifier.create(batchJobId));
    if (batchJob != null) {
      if (batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME).equals(businessApplicationName)) {
        return batchJob;

      }
    }
    throw new NoSuchRequestHandlingMethodException(getRequest());
  }

  public BatchJobService getBatchJobService() {
    return this.batchJobService;
  }

  public BusinessApplication getBusinessApplication(final String businessApplicationName) {
    return this.businessApplicationRegistry.getBusinessApplication(businessApplicationName);
  }

  public List<String> getBusinessApplicationNames() {
    final List<String> businessApplicationNames = new ArrayList<String>();
    final List<Module> modules = getModules();
    for (final Module module : modules) {
      final List<String> moduleAppNames = module.getBusinessApplicationNames();
      businessApplicationNames.addAll(moduleAppNames);
    }
    return businessApplicationNames;
  }

  public BusinessApplicationRegistry getBusinessApplicationRegistry() {
    return this.businessApplicationRegistry;
  }

  public List<BusinessApplication> getBusinessApplications() {
    final List<BusinessApplication> businessApplications = new ArrayList<BusinessApplication>();
    final List<Module> modules = getModules();
    for (final Module module : modules) {
      final List<BusinessApplication> moduleApps = module.getBusinessApplications();
      businessApplications.addAll(moduleApps);
    }
    return businessApplications;
  }

  protected String getConsumerKey() {
    final SecurityContext securityContext = SecurityContextHolder.getContext();
    final Authentication authentication = securityContext.getAuthentication();
    final String consumerKey = authentication.getName();
    return consumerKey;
  }

  public CpfDataAccessObject getDataAccessObject() {
    return this.dataAccessObject;
  }

  protected JobController getJobController() {
    return this.batchJobService.getJobController();
  }

  public int getMinTimeUntilNextCheck() {
    return this.minTimeUntilNextCheck;
  }

  protected Module getModule(final HttpServletRequest request, final String moduleName)
    throws NoSuchRequestHandlingMethodException {
    final Module module = this.businessApplicationRegistry.getModule(moduleName);
    if (module != null) {
      HttpServletUtils.setPathVariable("MODULE_NAME", moduleName);
      return module;
    }
    throw new NoSuchRequestHandlingMethodException(request);

  }

  public BusinessApplication getModuleBusinessApplication(final String moduleName,
    final String businessApplicationName) throws NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = this.businessApplicationRegistry
      .getModuleBusinessApplication(moduleName, businessApplicationName);
    if (businessApplication != null) {
      return businessApplication;
    }
    throw new NoSuchRequestHandlingMethodException(getRequest());
  }

  public List<String> getModuleNames() {
    final List<String> moduleNames = new ArrayList<String>();
    final List<Module> modules = getModules();
    for (final Module module : modules) {
      final String moduleName = module.getName();
      moduleNames.add(moduleName);
    }
    return moduleNames;
  }

  public List<Module> getModules() {
    final CpfUiBuilder moduleUiBuilder = getBuilder(Module.class);
    final List<Module> modules = moduleUiBuilder.getPermittedModules();
    return modules;
  }

  public List<Module> getPermittedModules() {
    final List<Module> modules = this.businessApplicationRegistry.getModules();
    if (!hasAnyRole(ADMIN)) {
      for (final Iterator<Module> iterator = modules.iterator(); iterator.hasNext();) {
        final Module module = iterator.next();
        final String moduleName = module.getName();
        if (!hasRoleRegex("ROLE_ADMIN_MODULE_" + moduleName + ".*")) {
          iterator.remove();
        }
      }
    }
    return modules;
  }

  /**
   * Get the time in milliseconds until the user should next check the status of
   * the job. If time is less than minValue then minValue will be returned.
   *
   * @return The time in milliseconds.
   */
  public long getTimeUntilNextCheck(final BatchJob batchJob) {
    final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);
    final BusinessApplication application = this.batchJobService
      .getBusinessApplication(businessApplicationName);
    long timeRemaining = 0;
    if (application != null) {
      final List<BusinessApplicationStatistics> statistics = this.batchJobService
        .getStatisticsList(businessApplicationName);
      if (!statistics.isEmpty()) {
        final BusinessApplicationStatistics stats = statistics.get(0);
        final String jobStatus = batchJob.getValue(BatchJob.JOB_STATUS);
        final int numRequests = batchJob.getInteger(BatchJob.NUM_SUBMITTED_REQUESTS);
        if (jobStatus.equals(BatchJobStatus.DOWNLOAD_INITIATED)
          || jobStatus.equals(BatchJobStatus.RESULTS_CREATED)
          || jobStatus.equals(BatchJobStatus.CANCELLED)) {
          return 0;
        } else if (jobStatus.equals(BatchJobStatus.CREATING_RESULTS)) {
          return numRequests * stats.getPostProcessedRequestsAverageTime();
        } else {
          if (!jobStatus.equals(BatchJobStatus.PROCESSED)) {
            final int numCompletedRequests = batchJob.getNumCompletedRequests();
            final int numFailedRequests = batchJob.getNumFailedRequests();
            final int numRequestsRemaining = numRequests - numCompletedRequests - numFailedRequests;
            final long executedRequestsAverageTime = stats
              .getApplicationExecutedRequestsAverageTime();
            timeRemaining += numRequestsRemaining * executedRequestsAverageTime;
            if (!jobStatus.equals(BatchJobStatus.PROCESSING)) {
              timeRemaining += numRequests * stats.getPreProcessedRequestsAverageTime();
            }
          }
        }
      }
    }
    return Math.max(timeRemaining / 1000, this.minTimeUntilNextCheck);
  }

  public Record getUserAccount(final String consumerKey) {
    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    final Record userAccount = dataAccessObject.getUserAccount(consumerKey);
    return userAccount;
  }

  public Record getUserGroup(final String groupName) {
    final CpfDataAccessObject dataAccessObject = getDataAccessObject();
    final Record userGroup = dataAccessObject.getUserGroup(groupName);
    return userGroup;
  }

  public boolean hasModule(final HttpServletRequest request, final String moduleName)
    throws NoSuchRequestHandlingMethodException {
    if (this.businessApplicationRegistry.hasModule(moduleName)) {
      return true;
    } else {
      throw new NoSuchRequestHandlingMethodException(request);
    }
  }

  @Override
  protected void insertObject(final Record object) {
    this.dataAccessObject.write(object);
  }

  @Resource
  public void setBatchJobService(final BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
    this.dataAccessObject = batchJobService.getDataAccessObject();
    this.businessApplicationRegistry = batchJobService.getBusinessApplicationRegistry();
    final RecordStore recordStore = this.dataAccessObject.getRecordStore();
    setRecordStore(recordStore);
  }

  public void setMinTimeUntilNextCheck(final int minTimeUntilNextCheck) {
    this.minTimeUntilNextCheck = minTimeUntilNextCheck;
  }

  @Override
  protected void updateObject(final Record object) {
    this.dataAccessObject.write(object);
  }

}
