********************************************************************************
* gvws
*
* $URL$
* $Author$
* $Date$
* $Revision$
* Release:       
* Creation Date: 
*
* Modification History
*                   
********************************************************************************

OVERVIEW
--------
This file describes the steps to configure, build and install the gvws
(1.0.0) plug-in to be deployed to the Cloud Processing Framework CPF running
on a UNIX Oracle Application Server (10.1.3) and a Unix Oracle version 10g
database.

These instructions are for the Ministry's database administrator and Java
Application Delivery Specialist, and assume familiarity with standard Oracle
administrative functions. gvws is the officially assigned application short
name.

REQUIREMENTS
------------------------
Prior to installing the application, the following requirements must be
verified:
 - This installation assumes installation of the web application on Oracle 
   10g Release 3 (10.1.3)
 - the UNIX user gvws has been created and is using the Korn Shell (ksh).
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
 - The Cloud Processing Framework must be installed.

ENHANCEMENTS
------------
1. 

BUG FIXES
---------
1.
 
MODIFIED COMPONENTS
-------------------
1.


OBSOLETE COMPONENTS
-------------------
1.

FILES IN THIS RELEASE
---------------------
See subversion for a difference between this release and the last release

INSTALLATION INSTRUCTIONS
-------------------------

1. LOGIN TO THE APPLICATION SERVER

  ssh gvws@smolder.geobc.gov.bc.ca
    password = ********

2. DOWNLOAD SOURCE CODE FROM SUBVERSION

cd /apps_ux/gvws

If /apps_ux/gvws/source does not exist checkout the source from Subversion.

svn co --username gvws https://poplar.idir.bcgov:88/svn/gvws/trunk/ source

NOTE: If prompted for a certificate validation error, accept the certificate
permanently.

NOTE: If you are prompted to store the password encrypted enter yes. The
directory the password is stored in can only be read by the current user.

If /apps_ux/gvws/source does exist update it to the latest revision.

svn up source

2. RUN THE DATABASE SCRIPTS

************************************ DEVELOPER NOTE ****************************
Delete any sections that are not applicable.
********************************************************************************

2.1. Run the DBA database scripts

  cd /apps_ux/gvws/source/ddl
  ./gvws-dba.sh GEODLV

NOTE: If there were any errors STOP and contact the developer before continuing.
The log file is in ../log/dba.log.

************************************ DEVELOPER NOTE ****************************
Edit gvws-dba.sh and gvws-dba-all.sql for your application, or delete them if
they are not required.

Change GEODLV to the TNS name or the database instance to deploy to in delivery.
********************************************************************************

2.2. Run the application database scripts

  cd /apps_ux/gvws/source/ddl
  ./ddl.sh GEODLV

NOTE: If there were any errors STOP and contact the developer before continuing.
The log file is in ../log/ddl.log.

************************************ DEVELOPER NOTE ****************************
Edit ddl.sh and ddl-all.sql for your application, or delete them if
they are not required.

Change GEODLV to the TNS name or the database instance to deploy to in delivery.
********************************************************************************

3. CONFIGURE THE APPLICATION

cd /apps_ux/gvws/source/config

If default.properties does not exist copy the sample to create it.

cp sample-default.properties default.properties

Edit default.properties to configure it for the application.

vi default.properties

The following property values must be configured for each environment.

************************************ DEVELOPER NOTE ****************************
Add a list of each property name and value in the format shown below, leave a
blank line between each property.

propertyName - Description of the property, indent each line by 4 spaces if the
   description wraps multiple lines.
********************************************************************************

4. CREATE DEPLOYMENT DIRECTORY

If the /apps_ux/cpf/deployment/plugins/gvws/ directory does not exist follow
these instructions to create it.

cd /apps_ux/cpf/deployment/plugins
mkdir gvws
chmod 755 gvws

5. BUILD AND DEPLOY PLUGIN

Compile and deploy the plug-in to the CPF plug-ins directory.

  cd /apps_ux/gvws/source
  ant

NOTE: If there was an error compiling the application it will not be deployed.
STOP and contact the developer before continuing.
  
6. RESTART THE CPF APPLICATION

When a plug-in has been deployed the CPF web application must be restarted using
the OC4j Enterprise Manager. There is no need to re-compile the CPF.

1. Login to the OC4J enterprise manager.
2. Select the Applications tab.
3. Click on the cpf application.
4. Click the Restart button.
5. Click the Yes button.

7. NOTIFICATION

Alert the Project Manager, the Application Manager, the Business Analyst,
and the IMB Delivery Specialist that the delivery is complete.
