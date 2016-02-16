CREATE TABLE cpf_config_properties (
  config_property_id bigint NOT NULL,
  environment_name character varying(255) NOT NULL,
  module_name character varying(255) NOT NULL,
  component_name character varying(255) NOT NULL,
  property_name character varying(255) NOT NULL,
  property_value character varying(4000),
  property_value_type character varying(50) NOT NULL,
  WHO_CREATED character varying(255) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(255) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL,
  
  CONSTRAINT config_properties_pk PRIMARY KEY (config_property_id)
);

CREATE SEQUENCE cpf_cp_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
