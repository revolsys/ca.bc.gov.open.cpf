/*
 * Copyright © 2008-2015, Province of British Columbia
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
package ca.bc.gov.open.cpf.api.domain;

import com.revolsys.io.PathName;

public interface ConfigProperty extends Common {
  String COMPONENT_NAME = "COMPONENT_NAME";

  PathName CONFIG_PROPERTY = PathName.newPathName("/CPF/CPF_CONFIG_PROPERTIES");

  Object CPF_TUNING = "CPF_TUNING";

  String DEFAULT = "default";

  String ENVIRONMENT_NAME = "ENVIRONMENT_NAME";

  String MODULE_BEAN_PROPERTY = "MODULE_BEAN_PROPERTY";

  String MODULE_CONFIG = "MODULE_CONFIG";

  String MODULE_NAME = "MODULE_NAME";

  String PROPERTY_NAME = "PROPERTY_NAME";

  String PROPERTY_VALUE = "PROPERTY_VALUE";

  String PROPERTY_VALUE_TYPE = "PROPERTY_VALUE_TYPE";

  String GLOBAL = "GLOBAL";
}
