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
  batch_job_id bigint NOT NULL,
  
  CONSTRAINT batch_job_results_pk PRIMARY KEY (batch_job_id, sequence_number),
  CONSTRAINT batch_job_results_fk FOREIGN KEY (batch_job_id) REFERENCES CPF.cpf_batch_jobs(batch_job_id) ON DELETE CASCADE
);
