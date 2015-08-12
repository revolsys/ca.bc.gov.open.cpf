
CREATE INDEX batch_jobs_status_app_idx ON cpf.cpf_batch_jobs (job_status, business_application_name);


CREATE INDEX bacth_job_files_job_idx ON cpf.cpf_batch_job_files (batch_job_id);
