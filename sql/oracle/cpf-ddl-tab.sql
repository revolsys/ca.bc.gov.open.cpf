

PROMPT Creating Table 'CPF_CONFIG_PROPERTIES'
CREATE TABLE CPF_CONFIG_PROPERTIES(
  CONFIG_PROPERTY_ID NUMBER(19,0) NOT NULL,
  ENVIRONMENT_NAME VARCHAR2(255) NOT NULL,
  MODULE_NAME VARCHAR2(255) NOT NULL,
  COMPONENT_NAME VARCHAR2(255) NOT NULL,
  PROPERTY_NAME VARCHAR2(255) NOT NULL,
  PROPERTY_VALUE_TYPE VARCHAR2(255) NOT NULL,
  PROPERTY_VALUE VARCHAR2(4000),
  WHO_CREATED VARCHAR2(36) NOT NULL,
  WHEN_CREATED TIMESTAMP NOT NULL,
  WHO_UPDATED VARCHAR2(36) NOT NULL,
  WHEN_UPDATED TIMESTAMP NOT NULL
  )
  TABLESPACE CPF
/

COMMENT ON TABLE CPF_CONFIG_PROPERTIES IS 'The CONFIG PROPERTY represents a value to set for a property overriding the default value provided in the application code.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.CONFIG_PROPERTY_ID IS 'This is the unique key for the CONFIG PROPERTY.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.ENVIRONMENT_NAME IS 'ENVIRONMENT NAME contains the host name of web server instance name that the configuration property should be applied to.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.MODULE_NAME IS 'MODULE NAME contains the name of the CPF application module the configuration property should be applied to.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.COMPONENT_NAME IS 'COMPONENT NAME contains the name of the component within a CPF application module the configuration property should be applied to.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.PROPERTY_NAME IS 'This is the name of the configuration property. It can contain multiple parts separated by a period to set the value of a nested property. For example mapTileByLocation.maximumConcurrentRequests.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.PROPERTY_VALUE_TYPE IS 'PROPERTY VALUE TYPE contains the data type name used to convert the PROPERTY VALUE to a Java data type (e.g. string, int, double, boolean, GEOMETRY).'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.PROPERTY_VALUE IS 'This is the value to set for the property.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHO_CREATED IS 'This is the database or web user that created the object.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHEN_CREATED IS 'This is the date that the object was created.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHO_UPDATED IS 'This is the database or web user that last updated the object.'
/

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHEN_UPDATED IS 'This is the date that the object was last updated.'
/

PROMPT Creating Table 'CPF_USER_ACCOUNTS'
CREATE TABLE CPF_USER_ACCOUNTS(
  USER_ACCOUNT_ID NUMBER(19,0) NOT NULL,
  USER_ACCOUNT_CLASS VARCHAR2(255) NOT NULL,
  USER_NAME VARCHAR2(4000) NOT NULL,
  CONSUMER_KEY VARCHAR2(36) NOT NULL,
  CONSUMER_SECRET VARCHAR2(36) NOT NULL,
  ACTIVE_IND NUMBER(1,0) NOT NULL,
  WHO_CREATED VARCHAR2(36) NOT NULL,
  WHEN_CREATED TIMESTAMP NOT NULL,
  WHO_UPDATED VARCHAR2(36) NOT NULL,
  WHEN_UPDATED TIMESTAMP NOT NULL
  )
  PCTFREE 10
  TABLESPACE CPF
/

COMMENT ON TABLE CPF_USER_ACCOUNTS IS 'The USER ACCOUNTS represents a user account created for an internal or external user, for example a OpenID account, BC Government IDIR account, or email address.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.USER_ACCOUNT_ID IS 'This is the unique identifier for the USER ACCOUNT.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.USER_ACCOUNT_CLASS IS 'This is the classification for the USER NAME, for example http://openid.net/ or http://idir.bcgov/.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.USER_NAME IS 'This is the user name, for example an OpenID, IDIR account name, or email address.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.CONSUMER_KEY IS 'This is the system generated unique user identifier for the user account.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.CONSUMER_SECRET IS 'This is the system generated password/encryption key for the user account.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.ACTIVE_IND IS 'This is the true (1), false (0) indicator if the object is active or has been deleted.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHO_CREATED IS 'This is the database or web user that created the object.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHEN_CREATED IS 'This is the date that the object was created.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHO_UPDATED IS 'This is the database or web user that last updated the object.'
/

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHEN_UPDATED IS 'This is the date that the object was last updated.'
/

PROMPT Creating Table 'CPF_USER_GROUP_ACCOUNT_XREF'
CREATE TABLE CPF_USER_GROUP_ACCOUNT_XREF(
  USER_GROUP_ID NUMBER(19,0) NOT NULL,
  USER_ACCOUNT_ID NUMBER(19,0) NOT NULL
  )
  PCTFREE 10
  TABLESPACE CPF
/

