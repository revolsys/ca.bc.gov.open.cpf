Description
-----------
Project:           cpf-api
Title:             Cloud Processing Framework Open API
Version:           4.0.1

Software/Hardware Requirements
------------------------------
Oracle:                       N/A
Java:                         6+
Maven:                        3.0.3+
App Server:                   N/A
App Server Additional Memory: N/A


1. Database Installation
------------------------

N/A

2. Configuration Files
----------------------

N/A

3. Ministry Continuous Integration System
-----------------------------------------

The application can be build and deployed using the  Ministry Continuous
Integration System, use the Ministry Standards below as a Guide.

http://apps.bcgov/standards/index.php/Migration_Task_with_CIS

Create a new maven 2/3 job with the following parameters.

Project name:                       revolys-cpf-api-deploy
Description:                        Build the shared CPF Open API and deploy to artifactory.
Source Code Management: 
  (*) Subversion:
    Repository URL:                 http://apps.bcgov/svn/cpf/api-source/trunk/
MVN Goals and options:              clean install
E-Mail Notification:                leo.lou@gov.bc.ca paul.austin@revolsys.com
Resolve Artifacts from Artifactory: Yes
Post-build Actions:
 Deploy artifacts to Artifactory:
   Artifactory Server:              http://delivery.apps.bcgov/artifactory/
     Target releases repository:    libs-release-local
     Target snapshots repository:   libs-snapshot-local
  Deploy to Tomcat:                 # This project is a library so is not
                                    # deployed to Tomcat.
  Build other projects              # Other than in delivery leave blank. 
                                    # Manually build other projects.
                                    # Do not allow other projects to be auto
                                    # built when this project is built.

4. Compilation & Deployment
---------------------------

Build the revolys-cpf-api-deploy job using the Ministry Continuous Integration
System.

5. Post Build Actions
---------------------

N/A

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

Perform a maven release using the following settings.

 * Test Migration

Test Version:             4.0.2.RC[1..9] Increment for each migration to test

 * Production Migration
Release Version:          4.0.1
Next Development Version: 4.0.2-SNAPSHOT
