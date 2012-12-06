package ca.bc.gov.open.cpf.plugin.api.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPluginExecutor;

public class StructuredToOpaquePluginTest {
  @Test
  public void testParameters() throws Exception {

    final BusinessApplicationPluginExecutor executor = new BusinessApplicationPluginExecutor();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Map<String, Object> parameters = Collections.emptyMap();
    executor.execute("StructuredToOpaque", parameters, "text/plain", out);
    final ObjectInputStream in = new ObjectInputStream(
      new ByteArrayInputStream(out.toByteArray()));
    System.out.println(in.readObject());
  }
}
