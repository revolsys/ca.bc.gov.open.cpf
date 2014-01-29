package ca.bc.gov.open.cpf.api.web.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;

import ca.bc.gov.open.cpf.api.domain.BatchJobResult;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.io.FileUtil;
import com.revolsys.io.json.JsonParser;

public class FileSystemFileController implements CpfFileController {
  private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols();

  static {
    DECIMAL_FORMAT_SYMBOLS.setGroupingSeparator('/');
  }

  public static String toPath(final long id) {
    final StringBuffer path = new StringBuffer();
    if (id < 0) {
      path.append('-');
    }
    final int numGroups;
    if (id == 0) {
      numGroups = 1;
    } else {
      numGroups = (int)Math.ceil((Math.floor(Math.log10(id)) + 1) / 3);
    }
    path.append(numGroups);
    path.append('/');
    final DecimalFormat decimalFormat = new DecimalFormat();
    decimalFormat.setMinimumIntegerDigits(numGroups * 3);
    decimalFormat.setDecimalFormatSymbols(DECIMAL_FORMAT_SYMBOLS);
    path.append(decimalFormat.format(id));
    return path.toString();
  }

  private final File rootDirectory;

  public FileSystemFileController(final File rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  protected File getGroupFile(final long batchJobId, final String type,
    final long subId, final String suffix) {
    final File jobDirectory = FileUtil.getDirectory(rootDirectory, "jobs/"
      + toPath(batchJobId));
    final File groupFile = FileUtil.getFile(jobDirectory, type + "/"
      + toPath(subId) + suffix + ".json");
    return groupFile;
  }

  @Override
  public InputStream getJobResultData(final long batchJobId,
    final long batchJobResultId, final DataObject batchJobResult) {
    final File resultFile = getGroupFile(batchJobId, "results",
      batchJobResultId, "");
    return FileUtil.getInputStream(resultFile);
  }

  @Override
  public long getJobResultSize(final long batchJobId,
    final long batchJobResultId, final DataObject batchJobResult) {
    final File resultFile = getGroupFile(batchJobId, "results",
      batchJobResultId, "");
    return resultFile.length();
  }

  @Override
  public String getStructuredInputData(final long batchJobId,
    final long sequenceNumber) {
    final File groupFile = getGroupFile(batchJobId, "groups",
      sequenceNumber, "-input");
    if (groupFile.exists()) {
      final String inputData = FileUtil.getString(groupFile);
      return inputData;
    } else {
      return "{}";
    }
  }

  @Override
  public Map<String, Object> getStructuredResultData(final long batchJobId,
    final long sequenceNumber, final DataObject batchJobExecutionGroup) {
    final File groupFile = getGroupFile(batchJobId, "groups",
      sequenceNumber, "-result");
    if (groupFile.exists()) {
      final Map<String, Object> resultData = JsonParser.read(groupFile);
      return resultData;
    } else {
      return null;
    }
  }

  @Override
  public void setJobResultData(final long batchJobId,
    final DataObject batchJobResult, final Object resultData) {
    final Long batchJobResultId = batchJobResult.getLong(BatchJobResult.BATCH_JOB_RESULT_ID);
    final File resultFile = getGroupFile(batchJobId, "results",
      batchJobResultId, "");
    resultFile.getParentFile().mkdirs();
    if (resultData instanceof File) {
      final File file = (File)resultData;
      FileUtil.copy(file, resultFile);
    } else if (resultData instanceof byte[]) {
      final byte[] bytes = (byte[])resultData;
      FileUtil.copy(new ByteArrayInputStream(bytes), resultFile);
    }
  }

  @Override
  public void setStructuredInputData(final long batchJobId,
    final long sequenceNumber, final DataObject executionGroup,
    final String structuredInputData) {
    final File groupFile = getGroupFile(batchJobId, "groups",
      sequenceNumber, "-input");
    groupFile.getParentFile().mkdirs();
    FileUtil.copy(structuredInputData, groupFile);
  }

  @Override
  public void setStructuredResultData(final long batchJobId,
    final long sequenceNumber, final DataObject executionGroup,
    final String structuredResultData) {
    final File groupFile = getGroupFile(batchJobId, "groups",
      sequenceNumber, "-result");
    groupFile.getParentFile().mkdirs();
    FileUtil.copy(structuredResultData, groupFile);
  }
}
