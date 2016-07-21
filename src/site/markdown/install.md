## CPF Installation

Plug-in developers will want to install a copy of CPF on their local environment to develop and
test their CPF plug-ins. The following sections included detailed steps on installing the CPF
database and web application.

## Database Installation

The CPF requires a PostgreSQL or Oracle database to be installed for use by the CPF application.

A local copy of the CPF databases should be deployed at a developer's site. For projects with
multiple developers it is recommended to install a local database on each developer's workstation
and one on a central integration test server.

> **NOTE:** Installation of database for any business application plug-ins is outside the
> scope of the CPF.

### Requirements

The CPF requires a database to store the CPF configuration, Job requests and Job results. The
following databases are currently supported.

* PostgreSQL 9.1+
* Oracle 10g, 11g, or 12c 

A developer may also require additional databases for use by their plug-in. They must deliver all
required SQL scripts and instructions on how to install these databases.

> **NOTE:** For PostgreSQL the server instance can be shared with other applications but it is recommended
> to have a separate "database" within that instance for CPF. For Oracle there is no such restriction.

### Download SQL Scripts

The SQL scripts to install the database can be downloaded from the https://github.com/bcgov/cpf
repository.

Use the following scripts will download the CPF Oracle and PostgreSQL scripts.

**UNIX/Mac**

```bash
svn co https://github.com/bcgov/cpf/trunk/sql
cd sql
```
  
**Windows**

```winbatch
svn co https://github.com/bcgov/cpf/trunk/sql
cd sql
```

> **NOTE:** If you have previously downloaded the SQL use the following command from the sql
directory to ensure that you have the latest version.

```
svn up
```

### CPF Database Install Configuration

The database install scripts use the db.properties configuration file for database connection
and configuration parameters. Copy the db-sample.properties file from the postgresql or oracle
directory to use as a template.

> **NOTE:** Change permissions on the `db.properties` so that only you have read/write permissions on
> the file to keep the passwords secret.

Edit the `db.properties` file.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Property</th>
      <th>Example Value</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>DB_HOSTNAME</code></td>
      <td><code>localhost</code></td>
      <td>The hostname of the PostgreSQL sever. Not used for Oracle.</td>
    </tr>
    <tr>
      <td><code>DB_PORT</code></td>
      <td><code>5432</code></td>
      <td>The port of the PostgreSQL sever. Not used for Oracle.</td>
    </tr>
    <tr>
      <td><code>DB_NAME</code></td>
      <td><code>cpf</code></td>
      <td>The PostgreSQL database name or Oracle TNSNAME, tnsnames.ora must be configured.</td>
    </tr>
    <tr>
      <td><code>CPF_PASSWORD</code></td>
      <td><code>cpf_0wn3r</code></td>
      <td>The password to create the CPF database account with.</td>
    </tr>
    <tr>
      <td><code>PROXY_CPF_WEB_PASSWORD</code></td>
      <td><code>c0ncurr3n7</code></td>
      <td>The password to create the PROXY_CPF_WEB database account with.</td>
    </tr>
    <tr>
      <td><code>TABLESPACE_DIR</code></td>
      <td><code>/data/postgres/cpf
c:\data\postgres\cpf</code></td>
      <td>The directory to create the database tablespace in. The directory must exist on the
      **server** and the PostgreSQL or Oracle process must have write permissions on this
      directory.</td>
    </tr>
  </tbody>
</table></div>


For PostgreSQL, to avoid needing to enter in the passwords for each SQL command create a `~/.pgpass`
on UNIX or `%APPDATA%\postgresql\pgpass.conf` file on Windows. Set the permissions so that only you
can read/write the `pgpass.conf` file. The file can be deleted after installation if required. The
file should look something like this.

```
localhost:5432:*:postgres:postgres
localhost:5432:*:cpf:cpf_0wn3r
```
  
### CPF Database and schema install

The CPF installation process will create the following database objects. If the objects already
exist the script will either ignore that step or in the case of CPF tables it will prompt to confirm
that the existing tables should be deleted.

* Create a cpf database for PostgreSQL. An existing Oracle database must already exist.
* Create a `CPF tablespace` to store the CPF data.
* Create a `CPF_WEB_PROXY` database role that will have CRUD permission on the tables.
* Create a `CPF` database user account that this the owner of all the CPF tables. **NOTE:**
  This account must not be used for any other purpose than managing the table definitions.
* Create a `PROXY_CPF_WEB` user account that is used by the CPF web application to access the database.
* Create a `CPF` schema for all of the CPF tables, sequences and indexes.
* Create all the CPF tables, sequences and indexes and grant appropriate permissions for these tables.

