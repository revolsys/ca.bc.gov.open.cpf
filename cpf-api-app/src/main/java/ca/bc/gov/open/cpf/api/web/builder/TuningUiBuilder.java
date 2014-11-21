package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ca.bc.gov.open.cpf.api.controller.CpfConfig;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobPostProcess;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobPreProcess;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobScheduler;
import ca.bc.gov.open.cpf.api.scheduler.BatchJobService;

import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class TuningUiBuilder extends CpfUiBuilder {

  @Resource
  private CpfConfig cpfConfig;

  @Resource
  private BatchJobPreProcess cpfJobPreProcess;

  @Resource
  private BatchJobPostProcess cpfJobPostProcess;

  @Resource
  private BatchJobScheduler cpfJobScheduler;

  @Resource
  private BatchJobService batchJobService;

  @Resource
  private BasicDataSource cpfDataSource;

  public TuningUiBuilder() {
  }

  private void addCounts(final List<Object> rows, final String name,
    final int active, final int size, final int maxSize) {
    final Map<String, Object> row = new LinkedHashMap<>();
    row.put("name", name);
    row.put("active", active);
    row.put("size", size);
    row.put("maxSize", maxSize);

    rows.add(row);
  }

  @RequestMapping(value = {
    "/admin/tuning"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Object createModulePageList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    checkAdminOrAnyModuleAdmin();
    HttpServletUtils.setAttribute("title", "Tuning");
    final List<Object> rows = new ArrayList<>();
    addCounts(rows, "Pre Process", this.cpfJobPreProcess.getActiveCount(),
      this.cpfJobPreProcess.getPoolSize(),
      this.cpfConfig.getPreProcessPoolSize());
    addCounts(rows, "Scheduler", this.cpfJobScheduler.getActiveCount(),
      this.cpfJobScheduler.getPoolSize(), this.cpfConfig.getSchedulerPoolSize());
    addCounts(rows, "Group Results",
      this.batchJobService.getGroupResultCount(),
      this.batchJobService.getGroupResultCount(),
      this.cpfConfig.getGroupResultPoolSize());
    addCounts(rows, "Post Process", this.cpfJobPostProcess.getActiveCount(),
      this.cpfJobPostProcess.getPoolSize(),
      this.cpfConfig.getPostProcessPoolSize());
    addCounts(rows, "Database Connections", this.cpfDataSource.getNumActive(),
      this.cpfDataSource.getNumActive() + this.cpfDataSource.getNumIdle(),
      this.cpfDataSource.getMaxTotal());
    return createDataTableHandler(request, "list", rows);
  }

}
