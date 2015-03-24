CREATE TABLE cpf_application_statistics (
    application_statistic_id bigint NOT NULL,
    business_application_name character varying(255) NOT NULL,
    start_timestamp timestamp without time zone NOT NULL,
    duration_type character varying(10) NOT NULL,
    statistic_values character varying(4000) NOT NULL
);

ALTER TABLE cpf_application_statistics
    ADD CONSTRAINT application_statistics_pk PRIMARY KEY (application_statistic_id);

CREATE SEQUENCE cpf_as_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE cpf_batch_jobs (
    batch_job_id bigint NOT NULL,
    user_id character varying(50) NOT NULL,
    business_application_name character varying(255) NOT NULL,
    business_application_params text,
    properties text,
    when_status_changed timestamp without time zone NOT NULL,
    completed_timestamp timestamp without time zone,
    input_data_content_type character varying(255),
    structured_input_data_url character varying(2000),
    job_status character varying(50) NOT NULL,
    last_scheduled_timestamp timestamp without time zone,
    notification_url character varying(2000),
    num_completed_requests integer NOT NULL,
    num_failed_requests integer NOT NULL,
    num_submitted_requests integer NOT NULL,
    result_data_content_type character varying(255) NOT NULL,
    who_created character varying(36) NOT NULL,
    when_created timestamp without time zone NOT NULL,
    who_updated character varying(36) NOT NULL,
    when_updated timestamp without time zone NOT NULL,
    num_submitted_groups numeric(19,0) NOT NULL,
    num_completed_groups numeric(19,0) NOT NULL,
    group_size numeric(5,0) NOT NULL
);

ALTER TABLE cpf_batch_jobs
    ADD CONSTRAINT batch_jobs_pk PRIMARY KEY (batch_job_id);

CREATE INDEX batch_jobs_status_app_idx ON cpf.cpf_batch_jobs (job_status, business_application_name);

CREATE SEQUENCE cpf_bj_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
CREATE TABLE cpf_batch_job_execution_groups (
    completed_ind numeric(1,0),
    input_data oid,
    input_data_content_type character varying(255),
    input_data_url character varying(2000),
    sequence_number integer NOT NULL,
    result_data oid,
    result_data_url character varying(2000),
    started_ind numeric(1,0),
    structured_input_data text,
    structured_result_data text,
    who_created character varying(36) NOT NULL,
    when_created timestamp without time zone NOT NULL,
    who_updated character varying(36) NOT NULL,
    when_updated timestamp without time zone NOT NULL,
    batch_job_id bigint NOT NULL,
    num_submitted_requests numeric(19,0) NOT NULL,
    num_completed_requests numeric(19,0) NOT NULL,
    num_failed_requests numeric(19,0) NOT NULL
);

ALTER TABLE cpf_batch_job_execution_groups
    ADD CONSTRAINT batch_job_execution_groups_pk PRIMARY KEY (batch_job_id, sequence_number);

ALTER TABLE cpf_batch_job_execution_groups
    ADD CONSTRAINT execution_group_batch_job_fk FOREIGN KEY (batch_job_id) REFERENCES CPF.cpf_batch_jobs(batch_job_id) ON DELETE CASCADE;

CREATE INDEX batch_jon_execution_idx ON cpf.cpf_batch_job_execution_groups (batch_job_id, started_ind);

CREATE TABLE cpf_batch_job_files (
    batch_job_id bigint NOT NULL,
    path character varying(20) NOT NULL,
    sequence_number bigint NOT NULL,
    data oid,
    content_type character varying(50)
);

ALTER TABLE cpf_batch_job_files
    ADD CONSTRAINT batch_job_files_pk PRIMARY KEY (batch_job_id, path, sequence_number);

ALTER TABLE cpf_batch_job_files
    ADD CONSTRAINT batch_job_files_job_fk FOREIGN KEY (batch_job_id) REFERENCES CPF.cpf_batch_jobs(batch_job_id);

CREATE INDEX bacth_job_files_job_idx ON cpf.cpf_batch_job_files (batch_job_id);

CREATE TABLE cpf_batch_job_results (
    batch_job_result_type character varying(50) NOT NULL,
    download_timestamp timestamp without time zone,
    sequence_number integer NOT NULL,
    result_data oid,
    result_data_content_type character varying(255) NOT NULL,
    result_data_url character varying(2000),
    who_created character varying(36) NOT NULL,
    when_created timestamp without time zone NOT NULL,
    who_updated character varying(36) NOT NULL,
    when_updated timestamp without time zone NOT NULL,
    batch_job_id bigint NOT NULL
);

