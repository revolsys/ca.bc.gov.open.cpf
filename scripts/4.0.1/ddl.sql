prompt CPF_BATCH_JOBS

DECLARE
  record_exists number := 0;  
BEGIN
  UPDATE CPF_BATCH_JOBS SET JOB_STATUS = 'cancelled' WHERE JOB_STATUS NOT IN ('creatingResults', 'resultsCreated', 'downloadInitiated');
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOBS' and column_name = 'NUM_EXECUTING_REQUESTS';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOBS RENAME COLUMN NUM_EXECUTING_REQUESTS TO NUM_SCHEDULED_GROUPS';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOBS' and column_name = 'GROUP_SIZE';
  if (record_exists <> 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOBS ADD GROUP_SIZE NUMBER(5,0)';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOBS' and column_name = 'NUM_SUBMITTED_GROUPS';
  if (record_exists <> 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOBS ADD NUM_SUBMITTED_GROUPS NUMBER(19,0)';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOBS' and column_name = 'NUM_COMPLETED_GROUPS';
  if (record_exists <> 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOBS ADD NUM_COMPLETED_GROUPS NUMBER(19,0)';
  end if;
END;
/

UPDATE CPF_BATCH_JOBS SET GROUP_SIZE = 0, NUM_SUBMITTED_GROUPS = 0, NUM_COMPLETED_GROUPS = 0 WHERE GROUP_SIZE IS NULL;

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOBS' and column_name = 'GROUP_SIZE' and nullable = 'Y';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOBS MODIFY GROUP_SIZE NUMBER(5,0) NOT NULL';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOBS' and column_name = 'NUM_SUBMITTED_GROUPS' and nullable = 'Y';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOBS MODIFY NUM_SUBMITTED_GROUPS NUMBER(19,0) NOT NULL';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOBS' and column_name = 'NUM_COMPLETED_GROUPS' and nullable = 'Y';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOBS MODIFY NUM_COMPLETED_GROUPS NUMBER(19,0) NOT NULL';
  end if;

end;
/

prompt CPF_BATCH_JOB_EXECUTION_GROUPS

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tables where table_name = 'CPF_BATCH_JOB_REQUESTS';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_REQUESTS RENAME TO CPF_BATCH_JOB_EXECUTION_GROUPS';
  end if;
END;
/

TRUNCATE TABLE CPF_BATCH_JOB_EXECUTION_GROUPS;

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'ERROR_CODE';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS DROP COLUMN ERROR_CODE';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'ERROR_DEBUG_MESSAGE';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS DROP COLUMN ERROR_DEBUG_MESSAGE';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'ERROR_MESSAGE';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS DROP COLUMN ERROR_MESSAGE';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'BATCH_JOB_REQUEST_ID';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS RENAME COLUMN BATCH_JOB_REQUEST_ID TO BATCH_JOB_EXECUTION_GROUP_ID';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'REQUEST_SEQUENCE_NUMBER';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS RENAME COLUMN REQUEST_SEQUENCE_NUMBER TO SEQUENCE_NUMBER';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'NUM_SUBMITTED_REQUESTS';
  if (record_exists <> 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS ADD NUM_SUBMITTED_REQUESTS NUMBER(19,0)';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'NUM_COMPLETED_REQUESTS';
  if (record_exists <> 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS ADD NUM_COMPLETED_REQUESTS NUMBER(19,0)';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'NUM_FAILED_REQUESTS';
  if (record_exists <> 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS ADD NUM_FAILED_REQUESTS NUMBER(19,0)';
  end if;
END;
/

UPDATE CPF_BATCH_JOB_EXECUTION_GROUPS SET NUM_SUBMITTED_REQUESTS = 0,  NUM_COMPLETED_REQUESTS = 0,  NUM_FAILED_REQUESTS = 0 WHERE NUM_SUBMITTED_REQUESTS IS NULL;

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'NUM_SUBMITTED_REQUESTS' and nullable = 'Y';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS MODIFY NUM_SUBMITTED_REQUESTS NUMBER(19,0) NOT NULL';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'NUM_COMPLETED_REQUESTS' and nullable = 'Y';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS MODIFY NUM_COMPLETED_REQUESTS NUMBER(19,0) NOT NULL';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_EXECUTION_GROUPS' and column_name = 'NUM_FAILED_REQUESTS' and nullable = 'Y';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_EXECUTION_GROUPS MODIFY NUM_FAILED_REQUESTS NUMBER(19,0) NOT NULL';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_sequences where sequence_name = 'CPF_BJRQ_SEQ';
  if (record_exists = 1) then
    execute immediate 'RENAME CPF_BJRQ_SEQ TO CPF_BJEG_SEQ';
  end if;
END;
/

prompt CPF_BATCH_JOB_RESULTS

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_RESULTS' and column_name = 'BATCH_JOB_REQUEST_ID';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_RESULTS RENAME COLUMN BATCH_JOB_REQUEST_ID TO BATCH_JOB_EXECUTION_GROUP_ID';
  end if;
END;
/

DECLARE
  record_exists number := 0;  
BEGIN
  select count(*) into record_exists from user_tab_cols where table_name = 'CPF_BATCH_JOB_RESULTS' and column_name = 'REQUEST_SEQUENCE_NUMBER';
  if (record_exists = 1) then
    execute immediate 'ALTER TABLE CPF_BATCH_JOB_RESULTS RENAME COLUMN REQUEST_SEQUENCE_NUMBER TO SEQUENCE_NUMBER';
  end if;
END;
/
