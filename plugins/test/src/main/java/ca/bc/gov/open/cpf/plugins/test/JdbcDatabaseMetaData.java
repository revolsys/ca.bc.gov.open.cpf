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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.jdbc.JdbcConnection;
import com.revolsys.jdbc.JdbcUtils;

@BusinessApplicationPlugin(description = "Test JDBC connectivity")
public class JdbcDatabaseMetaData {

  private DataSource dataSource;

  private String databaseProductName;

  public void execute() {
    try (
      JdbcConnection connection = new JdbcConnection(this.dataSource)) {
      final DatabaseMetaData metaData = connection.getMetaData();
      this.databaseProductName = metaData.getDatabaseProductName();
    } catch (final SQLException e) {
      JdbcUtils.getException(this.dataSource, null, "Get MetaData", null, e);
    }
  }

  @ResultAttribute
  public String getDatabaseProductName() {
    return this.databaseProductName;
  }

  public void setDataSource(final DataSource dataSource) {
    this.dataSource = dataSource;
  }
}
