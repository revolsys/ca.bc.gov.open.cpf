package ca.bc.gov.open.cpf.api.web.builder;

import java.io.File;

import com.revolsys.io.FileUtil;

public class ModuleLogFile {

  private final File file;

  public ModuleLogFile(final File file) {
    this.file = file;
  }

  public String getDate() {
    final String fileName = getId();
    final int index = fileName.indexOf('-');
    if (index == -1) {
      return null;
    } else {
      return fileName.substring(index + 1);
    }
  }

  public String getId() {
    return FileUtil.getBaseName(file);
  }

  public String getModuleName() {
    final String fileName = getId();
    final int index = fileName.indexOf('-');
    if (index == -1) {
      return fileName;
    } else {
      return fileName.substring(0, index);
    }
  }

  public long getSize() {
    return file.length();
  }
}
