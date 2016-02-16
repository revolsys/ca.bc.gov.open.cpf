PROMPT Creating Table 'CPF_BATCH_JOB_EXECUTION_GROUPS'
CREATE TABLE CPF_BATCH_JOB_EXECUTION_GROUPS (
  BATCH_JOB_ID NUMBER(19,0) NOT NULL,
  SEQUENCE_NUMBER NUMBER(19,0) NOT NULL,
  COMPLETED_IND NUMBER(1,0) NOT NULL,
  STARTED_IND NUMBER(1,0) NOT NULL,
  NUM_SUBMITTED_REQUESTS NUMBER(19,0) NOT NULL,
  COMPLETED_REQUEST_RANGE NUMBER(19,0) NOT NULL,
  FAILED_REQUEST_RANGE NUMBER(19,0) NOT NULL,
  INPUT_DATA BLOB,
  INPUT_DATA_URL VARCHAR2(2000),
  INPUT_DATA_CONTENT_TYPE VARCHAR2(255),
  RESULT_DATA BLOB,
  RESULT_DATA_URL VARCHAR2(2000),
  STRUCTURED_INPUT_DATA CLOB,
  STRUCTURED_RESULT_DATA CLOB,
  WHO_CREATED VARCHAR2(36) NOT NULL,
  WHEN_CREATED TIMESTAMP NOT NULL,
  WHO_UPDATED VARCHAR2(36) NOT NULL,
  WHEN_UPDATED TIMESTAMP NOT NULL
  )
  TABLESPACE CPF
/

COMMENT ON TABLE CPF_BATCH_JOB_EXECUTION_GROUPS IS 'The BATCH JOB REQUEST represents a single request to execute a BusinessApplication within a BATCH JOB. It contains the input data, result data and any permanent errors.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.BATCH_JOB_ID IS 'This is the unique key for the BATCH JOB.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.SEQUENCE_NUMBER IS 'This is the sequence number of the BATCH JOB REQUEST within a batch job. The sequence numbers start at one for the first input data record and increase by one for each subsequent record.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.COMPLETED_IND IS 'This is the true (1), false (0) indicator that the BATCH JOB REQUEST was completed successfully or failed to complete.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.STARTED_IND IS 'This is the true (1), false (0) indicator that the BATCH JOB REQUEST was started to be processed.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.INPUT_DATA IS 'This is the binary opaque input data for a business application containing the input data for the BATCH JOB REQUEST. For business applications which accept opaque input data either this or the INPUT DATA URL must be specified.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.INPUT_DATA_URL IS 'This is the client application URL to binary opaque input data for a business application containing the inputdata for the BATCH JOB REQUEST. For business applications which accept opaque input data either this or the INPUT DATA must be specified.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.INPUT_DATA_CONTENT_TYPE IS 'This is the MIME content type forthe  INPUT DATA or INPUT DATA URL for a BATCH JOB REQUEST. For business applications accept opaque input data either this must be specified. For example text/csv or application/json.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.RESULT_DATA IS 'This is the binary result data returned from a business application containing the result data for the BATCH JOB REQUEST. For business applications which return opaque result data either this or the RESULT DATA URL must be specified for successful BATCH JOB REQUESTS.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.RESULT_DATA_URL IS 'This is the URL returned from a business application containing the result data for the BATCH JOB REQUEST. For business applications which return opaque result data either this or the RESULT DATA must be specified for successful BATCH JOB REQUESTS.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.STRUCTURED_INPUT_DATA IS 'This is the structured input data for the BATCH JOB used to create the BATCH JOB REQUESTS. Either this or the STRUCTURED INPUT DATA URL can be specified for a business application which supports structured input data.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.STRUCTURED_RESULT_DATA IS 'This is the JSON encoded structured result data returned from the business application for the BATCH JOB REQUEST. For business applications which return structuredresult data this must be specified for successful BATCH JOB REQUESTS.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.WHO_CREATED IS 'This is the database or web user that created the object.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.WHEN_CREATED IS 'This is the date that the object was created.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.WHO_UPDATED IS 'This is the database or web user that last updated the object.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_EXECUTION_GROUPS.WHEN_UPDATED IS 'This is the date that the object was last updated.'
/
