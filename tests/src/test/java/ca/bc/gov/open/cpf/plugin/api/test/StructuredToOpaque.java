package ca.bc.gov.open.cpf.plugin.api.test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;

@BusinessApplicationPlugin(
  name = "StructuredToOpaque",
  version = "1.0.0",
  perRequestResultData = true)
public class StructuredToOpaque {

  private OutputStream resultData;

  private String resultDataContentType;

  public void execute() {
    try {
      final ObjectOutputStream out = new ObjectOutputStream(resultData);
      out.writeObject("test");
      out.close();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }

  public void setResultData(final OutputStream resultData) {
    this.resultData = resultData;
  }

  public void setResultDataContentType(final String format) {
    this.resultDataContentType = format;
  }
}
