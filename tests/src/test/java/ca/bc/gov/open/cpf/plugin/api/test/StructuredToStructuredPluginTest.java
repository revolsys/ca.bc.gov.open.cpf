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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationPluginExecutor;

public class StructuredToStructuredPluginTest {
  @Test
  public void testParameters() {
    final Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("booleanParameter", true);
    parameters.put("booleanListParameter", Collections.singletonList(true));
    parameters.put("booleanMapParameter", Collections.singletonMap("key", true));
    parameters.put("booleanObjectParameter", true);

    parameters.put("byteListParameter", Collections.singletonList((byte)1));
    parameters.put("byteMapParameter", Collections.singletonMap("key", (byte)2));
    parameters.put("byteObjectParameter", (byte)3);
    parameters.put("byteParameter", (byte)4);

    parameters.put("shortListParameter", Collections.singletonList((short)5));
    parameters.put("shortMapParameter",
      Collections.singletonMap("key", (short)6));
    parameters.put("shortObjectParameter", (short)7);
    parameters.put("shortParameter", (short)8);

    parameters.put("intListParameter", Collections.singletonList(9));
    parameters.put("intMapParameter", Collections.singletonMap("key", 10));
    parameters.put("intObjectParameter", 11);
    parameters.put("intParameter", 12);

    parameters.put("longListParameter", Collections.singletonList(13l));
    parameters.put("longMapParameter", Collections.singletonMap("key", 14l));
    parameters.put("longObjectParameter", 15l);
    parameters.put("longParameter", 16l);

    parameters.put("floatListParameter", Collections.singletonList(17.0f));
    parameters.put("floatMapParameter", Collections.singletonMap("key", 18.0f));
    parameters.put("floatObjectParameter", 19.0f);
    parameters.put("floatParameter", 20.0f);

    parameters.put("doubleListParameter", Collections.singletonList(21.0));
    parameters.put("doubleMapParameter", Collections.singletonMap("key", 22.0));
    parameters.put("doubleObjectParameter", 23.0);
    parameters.put("doubleParameter", 24.0);

    parameters.put("stringListParameter", Collections.singletonList("s1"));
    parameters.put("stringMapParameter", Collections.singletonMap("key", "s2"));
    parameters.put("stringParameter", "s3");

    final BusinessApplicationPluginExecutor executor = new BusinessApplicationPluginExecutor();
    final Map<String, Object> results = executor.execute(
      "StructuredToStructured", parameters);
    for (final String key : parameters.keySet()) {
      final Object parameterValue = parameters.get(key);
      final Object resultValue = results.get(key);
      // if (!Equals.equals(parameterValue, resultValue)) {
      // Assert.assertEquals(key, parameterValue, resultValue);
      // }
    }
  }
}
