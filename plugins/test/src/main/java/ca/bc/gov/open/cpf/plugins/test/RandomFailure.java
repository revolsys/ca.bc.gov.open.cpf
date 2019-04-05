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
package ca.bc.gov.open.cpf.plugins.test;

import org.jeometry.common.math.MathUtil;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

@BusinessApplicationPlugin(description = "Fails randomly", numRequestsPerWorker = 10)
public class RandomFailure {

  private int max;

  private int value;

  public void execute() {
    this.value = (int)Math.round(MathUtil.randomRange(0, this.max));
    final int mod = this.value % 3;
    if (mod == 0) {

    } else if (mod == 1) {
      throw new IllegalArgumentException("Argument Failure: " + this.value);
    } else {
      throw new RuntimeException("Execution failure: " + this.value);
    }
  }

  @ResultAttribute
  public int getValue() {
    return this.value;
  }

  @JobParameter
  @RequestParameter
  @DefaultValue("2")
  public void setMax(final int maxValue) {
    this.max = maxValue;
  }
}
