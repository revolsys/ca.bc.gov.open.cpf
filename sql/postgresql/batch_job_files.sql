CREATE TABLE cpf_batch_job_files (
  batch_job_id bigint NOT NULL,
  path character varying(20) NOT NULL,
  sequence_number bigint NOT NULL,
  data oid,
  content_type character varying(50),
  
  CONSTRAINT batch_job_files_pk PRIMARY KEY (batch_job_id, path, sequence_number),
  CONSTRAINT batch_job_files_job_fk FOREIGN KEY (batch_job_id) REFERENCES CPF.cpf_batch_jobs(batch_job_id) ON DELETE CASCADE
);

CREATE INDEX bacth_job_files_job_idx ON cpf.cpf_batch_job_files (batch_job_id);