NOTE: For Oracle you will need to know the password for the SYSTEM database account, you will be
prompted to enter this in the script.

**Unix/Mac**
```
DB_VENDOR={postgresql|oracle}
cd cpf/sql/${DB_VENDOR}
./install.sh
```

**Windows**
```
set DB_VENDOR={postgresql|oracle}
cd  cpf\sql\%DB_VENDOR%
install.cmd
```
  
During the installation script you may be prompted for the following information.


* Passwords for the `SYSTEM` or `postgres` user accounts.
* If the database already exists the following prompt will be displayed. Entering **YES** will delete
  all the existing data and create new tables.  
  ```WARN: Do you want to drop the existing database including all data (YES/NO)?```


> **NOTE:** Contact the CPF developer, if there are any errors while running the script that are NOT
> related to file permissions or unknown user account passwords.

## Web Application Installation

A local copy of the CPF web applications and databases should be deployed at the developer's site.
For projects with multiple developers it is recommended to install a local database and J2EE servlet
container (Apached Tomcat) on each developer's workstation and one on a central integration test server.

The CPF applications are deployed to a J2EE application server or servlet container. To deploy to a
J2EE Servlet container the individual wars are deployed to the J2EE Servlet container.

Deployment is currently supported on [Apache Tomcat > 8.x](http://tomcat.apache.org). CPF may work
withother J2EE Servlet or application containers but this has not been tested.

For Tomcat 8.x you will need to add a user account in the manager-script role to deploy the web
applications to the tomcat contained. If a user does not exist edit the `tomcat-users.xml`
file in the tomcat conf directory.

```xml
<role rolename="manager-script"/>
<user username="admin" password="*****" roles="manager-script"/>
```

### Create CPF directories

The CPF requires directories to be created on the server. The following directories must be created.

> **NOTE**: This assumes the CPF home directory is /apps/cpf. Modify the commands and configuration
> below if a different directory is used.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
  <tr>
    <th>Directory</th>
    <th>User Perms</th>
    <th>J2EE Server Perms</th>
    <th>Description</th>
  </tr>
  </thead>
  <tbody>
  <tr>
    <td><code>/apps/cpf/config</code></td>
    <td><code>rw</code></td>
    <td><code>r</code></td>
    <td>The directory containing the CPF configuration file for the database URL, username and
    password.</td>
  </tr>
  <tr>
    <td><code>/apps/cpf/log</code></td>
    <td><code>r</code></td>
    <td><code>rw</code></td>
    <td>The directory to store the CPF logs.</td>
  </tr>
  <tr>
    <td><code>/apps/cpf/repository`
    or
    `/home/{username}/.m2/repository</code></td>
    <td><code>rw</code></td>
    <td><code>rw</code></td>
    <td>The local Maven repository cache. If the J2EE server is on the developers workstation use
    the user's local maven repository cache.</td>
  </tr>
  </tbody>
</table></div>

Create the directories using the following commands. Make sure the directory permissions are set
as shown in the table above.

**UNIX/Mac**
```bash
mkdir -p /apps/cpf
mkdir -p /apps/cpf/config
mkdir -p /apps/cpf/log
mkdir -p /apps/cpf/repository
```

**Windows**
```winbatch
md \apps\cpf
md \apps\cpf\config
md \apps\cpf\log
md \apps\cpf\repository
```

### Create a CPF web application project

The CPF application can be configured to connect to different types of database and be configured or
extended in other ways. Therefore instead of delivering a pre-packaged war file a maven project is
created for each installation that contains the configuration for that environment.

Create the maven project using the following maven archetype commands. Replace any values in
**bold** with the correct values for your environment.

> **NOTE:** Java 1.8.0 and Maven 3.3+ must be install. JAVA_HOME and M2_HOME must be set and the
> bin directories from both must be in the PATH.

**UNIX/Mac**
```bash
CPF_VERSION=**5.0.0-SNAPSHOT**
cd **~/projects**
mvn archetype:generate -DinteractiveMode=false -DarchetypeGroupId=ca.bc.gov.open.cpf -DarchetypeArtifactId=cpf-archetype-web-DarchetypeVersion=${CPF_VERSION} -DgroupId=**com.mycompany** -DartifactId=**cpf* -Dversion=**1.0.0-SNAPSHOT** -DmodulePrefix=**cpf** -DdatabaseVendor=**postgresql** -DdatabasePassword=**c0ncurr3n7** -DworkerPassword=**cpf_w0rk3r** -DcpfLogDirectory=**/apps/cpf/log** -DcpfDirectoryUrl=**file:///apps/cpf** -DmavenCacheDirectoryUrl=**file:///home/$USER/.m2/repository**```

**Windows**
```winbatch
set CPF_VERSION=**5.0.0-SNAPSHOT**
cd **%HOMEDRIVE%%HOMEPATH%\projects**
mvn archetype:generate -DinteractiveMode=false -DarchetypeGroupId=ca.bc.gov.open.cpf -DarchetypeArtifactId=cpf-archetype-web -DarchetypeVersion=%CPF_VERSION% -DgroupId=**com.mycompany** -DartifactId=**cpf** -Dversion=**1.0.0-SNAPSHOT** -DmodulePrefix=**cpf** -DdatabaseVendor=**postgresql** -DdatabasePassword=**c0ncurr3n7** -DworkerPassword=**cpf_w0rk3r** -DcpfLogDirectory=**C:/apps/cpf/log** -DcpfDirectoryUrl=**file:/C:/apps/cpf** -DmavenCacheDirectoryUrl=**file:/C:/apps/cpf/repository**
```

> **NOTE:** Windows and Unix require commands to be entered on a single line. The \ or ^ character
> are line continuation character that treats multiple lines as a single line. Therefore you can cut
and paste the above text into a command window.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
    <th>Parameter</th>
    <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>archetypeVersion</code></td>
      <td>The most recent version of the CPF framework.</td>
    </tr>
    <tr>
      <td><code>groupId</code></td>
      <td>The maven group identifier. This should be your company name if deploying within your development environment.</td>
    </tr>
    <tr>
      <td><code>artifactId</code></td>
      <td>The base maven artifact identifier used for the maven modules created in the project.</td>
    </tr>
    <tr>
      <td><code>version</code></td>
      <td>The version identifier you’d like to give to your plug-in.</td>
    </tr>
    <tr>
      <td><code>modulePrefix</code></td>
      <td>The prefix to use on the web applications.</td>
    </tr>
    <tr>
      <td><code>databaseVendor</code></td>
      <td>The database type that the CPF uses for its data. Supported values include postgresql and oracle.</td>
    </tr>
    <tr>
      <td><code>databasePassword</code></td>
      <td>The password for the PROXY_CPF_WEB user (PROXY_CPF_WEB_PASSWORD from db.properties). </td>
    </tr>
    <tr>
      <td><code>workerPassword</code></td>
      <td>The password for the cpf_worker CPF user account. Default is cpf_w0rk3r. Change if required
      using the CPF admin application.</td>
    </tr>
    <tr>
      <td><code>cpfLogDirectory</code></td>
      <td>The directory for the CPF log files will be stored in (e.g. `/apps/cpf/log` or `C:\apps\cpf\log`).</td>
    </tr>
    <tr>
      <td><code>cpfDirectoryUrl</code></td>
      <td>The root directory the CPF configuration file and log files will be stored in (e.g. `file:///apps/cpf` or `file:/C:\apps\cpf`).</td>
    </tr>
    <tr>
      <td><code>mavenCacheDirectoryUrl</code></td>
      <td>The file URL to local Maven repository cache. <b>NOTE:</b> Must start with file:/// or file:/ and use web
      slashes / instead of windows slashes \. If the J2EE server is on the developers workstation
      use the user's local maven repository cache. Otherwise use the repository directory below the
      `cpfDirectory` defined above (e.g. `file:///apps/cpf/repository` or
      `file:///C:/apps/cpf/repository`).</td>
    </tr>
  </tbody>
</table></div>

The following directory structure would be created if the command were run using the parameters above.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
    <th>File/Directory</th>
    <th>Description</th>
    </tr>
    </thead>
    <tr>
    <td><code>cpf</code></td>
    <td>The root directory of the web project.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;cpf.app</code></td>
    <td>The maven module for the web application containing the web services and scheduler.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;pom.xml</code></td>
    <td>The maven build file for the web services and scheduler.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;src/main/resources</code></td>
    <td>The resources to be included in the web application jar file.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cpf-api-properties.sf.xml</code></td>
    <td>The configuration file for the CPF API components.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cpf-web-properties.sf.xml</code></td>
    <td>The configuration file for the CPF web components.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;src/main/webapp/META-INF/context.xml</code></td>
    <td>Tomcat context configuration.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;src/main/webapp/web.xml</code></td>
    <td>The web.xml file.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;cpf.worker</code></td>
    <td>The maven module for the web application containing the worker.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;pom.xml</code></td>
    <td>The maven build file for the worker.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;src/main/resources</code></td>
    <td>The resources to be included in the web applications jar file.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cpfWorker.json</code></td>
    <td>The configuration file for the worker.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;&nbsp;&nbsp;src/main/webapp/META-INF/context.xml</code></td>
    <td>Tomcat context configuration.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;pom.xml</code></td>
    <td>The parent maven build file that builds all modules.</td>
    </tr>
    <tr>
    <td><code>&nbsp;&nbsp;sample-config/cpf.properties</code></td>
    <td>A sample config file to copy to `/apps/cpf/conf/`.</td>
    </tr>
</table></div>

  
> **NOTE:** Developers shouldn't need to edit any of these configuration files. They are 
> populated using the parameters specified in the maven archetype. If you need to change the cpf 
> directory location or the database vendor it's probably easier to delete the project and create a 
> new project using the maven archetype. All database configurations are done using runtime 
> configuration files and plug-in configuration is stored in the database.

### Maven Settings Configuration

A profile must be created in the `~/.m2/settings.xml` for each server environment that the CPF 
components are deployed to. The following example shows a full settings file with a single profile 
for the localhost server.

If you want to deploy the application to multiple servers you can create a profile for each server 
in your `~/.m2/settings.xml`. The profile id should be the name of the server to deploy to. For 
example the following shows a profile for the localhost.

```xml
<settings>
  <profile>
    <id><b>localhost</b></id>
    <properties>
      <!-- Include the following for Tomcat deployment -->
      <tomcatManagerUrl><b>http://localhost:8080/manager/text</b></tomcatManagerUrl>
      <tomcatManagerUsername><b>admin</b></tomcatManagerUsername>
      <tomcatManagerPassword><b>********</b></tomcatManagerPassword>
    </properties>
  </profile>
</settings>
```

### CPF Runtime Configuration

Any configuration that changes from one server to another is included in a runtime configuration
file in the `/apps/cpf/config` directory on the server. No environment specific configuration is
included in the compiled WAR files, i.e. the same war could be deployed to delivery, test and
production environments.

Copy the `sample-config/cpf.properties` file to the `/apps/cpf/config/cpf.properties` directory on
the server.


<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
    <th>Property</th>
    <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
    <td><code>cpfConfig.baseUrl</code></td>
    <td>The base URL to the CPF apps web application (e.g. `http://localhost/pub/cpf`).</td>
    </tr>
    <tr>
    <td><code>cpfDataSource.url</code></td>
    <td>The full JDBC URL to the CPF database server (e.g. jdbc:postgresql:cpf).</td>
    </tr>
    <tr>
    <td><code>cpfDataSource.password</code></td>
    <td>The password for the CPF database (e.g. c0ncurr3n7).</td>
    </tr>
    <tr>
    <td><code>cpfWorker.webServiceUrl</code></td>
    <td>The base url to the internal web services (e.g. `http://localhost:8080/pub/cpf`). Must be the
    direct tomcat HTTP port and not behind an Apache reverse proxy.</td>
    </tr>
    <tr>
    <td><code>cpfWorker.password</code></td>
    <td>The password used in the internal web services (e.g. cpf_w0rk3r). Must be an
     `http://open.gov.bc.ca/cpf/SystemUser` user in the cpf.cpf_user_accounts table.</td>
    </tr>
  </tbody>
</table></div>

### External Maven Repository Configuration

If the CPF is not deployed on the development server then the worker will need to be configured to
point to the maven respositories that the CPF plug-ins are deployed to.

Edit the following configuration file. You will need to re-build and deploy if this file is modified.

```
~/projects/cpf/cpf.app/src/main/resources/cpf-web-properties.sf.xml
```

Add the following entries to the cpfWorkerProperties map. The first entry must be modified to the
value used for the mavenCacheDirectory specified above. The second entry value must be the local
file URL or remote http URL to a shared maven repository that you will deploy the plug-ins to.
See [Maven Deploy Plug-in](http://maven.apache.org/plug-ins/maven-deploy-plug-in/) for deploying to
a maven repository.

```xml
<entry
  key="mavenRepository.root"
  value="<b>file:///apps/cpf/repository/</b>"
/>

<entry key="mavenRepository.repositoryLocations">
  <list>
    <value>http://mycompany.com/maven/repository/</value>
  </list>
</entry>
```

### Deploy to Tomcat 8

The plug-in project web services &amp; scheduler war and worker war files can be deployed to a
Tomcat 8 server.

Use the following command to compile and deploy to Tomcat.

```
mvn -P tomcat8Deploy,**localhost** clean install
```

If you created multiple profiles use the profile name of the server you wish to deploy to.
