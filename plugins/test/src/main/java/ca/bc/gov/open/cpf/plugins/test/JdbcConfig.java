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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import com.revolsys.jdbc.io.JdbcDatabaseFactory;

public class JdbcConfig {
  private String url;

  private String username;

  private String password;

  private DataSource dataSource;

  @PreDestroy
  public void close() {
    this.dataSource = JdbcDatabaseFactory.closeDataSource(this.dataSource);
  }

  public DataSource getDataSource() {
    return this.dataSource;
  }

  public String getPassword() {
    return this.password;
  }

  public String getUrl() {
    return this.url;
  }

  public String getUsername() {
    return this.username;
  }

  @PostConstruct
  public void init() {
    this.dataSource = JdbcDatabaseFactory.dataSource(this.url, this.username, this.password);
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public void setUsername(final String username) {
    this.username = username;
  }
}
