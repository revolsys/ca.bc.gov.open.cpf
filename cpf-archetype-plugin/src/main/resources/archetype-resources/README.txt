Description
-----------
Project:           ${pluginAcronym}
Title:             ${pluginName}
Version:           ${version}

Software/Hardware Requirements
------------------------------
Oracle:                       Oracle 10+
Java:                         6+
Maven:                        3.0.3+
App Server:                   Tomcat 7+
App Server Additional Memory: 20MB
CPF:                          4.0.0+


1. Database Installation
------------------------

svn co http://apps.bcgov/svn/${pluginAcronym}/source/trunk/scripts
cd scripts
sqlplus ${pluginAcronym}@<SERVER> @${pluginAcronym}-${version}.sql

NOTE: Check the ${pluginAcronym}-${version}.log files for any errors. Email the
      file to thedeveloper if there were any errors.

2. Configuration Files
----------------------

N/A

3. Ministry Continuous Integration System
-----------------------------------------

The application can be build and deployed using the  Ministry Continuous
Integration System, use the Ministry Standards below as a Guide.

http://apps.bcgov/standards/index.php/Migration_Task_with_CIS

Construct a new new maven 2/3 job with the following parameters.

Project name:                       revolys-${pluginAcronym}-deploy
Description:                        Build the ${pluginName} and deploy to artifactory.
Source Code Management: 
  (*) Subversion:
    Repository URL:                 http://apps.bcgov/svn/${pluginAcronym}/trunk/
MVN Goals and options:              clean install
Resolve Artifacts from Artifactory: Yes
Post-build Actions:
 Deploy artifacts to Artifactory:
   Artifactory Server:              http://delivery.apps.bcgov/artifactory/
     Target releases repository:    libs-release-local
     Target snapshots repository:   libs-snapshot-local
  Deploy to Tomcat:                 # This project is a library so is not
                                    # deployed to Tomcat
  Build other projects              # Other than in delivery leave blank 
                                    # Manually build other projects
                                    # Do not allow other projects to be auto
                                    # built when this project is built

4. Compilation & Deployment
---------------------------

Build the revolys-${pluginAcronym}-deploy job using the Ministry Continuous Integration
System.

5. Post Build Actions
---------------------

The following actions must be followed the first time the module is deployed
to a server.

5.1 CPF Module Deployment
-------------------------

a. Open https://delivery.apps.gov.bc.ca/pub/cpf/secure/admin/modules/
b. If the module ${pluginAcronym} does not exist follow these instructions
  1. Click the 'Add' button
  2. Module Name:      ${pluginAcronym}
  3. Maven Module Id:  ${groupId}:${pluginAcronym}:${version} (replace -SNAPSHOT with .RC# for test, remove -SNAPSHOT for production)
  4. Click the 'Save' button and the Module ${pluginAcronym} view page should be displayed
  5. If there is an error on the next page contact the developer for resolution
c. If the module ${pluginAcronym} does exist follow these instructions
  1. Click the link in the Name column for ${pluginAcronym}
  2. Click the 'Edit' button
  3. Maven Module Id:  ${groupId}:${pluginAcronym}:${version} (replace -SNAPSHOT with .RC# for test, remove -SNAPSHOT for production)
  4. Enabled: Yes
  5. Click the 'Save' button and the Module ${pluginAcronym} view page should be displayed
  6. If there is an error on the next page contact the developer for resolution

5.2 CPF Admin User Permissions
------------------------------
In delivery the developer will need to be a member of the MODULE_ADMIN_${pluginAcronym}
group.

a. On the Module ${pluginAcronym} view page (access from the Modules menu then click on the
   ${pluginAcronym} module).
b. Click the [+] on Module Admin User Groups
c. Click the Id next to MODULE_ADMIN_${pluginAcronym} to view the group
d. Click the [+] on User Accounts for Groups
e. In the username box type at least 3 characters of the user's IDIR name
   (e.g. idir:pxaustin). Note the use of : instead of \. If the user doesn't
   exist, email and ask them to login to the following URL to force the user
   account to be created
   https://delivery.apps.gov.bc.ca/pub/cpf/secure/admin/
f. Click Add when the user is added

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
  ca.bc.gov.open.cpf.version: 4.0.0+

Test Migration
**************

Test Version:                 1.0.0.RC# Increment for each migration to test

Production Migration
********************
Release Version:              1.0.0
Next Development Version:     1.0.1-SNAPSHOT
