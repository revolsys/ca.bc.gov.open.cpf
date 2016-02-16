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
  group_size numeric(5,0) NOT NULL,
  
  CONSTRAINT batch_jobs_pk PRIMARY KEY (batch_job_id)
);


CREATE INDEX batch_jobs_status_app_idx ON cpf.cpf_batch_jobs (job_status, business_application_name);

CREATE SEQUENCE cpf_bj_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