ALTER TABLE cpf_batch_job_results
    ADD CONSTRAINT batch_job_results_pk PRIMARY KEY (batch_job_id, sequence_number);

ALTER TABLE cpf_batch_job_results
    ADD CONSTRAINT batch_job_results_fk FOREIGN KEY (batch_job_id) REFERENCES CPF.cpf_batch_jobs(batch_job_id) ON DELETE CASCADE;

CREATE TABLE cpf_config_properties (
    config_property_id bigint NOT NULL,
    environment_name character varying(255) NOT NULL,
    module_name character varying(255) NOT NULL,
    component_name character varying(255) NOT NULL,
    property_name character varying(255) NOT NULL,
    property_value character varying(4000),
    property_value_type character varying(50) NOT NULL,
    who_created character varying(255) NOT NULL,
    when_created timestamp without time zone NOT NULL,
    who_updated character varying(255) NOT NULL,
    when_updated timestamp without time zone NOT NULL
);

ALTER TABLE cpf_config_properties
    ADD CONSTRAINT config_properties_pk PRIMARY KEY (config_property_id);

CREATE SEQUENCE cpf_cp_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE cpf_user_accounts (
    user_account_id bigint NOT NULL,
    user_account_class character varying(255) NOT NULL,
    user_name character varying(255) NOT NULL,
    consumer_key character varying(36) NOT NULL,
    consumer_secret character varying(36) NOT NULL,
    active_ind numeric(1,0) NOT NULL,
    who_created character varying(255) NOT NULL,
    when_created timestamp without time zone NOT NULL,
    who_updated character varying(255) NOT NULL,
    when_updated timestamp without time zone NOT NULL
);

ALTER TABLE cpf_user_accounts
    ADD CONSTRAINT user_accounts_pk PRIMARY KEY (user_account_id);

CREATE SEQUENCE cpf_ua_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE cpf_user_groups (
    user_group_id bigint NOT NULL,
    module_name character varying(255) NOT NULL,
    user_group_name character varying(255) NOT NULL,
    description character varying(4000),
    active_ind numeric(1,0) NOT NULL,
    who_created character varying(255) NOT NULL,
    when_created timestamp without time zone NOT NULL,
    who_updated character varying(255) NOT NULL,
    when_updated timestamp without time zone NOT NULL
);

ALTER TABLE cpf_user_groups
    ADD CONSTRAINT user_groups_pk PRIMARY KEY (user_group_id);

ALTER TABLE cpf_user_groups
    ADD CONSTRAINT user_groups_user_group_name_uk UNIQUE (user_group_name);

CREATE SEQUENCE cpf_ug_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE cpf_user_group_account_xref (
    user_account_id bigint NOT NULL,
    user_group_id bigint NOT NULL
);

ALTER TABLE cpf_user_group_account_xref
    ADD CONSTRAINT user_group_account_xref_pk PRIMARY KEY (user_group_id, user_account_id);

ALTER TABLE cpf_user_group_account_xref
    ADD CONSTRAINT user_group_xref_account_fk FOREIGN KEY (user_account_id) REFERENCES CPF.cpf_user_accounts(user_account_id);

ALTER TABLE cpf_user_group_account_xref
    ADD CONSTRAINT user_account_xref_group_fk FOREIGN KEY (user_group_id) REFERENCES CPF.cpf_user_groups(user_group_id);

CREATE TABLE cpf_user_group_permissions (
    user_group_permission_id bigint NOT NULL,
    module_name character varying(255) NOT NULL,
    resource_class character varying(255) NOT NULL,
    resource_id character varying(255) NOT NULL,
    action_name character varying(255) NOT NULL,
    active_ind numeric(1,0) NOT NULL,
    when_created timestamp without time zone NOT NULL,
    who_created character varying(30) NOT NULL,
    when_updated timestamp without time zone NOT NULL,
    who_updated character varying(30) NOT NULL,
    user_group_id bigint NOT NULL
);

ALTER TABLE cpf_user_group_permissions
    ADD CONSTRAINT user_group_permissions_pk PRIMARY KEY (user_group_permission_id);


ALTER TABLE cpf_user_group_permissions
    ADD CONSTRAINT user_group_permissions_fk FOREIGN KEY (user_group_id) REFERENCES CPF.cpf_user_groups(user_group_id) ON DELETE CASCADE;

CREATE SEQUENCE cpf_ugp_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