COMMENT ON TABLE CPF_USER_GROUP_ACCOUNT_XREF IS 'USER GROUP ACCOUNT XREF represents a relationship between a USER GROUP and the USER ACCOUNT that is a member of that group.'
/

COMMENT ON COLUMN CPF_USER_GROUP_ACCOUNT_XREF.USER_GROUP_ID IS 'USER GROUP ID is a unique surrogate identifier for the object USER GROUP.'
/

COMMENT ON COLUMN CPF_USER_GROUP_ACCOUNT_XREF.USER_ACCOUNT_ID IS 'USER ACCOUNT ID is a unique surrogate identifier for the object USER ACCOUNT.'
/

PROMPT Creating Table 'CPF_USER_GROUPS'
CREATE TABLE CPF_USER_GROUPS(
  USER_GROUP_ID NUMBER(19,0) NOT NULL,
  MODULE_NAME VARCHAR2(255) NOT NULL,
  USER_GROUP_NAME VARCHAR2(255) NOT NULL,
  DESCRIPTION VARCHAR2(4000),
  ACTIVE_IND NUMBER(1,0) NOT NULL,
  WHO_CREATED VARCHAR2(36) NOT NULL,
  WHEN_CREATED TIMESTAMP NOT NULL,
  WHO_UPDATED VARCHAR2(36) NOT NULL,
  WHEN_UPDATED TIMESTAMP NOT NULL
  )
  PCTFREE 10
  TABLESPACE CPF
/

COMMENT ON TABLE CPF_USER_GROUPS IS 'USER GROUP represents a named group of CPF or external users that can be granted permissions via USER GROUP PERMISSION. The members of the group are defined in the USER GROUP ACCOUNT XFREF.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.USER_GROUP_ID IS 'USER GROUP ID is a unique surrogate identifier for the object USER GROUP.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.MODULE_NAME IS 'MODULE NAME contains the name of the CPF application module the USER GROUP was created for.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.USER_GROUP_NAME IS 'This is the name of the SECURITY GROUP.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.DESCRIPTION IS 'This is a description of the security group.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.ACTIVE_IND IS 'This is the true (1), false (0) indicator if the object is active or has been deleted.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.WHO_CREATED IS 'This is the database or web user that created the object.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.WHEN_CREATED IS 'This is the date that the object was created.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.WHO_UPDATED IS 'This is the database or web user that last updated the object.'
/

COMMENT ON COLUMN CPF_USER_GROUPS.WHEN_UPDATED IS 'This is the date that the object was last updated.'
/

PROMPT Creating Table 'CPF_USER_GROUP_PERMISSIONS'
CREATE TABLE CPF_USER_GROUP_PERMISSIONS(
  USER_GROUP_PERMISSION_ID NUMBER(19,0) NOT NULL,
  MODULE_NAME VARCHAR2(255) NOT NULL,
  USER_GROUP_ID NUMBER(19) NOT NULL,
  RESOURCE_CLASS VARCHAR2(255) NOT NULL,
  RESOURCE_ID VARCHAR2(4000) NOT NULL,
  ACTION_NAME VARCHAR2(255) NOT NULL,
  ACTIVE_IND NUMBER(1,0) NOT NULL,
  WHO_CREATED VARCHAR2(36) NOT NULL,
  WHEN_CREATED TIMESTAMP NOT NULL,
  WHO_UPDATED VARCHAR2(36) NOT NULL,
  WHEN_UPDATED TIMESTAMP NOT NULL
  )
  PCTFREE 10
  TABLESPACE CPF
/

COMMENT ON TABLE CPF_USER_GROUP_PERMISSIONS IS 'USER GROUP P ERMISSION represents a permission for a member of a USER GROUP to perfom an action on a given resource for a module.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.USER_GROUP_PERMISSION_ID IS 'USER GROUP PERMISSION ID is a unique surrogate identifier for the object USER GROUP PERMISSION.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.MODULE_NAME IS 'MODULE NAME contains the name of the CPF application module the USER GROUP PERMISSION was created for.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.USER_GROUP_ID IS 'USER GROUP ID is a unique surrogate identifier for the object USER GROUP.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.RESOURCE_CLASS IS 'This is the classification of the RESOURCE NAME. For example URL (web page), JavaClass, JavaObject.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.RESOURCE_ID IS 'This is the unqiue id of the resource the permission applies to. For example a url, java class name, java class name and object identifier.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.ACTION_NAME IS 'This is the name of the action the user is granted permission to perform (e.g. any, edit, view).'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.ACTIVE_IND IS 'This is the true (1), false (0) indicator if the object is active or has been deleted.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHO_CREATED IS 'This is the database or web user that created the object.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHEN_CREATED IS 'This is the date that the object was created.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHO_UPDATED IS 'This is the database or web user that last updated the object.'
/

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHEN_UPDATED IS 'This is the date that the object was last updated.'
/
