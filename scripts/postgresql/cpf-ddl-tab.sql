CREATE TABLE CPF_APPLICATION_STATISTICS (
  APPLICATION_STATISTIC_ID bigint NOT NULL,
  BUSINESS_APPLICATION_NAME character varying(255) NOT NULL,
  START_TIMESTAMP timestamp without time zone NOT NULL,
  DURATION_TYPE character varying(10) NOT NULL,
  STATISTIC_VALUES character varying(4000) NOT NULL
);


COMMENT ON TABLE CPF_APPLICATION_STATISTICS IS 'The APPLICATION STATISTIC object represents a collection of statistic values for a business application during a time peroid hour, day, month, year). The statistics are stored as a JSON map of keys/values to allow additional statistics to be added in the future. The statistic values are processed by the application and are not intended to be queried using SQL.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.APPLICATION_STATISTIC_ID IS 'This is the unique key for the APPLICATION STATISTIC.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.BUSINESS_APPLICATION_NAME IS 'This is the name of the business application the statistic was created for.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.START_TIMESTAMP IS 'This is the timestamp of the start of the duration the statistic was created for.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.DURATION_TYPE IS 'This is the type of duration (hour, day, month, year).'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.STATISTIC_VALUES IS 'This is the JSON encoded object of statistic name/values (e.g. {''jobsSumbitted'': 1, ''jobsCompleted'':11}.'
;

CREATE TABLE cpf_batch_jobs (
  BATCH_JOB_ID bigint NOT NULL,
  user_id character varying(50) NOT NULL,
  BUSINESS_APPLICATION_NAME character varying(255) NOT NULL,
  business_application_params text,
  properties text,
  when_status_changed timestamp without time zone NOT NULL,
  completed_timestamp timestamp without time zone,
  input_data_content_type character varying(255),
  structured_input_data_url character varying(2000),
  JOB_STATUS character varying(50) NOT NULL,
  last_scheduled_timestamp timestamp without time zone,
  notification_url character varying(2000),
  COMPLETED_REQUEST_RANGE text,
  COMPLETED_GROUP_RANGE text,
  FAILED_REQUEST_RANGE text,
  num_submitted_requests integer NOT NULL,
  result_data_content_type character varying(255) NOT NULL,
  WHO_CREATED character varying(36) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(36) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL,
  num_submitted_groups numeric(19,0) NOT NULL,
  group_size numeric(5,0) NOT NULL
);

CREATE TABLE cpf_batch_job_files (
  batch_job_id bigint NOT NULL,
  path character varying(20) NOT NULL,
  sequence_number bigint NOT NULL,
  data oid,
  content_type character varying(50)
);

CREATE TABLE cpf_batch_job_results (
  batch_job_result_type character varying(50) NOT NULL,
  download_timestamp timestamp without time zone,
  sequence_number integer NOT NULL,
  result_data oid,
  result_data_content_type character varying(255) NOT NULL,
  result_data_url character varying(2000),
  WHO_CREATED character varying(36) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(36) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL,
  batch_job_id bigint NOT NULL
);

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
  WHEN_UPDATED timestamp without time zone NOT NULL
);

CREATE TABLE cpf_user_accounts (
  user_account_id bigint NOT NULL,
  user_account_class character varying(255) NOT NULL,
  user_name character varying(255) NOT NULL,
  consumer_key character varying(36) NOT NULL,
  consumer_secret character varying(36) NOT NULL,
  active_ind numeric(1,0) NOT NULL,
  WHO_CREATED character varying(255) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(255) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL
);

CREATE TABLE cpf_user_groups (
  user_group_id bigint NOT NULL,
  module_name character varying(255) NOT NULL,
  user_group_name character varying(255) NOT NULL,
  description character varying(4000),
  active_ind numeric(1,0) NOT NULL,
  WHO_CREATED character varying(255) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(255) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL
);

CREATE TABLE cpf_user_group_account_xref (
  user_account_id bigint NOT NULL,
  user_group_id bigint NOT NULL
);

CREATE TABLE cpf_user_group_permissions (
  user_group_permission_id bigint NOT NULL,
  module_name character varying(255) NOT NULL,
  resource_class character varying(255) NOT NULL,
  resource_id character varying(255) NOT NULL,
  action_name character varying(255) NOT NULL,
  active_ind numeric(1,0) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_CREATED character varying(30) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(30) NOT NULL,
  user_group_id bigint NOT NULL
);
