package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.BatchJob;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;
import ca.bc.gov.open.cpf.api.scheduler.BusinessApplicationStatistics;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;

import com.revolsys.beans.InvokeMethodCallable;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.io.xml.XmlWriter;
import com.revolsys.ui.html.view.TabElementContainer;

@Controller
public class BusinessApplicationStatisticsUiBuilder extends CpfUiBuilder {

  public BusinessApplicationStatisticsUiBuilder() {
    super("statistic", "Business Application Statistic",
      "Business Application Statistics");
    setIdParameterName("statisticId");
    setIdPropertyName("id");
  }

  public void businessApplication(final XmlWriter out, final Object object) {
    final DataObject batchJob = (DataObject)object;
    final String businessApplicationName = batchJob.getValue(BatchJob.BUSINESS_APPLICATION_NAME);

    final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
    final BusinessApplicationUiBuilder appBuilder = getBuilder(BusinessApplication.class);
    final Map<String, String> parameterKeys = new HashMap<String, String>();
    parameterKeys.put("moduleName", "moduleName");
    parameterKeys.put("businessApplicationName", "name");
    appBuilder.serializeLink(out, businessApplication, "name", "moduleView",
      parameterKeys);
  }

  public ModelAndView createStatsViewPage(final String businessApplicationName,
    final BusinessApplicationStatistics stats) {
    final ModelMap model = new ModelMap();
    model.put("title", businessApplicationName + " Statistics " + stats.getId());
    model.put("statisitcs", stats);
    model.put("body",
      "/WEB-INF/jsp/builder/businessApplicationStatisticsView.jsp");
    return new ModelAndView("/jsp/template/page", model);
  }

  @Override
  public Object getProperty(final Object object, final String keyName) {
    if (object instanceof BusinessApplicationStatistics) {
      final BusinessApplicationStatistics statistics = (BusinessApplicationStatistics)object;
      if (keyName.equals("businessApplication")) {
        final String businessApplicationName = statistics.getBusinessApplicationName();

        final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
        return businessApplication;
      } else if (keyName.equals("module")) {
        final String businessApplicationName = statistics.getBusinessApplicationName();

        final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
        if (businessApplication == null) {
          return null;
        } else {
          return businessApplication.getModule();
        }
      } else if (keyName.equals("moduleName")) {
        final String businessApplicationName = statistics.getBusinessApplicationName();

        final BusinessApplication businessApplication = getBusinessApplication(businessApplicationName);
        if (businessApplication == null) {
          return null;
        } else {
          return businessApplication.getModule().getName();
        }
      }
    }
    return super.getProperty(object, keyName);
  }

  public List<BusinessApplicationStatistics> getStatistics(
    final BusinessApplication businessApplication) {
    final String businessApplicationName = businessApplication.getName();
    final BatchJobService batchJobService = getBatchJobService();
    final List<BusinessApplicationStatistics> statistics = batchJobService.getStatisticsList(businessApplicationName);
    Collections.reverse(statistics);
    return statistics;
  }

  public List<BusinessApplicationStatistics> getSummaryStatistics(
    final String durationType) {
    final BatchJobService batchJobService = getBatchJobService();
    final String statisticId = BusinessApplicationStatistics.getId(durationType);
    final List<BusinessApplication> apps = getBusinessApplications();
    final List<BusinessApplicationStatistics> statistics = new ArrayList<BusinessApplicationStatistics>();
    for (final BusinessApplication businessApplication : apps) {
      final String businessApplicationName = businessApplication.getName();
      final BusinessApplicationStatistics statistic = batchJobService.getStatistics(
        businessApplicationName, statisticId);
      statistics.add(statistic);
    }
    return statistics;
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/apps/{businessApplicationName}/dashboard"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageBusinessApplicationList(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String moduleName,
    final @PathVariable String businessApplicationName) throws IOException,
    NoSuchRequestHandlingMethodException {
    final BusinessApplication businessApplication = getBusinessApplicationRegistry().getModuleBusinessApplication(
      moduleName, businessApplicationName);
    if (businessApplication != null) {
      final InvokeMethodCallable<Collection<? extends Object>> rowCallback = new InvokeMethodCallable<Collection<? extends Object>>(
        this, "getStatistics", businessApplication);
      return createDataTableHandlerOrRedirect(request, response,
        "moduleAppList", rowCallback, BusinessApplication.class, "moduleView");
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(
      value = {
        "/admin/modules/{moduleName}/apps/{businessApplicationName}/dashboard/{statisticId}"
      }, method = RequestMethod.GET)
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public ModelAndView pageBusinessApplicationView(
    final HttpServletRequest request, final HttpServletResponse response,
    final @PathVariable String moduleName,
    final @PathVariable String businessApplicationName,
    final @PathVariable String statisticId) throws IOException,
    ServletException {
    try {
      final BusinessApplication businessApplication = getBusinessApplicationRegistry().getModuleBusinessApplication(
        moduleName, businessApplicationName);
      if (businessApplication != null) {
        final BatchJobService batchJobService = getBatchJobService();
        final BusinessApplicationStatistics statistics = batchJobService.getStatistics(
          businessApplicationName, statisticId);

        if (statistics != null) {
          final ModelAndView viewPage = createStatsViewPage(
            businessApplicationName, statistics);

          return viewPage;
        }
      }
    } catch (final IllegalArgumentException e) {
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/dashboard"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ANY_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageIndex(final HttpServletRequest request) {
    setPageTitle(request, "summary");

    final TabElementContainer tabs = new TabElementContainer();

    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("serverSide", Boolean.FALSE);

    addTabDataTable(tabs, this, "hourList", parameters);

    addTabDataTable(tabs, this, "dayList", parameters);

    addTabDataTable(tabs, this, "monthList", parameters);

    addTabDataTable(tabs, this, "yearList", parameters);

    return tabs;
  }

  @RequestMapping(value = {
    "/admin/dashboard/{durationType}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ANY_ADMIN_FOR_MODULE)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Object pageSummaryList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String durationType)
    throws IOException, NoSuchRequestHandlingMethodException {
    final InvokeMethodCallable<Collection<? extends Object>> rowCallback = new InvokeMethodCallable<Collection<? extends Object>>(
      this, "getSummaryStatistics", durationType);
    return createDataTableHandlerOrRedirect(request, response, durationType
      + "List", rowCallback, this, "summary");
  }

}