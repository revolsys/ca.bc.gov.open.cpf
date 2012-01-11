********************************************************************************
* Cloud Processing Framework
*
* $URL: https://poplar.idir.bcgov:88/svn/cpf/trunk/install/readme.txt $
* $Author: cpf $
* $Date: 2011-04-14 10:46:13 -0700 (Thu, 14 Apr 2011) $
* $Revision: 34 $
* Release: 2.0.0
*
********************************************************************************

OVERVIEW
--------
This file describes the steps to configure, build and install the Cloud
Processing Framework (2.0.0) on a UNIX Oracle Application Server (10.1.3)
and a Unix Oracle version 10g database.

These instructions are for the Ministry's database administrator and Java
Application Delivery Specialist, and assume familiarity with standard Oracle
administrative functions. CPF is the officially assigned application short
name.

REQUIREMENTS
------------------------
Prior to installing the application, the following requirements must be
verified:
 - This installation assumes installation of the web application on Oracle 
   10g Release 3 (10.1.3)
 - the UNIX user cpf has been created and is using the Korn Shell (ksh).
 - The Ant tool is installed on the Java Application Delivery Server and is
   available in the path.
 - Oracle must be installed and running on the server.
 - an appropriate database instance has been created and is running on the
   server.
 - the Ministry standard application delivery directory structure is in place as
   defined in the Application Delivery Standards and Java Application Delivery
   Standards documents.
 - J2SE 1.5.0+ is installed on the server. The JAVA_HOME environment variable is
   set to the directory where J2SE is installed.
 - The application uses several new frameworks that are not accounted for in the
   standard.

INSTALLATION INSTRUCTIONS
-------------------------

1. LOGIN TO THE APPLICATION SERVER

  ssh cpf@smolder.geobc.gov.bc.ca
    password = ********

####### OC4j migration###

undeploy from oc4j
map to tomcat 7
change perms on /apps_ux/logs/cpf and delete old logs

2. DOWNLOAD SOURCE CODE FROM SUBVERSION

cd /apps_ux/cpf

If /apps_ux/cpf/source does not exist checkout the source from Subversion.

svn co --username cpf https://poplar.idir.bcgov:88/svn/cpf/trunk/ source

NOTE: If prompted for a certificate validation error, accept the certificate
permanently.

NOTE: If you are prompted to store the password encrypted enter yes. The
directory the password is stored in can only be read by the current user.

If /apps_ux/cpf/source does exist update it to the latest revision.

svn up source

2. RUN THE DATABASE SCRIPTS

2.1. Run the DBA database scripts

  cd /apps_ux/cpf/source/ddl
  ./cpf-dba.sh GEODLV

NOTE: If there were any errors STOP and contact the developer before continuing.
The log file is in ../log/dba.log.


2.2. Run the WebAde database scripts

  cd /apps_ux/cpf/source/ddl
  ./complete/cpf-webade.sh GEODLV

NOTE: If there were any errors STOP and contact the developer before continuing.
The log file is in ../log/webade.log.

2.3. Run the application database scripts

  cd /apps_ux/cpf/source/ddl/complete
  ./cpf-ddl.sh GEODLV

NOTE: If there were any errors STOP and contact the developer before continuing.
The log file is in ../log/ddl.log.

3. CONFIGURE THE APPLICATION

cd /apps_ux/cpf/source/config

If default.properties does not exist copy the sample to create it.

cp sample-default.properties default.properties

Edit default.properties to configure it for the application.

vi default.properties

The following property values must be configured for each environment.

ca.bc.gov.cpf.app.serverUrl      The URL to the server cpf is deployed to
ca.bc.gov.cpf.db.url             The JDBC URL to the cpf database
ca.bc.gov.cpf.db.user            The PROXY_CPF_WEB user account (DON'T CHANGE)
ca.bc.gov.cpf.db.password        The password for the PROXY_CPF_WEB user account
ca.bc.gov.cpf.db.maxConnections  The maximum number of database connections

ca.bc.gov.cpf.ws.consumerKey     The internal web service user (DON'T CHANGE)
ca.bc.gov.cpf.ws.consumerSecret  The password for the internal web service user

ca.bc.gov.cpf.fromEmail          The email address any emails will be sent from
ca.bc.gov.cpf.mailServer         The mail server to send emails via


4. CREATE DEPLOYMENT DIRECTORY

If the /apps_ux/cpf/deployment/plugins/ directory does not exist follow
these instructions to create it.

mkdir -p /apps_ux/cpf/deployment/plugins
chmod 775 /apps_ux/cpf/deployment/plugins

5. BUILD AND DEPLOY

Compile and deploy the application to the OC4J server.

  cd /apps_ux/cpf/source
  ant

NOTE: If there was an error compiling the application it will not be deployed.
STOP and contact the developer before continuing.

7. NOTIFICATION

Alert the Project Manager, the Application Manager, the Business Analyst,
and the IMB Delivery Specialist that the delivery is complete.
