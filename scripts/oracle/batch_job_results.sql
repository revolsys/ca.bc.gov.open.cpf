PROMPT Creating Table 'CPF_BATCH_JOB_RESULTS'
CREATE TABLE CPF_BATCH_JOB_RESULTS(
  BATCH_JOB_RESULT_ID NUMBER(19,0) NOT NULL,
  BATCH_JOB_ID NUMBER(19,0) NOT NULL,
  BATCH_JOB_RESULT_TYPE VARCHAR2(50) NOT NULL,
  RESULT_DATA_CONTENT_TYPE VARCHAR2(255) NOT NULL,
  DOWNLOAD_TIMESTAMP TIMESTAMP,
  SEQUENCE_NUMBER NUMBER(19,0),
  RESULT_DATA BLOB,
  RESULT_DATA_URL VARCHAR2(2000),
  WHO_CREATED VARCHAR2(36) NOT NULL,
  WHEN_CREATED TIMESTAMP NOT NULL,
  WHO_UPDATED VARCHAR2(36) NOT NULL,
  WHEN_UPDATED TIMESTAMP NOT NULL
  )
  TABLESPACE CPF
/

COMMENT ON TABLE CPF_BATCH_JOB_RESULTS IS 'The BATCH JOB RESULT is a result file generated after the execution of a BATCH JOB. For structured output data one file will be generated containing all the results. For opaque output data one file will be created for each BATCH JOB REQUEST. A BATCH JOB may also have one file containing any errors generated.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.BATCH_JOB_RESULT_ID IS 'This is the unique key for the BATCH JOB RESULT.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.BATCH_JOB_ID IS 'This is the unique key for the BATCH JOB.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.BATCH_JOB_RESULT_TYPE IS 'This is the purpose of the result data stored in the BATCH JOB RESULT. It can have the values structuredResultData, opaqueResultData, or errorResultData.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.RESULT_DATA_CONTENT_TYPE IS 'This is the MIME content type of the RESULT DATA or RESULT DATA URL for the BATCH JOB RESULT. For example text/csv or application/json.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.DOWNLOAD_TIMESTAMP IS 'This is the timestamp when the last byte was sent to the client for RESULT DATA or when the user was redirected to RESULT DATA URL.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.SEQUENCE_NUMBER IS 'This is the REQUEST SEQUENCE NUMBER of the BATCH JOB REQUEST if the BATCH JOB RESULT is of type opaqueResultData.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.RESULT_DATA IS 'This is the byte content of the result file for the BATCH JOB RESULT. Either this or the RESULT DATA URL must be specfied.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.RESULT_DATA_URL IS 'This is the URL to the byte content of the result file for the BATCH JOB RESULT. Either this or the RESULT DATA must be specfied.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.WHO_CREATED IS 'This is the database or web user that created the object.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.WHEN_CREATED IS 'This is the date that the object was created.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.WHO_UPDATED IS 'This is the database or web user that last updated the object.'
/

COMMENT ON COLUMN CPF_BATCH_JOB_RESULTS.WHEN_UPDATED IS 'This is the date that the object was last updated.'
/
