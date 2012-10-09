Description
-----------
Project:           cpf
Title:             Cloud Processing Framework Web Application
Version:           3.0.0

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

svn co http://maps.bcgov/svn/cpf/source/trunk/scripts
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
ca.bc.gov.cpf.repositoryDirectory  The directory to store maven artifacts


Create the directory and configuration file.

mkdir -p /apps/config/cpf
cd /apps/config/cpf
vi cpf.properties

Sample Values
-------------
The latest sample config file can be obtained from:

https://maps.bcgov/svn/cpf/config/delivery/trunk/sample-cpf.properties

It contains the following values for the delivery environment.

ca.bc.gov.cpf.app.baseUrl=http\://delivery.apps.gov.bc.ca/pub/cpf
ca.bc.gov.cpf.app.secureBaseUrl=https\://delivery.apps.gov.bc.ca/pub/cpf/secure
ca.bc.gov.cpf.db.url=jdbc\:oracle\:thin\:@fry.geobc.gov.bc.ca\:1521\:GEODLV
ca.bc.gov.cpf.db.user=proxy_cpf_web
ca.bc.gov.cpf.db.password=cpf_2009
ca.bc.gov.cpf.db.maxConnections=20

ca.bc.gov.cpf.ws.consumerKey=cpf_worker
ca.bc.gov.cpf.ws.consumerSecret=cpf_2009

ca.bc.gov.cpf.fromEmail=noreply@gov.bc.ca
ca.bc.gov.cpf.mailServer=apps.smtp.gov.bc.ca

ca.bc.gov.cpf.repositoryDirectory=/tmp/cpf/repository/

3. Ministry Continuous Integration System
-----------------------------------------

Create a new deploy job using the Ministry Continuous Integration System.

http://apps.gov.bc.ca/gov/standards/index.php/Migration_Task_with_CIS

Job name:                       revolys-cpf-deploy
Subversion:                     http://maps.bcgov/svn/cpf/source/trunk
MVN Goals and options:          clean install
Ministry Artifacts Repository:  Yes
Deploy to Tomcat:               Yes

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

Follow the CPF Module Deployment of each CPF module to be deployed.

6. Notification
---------------

Notify all developers and contributors listed in the pom.xml that the deployment
is complete.

7. Perform Release 
------------------

This step is performed before migration of an application to the production
environment. The migration occurs after the developer has tested the application
in the delivery environment and the business area have completed the user
acceptance testing in the test environment.

Use the Ministry Continuous Integration System to tag the version in Subversion,
build the release version and deploy it to the Ministry Artifacts Repository.

Release Version:          3.0.0
Next Development Version: 3.0.1-SNAPSHOT
