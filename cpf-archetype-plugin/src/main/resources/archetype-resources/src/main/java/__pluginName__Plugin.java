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
package $package;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

@BusinessApplicationPlugin(name = "${pluginName}", version = "${version}")
public class ${pluginName}Plugin {
  private String resultAttribute;
  
  private String parameter;
  
  @ResultAttribute
  public String getResultAttribute() {
    // TODO Replace this method with on method for each response field
    return resultAttribute;
  }

  @JobParameter
  @RequestParameter
  public void setParameter(String parameter) {
    // TODO Replace this method with one method for each parameter
    this.parameter = parameter;
  }

  public void execute() {
    // TODO Replace this with code to perform the function of your plugin
    this.resultAttribute = parameter;
  }
}