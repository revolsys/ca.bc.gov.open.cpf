
ALTER TABLE CPF_APPLICATION_STATISTICS
    ADD CONSTRAINT application_statistics_pk PRIMARY KEY (application_statistic_id);

ALTER TABLE cpf_batch_jobs
    ADD CONSTRAINT batch_jobs_pk PRIMARY KEY (batch_job_id);

ALTER TABLE cpf_batch_job_files
    ADD CONSTRAINT batch_job_files_pk PRIMARY KEY (batch_job_id, path, sequence_number);

ALTER TABLE cpf_batch_job_files
    ADD CONSTRAINT batch_job_files_job_fk FOREIGN KEY (batch_job_id) REFERENCES CPF.cpf_batch_jobs(batch_job_id);

ALTER TABLE cpf_batch_job_results
    ADD CONSTRAINT batch_job_results_pk PRIMARY KEY (batch_job_id, sequence_number);

ALTER TABLE cpf_batch_job_results
    ADD CONSTRAINT batch_job_results_fk FOREIGN KEY (batch_job_id) REFERENCES CPF.cpf_batch_jobs(batch_job_id) ON DELETE CASCADE;

ALTER TABLE cpf_config_properties
    ADD CONSTRAINT config_properties_pk PRIMARY KEY (config_property_id);

ALTER TABLE cpf_user_accounts
    ADD CONSTRAINT user_accounts_pk PRIMARY KEY (user_account_id);

ALTER TABLE cpf_user_groups
    ADD CONSTRAINT user_groups_pk PRIMARY KEY (user_group_id);

ALTER TABLE cpf_user_groups
    ADD CONSTRAINT user_groups_user_group_name_uk UNIQUE (user_group_name);

ALTER TABLE cpf_user_group_account_xref
    ADD CONSTRAINT user_group_account_xref_pk PRIMARY KEY (user_group_id, user_account_id);

ALTER TABLE cpf_user_group_account_xref
    ADD CONSTRAINT user_group_xref_account_fk FOREIGN KEY (user_account_id) REFERENCES CPF.cpf_user_accounts(user_account_id);

ALTER TABLE cpf_user_group_account_xref
    ADD CONSTRAINT user_account_xref_group_fk FOREIGN KEY (user_group_id) REFERENCES CPF.cpf_user_groups(user_group_id);

ALTER TABLE cpf_user_group_permissions
    ADD CONSTRAINT user_group_permissions_pk PRIMARY KEY (user_group_permission_id);

ALTER TABLE cpf_user_group_permissions
    ADD CONSTRAINT user_group_permissions_fk FOREIGN KEY (user_group_id) REFERENCES CPF.cpf_user_groups(user_group_id) ON DELETE CASCADE;
