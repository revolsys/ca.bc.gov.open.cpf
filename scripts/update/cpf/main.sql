-- 5.0.0

DELETE FROM CPF.CPF_BATCH_JOB_EXECUTION_GROUPS;
DELETE FROM CPF.CPF_BATCH_JOB_RESULTS;
DELETE FROM CPF.CPF_BATCH_JOBS;

DROP TABLE CPF.CPF_BATCH_JOB_EXECUTION_GROUPS;

@batch_jobs.sql
@batch_job_results.sql
@batch_job_files.sql
@batch_job_status_change.sql
@user_group_account_xref.sql

commit;
exit