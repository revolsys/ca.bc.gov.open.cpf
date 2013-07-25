Description
-----------
Project:           cpf
Title:             Cloud Processing Framework Web Application
Version:           4.0.3

Software/Hardware Requirements
------------------------------
Oracle:                       N/A
Java:                         6+
Maven:                        3.0.3+
App Server:                   Tomcat 7+
App Server Additional Memory: 100MB


1. Database Installation
------------------------

N/A

2. Configuration Files
----------------------

CPF requires a configuration file on each server.

Property                             Description
-------------------------------      ------------------------------------------
ca.bc.gov.cpf.app.baseUrl            The HTTP URL to the server cpf is deployed to
ca.bc.gov.cpf.app.secureBaseUrl      The HTTPS URL to the server cpf is deployed to
ca.bc.gov.cpf.internal.webServiceUrl The HTTP URL to the internal web service for cpf
ca.bc.gov.cpf.db.url                 The JDBC URL to the cpf database
ca.bc.gov.cpf.db.password            The password for the PROXY_CPF_WEB user account
ca.bc.gov.cpf.db.maxConnections      The maximum number of database connections
batchJobService.maxWorkerWaitTime    The maximum time the worker will wait in a HTTP
                                     request for group to process before trying a new
                                     HTTP request. This limits the number of polling
                                     requests to the server.
cpfWorker.password                   The password for the internal web service user
cpfWorker.maximumPoolSize            The maximum number of threads on the worker to
                                     execute requests.
batchJobService.fromEmail            The email address any emails will be sent from
mailSender.host                      The mail server to send emails via
ca.bc.gov.cpf.repositoryServer       The maven repository to download plugins from
ca.bc.gov.cpf.repositoryDirectory    The cache directory to store maven artifacts

Create the directory and configuration file.

NOTE: Configuration for delivery, test and production can be managed in
subversion https://apps.bcgov/svn/cpf/config/ and checked out to this directory.

mkdir -p /apps/config/cpf
cd /apps/config/cpf
vi cpf.properties

Sample Values
-------------
The latest sample config file can be obtained from:

https://apps.bcgov/svn/cpf/config/delivery/trunk/cpf.properties

It contains the values for the delivery environment. Verify that the test and production versions
are similar.

3. Ministry Continuous Integration System
-----------------------------------------

The application can be build and deployed using the  Ministry Continuous
Integration System, use the Ministry Standards below as a Guide.

http://apps.bcgov/standards/index.php/Migration_Task_with_CIS

Create a new maven 2/3 job with the following parameters.

Project name:                       revolys-cpf-deploy
Description:                        Build the CPF web application and deploy to Tomcat.
Source Code Management: 
  (*) Subversion:
    Repository URL:                 http://apps.bcgov/svn/cpf/source/trunk/
MVN Goals and options:              clean install
E-Mail Notification:                leo.lou@gov.bc.ca paul.austin@revolsys.com
Resolve Artifacts from Artifactory: Yes
Post-build Actions:
 Deploy artifacts to Artifactory:
   Artifactory Server:              http://delivery.apps.bcgov/artifactory/
     Target releases repository:    libs-release-local
     Target snapshots repository:   libs-snapshot-local
  Deploy war/ear to a container:
    WAR/EAR files:                  **/*.war
    Container:                      Tomcat 7.x
      Manager user name:            catbot
      Manager password:             ********
      Tomcat URL:                   http://localhost:9501/
  Build other projects:              # Other than in delivery leave blank. 
                                    # Manually build other projects.
                                    # Do not allow other projects to be auto
                                    # built when this project is built.

4. Compilation & Deployment
---------------------------

Build the revolys-cpf-deploy job using the Ministry Continuous Integration
System.

5. Post Build Actions
---------------------
Follow the README.txt file for each CPF plug-in to be deployed to the CPF.

6. Notification
---------------

Notify all developers and contributors listed in the pom.xml that the deployment
is complete.

7. Perform Release 
------------------

This step is performed before migration of an application to the test or
production environment.

The migration to test occurs after the developer has tested the application in
the delivery environment. The migration to production occurs after the business
area has tested the application in the test environment.

Perform a Maven release using the following settings.

Update property dependencies to latest RC or release version:
  ca.bc.gov.open.cpf.version: 4.0.3+

Test Migration
**************

Test Version:                 4.0.3.RC

Production Migration
********************
Release Version:              4.0.3
