## Administration

This page describes how to use the CPF administration application to manage the CPF and business
application plug-ins.

### Overview

The following sections provide a quick jumping point to various administration tasks.

#### Configuring CPF

The following sections describe how to configure the CPF.

* Configure any [CPF Configuration Properties](#CPF_Configuration_Properties).
* Add the administration users to the [CPF_ADMIN User Group](#User_Group_List).

#### Adding Modules

The following sections describe how to manage users, groups and permissions the CPF.

* [Add a new Module](#Add_Module).
* Configure any [Module Configuration Properties](#Module_Configuration_Properties) documented in the plug-ins readme.
* Configure any [Business Application Configuration Properties](#Business_Application_Configuration_Properties) documented in the plug-ins readme.
* Add the module administration users to the [Module Admin User Groups](#Module_Admin_User_Group_List).
* Add users to the [Module User Groups](#Module_User_Group_List) documented in the plug-ins readme.
* Add any [User Group Permission](#User_Group_Permission) documented in the plug-ins readme.

#### Security

The following sections describe how to manage users, groups and permissions the CPF.

* View and manage [User Accounts](#User).
* View and manage [User Groups](#User Group).
* View and manage [User Group Permission](#User_Group_Permission).

#### Monitoring

The following sections describe how to monitor the CPF.

* View [Module Status](#Module_List).
* View and manage [Batch Jobs](#Jobs).
* View [Dashboard Statistics](#Statistics).
* View [Workers](#Worker), [Module Status](#Worker_Module_List) and
  [Worker Executing Group List](#Worker_Executing_Group_List).
* View [Log Files](#Log)

### Module

#### Module Overview

The business application plug-ins are deployed to CPF as Maven Modules. For each plug-in
a new Module is created in the admin application that specifies a [module name](#Module_moduleName)
and [maven module id](#Module_mavenModuleId).
 
The CPF then automatically downloads the Maven Module from the maven servers when the module is
started. TheCPF caches modules in the local Maven Repository Cache Directory.
More details on plug-in modules can be viewed in the [Plug-in Developers Guide](plugin.html).

In addition to the module administration pages described here the following pages describe how to
manage items related to a module.

* [Business Application](#Application)
  * [Configuration Property](#Configuration)
  * [Dashboard Statistics](#Statistics)
  * [Business Application Jobs](#Business_Application_Batch_Job_List)
* [Module User Groups](#Module_User_Group_List)
* [Module Admin User Groups](#Module_Admin_User_Group_List)
* [User Group Permissions](#User_Group_Permission)
* [Module Configuration Properties](#Module_Configuration_Properties)

> **NOTE** The user must be in the ADMIN, ADMIN_MODULE_[MODULE_NAME]_ADMIN, or
> ADMIN_MODULE_[MODULE_NAME]_SECURITY groups to manage modules. Module admins will only
> see modules they are admins for.

##### Module Fields

The following table summarizes the fields used for modules.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Field</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td id="Module_moduleName">Module Name</td>
      <td>The upper case name (acronym) for the module (e.g. BGEO,DEMO).
      Consult the plug-ins readme for the correct value to use.</td>
    </tr>
    <tr>
      <td id="Module_mavenModuleId">Maven Module ID</td>
      <td>The maven module Id for a module plug-in in the format <code>{groupId}:{artifactId}:{version}</code>
      (e.g <code>com.myapplication:demo:1.0.0-SNAPSHOT</code>). Consult the plug-ins readme for the correct
      value to use. <span class="note">When a new non-snapshot version is deployed the version part must be updated to the new version.</span></td>
    </tr>
    <tr>
      <td id="Module_enabled">Enabled</td>
      <td>Flag to control if the module is enabled. If not enabled the plug-in will be stopped 
      on the master and worker and won't be available for clients to submit jobs. <span class="note">Disabled modules
      may cause broken links from jobs to apps and modules.</span></td>
    </tr>
    <tr>
      <td id="Module_status">Status</td>
      <td>The current status of the module. Disabled, Enabled, Stopped, Stop Requested, Started, Start Requested, Starting, Start Failed.</td>
    </tr>
    <tr>
      <td id="Module_started">Started</td>
      <td>Flag indicating if the module has been started.</td>
    </tr>
    <tr>
      <td id="Module_startTime">Start Time</td>
      <td>The timestamp when the module was started or blank if it has not been started.</td>
    </tr>
    <tr>
      <td id="Module_moduleError">Module Error</td>
      <td>Error message and stack trace if the module could not be started.</td>
    </tr>
    <tr>
      <td id="Module_actions">Actions</td>
      <td>The actions field will display buttons  depending on the state of the module.
      <ul>
      <li>Delete for disabled modules.</li>
      <li>Restart, stop and delete for started modules.</li>
      <li>Start and delete for stopped modules.</li>
      </ul></td>
    </tr>
  </tbody>
</table></div>

#### Module List

The modules page contains a scrolling table of the modules. The contents of this table can be
filtered by typing at least 3 characters in search box. The table will show modules where the name,
maven module id, status, or start time contain those characters characters (ignoring case).

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `'Modules'` menu item.

#### View Module

Modules can be viewed using the following steps.

1. Open the [Module List](#Module_List) page.
2. Scroll or search for the module.
3. Click the link in the `Name` column.

#### Add Module

New modules can be added using the following steps

> **NOTE** The user must be in the ADMIN group to add modules. 

1. Open the [Module List](#Module_List) page.
2. Click <button>Add</button> button.
3. Edit the field values.
    1. Enter a Module Name.
        * Must be unique across all module names.
        * Can only contain the characters `a-z`, `0-9`, or `_`.
        * Automatically converted to upper case.
        * Must not be one of the reserved words CPF, VIEW, EDIT, ADD, DELETE, APP, ADMIN, DEFAULT,
          COPY, CLONE, MODULE, GROUP.
    2. Enter the [Maven Module Id](#Module_mavenModuleId).
    3. Select if the module is to be [enabled](#Module_enabled).
4. Click the <button>Save</button> button to create the module, <button>Clear Fields</button> to undo any changes, or <button>Cancel</button> to
   return to the previous page.

#### Delete Module

A module can be deleted using the following steps.

> **NOTE** The user must be in the ADMIN group to delete modules.

> **NOTE** This will permanently delete the module, module configuration properties and
  module user groups. It will not delete any jobs for business applications in the module. Any
  uncompleted jobs will not complete unless the module is added again. To update a module to a new
  version edit the module rather than deleting and adding a new module.

1. Open the [Module List](#Module_List) or [View Module](#View_Module) page.
2. Click trash button in the `Actions` field for the module to delete.
3. Click the <button>OK</button> button on the confirm delete dialog to delete the module, or <button>Cancel</button> to return
   to the page.

#### Edit Module

Modules can be edited using the following steps.
only the [Maven Module Id](#Module_mavenModuleId) and [enabled](#Module_enabled) flag can be edited.

> **NOTE** The user must be in the ADMIN group to edit modules. 

> **NOTE** Editing a module will cause an immediate restart of the module on the master and all workers.

1. Open the [View Module](#View_Module) page.
2. Click the <button>Edit</button> button.
3. Edit the field values.
    1. Enter the [Maven Module Id](#Module_mavenModuleId).
    2. Select if the user is to be [enabled](#Module_enabled).
4. Click the <button>Save</button> button to save changes, `Revert to Saved` to undo any changes, or <button>Cancel</button> to return to previous page.

#### Start Module

A module can be started using the following steps.

> **NOTE** The user must be in the ADMIN group to start modules. 

> **NOTE** Starting a module will cause an immediate start of the module on the master and all workers.

1. Open the [Module List](#Module_List) or [View Module](#View_Module) page.
2. Click restart button in the `'Actions'` field for the module to start.
3. Click the <button>OK</button> button on the confirm dialog to start the module, or <button>Cancel</button> to return to the page.

#### Restart Module

A module can be restarted using the following steps.

> **NOTE** The user must be in the ADMIN group to restart modules. 

> **NOTE** Restarting a module will cause an immediate restart of the module on the master and all workers.

1. Open the [Module List](#Module_List) or [View Module](#View_Module) page.
2. Click restart button in the `'Actions'` field for the module to restart.
3. Click the <button>OK</button> button on the confirm dialog to restart the module, or <button>Cancel</button> to return to the page.

#### Stop Module

A module can be stopped using the following steps.

> **NOTE** The user must be in the ADMIN group to stop modules. 

> **NOTE** Stopping a module will cause an immediate stop of the module on the master and all workers.

1. Open the [Module List](#Module_List) or [View Module](#View_Module) page.
2. Click stop button in the `'Actions'` field for the module to stop.
3. Click the <button>OK</button> button on the confirm dialog to stop the module, or <button>Cancel</button> to return to the page.

### Application

#### Business Application Overview

Each CPF [Module](#Module) can contain one or more business application.
This page describes how to manage those business applications.

In addition to the business application administration pages described here the following sections
describe how to manage items related to a business application.

* [Dashboard Statistics](#Business_Application_Statistics)
* [Config Properties](#Business_Application_Configuration_Properties)
* [Batch Jobs](#Business_Application_Batch_Job_List)

> **NOTE:** The user must be in the ADMIN, or ADMIN_MODULE_[MODULE_NAME]_ADMIN groups
> to manage business applications. Module admins will only see business applications they are
> module admins for.

##### Business Application Fields

The following table summarizes the fields used for business applications.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th id="BusinessApplication_name">Name</th>
      <td>The name of the module. Used for links and to flag the app for a job.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_moduleName">Module</th>
      <td>The name of the <a href="#Module">Module</a> the business application is loaded from.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_title">Title</th>
      <td>The display title shown on the web app for the business application.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_descriptionUrl">Description Url</th>
      <td>URL to an external page providing more documentation for the module.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_logLevel">Log Level</th>
      <td>The level log logging ERROR, INFO, DEBUG. ERROR should be used unless
      there is an issue to be diagnosed. INFO and DEBUG generate large log files and slow
      down the application.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_testModeEnabled">Test Mode Enabled</th>
      <td>Flag indicating if the web services show the test mode fields when submitting a job.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_batchModePermission">Batch Mode Permission</th>
      <td>The <a href="#Permission_Strings">permission</a> to restrict access to submission of jobs.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_instantModePermission">Instant Mode Permission</th>
      <td>The <a href="#Permission_Strings">permission</a> to restrict access to submission of instant mode requests.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_geometryFactory">Geometry Factory</th>
      <td>The geometry factory that all geometries will be converted to before execution on the plug-in.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_validateGeometry">Validate Geometry</th>
      <td>Flag indicating if the geometries should be validated and rejected if invalid before execution by the plug-in.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_perRequestInputData">Per Request Input Data</th>
      <td>Flag indicating if the plug-in accepts multiple files with each file being a separate
      request or a single structured data file with each record being a separate request.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_inputDataContentTypes">Input Data Content Types</th>
      <td>The list of MIME types of for the supported input data files.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_hasGeometryRequestAttribute">Has Geometry Request Attribute</th>
      <td>Flag indicating if the business application accepts a geometry as one of the request parameters.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_perRequestResultData">Per Request Result Data</th>
      <td>Flag indicating if the plug-in returns a separate file for each request or generates
      a single structured data file with each record being a the result for each request.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_resultDataContentTypes">Result Data Content Types</th>
      <td>The list of MIME types of for the supported result data file formats.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_hasCustomizationProperties">Has Customization Properties</th>
      <td>Flag indicating if the plug-in customizes the form or result formats.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_resultListProperty">Result List Property</th>
      <td>Flag indicating if the plug-in returns more than one result for a single request.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_hasResultListCustomizationProperties">Has Result List Customization Properties</th>
      <td>Flag indicating if the plug-in has customization properties on a result list property.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_hasGeometryResultAttribute">Has Geometry Result Attribute</th>
      <td>Flag indicating if the business application returns a geometry as one of the result parameters.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_maxRequestsPerJob">Max Requests Per Job</th>
      <td>The maximum number of requests that can be submitted in a job. Jobs with more requests
      than the maximum will be rejected.</td>
    </tr>
    <tr id="BusinessApplication_maxConcurrentRequests">
      <th>Max Concurrent Requests</th>
      <td>The maximum number of execution groups that will be scheduled at a single time. This
      should not be more than the total number of <a href="#ConfigProperty_workerPoolSize">cpfWorker.maximumPoolSize</a>
      across all workers. If plug-ins use database connections then it should not exceed the
      maximum database connection pool size or connection limits.</td>
    </tr>
    <tr>
      <th id="BusinessApplication_numRequestsPerWorker">Num Requests Per Worker</th>
      <td>The maximum number of requests that will be set in an execution group to a worker for
      sequential execution. The CPF splits the request into this number of execution groups
      when the job is pre-processed. The goal should be to have execution groups take between
      1-10 seconds to execute on the worker. The balance should be between the size of data
      for an execution group and the execution time. This will help reduce the overhead of the system.
      For example if a request takes 10ms to run a value of 100 would take approximately 1 second
      to run on the worker.</td>
    </tr>
  </tbody>
</table></div>

##### Permission Strings
Permissions are defined using a
[Spring Security Expression](http://docs.spring.io/spring-security/site/docs/3.0.x/reference/el-access.html)
that if evaluated to true will grant the user permission.

The simplest permission is uses the `hasRole` function to see if the user is a member of a CPF
group. The function takes a single argument that is the CPF group name prefixed by `ROLE_`. The
following example grants access to all internal users.

```
hasRole('ROLE_BCGOV_INTERNAL')
```

The `hasAnyRole` function will allow access if the user is a member of any of the listed groups. The
following example grants access to all internal and business users.
  
```
hasAnyRole('ROLE_BCGOV_INTERNAL','ROLE_BCGOV_BUSINESS')
```

This could also be written using the `or` operator.
    
```
hasRole('ROLE_BCGOV_INTERNAL') or hasRole('ROLE_BCGOV_BUSINESS')
```

The `hasRoleRegex` will allow users who have a CPF group that matches the
[Java regular expression](http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html).
The following example matches all CPF groups that start with BCGOV

```
hasRoleRegex('ROLE_BCGOV_.*')
```

The permission `permitAll` grants all users permission.

The permission `denyAll` grants no users permission.

#### Business Application List

The business applications pages contain a scrolling table of the business applications for a module.
The contents of this  table can be filtered by typing at least 3 characters in search box. The table
will show business applications where the name, module or title contain those characters characters
(ignoring case).

##### All Business Applications List
The list of all business applications a user has permission for can be viewed using the following steps.

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `'Business Applications'` menu item.

##### Module Business Application List
The list of business applications for a module can be viewed using the following steps.

1. Open the [View Module](#View_Module) page.
2. Click the `'Business Applications'` tab.

#### View Business Application

Business Applications can be viewed using the following steps.

1. Open one of the [Business Application List](#Business_Application_List) pages.
2. Scroll or search for the business application.
3. Click the link in the `'Name'` column.

#### Edit Business Application
Business Applications can be edited using the following steps.

> **NOTE:** Editing a business application will require a manual restart of the module for the
> settings to take affect.

1. Open the [View Business Application](#View_Business_Application) page.
2. Click the <button>Edit button</button>.
3. Edit the field values.
    1. [Log Level](#BusinessApplication_logLevel)
    2. [Test Mode Enabled](#BusinessApplication_testModeEnabled)
    3. [Batch Mode Permission](#BusinessApplication_batchModePermission)
    4. [Instant Mode Permission](#BusinessApplication_instantModePermission)
    5. [Max Requests Per Job](#BusinessApplication_maxRequestsPerJob)
    6. [Max Concurrent Requests](#BusinessApplication_maxConcurrentRequests)
    7. [Num Requests Per Worker](#BusinessApplication_numRequestsPerWorker)
4. Click the <button>Save</button> button to save changes, <button>Revert to Saved</button> to undo
   any changes, or <button>Cancel</button> to return to previous page.

### Configuration

#### Configuration Property Overview

The CPF master web application, CPF workers and modules can be configured by setting config
properties.

##### Configuration Property Fields

The following table summarizes the fields used for configuration properties.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Field</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Configuration Property Id</td>
      <td>Unique sequence generated primary key for a configuration property.</td>
    </tr>
    <tr>
      <td>Environment Name</td>
      <td>The <a href="#Environment_Name">environment name</a> the configuration property will be used for.</td>
    </tr>
    <tr>
      <td>Module Name</td>
      <td>The name of the module the configuration property applies to. The special module names CPF
      and CPF_WORKER are used for CPF configuration properties.</td>
    </tr>
    <tr>
      <td>Property Name</td>
      <td>The name of the property.</td>
    </tr>
    <tr>
      <td>Value</td>
      <td>The value of the property.</td>
    </tr>
    <tr>
      <td>Value Type</td>
      <td>The <a href="#Data Type">data type</a> the value will be converted to.</td>
    </tr>
  </tbody>
</table></div>

##### Environment Name

The configuration properties support the concept of having different configuration properties
for different environments. Environment names are currently only used for workers.

The special environment name 'default' is used as a default value if a configuration property is not
created for an environment. For most properties the value of 'default' should be used.

For example the system could have two worker servers. The first has 2 CPUs and the second has 4.
The first could support 16 threads and the second 32 threads. Two environment names 2cpu and 4cpu
could be created. The first worker's environment name would be set to 2cpu and the second 4cpu.
A configuration property can be created for the property name cpfWorker.maximumPoolSize for each environment
with the values 16 and 32 respectively.

##### Data Type

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <th>Type</th>
    <th>Description</th>
  </thead>
  <tbody>
    <tr>
      <td><code>string</code></td>
      <td>String</td>
    </tr>
    <tr>
      <td><code>boolean</code></td>
      <td>Boolean</td>
    </tr>
    <tr>
      <td><code>long</code></td>
      <td>Long</td>
    </tr>
    <tr>
      <td><code>int</code></td>
      <td>Int</td>
    </tr>
    <tr>
      <td><code>double</code></td>
      <td>Double</td>
    </tr>
    <tr>
      <td><code>float</code></td>
      <td>Float</td>
    </tr>
    <tr>
      <td><code>short</code></td>
      <td>Short</td>
    </tr>
    <tr>
      <td><code>byte</code></td>
      <td>Byte</td>
    </tr>
    <tr>
      <td><code>decimal</code></td>
      <td>Big Decimal</td>
    </tr>
    <tr>
      <td><code>integer</code></td>
      <td>Big Integer</td>
    </tr>
    <tr>
      <td><code>QName</code></td>
      <td>Qualified name</td>
    </tr>
    <tr>
      <td><code>anyURI</code></td>
      <td>URI</td>
    </tr>
    <tr>
      <td><code>date</code></td>
      <td>Date (YYYY-MM-DD)</td>
    </tr>
    <tr>
      <td><code>dateTime</code></td>
      <td>Date + Time (YYYY-MM-DDTHH:MM:SS)</td>
    </tr>
    <tr>
      <td><code>Geometry</code></td>
      <td>WKT Geometry</td>
    </tr>
    <tr>
      <td><code>Point</code></td>
      <td>WKT Point</td>
    </tr>
    <tr>
      <td><code>LineString</code></td>
      <td>WKT LineString</td>
    </tr>
    <tr>
      <td><code>Polygon</code></td>
      <td>WKT Polygon</td>
    </tr>
    <tr>
      <td><code>GeometryCollection</code></td>
      <td>WKT Geometry Collection</td>
    </tr>
    <tr>
      <td><code>MultiPoint</code></td>
      <td>WKT Multi-Point</td>
    </tr>
    <tr>
      <td><code>MultiLineString</code></td>
      <td>WKT Multi-LineString</td>
    </tr>
    <tr>
      <td><code>MultiPolygon</code></td>
      <td>WKT Multi-Polygon</td>
    </tr>
  </tbody>
</table></div>

#### Configuration Property List

The configuration properties page contains a scrolling table of the configuration properties. The
contents of this table can be filtered by typing at least 3 characters in search box. The table
will show configuration properties where the environment name, module name, property name, or
property value contain those characters characters (ignoring case).

##### CPF Configuration Properties

The list of configuration properties for the [CPF Master](#CPF_Master_Configuration_Properties) and
[CPF Worker](#CPF_Worker_Configuration_Properties) can be viewed using
the following steps.

> **NOTE:** The user must be in the ADMIN user group.

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `'Configuration Properties'` menu item.

##### Module Configuration Properties

The list of configuration properties for a module can be viewed using the following steps.

> **NOTE:** The user must be in the ADMIN, or ADMIN_MODULE_[MODULE_NAME]_ADMIN groups.

> **NOTE:** Consult the module's readme file for details on the supported configuration properties.

1. Open the [View Module](#View_Module) page.
2. Click the `'Configuration Properties'` tab.

##### Business Application Configuration Properties

The list of configuration properties for a business application can be viewed using the following steps.

> **NOTE:** The user must be in the ADMIN, or ADMIN_MODULE_[MODULE_NAME]_ADMIN groups.

> **NOTE:** Consult the module's readme file for details on the supported
configuration properties for a business application.

1. Open the [View Business Application](#View Business Application) page.
2. Click the `'Configuration Properties'` tab.

#### View Configuration Property

Configuration properties can be viewed using the following steps.

1. Open one of the [Configuration Property List](#Configuration_Property_List) pages.
2. Scroll or search for the configuration property.
3. Click the link in the 'ID' column.

#### Add Configuration Property

Configuration properties not shown in the list can be added using the following steps. See

[CPF Master](#CPF_Master_Configuration_Properties) and
[CPF Workers](#CPF_Worker_Configuration_Properties) for the supported configuration properties.
Consult the plug-in's documentation for the configuration properties they support

1. Open one of the [Configuration Property List](#Configuration_Property_List) pages.
2. Click <button>Add</button> button.
    1. Edit the field values.
    2. Click the <button>Save</button> button to create the configuration property,
       <button>Clear Fields</button> to undo any changes,
       or <button>Cancel</button> to return to the previous page.

#### Delete Configuration Property

A configuration property can be deleted using the following steps.

1. Open one of the [Configuration Property List](#Configuration_Property_List) or
   [View Configuration Property](#View_Configuration_Property) pages.
2. Click trash button in the `'Actions'` field for the configuration property to delete.
3. Click the <button>OK</button> button on the confirm delete dialog to delete the configuration
   property, or <button>Cancel</button> to return to the page.

#### Edit Configuration Property

Configuration properties can be edited using the following steps.

1. Open the [View Configuration Property](#View_Configuration_Property) page.
2. Click the 'Edit button'.
3. Edit the field values.
4. Click the <button>Save</button> button to save changes, <button>Revert to Saved</button> to undo any changes,
   or <button>Cancel</button> to return to previous page.

#### Configuration Property Names & Descriptions

This section describes the configuration properties used for the CPF master web application and CPF workers.

The CPF uses [Java Properties](http://docs.oracle.com/javase/7/docs/api/java/util/Properties.html#load(java.io.Reader)) files
for bootstrap configuration of the CPF master web application and CPF workers. In the properties
files use '=' not ':' between the keys and values and escape any ':' characters in property values using '\:'.
Properties with a tick in the 'Admin App' column can also be defined using the admin application.

> **NOTE:** Any changes to configuration properties using the admin application or in the properties
> file will require the CPF master or CPF worker to be restarted for these properties to take affect.
  
##### CPF Master Configuration Properties

The CPF master web application requires the following properties to be defined in the
`/apps/config/cpf.properties` file.
The master will ignore any other properties in this file. So where the master and worker are on
the same machine they can share the same configuration file.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Property Name</th>
      <th>Description</th>
      <th>Admin App</th>
      <th>Data Type</th>
      <th>Example</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>cpfConfig.baseUrl</code></td>
      <td>The HTTP URL to the OAuth/Digest secured CPF web services, including context path on the server cpf is deployed to. The trailing / must be omitted.</td>
      <td><img src="images/cross.png" alt="No" title="No" /></td>
      <td>URI</td>
      <td><code>http://localhost/cpf/</code></td>
    </tr>
    <tr>
      <td><code>cpfDataSource.url</code></td>
      <td>The JDBC URL to the cpf database.</td>
      <td><img src="images/cross.png" alt="No" title="No" /></td>
      <td>URI</td>
      <td><code>jdbc:oracle:thin:@localhost:1521:cpf</code> or <code>jdbc\:postgresql\://localhost\:5432/cpf</code></td>
    </tr>
    <tr>
      <td><code>cpfDataSource.password</code></td>
      <td>The password for the PROXY_CPF_WEB user account.</td>
      <td><img src="images/cross.png" alt="No" title="No" /></td>
      <td>String</td>
      <td>c0ncurr3n7</td>
    </tr>
    <tr>
      <td><code>batchJobService.maxWorkerWaitTime</code></td>
      <td>The maximum time (in seconds) the worker will wait in a HTTP request for group to process before
      trying a new HTTP request. This limits the number of polling requests to the server. Must
      be less than any HTTP server inactivity timeouts.</td>
      <td><img src="images/tick.png" alt="Yes" title="Yes" /></td>
      <td>int</td>
      <td>100</td>
    </tr>
    <tr>
      <td><code>batchJobService.fromEmail</code></td>
      <td>The email address any emails will be sent from.</td>
      <td><img src="images/tick.png" alt="Yes" title="Yes" /></td>
      <td>String</td>
      <td>cpf@localhost</td>
    </tr>
    <tr>
      <td><code>mailSender.host</code></td>
      <td>The mail server to send emails via.</td>
      <td><img src="images/tick.png" alt="Yes" title="Yes" /></td>
      <td>String</td>
      <td>localhost</td>
    </tr>
    <tr>
      <td>removeOldBatchJobs.daysToKeepOldJobs</td>
      <td>The number of days that jobs will be kept after the results have been created. After
      that time the jobs will be removed in the next daily cleanup.</td>
      <td><img src="images/tick.png" alt="Yes" title="Yes" /></td>
      <td>int</td>
      <td>7</td>
    </tr>
  </tbody>
</table></div>

#####CPF Worker Configuration Properties

The CPF worker web application requires the following properties to be defined in the
`/apps/config/cpf.properties` file.
The worker will ignore any other properties in this file. So where the worker and master are on
the same machine they can share the same configuration file.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Property Name</th>
      <th>Description</th>
      <th>Admin App</th>
      <th>Data Type</th>
      <th>Example</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>cpfConfig.internalWebServiceUrl</code></td>
      <td>The HTTP URL to the CPF web services behind any reverse proxies. This is used to speed up
      access from the workers.</td>
      <td><img src="images/cross.png" alt="No" title="No" /></td>
      <td>URL</td>
      <td><code>http://localhost/pub/cpf</code></td>
    </tr>
    <tr>
      <td><code>cpfWorker.password</code></td>
      <td>The password for the cpf_worker user account.</td>
      <td><img src="images/cross.png" alt="No" title="No" /></td>
      <td>String</td>
      <td>cpf_w0rk3r</td>
    </tr>
    <tr>
      <td><code>cpfWorker.environmentName</code></td>
      <td>The <a href="#Environment_Name">environment name</a> used for the worker.</td>
      <td><img src="images/cross.png" alt="No" title="No" /></td>
      <td>String</td>
      <td>default</td>
    </tr>
    <tr>
      <td id="ConfigProperty_workerPoolSize"><code>cpfWorker.maximumPoolSize</code></td>
      <td>The maximum number of threads on the worker used to execute requests.</td>
      <td><img src="images/tick.png" alt="Yes" title="Yes" /></td>
      <td>int</td>
      <td>32</td>
    </tr>
  </tbody>
</table></div>

### Tuning
This section describes the parameters available for tuning the performance and resources
used by the system. This section does not include memory tuning as that depends on the
specific memory usage for each business application.

* [Master Tuning](#Master_Tuning)
* [Worker Tuning](#Worker_Tuning)
* [Application Tuning](#Application_Tuning)

#### Master Tuning

The tuning parameters for the master can be viewed using the `'Tuning'` page
(e.g. `http://localhost/cpf/admin/tuning/`).
The tuning page shows the number of active threads or connections, the current pool size,
largest pool size reached, and the maximum pool size. The maximum pool sizes can be configured using the
<button>Config</button>  button. Changes to the pool sizes will be 
applied upon saving. If the current active count is greater than the new pool size then those
active threads or connections will still be used until they are returned to the pool.

The table below shows the thread and database connection pools that can be configured
to tune the system.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <th>Name</th>
    <th>Description</th>
    <th>Example Value</th>
  </thead>
  <tbody>
    <tr>
      <td>Pre Process Thread Pool Size</td>
      <td>The number of threads that can be used to pre-process new jobs. The pre-processing
      splits the structured input data requests into groups of requests for execution. During pre-processing
      a datbase connection will typically be held for the entire duration of the pre-processing.
      Pre-processing is typically I/O bound.</td>
      <td>8 = Num Cores * 10% * 5 (thread multiplier)</td>
    </tr>
    <tr>
      <td>Scheduler Thread Pool Size</td>
      <td>The number of threads that can be used to schedule groups for jobs. The 
      scheduler reads the structured data for the group from the database into memory.
      The scheduling operation is typically very fast for groups with small amounts of data.
      Scheduling is typically I/O bound.</td>
      <td>16 = Num Cores * 20% * 5 (thread multiplier)</td>
    </tr>
    <tr>
      <td>Group Result Thread Pool Size</td>
      <td>The number of threads that can be used to process the group results from the workers
      via the internal web service. This is not a separate pool of threads. Instead it is the number
      of servlet container request handling threads that can be processing the group results. If
      the maximim pool size is reached the web service request
      will be bocked until a free slot is available. The group results operation is typically very fast
      for groups with small amounts of data.
      Processing group results is typically I/O bound.</td>
      <td>16 = Num Cores * 20% * 5 (thread multiplier)</td>
    </tr>
      <td>Post Process Thread Pool Size</td>
      <td>The number of threads that can be used to post-process completed jobs. The post-processing
      creates the structured result file from the results from the execution groups. During post-processing
      a datbase connection will typically be held for the entire duration of the post-processing.
      Post-processing is typically I/O bound.</td>
      <td>8 = Num Cores * 10% * 5 (thread multiplier)</td>
    <tr>
      <td>Database Connection Pool Size</td>
      <td>The number of database connections available to all web service and threads on the
      CPF master application. The minimum value for this is 10% greater than the sum of
      (Pre Process + Scheduler + Group Result + Post Process) pool sizes.</td>
      <td>60 = (Pre Process + Scheduler + Group Result + Post Process) * 125%</td>
    </tr>
  </tbody>
</table></div>

The size of the thread pools will depend on the number of processor cores. The example
values shown are for a 4 processor system, each processor with 4 cores. This gives a total
of 8 cores (2 * 4).

The number of threads configured for the CPF thread pools can exceed the number of cores
available on the server. In this case the operating system will schedule the different
threads for execution. For processes that are I/O bound (network connections, database calls,
or file I/O) the thread will be paused when the I/O operation is blocked. This allows other threads
to execute. For I/O bound tasks the number of connections in the pool can be increased to take advantage
of the idle time during blocked I/O operations. In the examples this is indicated as the thread
multiplier.

> **NOTE:** the other load on the server should also be considered when defining
> the size of the pools. The first factor is the number of threads that the servlet container
> is configured for serving HTTP requests. These connections will consume additional database
> connections for the CPF web service and CPF admin requests. Also if the database is on the
>  same server then less threads will be available for the CPF.
  
> **NOTE:** The exact values for a specific system will need to be defined
> by experimenting with typical workloads for the business application and server hardware
> configurations.

#### Worker Tuning
The CPF worker has a single tuning property [cpfWorker.maximumPoolSize](#ConfigProperty_workerPoolSize). This 
defines the number of threads used to process groups of requests. The value for this
property should be about the same as the Group Result Pool Size on the CPF master. Although
if the groups take a lot longer to execute than it takes to save the results on the master then
this value can be greater (e.g. 2x or 5x) than the Group Result Pool Size.

If the business applications are processor rather than I/O bound then the pool size can
be reduced to avoid processor thrashing when switching between threads.

> **NOTE:** the worker pool size can only be configured in the properties
> file. See [CPF Worker Configuration Properties](#CPF_Worker_Configuration_Properties) to set the
> `cpfWorker.maximumPoolSize`.

#### Application Tuning
The final part of tuning is the application tuning. See [Business Application Fields](#Business_Application_Fields)
to see the full description of the application tuning parameters and how to edit them.
  
The first parameter [numRequestsPerWorker](#BusinessApplication_numRequestsPerWorker) controls the
number of requests bundled into an execution group to send to the worker. The group size
should be configured for business applications that only take milliseconds to process each request.
In this case the value can be set to about 100 so that the group execution time takes 1-5 seconds.
This reduces the communications overhead per request due to bundling into a group. The value
should not be set to large as it will then result in more memory when the group is scheduled and
for storage in the database. This could slow down the scheduler.
  
> **NOTE:** This value is applied at the time a job is created and can't be changed for that job
> afterwards. New jobs get the new group size.
  
The second parameter [maxConcurrentRequests](#BusinessApplication_maxConcurrentRequests)
controls how many groups for the business application can be scheduled at one time. The actual
limit for the concurrent groups executing will depend on the number of threads specified in
[cpfWorker.maximumPoolSize](#ConfigProperty_workerPoolSize). For slow business applications this
can be used to limit the number of concurrent groups so that it does not prevent other apps
from being processed. For fast business applications this could be configured to be greater
than the pool of threads on the worker. This has the affect of pre-loading the scheduling queue
so that work is ready for the worker as soon as the previous group results are finished.
### User

#### User Account Overview

The CPF maintains a table of user accounts. These user accounts are used to authenticate
(login) access to the CPF web services and admin application. The user accounts are also used
to assign users to user groups.

> **NOTE:** The master and worker for a given user may cache user accounts, user groups
> and user group permissions for up to 5 minutes. Wait 5 minutes if the user doesn't have the
> correct permissions after a change.

> **NOTE:** The user must be in the ADMIN user group to view the list of all user accounts and to manage user accounts.

##### User Account Fields

The following table summarizes the fields used for user accounts.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Field</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>User Account Id</td>
      <td>Unique sequence generated primary key for a user account.</td>
    </tr>
    <tr>
      <td>Consumer Key</td>
      <td>The unique name for the user account. For CPF user accounts this is their login name.</td>
    </tr>
    <tr>
      <td>Consumer Secret</td>
      <td>The password/signing key for the user. For CPF user accounts this can be modified in the
      admin application, end users can't change it.</td>
    </tr>
    <tr>
      <td>User Account Class</td>
      <td>The class of user account CPF or BCGOV.</td>
    </tr>
    <tr>
      <td>User Name</td>
      <td>For CPF classes the user name will be kept in-sync with the consumer key.</td>
    </tr>
    <tr>
      <td>Active</td>
      <td>Boolean flag indicating if the user is active. Setting to false will prevent the user
      from accessing the CPF.</td>
    </tr>
  </tbody>
</table></div>

#### User Account Classes

The CPF provides support for multiple classes of user accounts. Developers can add new types of 
user account class to link in with their existing security mechanisms. By default the CPF is
installed with the [CPF](#CPF_User_Account_Class).

##### CPF User Account Class

The CPF user account class is used to authenticate access resource under /pub/cpf/ws/ and /pub/cpf/admin/.
In the BC Government environments users with the CPF class can be created for applications (as opposed
to end users) who are authorized to use the CPF. These applications use these accounts on the server
side to access the CPF. In this case client JavaScript use is not recommended as there is no way to hide
the consumer key or secret from end-users. In non BC Government environments (e.g. development) these
can be used for end-users or developers.

Each CPF user account has a consumer key (User identifier) and consumer secret (Password/signing key).
These are used by the user when authenticating using one of the following two authentication mechanisms.

* [HTTP Digest Access Authentication](http://tools.ietf.org/html/rfc2617) - Supported by most web browsers.
* [OAuth 1.0](http://tools.ietf.org/html/rfc5849) - Supported by the CPF Java API for direct application access to the CPF.

The CPF is installed with the following two accounts defined.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Consumer Key</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>cpf_admin</td>
      <td>The initial user account that has access to the CPF admin application. For production
      environments user accounts should be created for each admin user.
      Those accounts must be added to the ADMIN user group. Once you have been able to login using
      your own admin account the cpf_admin account should be deactivated.</td>
    </tr>
    <tr>
      <td>cpf_worker</td>
      <td>The user account that the CPF worker processes use to connect to the CPF internal web
      service. The name and password of this account can be changed as long as the worker is
      configured to use the new values. Additional worker user accounts can be added. These
      accounts must be added to the WORKER user group.</td>
    </tr>
  </tbody>
</table></div>

#### User Account List

The user accounts page contains a scrolling table of the user accounts. The contents of this 
table can be filtered by typing at least 3 characters in search box. The table will show user accounts where
the consumer key, user account class or user name contain those characters characters (ignoring case).

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `'User Accounts'` menu item.

#### View User Account

User accounts can be viewed using the following steps.

1. Open the [User Account List](#User_Account_List) page.
2. Scroll or search for the user account.
3. Click the link in the `'Consumer Key'` column.

#### Add User Account

New user accounts can be added using the following steps. Only accounts with the CPF class can
be added, therefore the User Account Class field is not shown as it is set to CPF. The created
user account's user name field will be set to the consumer key entered.

1. Open the [User Account List](#User_Account_List) page.
2. Click 'Add' button.
3. Edit the field values.
    4. Enter a Consumer Key.
        * Must be unique across all consumer keys from all classes.
        * Can only contain the characters `a-z`, `0-9`, and `_`.
        * Automatically converted to lower case.
    5. Enter a Password. A random UUID password is provided as a default.
    6. Select if the user is to be active.
7. Click the <button>Save</button> button to create the user account,
   <button>Clear Fields</button> to undo any changes,
   or <button>Cancel</button> to return to the previous page.

#### Delete User Account

A user account can be deleted using the following steps. This will also remove the user
account from any user group and delete the user's jobs. For the CPF class deleting accounts is
permanent. For BCGOV class this deletes the record, but a new record will be created when the user
next accesses CPF.

1. Open the [User Account List](#User_Account_List) or [View User Account](#View_User_Account) page.
2. Click trash button in the `'Actions'` field for the user account to delete.
3. Click the <button>OK</button> button on the confirm delete dialog to delete the user account,
   or <button>Cancel</button> to return to the page.

#### Edit User Account
User accounts can be edited using the following steps.
For the CPF class the consumer key, consumer secret and active flag can be edited.
For the BCGOV class only the active flag can be edited.

1. Open the [View User Account](#View_User_Account) page.
2. Click the 'Edit button'.
3. Edit the field values.
    1. Enter a Consumer Key.
        * Must be unique across all consumer keys from all classes.
        * Can only contain the characters `a-z`, `0-9`, and `_`.
        * Automatically converted to lower case.
    2. Enter a Password.
    3. Select if the user is to be active.
4. Click the <button>Save</button> button to save changes, <button>Revert to Saved</button> to
   undo any changes, or <button>Cancel</button> to return to previous page.
### User Group

#### User Group Overview

The CPF maintains a table of user groups. The CPF uses user groups to control access to the CPF
admin application and for modules to define the permissions end users have to use the business
applications.

> **NOTE:** The master and worker for a given user may cache user accounts, user groups
> and user group permissions for up to 5 minutes. Wait 5 minutes if the user doesn't have the
> correct permissions after a change.

##### User Group Fields

The following table summarizes the fields used for user groups.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Field</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>User Group Id</td>
      <td>Unique sequence generated primary key for a user group.</td>
    </tr>
    <tr>
      <td>Module Name</td>
      <td>The <a href="#User_Group_Module_Name">module name</a> of the CPF component or plug-in module that the group is owned by.</td>
    </tr>
    <tr>
      <td>User Group Name</td>
      <td>The unique name of the user group.
      <ul>
      <li>Must start with <code>[MODULE_NAME]_</code>.</li>
      <li>Must be unique across all user groups.</li>
      <li>Can only contain the characters <code>A-Z</code>, <code>0-9</code>, and <code>_</code>.</li>
      <li>Automatically converted to upper case.</li>
      </ul></td>
    </tr>
    <tr>
      <td>Description</td>
      <td>A human readable description of the purpose of the group.</td>
    </tr>
    <tr>
      <td>Active</td>
      <td>Boolean flag indicating if the group is active. Setting to false will prevent the group
      from being used to control security policies.</td>
    </tr>
  </tbody>
</table></div>

##### User Group Module Name

The following table describes the Module Name's that are used for User Groups.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Module Name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>GLOBAL</td>
      <td>A global user group that can be used across all modules.</td>
    </tr>
    <tr>
      <td>ADMIN</td>
      <td>Only used for the ADMIN group that defines the users who are global administrators.</td>
    </tr>
    <tr>
      <td>WORKER</td>
      <td>Only used for the WORKER group that defines the system users who can use the internal web service API.</td>
    </tr>
    <tr>
      <td>USER_TYPE</td>
      <td>Special groups that define sub classes of users for a specific user account class.
      Users can't be added to these types. Membership of these groups is automatically defined
      by the user account class plug-in for each user. It is not possible to get a list of users
      who are members of these groups. Security permissions can however be assigned to these 
      users.</td>
    </tr>
    <tr>
      <td>[MODULE_NAME]</td>
      <td>Groups can be created for a module. These can then be used to grant permissions to use
      that module. The module groups must start with [MODULE]_ (e.g. TEST_CLIENTS). The Module Name
      in this case is the name of the module.</td>
    </tr>
    <tr>
      <td>ADMIN_MODULE_[MODULE_NAME]</td>
      <td>For each module a ADMIN_MODULE_[MODULE_NAME]_ADMIN (module administrator) and
      ADMIN_MODULE_[MODULE_NAME]_SECURITY (module security administrator) group are automatically
      created. Users can be added as members of these groups to grant them permission to manage
      the module.</td>
    </tr>
  </tbody>
</table></div>

##### Default User Groups

The following table describes the user groups that are automatically created by the CPF. In
addition to these new GLOBAL and plug-in module specific groups can be created. 

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Module Name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>ADMIN</td>
      <td>Global administrator users who can manage any aspect of the CPF using the CPF admin application.</td>
    </tr>
    <tr>
      <td>SECURITY_ADMIN</td>
      <td>Global security administrator users who can manage any security permission using the CPF admin application.</td>
    </tr>
    <tr>
      <td>WORKER</td>
      <td>Only used for the WORKER group that defines the system users who can use the internal web service API.</td>
    </tr>
    <tr>
      <td>USER_TYPE</td>
      <td>Special groups that define sub classes of users for a specific user account class.
      Users can't be added to these types. Membership of these groups is automatically defined
      by the user account class plugin for each user. It is not possible to get a list of users
      who are members of these groups. Security permissions can however be assigned to these 
      users.</td>
    </tr>
    <tr>
      <td>ADMIN_MODULE_[MODULE_NAME]_ADMIN</td>
      <td>For each module a module administrator group is automatically
      created. Users can be added as members of these groups to grant them permission to manage
      the module.</td>
    </tr>
    <tr>
      <td>ADMIN_MODULE_[MODULE_NAME]_SECURITY</td>
      <td>For each module a module security administrator group id automatically
      created. Users can be added as members of these groups to grant them permission to manage
      the groups and security permissions for a module.</td>
    </tr>
  </tbody>
</table></div>

#### User Group List

The user groups pages contain a scrolling table of the user groups. The contents of this 
table can be filtered by typing at least 3 characters in search box. The table will show user groups where
the user group name, module name, description contain those characters (ignoring case).

##### All User Group List

The list of all user groups can be viewed using the following steps.

> **NOTE:** The user must be in the ADMIN user group.

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `'User Groups'` menu item.

##### Module User Group List

The list of module specific user groups can be viewed using the following steps.
The list also includes the USER_TYPE and GLOBAL user groups so that permissions can be granted to them.

> **NOTE:** The user must be in the ADMIN, ADMIN_MODULE_[MODULE_NAME]_ADMIN, or
> ADMIN_MODULE_[MODULE_NAME]_SECURITY groups.

> **NOTE:** If a GLOBAL group is viewed from this page then the option to delete
> the group or view, add, or remove members from the group will not be available. That is only
> available from the [All User Group List](#All_User_Group_List) page.

1. Open the [Module View](#View_Module) page.
2. Click the 'User Groups' tab.

##### Module Admin User Group List

The list of admin user groups for a module can be viewed using the following steps.

> **NOTE:** The user must be in the ADMIN, or ADMIN_MODULE_[MODULE_NAME]_ADMIN groups.

1. Open the [Module View](#View_Module) page.
2. Click the `'Module Admin User Groups'` tab.


#### View User Group

The details of a user group can be viewed from the [User Group List][User_Group_List] pages.

1. Open one of the [User Group List][User_Group_List] pages.
2. Scroll or search for the group to view.
3. Click the link in the 'User Group Name' column for the group.

#### Add User Group

##### Add a Global User Group

New global user groups can be added using the following steps. Only accounts with the GLOBAL
module name can be created using these steps.

> **NOTE:** The user must be in the ADMIN user group.

1. Open the [All User Group List](#All_User_Group_List) page.
2. Click <button>Add</button> button on the 'User Groups' page.
    1. Enter a User Group Name.
        * Must be unique across all user groups from all modules.
        * Can only contain the characters `A-Z`, `0-9`, and `_`.
        * Automatically converted to upper case.
        * Must start with `GLOBAL_`.
        * Must have additional characters after `GLOBAL_`.
    2. Enter a human readable description of the purpose of the group.
    3. Select if the group is to be active.
3. Click the <button>Save</button> button to create the user group, <button>Clear Fields</button>
   to undo any changes, or <button>Cancel</button> to return to the user groups page.
  

##### Add Module User Group

New user groups for a module can be added using the following steps. Only accounts with the
[MODULE_NAME] module name can be created using these steps.

1. Open the [Module User Group List](#Module_User_Group_List) page.
2. Click <button>Add</button> button on the 'User Groups' page.
    1. Enter a User Group Name.
        * Must be unique across all user groups from all modules.
        * Can only contain the characters `A-Z`, `0-9`, and `_`.
        * Automatically converted to upper case.
        * Must start with the [MODULE_NAME]_ (e.g. TEST_).
        * Must have additional characters after [MODULE_NAME]_ (e.g. TEST_USERS).
    2. Enter a human readable description of the purpose of the group.
    3. Select if the group is to be active.
3. Click the <button>Save</button> button to create the user group, <button>Clear Fields</button>
   to undo any changes, or <button>Cancel</button> to return to the user groups page.

#### Delete User Group

Only global or module groups can be deleted. All other groups are managed by the system. Deleting
a group will permanently delete the group, list of group members and any permissions granted to the
group.

##### Delete Global User Group

> **NOTE:** The user must be in the ADMIN user group.

1. Open the [All User Group List](#All_User_Group_List), or [View User Group](#View_User_Group) page.
2. Click the trash can button in the `'Actions'` field for the the group.
3. Click <button>OK</button> in the confirmation dialog to delete the group, or
   <button>Cancel</button> to return to the page without deleting.

##### Delete Module User Group

> **NOTE:** The user must be in the ADMIN, ADMIN_MODULE_[MODULE_NAME]_ADMIN, or
> ADMIN_MODULE_[MODULE_NAME]_SECURITY groups.


1. Open the [Module User Group List](#Module_User_Group_List), or [View User Group](#View_User_Group) page.
2. Click the trash can button in the `'Actions'` field for the the group.
3. Click <button>OK</button> in the confirmation dialog to delete the group, or
   <button>Cancel</button> to return to the page without deleting.

#### Edit User Group

Only global or module groups can be edited. All other groups are managed by the system.

##### Edit Global User Group

> **NOTE:** The user must be in the ADMIN user group.

1. Open the [View User Group](#View_User_Group) page.
2. Click the <button>Edit</button> button.
3. Edit the field values.
4. Click the <button>Save</button> button to save changes, <button>Revert to Saved</button>
   to undo any changes, or <button>Cancel</button> to return to the previous page.

##### Edit Module User Group

> **NOTE:** The user must be in the ADMIN, ADMIN_MODULE_[MODULE_NAME]_ADMIN, or
> ADMIN_MODULE_[MODULE_NAME]_SECURITY groups.

1. Open the [View User Group](#View_User_Group) page.
2. Click the <button>Edit</button> button.
3. Edit the field values.
4. Click the <button>Save</button> button to save changes, <button>Revert to Saved</button>
   to undo any changes, or <button>Cancel</button> to return to the previous page.

#### User Accounts for a Group

Each group has the list of the user accounts who are members of that group. The user accounts
in the group will inherit any permissions granted to the group. The user accounts for a group
can be viewed managed using the following instructions. The exception is the USER_TYPE group,
which has implicit members based on the user accounts. The list of user accounts cannot be viewed
and managed for those groups.

> **NOTE:** The CPF caches the groups a user is a member of for up to 5 minutes. If adding
> or removing users from a group wait at least 5 minutes before testing.

##### User Accounts for Group List

The user accounts for group list contains a scrolling table of the user accounts who are
members of a user group. The contents of this table can be filtered by typing at least 3
characters in search box (on the top right of the table). The table will show user accounts
where the user account consumer key, user account class or user name contain those characters
(ignoring case).

1. Open the [View User Group](#View_User_Group) page.
2. Click on the `'User Accounts for Group'` tab.

##### Add User Account to Group

A user account can be added as a member of a group using the following steps.

1. Open a [User Accounts for Group List](#User_Accounts_for_Group_List) page.
2. Type at least 3 characters in the `'Username'` field on the top left of the form. This
  will show a drop down of matching names. Select the required name from the list.
  Type more characters if the required name is not shown.
  If the user is not shown have them login to the CPF web services to make sure the user account
  is in the CPF database.
3. Click the <button>Add</button> button to add the user account to the group.

##### Delete User Account from Group

A user account can be deleted from being a member of a group using the following steps.

1. Open a [User Accounts for Group List](#User_Accounts_for_Group_List) page.
2. Scroll or search for the user account.
3. Click the trash can button in the 'Actions' field for the the user account.
4. Click <button>OK</button> in the confirmation dialog to delete the user account from the group,
   or <button>Cancel</button> to return to the page without deleting.

#### User Group Permission

CPF modules can use User Group Permissions to grant permissions for users who are a member of
a group permission to perform an action on a resource. Permissions are granted to a User Group for a
Module. Cross module permissions are not supported. User group permissions grant the group
permission to perform an action (Action Name) on a resource (Resource Class + Resource Id). For
example `view` (Action Name) the `reportUrl` (Resource Class) `http://reports.com/demoReport.json` (Resource Id).

> **NOTE:** NOTE: CPF itself does not use user group permissions. Each CPF module can decide if &amp;
how they use user group permissions. Consult the module's readme file for details on the supported
resource classes, resource id schemes and action names.

> **NOTE:** NOTE: The master and worker for a given user may cache user accounts, user groups
and user group permissions for up to 5 minutes. Wait 5 minutes if the user doesn't have the
correct permissions after a change.


> **NOTE:** The user must be in the ADMIN, ADMIN_MODULE_[MODULE_NAME]_ADMIN, or
> ADMIN_MODULE_[MODULE_NAME]_SECURITY user groups to view and manage user group permissions for a module.

##### User Group Permissions Fields

The following table summarizes the fields used for user group permissions.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Field</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>User Group Permission Id</td>
      <td>Unique sequence generated primary key for a user group permission.</td>
    </tr>
    <tr>
      <td>Module Name</td>
      <td>The name of the CPF module the permission was created for.</td>
    </tr>
    <tr>
      <td>User Group Name</td>
      <td>The name of the user group the permission applies to.</td>
    </tr>
    <tr>
      <td>Resource Class</td>
      <td>The resource class is the categorization of the resource. For example databaseInstance,
      schemaName, tableName, url, reportUrl.
      Modules are free to decide on their own resource classes, consult the module readme for more details.
      The special resource class ALL grants the users permission for all resource classes.</td>
    </tr>
    <tr>
      <td>Resource Id</td>
      <td>The unique identifier of the resource within the resource class that the users in the group have
      permission to perform the action. For example a record primary key, resource URL, report name.
      Modules are free to decide on their own resource id schemes, consult the module readme for more details.
      The special resource id ALL grants the users permission for all resources in the resource class.</td>
    </tr>
    <tr>
      <td>Action Name</td>
      <td>The name of the action users in the group are able to perform on the resource. For example ALL, view, edit.
      The special action name ALL grants the user permission for all actions on the resource.</td>
    </tr>
    <tr>
      <td>Active</td>
      <td>Boolean flag indicating if the user is active. Setting to false will prevent the user
      from accessing the CPF.</td>
    </tr>
  </tbody>
</table></div>

##### User Group Permission List

The user group permission page contains a scrolling table of the permissions for a user group.
The contents of this table can be filtered by typing at least 3 characters in search box. The table 
will show user group permissions where the user group permission id, resource class, resource id or 
action name contain those characters characters (ignoring case).

1. Open the [Module User Group List](#Module_User_Group_List) page.
2. Scroll or search for the group.
3. Click the link in the `'User Group Name'` column for the group.
4. Click the `'User Group Permissions'` tab.
5. Scroll or search for the permission.

##### View User Group Permission

User group permissions can be viewed using the following steps.

1. Open a [User Group Permission List](#User_Group_Permission_List) page.
2. Scroll or search for the user group permission.
3. Click the link in the `'User Group Permission Id'` column.

##### Add User Group Permission

New user group permissions can be added using the following steps.

1. Open a [User Group Permission List](#User_Group_Permission_List) page.
2. Click <button>Add</button> button.
3. Enter the field values. Consult the plug-in's readme for more information.
4. Click the <button>Save</button> button to create the user group permission,
   <button>Clear Fields</button> to undo any changes,
   or <button>Cancel</button> to return to the previous page.

##### Delete User Group Permission

A user group permission can be deleted using the following steps.

1. Open a [User Group Permission List](#User_Group_Permission_List) or [View User Group Permission](#View_User_Group_Permission) page.
2. Click trash button in the `'Actions'` field for the user group permission to delete.
3. Click the <button>OK</button> button on the confirm delete dialog to delete the user group permission,
  or <button>Cancel</button> to return to the previous page.

##### Edit User Group Permission

User group permissions can be edited using the following steps.

1. Open the [View User Group Permission](#View_User_Group_Permission) page.
2. Click the <button>Edit</button> button'.
3. Edit the field values.
4. Click the <button>Save</button> button to save changes,
  <button>Revert to Saved</button> to undo any changes,
  or <button>Cancel</button> to return to the previous page.
### Jobs

#### Batch Jobs Overview
The main purpose of the CPF is to execute batch jobs on behalf of users. The CPF admin application
can be used to view the list of jobs in the system and to monitor the progress of the execution
of the batch job execution groups.

> **NOTE:** The user must be in the ADMIN, or ADMIN_MODULE_[MODULE_NAME]_ADMIN user groups
to view batch jobs. The module admins will only be able to see batch jobs for their modules.

#### Batch Job List

The batch job list pages contain a scrolling table of the batch jobs. The contents of this 
table can be filtered by typing at least 3 characters in search box. The table will show batch jobs
where the id, business application, start time, modification time, job status, submitted requests,
completed requests, failed requests or user id contain those characters characters (ignoring case).

##### All Batch Job List

The all batch jobs page shows the batch jobs for all business applications the user has
admin permission for.

The batch jobs can be viewed using the following steps.

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `Batch Jobs` menu item.

##### Business Application Batch Job List

The business application batch jobs page shows the batch jobs for a business application that the
user has admin permission for.

1. Open a [View Business Application](#View_Business_Application) page.
2. Click the `'Batch Jobs'` tab.

##### View Batch Job

The batch jobs can view viewed using the following steps.

1. Open one of the [Batch Job List](#Batch_Job_List) pages.
2. Scroll/Search for the batch jobs to view.
3. Click the link in the 'Id' column.

##### Delete Batch Job
A batch job can be deleted using the following steps. This will also cancel any scheduled
groups, delete the execution groups and result files .

1. Open the [Batch Job List](#Batch_Job_List) or [View Batch Job List](#View_Batch_Job) page.
2. Click trash button in the `'Actions'` field for the batch job to delete.
3. Click the <button>OK</button> button on the confirm delete dialog to delete the batch job, or
   <button>Cancel</button> to return to the page.

##### Batch Job Execution Group List

The batch job execution group list page contain a scrolling table of the execution groups. The
contents of this table can be filtered by typing at least 3 characters in search box. The table 
will show batch job execution groups where the sequence number (#), completed (count),
failed (count), submitted (count), creation time or modification time contain those characters characters (ignoring case).

1. Open the [View Batch Job List](#View_Batch_Job) page.
2. Click the `'Execution Groups'` tab.
3. To download the input data click on the button in the Input column.
4. To download the result data click on the button in the Result column. A red x will be shown if the group has not been completed.


##### Batch Job Result File List
When the batch job is completed the batch job result file list page contain a scrolling table of the
result files. The contents of this table can be filtered by typing at least 3 characters in search
box. The table will show batch job result files where the sequence number (#), result type, content
type, creation time or download time contain those characters characters (ignoring case).

1. Open the [View Batch Job List](#View_Batch_Job) page.
2. Click the `'Result Files'` tab.
3.To download the file click on the button in the Download column.
### Statistics

#### Dashboard Statistics Overview

The CPF collects statistics for all phases of job processing. These statistics are collated for each hour
and business application. Statistics are automatically rolled up for each day, month and year.


> **NOTE:** The user must be in the ADMIN, or ADMIN_MODULE_[MODULE_NAME]_ADMIN user groups
> to view statistics. The module admins will only be able to see statistics for their modules.

The following table shows the statistics that are displayed on the [View Statistics](#View_Statistics) pages.

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Field</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Duration Type</td>
      <td>The type of duration (hour, day, month, year) that the statistics were calculated for.</td>
    </tr>
    <tr>
      <td>Start Time</td>
      <td>The start time of the time period the statistics were calculated for.</td>
    </tr>
    <tr>
      <td>End Time</td>
      <td>The end time of the time period the statistics were calculated for.</td>
    </tr>
    <tr>
      <td>Jobs Completed</td>
      <td>Statistics calculated for jobs that were completed during the time period.
      Statistics are split by jobs and requests.
      Statistics are calculated for the total execution average time, total of all times and a count.</td>
    </tr>
    <tr>
      <td>Jobs Submitted</td>
      <td>Statistics calculated for jobs that were submitted during the time period.
      Statistics are not calculated for requests as this is not known until the pre-processing stage.
      Statistics are calculated for the average and total times to process the job submission and a count.</td>
    </tr>
    <tr>
      <td>Jobs Pre-Process Scheduled</td>
      <td>Statistics calculated for jobs that were scheduled for pre-processing during the time period.
      Statistics are calculated for the pre-process schedule average time, total of all times and a count.</td>
    </tr>
    <tr>
      <td>Jobs Pre-Processed</td>
      <td>Statistics calculated for jobs that were pre-processed during the time period.
      Statistics are split by jobs and requests.
      Statistics are calculated for the pre-process average time, total of all times and a count.</td>
    </tr>
    <tr>
      <td>Jobs Request Groups Scheduled</td>
      <td>Statistics calculated for job groups that were scheduled for execution during the time period.
      Statistics are calculated for the schedule average time, total of all times and a count.</td>
    </tr>
    <tr>
      <td>Jobs Request Groups Executed</td>
      <td>Statistics calculated for job groups that were executed during the time period. These
      times include master and worker time to execute the group including any wait times.
      Statistics are split by jobs and requests.
      Statistics are calculated for the job group execution average time, total of all times and a count.</td>
    </tr>
    <tr>
      <td>Jobs Request Groups Application Executed</td>
      <td>Statistics calculated for job groups that were executed during the time period. These
      times are the time the business application takes to execute the group without any overhead.
      Statistics are split by jobs and requests.
      Statistics are calculated for the job group application execution average time, total of all times and a count.</td>
    </tr>
    <tr>
      <td>Jobs Post-Process Scheduled</td>
      <td>Statistics calculated for jobs that were scheduled for post-processing during the time period.
      Statistics are calculated for the post-process schedule average time, total of all times and a count.</td>
    </tr>
    <tr>
      <td>Jobs Post-Processed</td>
      <td>Statistics calculated for jobs that were post-processed during the time period.
      Statistics are split by jobs and requests.
      Statistics are calculated for the post-process average time, total of all times and a count.</td>
    </tr>
  </tbody>
</table></div>

#### Summary Statistics

The summary statistics page shows the statistics for all business applications the user has
admin permission for. The page has tabs for summary statistics for the current 'Hour', total for 'Today', total
for this 'Month' and totals for each 'Year'. To view all time periods use the 
[Business Application Statistics}(#Business_Application_Statistics) page.

Each tab contains a scrolling table of the statistics. The contents of this 
table can be filtered by typing at least 3 characters in search box. The table will show statistics
where the id, module, business application, jobs submitted, jobs completed, app groups executed,
app requests completed, app requests failed contain those characters characters (ignoring case).

The statistics can be viewed using the following steps.

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `'Dashboard'` menu item.
3. Click on the Hour, Today, Month or Year tabs to view statistics for those time periods.

#### Business Application Statistics

The business statistics tab shows the statistics for a business application. The tab has a single
list containing all the statistics for the current 'Hour', total for 'Today', total for this 'Month'
and totals for each 'Year'.

Each tab contains a scrolling table of the statistics. The contents of this 
table can be filtered by typing at least 3 characters in search box. The table will show statistics
where the id, duration type, jobs submitted, jobs completed, app groups executed,
app requests completed, app requests failed contain those characters characters (ignoring case).

1. Open the [View Business Application](#View_Business_Application) page.
2. Click the `'Dashboard'` tab.

#### View Statistics

The statistics can view viewed using the following steps.

1. Open the [Summary Statistics](#Summary_Statistics), or
   [Business Application Statistics](#Business_Application_Statistics) page.
2. Scroll/Search for the statistics to view.
3. Click the link in the 'Id' column.
### Worker

#### Workers Overview

The CPF uses multiple external worker processes to execute groups of requests from a batch job.
The status of these workers can be viewed and groups restarted using the CPF Admin Application. The
list of workers and modules is updated on the server whenever workers connect, disconnect or
modules are started or stopped. These pages require manual refresh to view and changes.

Each worker is given a unique ID that will not change unless the component parts that make up
the ID change. The ID is made up from the following components. Note the workerTomcatContextPath has
any '/' characters replaced by '-'.

```
{environmentName}:{workerHostName}:{workerTomcatPort}:{workerTomcatContextPath}
```

For example:

```
default:192.168.1.102:8009:pub-cpf-worker
```

> **NOTE:** The user must be in the ADMIN user group to view workers.

#### Worker List

The worker list page shows a scrolling table of workers who have connected in the last 2 minutes.
The contents of this table can be filtered by typing at least 3 characters in search box.
The table will workers where the id or last connect time contain those characters characters (ignoring case).

1. Open the CPF Admin Application (e.g `http://localhost/cpf/admin/`).
2. Click the `'Workers'` menu item.

#### View Worker

The worker can view viewed using the following steps.

1. Open the [Worker List](#Worker_List) page.
2. Scroll/Search for the workers to view.
3. Click the link in the `'Id'` column.

#### Worker Executing Group List

The list of groups that are currently executing can be viewed using the following steps. This
can be used to check for groups that have been running for too long. Note that this contents of this
list changes very frequently as new groups are scheduled or completed.

1. Open the [View Worker](#View_Worker) page.
2. Click the `'Executing Groups'` tab.

#### Restart Worker Executing Group

If an execution group has been stuck on a worker it can be rescheduled using the following steps.

1. Open a [Worker Executing Group List](#Worker_Executing_Group_List) page.
2. Click the <button>Restart</button> button in the `'Actions'` column.
3. Click the <button>OK</button> button on the confirm restart dialog to restart the group,
   or <button>Cancel</button> to return to the page.

#### Worker Module List

The list of modules that are loaded on the worker cab be viewed using the following steps.

1. Open the [View Worker](#View_Worker) page.
2. Click the `'Modules'` tab.

#### View Worker Module

The full details of the module on the worker including any startup errors viewed using the following steps.

1. Open the [Worker Module List](#Worker_Module_List) page.
2. Click the link in the 'Name' column.

### Log

#### Log Files & Diagnostics Overview
The CPF creates additional log files in addition to those created for the Apache web server
and tomcat servlet container. These log files can be accessed on the machine that the master and
worker are running on. These maybe exposed for download via the Apache web server if configured.

##### Log File Format

Each log file has lines with the following format, separated by tabs (\t).

```
{Date YYYY-MM-DD HH:MI:SS,SSS}\t{Level DEBUG, INFO,WARN, or ERROR}\t{Category}\t{Message}
```

For example:

```
2014-07-30 13:56:16,432 INFO  CPF-TEST  Start Module Start  moduleName=CPF-TEST
```

Log entries may also have a
<a href="http://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html#printStackTrace()">java stack trace</a>,
shown on multiple lines. For example.

```
2014-07-17 17:08:25,305  ERROR ca.bc.gov.open.cpf.api.scheduler.BatchJobPreProcess Could not open JDBC Connection for transaction; nested exception is java.sql.SQLRecoverableException: IO Error: The Network Adapter could not establish the connection
java.sql.SQLRecoverableException: IO Error: The Network Adapter could not establish the connection
  at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:458)
  at oracle.jdbc.driver.PhysicalConnection.<init>(PhysicalConnection.java:546)
  at oracle.jdbc.driver.T4CConnection.<init>(T4CConnection.java:236)
  at oracle.jdbc.driver.T4CDriverExtension.getConnection(T4CDriverExtension.java:32)
  at oracle.jdbc.driver.OracleDriver.connect(OracleDriver.java:521)
  at oracle.jdbc.pool.OracleDataSource.getPhysicalConnection(OracleDataSource.java:280)
  at oracle.jdbc.pool.OracleDataSource.getConnection(OracleDataSource.java:207)
  at oracle.jdbc.pool.OracleConnectionPoolDataSource.getPhysicalConnection(OracleConnectionPoolDataSource.java:139)
  at oracle.jdbc.pool.OracleConnectionPoolDataSource.getPooledConnection(OracleConnectionPoolDataSource.java:88)
  at oracle.jdbc.pool.OracleImplicitConnectionCache.makeCacheConnection(OracleImplicitConnectionCache.java:1598)
  at oracle.jdbc.pool.OracleImplicitConnectionCache.makeOneConnection(OracleImplicitConnectionCache.java:515)
  at oracle.jdbc.pool.OracleImplicitConnectionCache.getCacheConnection(OracleImplicitConnectionCache.java:475)
  at oracle.jdbc.pool.OracleImplicitConnectionCache.getConnection(OracleImplicitConnectionCache.java:357)
  at oracle.jdbc.pool.OracleDataSource.getConnection(OracleDataSource.java:395)
  at oracle.jdbc.pool.OracleDataSource.getConnection(OracleDataSource.java:179)
  at oracle.jdbc.pool.OracleDataSource.getConnection(OracleDataSource.java:157)
  ... 13 more
Caused by: oracle.net.ns.NetException: The Network Adapter could not establish the connection
  at oracle.net.nt.ConnStrategy.execute(ConnStrategy.java:392)
  at oracle.net.resolver.AddrResolution.resolveAndExecute(AddrResolution.java:434)
  at oracle.net.ns.NSProtocol.establishConnection(NSProtocol.java:687)
  at oracle.net.ns.NSProtocol.connect(NSProtocol.java:247)
  at oracle.jdbc.driver.T4CConnection.connect(T4CConnection.java:1102)
  at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:320)
  ... 29 more
Caused by: java.net.SocketTimeoutException: connect timed out
  at java.net.PlainSocketImpl.socketConnect(Native Method)
  at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:339)
  at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:200)
  at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:182)
  at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392)
  at java.net.Socket.connect(Socket.java:579)
  at oracle.net.nt.TcpNTAdapter.connect(TcpNTAdapter.java:150)
  at oracle.net.nt.ConnOption.connect(ConnOption.java:133)
  at oracle.net.nt.ConnStrategy.execute(ConnStrategy.java:370)
  ... 34 more
```

#### Log Files

The following table summarizes the log files created by the CPF.
The log files by default are stored in `/apps/logs/cpf/`.
Each module and business application has separate directories for their log files. Log files will
be rolled over once they reach 10MB. The last 7 log files will be retained with extensions `.{i}.log` (e.g. `.2.log`).

<div class="table-responsive"><table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>File</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>catalina.out</code></td>
      <td>If running on tomcat the tomcat server log file. Exact file name and location will vary
      based on the servlet container used and it's configuration. If the CPF does not start check this
      log file for any errors.</td>
    </tr>
    <tr>
      <td><code>cpf-app.log</code></td>
      <td>Log messages for components on the master not related to a module or a business application.
      New log file created on module restart or if &gt; 10MB. Previous 7 log files retained with extensions .{1-7}.log (e.g. .1.log).</td>
    </tr>
    <tr>
      <td><code>worker_{environmentName}_{hostName}_{port}_{contextPath}.log</code></td>
      <td>Log messages for components on the worker not related to a module or a business application.
      New log file created on module restart or if &gt; 10MB. Previous 7 log files retained with extensions .{1-7}.log (e.g. .1.log).</td>
    </tr>
    <tr>
      <td><pre>{moduleName}/
{moduleName}_master.log</pre></td>
      <td>Log messages for a module's components on the master not related to a business application.
      New log file created on module restart or if &gt; 10MB. Previous 7 log files retained with extensions .{1-7}.log (e.g. .1.log).</td>
    </tr>
    <tr>
      <td><pre>{moduleName}/
{moduleName}_worker_{environmentName}_{hostName}_{port}_{contextPath}.log</pre></td>
      <td>Log messages for a module's components on the worker not related to a business application.
      New log file created on module restart or if &gt; 10MB. Previous 7 log files retained with extensions .{1-7}.log (e.g. .1.log).</td>
    </tr>
    <tr>
      <td><pre>{moduleName}/
{appName}/
  {moduleName}_{appName}_master.log</pre></td>
      <td>Log messages for a business application's components on the master.
      New log file created on module restart or if &gt; 10MB. Previous 7 log files retained with extensions .{1-7}.log (e.g. .1.log).</td>
    </tr>
    <tr>
      <td><pre>{moduleName}/
{appName}/
  {moduleName}_{appName}_worker_{environmentName}_{hostName}_{port}_{contextPath}.log</pre></td>
      <td>Log messages for a business application's components on the worker.
      New log file created on module restart or if &gt; 10MB. Previous 7 log files retained with extensions .{1-7}.log (e.g. .1.log).</td>
    </tr>
  </tbody>
</table></div>

#### Diagnosing Issues

This section describes how to diagnose issues with the CPF system or plug-ins running within the
system.

The first step in diagnosing an issue is to detect if it is a plug-in specific issue that must
be reported to the plug-in developer or a general CPF issue that must be reported to the CPF
developer. The general rule is check to see if the problem is isolated to this plug-in by submitting
jobs to other plug-ins (e.g. MapTileByTileId). **If the error is specific to a single plug-in then
contact the plug-in developer for resolution first. Only contact the CPF developer if the plug-in
developer cannot resolve the issue.**

##### Server Errors

Sometimes the server will return an error page instead of a valid page. The CPF returns a
formatted and styled error page similar to as shown below. This kind of error is generated by the
CPF application. If the server returns an unformatted page (black text on white background) then
this is an error generated by the web server. For example the application server was down, or the
CPF was not deployed. Those errors must be reported to the server administrator.

<img src="images/error.png" />

###### 400 Bad Data

If the input data to the web service was incorrect the CPF may return a 400 error with a message
about the invalid parameter. Check the parameters are correct using the
[Client REST API](cpf-api-app/rest-api/) or the Overview tab for the business application.
If the issue can't be resolved by reading the documentation. contact the plug-in developer for
business application specific parameters or the CPF developer for general CPF parameters.

###### 404 Not Found

This error indicates that the resource could not be found. Verify that the plug-in is loaded,
the resource exists (e.g. correct job id and hasn't been deleted), or that the correct URL is used.

###### 500 Server Error

This error indicates an unknown error has occurred on the server. Verify that the error can be repeated.
Check the cpf-app.log or catalina.out log files for more details. The contents of these files should
be sent to the CPF developer with a detailed description of the steps (pages) and parameters used
to generate the error.

It may be possible to diagnose and resolve the following types of error without contacting the
CPF developer. 

* `java.io.IOException` subclasses
    * If the error is a broken pipe within a tomcat/catalina stack
      trace then it's probably that the connection from the web browser to the CPF process was disconnected.
      Not much can be done except increasing the timeouts for communications between the apache and tomcat servers.
    * If the error is a file not found, permission denied, out of disk space error then verify the
      server configuration and check available disk space.
* `java.sql.SQLException` subclasses
    * Check the error code/message to verify if it is an invalid username or password or for password expiry.
      Update the passwords or configuration as required.
    * Check the error code/message to verify that the database server is available. It might be down for a cold
      backup, daily restart or maintenance. Verify the JDBC URL to check it's pointing to the correct server
    * Check the error code/message to verify if the database server was out of tablespace storage. Delete old jobs
      or increase the storage as required.
    * Check the error code/message to verify if it's an invalid SQL statement. If it is contact the CPF developer
      if it has CPF has a table schema name in the SQL, otherwise contact the plug-in developer.
* `java.lang.OutOfMemoryError` This could occur if the request data is to large to fit into
  memory or if the plug-ins or CPF has been re-dployed too many times and is not being garbage collected (perm gen errors).
  Typically this error will show up in the catalina.out file. Rebooting the application server is the best
  way to resolve these issues. If the error occurs repeatedly then 
  verify that the `-Xmx` option for tomcat is large enough for the business applications and for perm gen
  errors check the `-XX:MaxPermSize` option. The CPF and plug-ins should be developed so
  that they can be garbage collected when stopped or restarted. Details on this are in the
  [client developers guide](client.html).
* `org.springframework.security.authentication.*Exception` This indicates that the
  user is not valid or does not have permission to access the resource. Contact the CPF developer
  as these should not be logged in the log file and just displayed to the user.

##### Verify Plug-in Started
Use the following steps to verify that the plug-in is started on the master or worker.

* Check that the plug-in is enabled and started on the master using the [View Module](#View_Module) page.
    * Enable the module if it is not enabled.
    * Start the module if not started.
    * If there is a module error:
        * Check the master module log file `{moduleName}/{moduleName}_master.log` for more details on the error.
        * Check the configuration properties using the plug-ins documentation to verify they are correct.
          Especially check database URL, username and password.
        * Check that the Maven Module Id is correct. Fix and start module if incorrect.
        * If there is a Maven error downloading files verify that the Maven repository has all the
          required modules for the plug-in. Clear the CPF cache if necessary to reset the system.
          Verify using a web browser that the Maven modules exist in the Maven repository and can be downloaded.
          If not contact the Maven repository administrator.
* Check there is a worker connected using the [Worker List](#Worker_List) page.
* Check the module is started using the [Worker Module List](#Worker_Module_List) page.
* If not started check for module errors using the [View Worker Module](#View_Worker_Module) page.
* Check the worker module log file `{moduleName}/{moduleName}_worker_{environmentName}_{hostName}_{port}_{contextPath}.log` for more details on the error.

If the plug-in loaded successfully
the master module log file `{moduleName}/{moduleName}_master.log` and
worker module log file `{moduleName}/{moduleName}_worker_{environmentName}_{hostName}_{port}_{contextPath}.log`
will be rolled over and the following entries will appear at the top of the log file.

```
2014-07-30 16:28:24,183 INFO  [Module Name]  Start Module Start    moduleName=[Module Name]
2014-07-30 16:28:24,200 INFO  [Module Name]  Start Loading plugin  class=[businessApplicationClass]
2014-07-30 16:28:24,212 INFO  [Module Name]  End Loading plugin    class=[businessApplicationClass]  businessApplicationName=[businessApplicationName]
2014-07-30 16:28:24,238 INFO  [Module Name]  End Module Start      moduleName=[Module Name] time=0.06
```

##### Job Submission

Use the following steps if experiencing difficulty accessing the business application pages
and in submitting instant, single or multiple request jobs.

* Using above verify the plug-in for the business application is started.
* Verify the business application is available on the `'Business Application'` page in the client application.
  Refresh if necessary. If it does not appear in the list then the plug-in does not exist (contact plug-in developer) or is not loaded.
* Click on the name of the business application and verify that the Overview,
  Create Single Request Job, Create Multi-Request Job and Batch Job Tabs are available. If the plug-in
  supports instant mode check the 'Instant' tab is visible. Click on each tab to verify it is
  displayed correctly.
* Use the Create Single Request Job and Create Multi-Request Job tabs to verify that a job can be submitted.
    * If the job status page was returned then the job was submitted correctly.
    * If you get a red error message at the top of the form or next to a field then the parameters were incorrect.
      Verify the parameters used. Consult the Overview tab for more details or the plug-in developer.
    * If the size of a mult-request job file and other form parameters exceed 20MB then a 400 error will be returned.
      Use the URL upload instead.
    * If a 400 error is returned check the input parameters to see if they are valid. Contact the
      plug-in developer for application specific parameters or the CPF developer for general parameters.
    * If a 500 error is returned check the cpf-app.log for errors and send the log file to the CPF developer.

##### Job Processing

If the job was submitted but is not completed in the expected time check the following.

* Check the module is loaded on the worker using the [View Worker Module](#View_Worker_Module) page.
  Some modules may take 30 minutes to initialize after restart on the worker
* Submit jobs to another business application so see if the system generally works.
* If the job is stuck in the creatingRequests or creatingResults states check the master module and app log file for any errors.
  If the error was due to out of memory or database space allocate more space or cancel the job.
  Otherwise send the errors to the CPF developer for review, don't cancel delete the job until it has been investigated.
* If the job is stuck in the processing stage check the master and worker module and app log files for any errors.
  Send to the CPF developer for review.
* Additional information about the scheduling and execution of execution groups and requests can be enabled by
  [Edit Business Application](#Edit_Business_Application) to turn on Debug level logging for the business application.
  **NOTE: Change back to ERROR logging after the problem is diagnosed as the logs can get large and slow down the system.**
