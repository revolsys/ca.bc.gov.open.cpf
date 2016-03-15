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
package ca.bc.gov.open.cpf.api.domain;

import com.revolsys.io.PathName;

public interface UserAccount extends Common {
  String ACTIVE_IND = "ACTIVE_IND";

  String CONSUMER_KEY = "CONSUMER_KEY";

  String CONSUMER_SECRET = "CONSUMER_SECRET";

  PathName USER_ACCOUNT = PathName.newPathName("/CPF/CPF_USER_ACCOUNTS");

  String USER_ACCOUNT_CLASS = "USER_ACCOUNT_CLASS";

  String USER_ACCOUNT_CLASS_CPF = "CPF";

  String USER_NAME = "USER_NAME";

  String USER_ACCOUNT_ID = "USER_ACCOUNT_ID";
}
