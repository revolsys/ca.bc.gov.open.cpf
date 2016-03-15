/*
 * Copyright © 2008-2016, Province of British Columbia
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
package ca.bc.gov.open.cpf.plugin.api.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationPluginExecutor;

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
