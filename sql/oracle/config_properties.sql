CREATE TABLE CPF_CONFIG_PROPERTIES (
  CONFIG_PROPERTY_ID              NUMBER(19)       NOT NULL,
  ENVIRONMENT_NAME                VARCHAR2(255)    NOT NULL,
  MODULE_NAME                     VARCHAR2(255)    NOT NULL,
  COMPONENT_NAME                  VARCHAR2(255)    NOT NULL,
  PROPERTY_NAME                   VARCHAR2(255)    NOT NULL,
  PROPERTY_VALUE                  VARCHAR2(4000),
  PROPERTY_VALUE_TYPE             VARCHAR2(50)     NOT NULL,
  WHO_CREATED                     VARCHAR2(255)    NOT NULL,
  WHEN_CREATED                    TIMESTAMP        NOT NULL,
  WHO_UPDATED                     VARCHAR2(255)    NOT NULL,
  WHEN_UPDATED                    TIMESTAMP        VARCHAR2(36)NOT NULL,
  CONSTRAINT CONFIG_PROPERTIES_PK PRIMARY KEY (CONFIG_PROPERTY_ID)
);

-- Sequence

CREATE SEQUENCE CPF_CP_SEQ;

-- Grants

GRANT DELETE, INSERT, SELECT, UPDATE ON CPF_CONFIG_PROPERTIES TO CPF_USER;

GRANT SELECT ON CPF_CP_SEQ TO CPF_USER;

-- Comments

COMMENT ON TABLE CPF_CONFIG_PROPERTIES IS 'The CONFIG PROPERTY represents a value to set for a property overriding the default value provided in the application code.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.CONFIG_PROPERTY_ID IS 'This is the unique key for the CONFIG PROPERTY.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.ENVIRONMENT_NAME IS 'ENVIRONMENT NAME contains the host name of web server instance name that the configuration property should be applied to.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.MODULE_NAME IS 'MODULE NAME contains the name of the CPF application module the configuration property should be applied to.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.COMPONENT_NAME IS 'COMPONENT NAME contains the name of the component within a CPF application module the configuration property should be applied to.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.PROPERTY_NAME IS 'This is the name of the configuration property. It can contain multiple parts separated by a period to set the value of a nested property. For example mapTileByLocation.maximumConcurrentRequests.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.PROPERTY_VALUE IS 'This is the value to set for the property.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.PROPERTY_VALUE_TYPE IS 'PROPERTY VALUE TYPE contains the data type name used to convert the PROPERTY VALUE to a Java data type (e.g. string, int, double, boolean, GEOMETRY).';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHO_CREATED IS 'This is the database or web user that created the object.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHEN_CREATED IS 'This is the date that the object was created.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHO_UPDATED IS 'This is the database or web user that last updated the object.';

COMMENT ON COLUMN CPF_CONFIG_PROPERTIES.WHEN_UPDATED IS 'This is the date that the object was last updated.';
