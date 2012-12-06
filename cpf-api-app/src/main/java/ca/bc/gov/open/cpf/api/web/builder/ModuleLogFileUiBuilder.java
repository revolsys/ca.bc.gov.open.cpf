package ca.bc.gov.open.cpf.api.web.builder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.security.CpfMethodSecurityExpressions;
import ca.bc.gov.open.cpf.plugin.api.module.Module;

import com.revolsys.io.FileUtil;
import com.revolsys.parallel.process.InvokeMethodCallable;
import com.revolsys.ui.html.builder.HtmlUiBuilder;

@Controller
public class ModuleLogFileUiBuilder extends HtmlUiBuilder<ModuleLogFile>
  implements CpfMethodSecurityExpressions {

  private File directory;

  public ModuleLogFileUiBuilder() {
    super("moduleLogFile", "Module Log File", "ModuleLogFiles");

  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/logFiles/{logName}"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  public void createObjectViewPage(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String moduleName,
    final @PathVariable String logName) throws IOException, ServletException {
    final ModuleUiBuilder moduleBuilder = getBuilder(Module.class);

    // Check to see if the user can access the module
    moduleBuilder.getModule(request, moduleName);

    final File moduleLogDirectory = new File(directory, moduleName);
    final String fileName = logName + ".csv";
    final File file = new File(moduleLogDirectory, fileName);
    if (file.exists() && file.canRead()) {
      response.setHeader("Content-Disposition", "attachment; filename="
        + fileName);
      response.setContentLength((int)file.length());
      response.setContentType("text/csv");
      final OutputStream out = response.getOutputStream();
      try {
        FileUtil.copy(file, out);
      } finally {
        FileUtil.closeSilent(out);
      }
    } else {
      throw new NoSuchRequestHandlingMethodException(request);
    }
  }

  public File getDirectory() {
    return directory;
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/logFiles"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ANY_ADMIN_FOR_MODULE)
  public Object getModuleLogFileListPage(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String moduleName)
    throws IOException, NoSuchRequestHandlingMethodException {
    final ModuleUiBuilder moduleBuilder = getBuilder(Module.class);

    // Check to see if the user can access the module
    moduleBuilder.getModule(request, moduleName);

    Callable<Collection<? extends Object>> rowsCallable = new InvokeMethodCallable<Collection<? extends Object>>(
      this, "getLogFiles", moduleName);

    return createDataTableHandlerOrRedirect(request, response, "moduleList",
      rowsCallable, Module.class, "view");
  }

  public List<ModuleLogFile> getLogFiles(final String moduleName) {
    final List<ModuleLogFile> files = new ArrayList<ModuleLogFile>();

    final File moduleLogDirectory = new File(directory, moduleName);
    if (moduleLogDirectory.exists() && moduleLogDirectory.canRead()) {
      for (final File file : moduleLogDirectory.listFiles()) {
        final ModuleLogFile moduleLogFile = new ModuleLogFile(file);
        files.add(moduleLogFile);
      }
    }
    return files;
  }

  public void setDirectory(final File directory) {
    this.directory = directory;
  }
}
