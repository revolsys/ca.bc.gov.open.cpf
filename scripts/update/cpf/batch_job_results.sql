DROP CONSTRAINT BATCH_JOB_RESULTS_PK;

ALTER TABLE CPF.CPF_BATCH_JOB_RESULTS MODIFY SEQUENCE_NUMBER NUMBER(19,0) NOT NULL;

ALTER TABLE CPF.CPF_BATCH_JOB_RESULTS ADD CONSTRAINT BATCH_JOB_RESULTS_PK PRIMARY KEY (BATCH_JOB_ID, BATCH_JOB_RESULT_TYPE, SEQUENCE_NUMBER) using index tablespace cpf_ndx;

ALTER TABLE CPF.CPF_BATCH_JOB_RESULTS DROP COLUMN WHO_CREATED;
ALTER TABLE CPF.CPF_BATCH_JOB_RESULTS DROP COLUMN WHO_UPDATED;
ALTER TABLE CPF.CPF_BATCH_JOB_RESULTS DROP COLUMN WHEN_UPDATED;
ALTER TABLE CPF.CPF_BATCH_JOB_RESULTS DROP COLUMN BATCH_JOB_RESULT_ID;
ALTER TABLE CPF.CPF_BATCH_JOB_RESULTS DROP COLUMN BATCH_JOB_EXECUTION_GROUP_ID;