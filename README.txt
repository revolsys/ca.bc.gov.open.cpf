Description
-----------
Project:           cpf
Title:             Cloud Processing Framework Web Application
Version:           4.0.0

Software/Hardware Requirements
------------------------------
Oracle:                       N/A
Java:                         6+
Maven:                        3.0.3+
App Server:                   Tomcat 7+
App Server Additional Memory: 100MB


1. Database Installation
------------------------

1.1 Download SQL Scripts from Subversion

svn co http://apps.bcgov/svn/cpf/source/trunk/scripts
cd scripts

1.2 Run the DBA scripts to create the tablespaces and users

sqlplus system@GEODLV @cpf-dba-all.sql

The two user accounts require a new password to be set, you will be prompted
for these passwords.

CPF_PW              The password for the CPF user account
PROXY_CPF_WEB_PW    The password for the PROXY_CPF_WEB user account

1.3 Run the scripts to create the CPF roles and database objects

./cpf-ddl.sh GEODLV

The two user accounts require a new password to be set, you will be prompted
for these passwords.

CPF_ADMIN_PW   The password used for the initial admin user cpf_admin.
CPF_WORKER_PW  The password used by the cpf_worker account that is used by the
               worker process. This must be entered in the configuration file in
               step #2.
               
2. Configuration Files
----------------------

CPF requires a configuration file on each server.

Property                           Description
-------------------------------    ------------------------------------------
ca.bc.gov.cpf.app.baseUrl          The HTTP URL to the server cpf is deployed to
ca.bc.gov.cpf.app.secureBaseUrl    The HTTPS URL to the server cpf is deployed to
ca.bc.gov.cpf.db.url               The JDBC URL to the cpf database
ca.bc.gov.cpf.db.user              The PROXY_CPF_WEB user account (DON'T CHANGE)
ca.bc.gov.cpf.db.password          The password for the PROXY_CPF_WEB user account
ca.bc.gov.cpf.db.maxConnections    The maximum number of database connections
ca.bc.gov.cpf.ws.consumerKey       The internal web service user (DON'T CHANGE)    
ca.bc.gov.cpf.ws.consumerSecret    The password for the internal web service user
ca.bc.gov.cpf.fromEmail            The email address any emails will be sent from
ca.bc.gov.cpf.mailServer           The mail server to send emails via
ca.bc.gov.cpf.repositoryServer     The maven repository to download plugins from
ca.bc.gov.cpf.repositoryDirectory  The cache directory to store maven artifacts


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

It contains the following values for the delivery environment.

ca.bc.gov.cpf.app.baseUrl=http\://delivery.apps.gov.bc.ca/pub/cpf
ca.bc.gov.cpf.app.secureBaseUrl=https\://delivery.apps.gov.bc.ca/pub/cpf/secure
ca.bc.gov.cpf.db.url=jdbc\:oracle\:thin\:@fry.geobc.gov.bc.ca\:1521\:GEODLV
ca.bc.gov.cpf.db.user=proxy_cpf_web
ca.bc.gov.cpf.db.password=cpf_2009
ca.bc.gov.cpf.db.maxConnections=50
ca.bc.gov.cpf.ws.consumerKey=cpf_worker
ca.bc.gov.cpf.ws.consumerSecret=cpf_2009
ca.bc.gov.cpf.fromEmail=noreply@gov.bc.ca
ca.bc.gov.cpf.mailServer=apps.smtp.gov.bc.ca
ca.bc.gov.cpf.repositoryServer=http://apps.bcgov/artifactory/repo/
ca.bc.gov.cpf.repositoryDirectory=/tmp/cpf/repository/

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

5.1 CPF Admin Users
-------------------------

a. Open https://delivery.apps.gov.bc.ca/pub/cpf/secure/admin/ and login using
   your IDIR account. Get each GLOBAL ADMIN user to do this so a proxy user
   account is created in CPF.
b. You will get a 403 error which is expected.
c. Open https://delivery.apps.gov.bc.ca/pub/cpf/admin/
d. Login using user cpf_admin with the password created in step #1.2
e. Click the User Groups link on the menu on the left side of the page
f. Click the ADMIN User Group link in the table on that page
g. Click the User Accounts for Group section to open it
h. Search for the user name by typing at least 3 characters in the Username box
   (e.g. pxau). Select one of the names and click the add button.
i. Repeat for each GLOBAL ADMIN user.
k. Open https://delivery.apps.gov.bc.ca/pub/cpf/secure/admin/ and verify you
   have access to the siteminder enabled version of the site.
l. Click the User Accounts link on the menu on the left side of the page
m. Click on the cpf_admin Consumer Key link on the table in that page
n. Click the edit button
o. Uncheck the Active checkbox
p. Click Save. The cpf_admin account is now disabled and admin access requires
   Siteminder login by one of the admin users via the link in step #a.

5.2 CPF Module Deployment
-------------------------

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

7.1 Update snapshot dependencies
--------------------------------

Before performing a release the -SNAPSHOT dependencies for the project's
dependencies must be updated to release candidate versions for test and
release versions for production. Edit the pom.xml file replacing the version
number in the value of the following properties that can be found at the bottom
of the pom.xml file. Commit the changes to the pom.xml before preparing a
release.

This property is the version of the http://apps.bcgov/svn/cpf/api-source project.

<ca.bc.gov.open.cpf.version>4.0.0</ca.bc.gov.open.cpf.version> 

7.2 Perform and prepare the release
-----------------------------------

Perform a maven release using the following settings.

 * Test Migration

Test Version:             4.0.0.RC[1..9] Increment for each migration to test

 * Production Migration
Release Version:          4.0.0
Next Development Version: 4.0.1-SNAPSHOT
