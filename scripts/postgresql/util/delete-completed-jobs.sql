delete from cpf.cpf_batch_job_requests where batch_job_id in (select batch_job_id from cpf.cpf_batch_jobs where job_status in ('cancelled', 'resultsCreated', 'downloadInitiated'));
delete from cpf.cpf_batch_job_results where batch_job_id in (select batch_job_id from cpf.cpf_batch_jobs where job_status in ('cancelled', 'resultsCreated', 'downloadInitiated'));
delete from cpf.cpf_batch_jobs where job_status in ('cancelled', 'resultsCreated', 'downloadInitiated');
