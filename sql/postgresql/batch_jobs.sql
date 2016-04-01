CREATE TABLE CPF_BATCH_JOBS (
  BATCH_JOB_ID                    BIGINT          NOT NULL,
  USER_ID                         VARCHAR(50)     NOT NULL,
  BUSINESS_APPLICATION_NAME       VARCHAR(255)    NOT NULL,
  BUSINESS_APPLICATION_PARAMS     TEXT,
  PROPERTIES                      TEXT,
  WHEN_STATUS_CHANGED             TIMESTAMP       NOT NULL,
  COMPLETED_TIMESTAMP             TIMESTAMP,
  INPUT_DATA_CONTENT_TYPE         VARCHAR(255),
  STRUCTURED_INPUT_DATA_URL       VARCHAR(2000),
  JOB_STATUS                      VARCHAR(50)     NOT NULL,
  LAST_SCHEDULED_TIMESTAMP        TIMESTAMP,
  NOTIFICATION_URL                VARCHAR(2000),
  NUM_SUBMITTED_REQUESTS          INTEGER         NOT NULL,
  NUM_SUBMITTED_GROUPS            INTEGER         NOT NULL,
  GROUP_SIZE                      SMALLINT        NOT NULL,
  COMPLETED_REQUEST_RANGE         TEXT,
  COMPLETED_GROUP_RANGE           TEXT,
  FAILED_REQUEST_RANGE            TEXT,
  RESULT_DATA_CONTENT_TYPE        VARCHAR(255)    NOT NULL,
  WHO_CREATED                     VARCHAR(255)    NOT NULL,
  WHEN_CREATED                    TIMESTAMP       NOT NULL,
  WHO_UPDATED                     VARCHAR(255)    NOT NULL,
  WHEN_UPDATED                    TIMESTAMP       NOT NULL,
  CONSTRAINT BATCH_JOBS_PK PRIMARY KEY (BATCH_JOB_ID)
);

CREATE INDEX BATCH_JOBS_STATUS_APP_IDX ON CPF.CPF_BATCH_JOBS (JOB_STATUS, BUSINESS_APPLICATION_NAME);

-- Sequence

CREATE SEQUENCE CPF_BJ_SEQ;

-- Grants

GRANT DELETE, INSERT, SELECT, UPDATE ON CPF_BATCH_JOBS TO CPF_USER;

GRANT SELECT ON CPF_BATCH_JOBS TO CPF_VIEWER;

GRANT USAGE ON CPF_BJ_SEQ TO CPF_USER;

-- Comments

COMMENT ON TABLE CPF_BATCH_JOBS IS 'The BATCH JOB object represents a batch of requests to a BusinessApplication.';

COMMENT ON COLUMN CPF_BATCH_JOBS.BATCH_JOB_ID IS 'This is the unique key for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.USER_ID IS 'This is the login user identifier of the user who submitted the job.';

COMMENT ON COLUMN CPF_BATCH_JOBS.BUSINESS_APPLICATION_NAME IS 'This is the name of the business application to be invoked in this job.';

COMMENT ON COLUMN CPF_BATCH_JOBS.BUSINESS_APPLICATION_PARAMS IS 'This is the CSV encoded global parameters to be passed to the business application for the BATCH JOB. The CSV encoding will have a header row with the names of the parameters and a single data row containing the parameter values.';

COMMENT ON COLUMN CPF_BATCH_JOBS.PROPERTIES IS 'PROPERTIES contains a JSON encoded map of additional properties associated with a BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.WHEN_STATUS_CHANGED IS 'This is the timestamp when the status of the job was last updated.';

COMMENT ON COLUMN CPF_BATCH_JOBS.COMPLETED_TIMESTAMP IS 'This is the timestamp when all of the BATCH JOB REQUESTS and BATCH JOB RESULTS have been processed for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.INPUT_DATA_CONTENT_TYPE IS 'This is the mime content-type for the structured input data. For example text/csv or application/json.';

COMMENT ON COLUMN CPF_BATCH_JOBS.STRUCTURED_INPUT_DATA_URL IS 'This is the URL to download the structured input data for the BATCH JOB used to create the BATCH JOB REQUESTS. Either this or the STRUCTURED INPUT DATA can be specified for a business application which supports structured input data.';

COMMENT ON COLUMN CPF_BATCH_JOBS.JOB_STATUS IS 'This is the current status of the job (submitted, processing, processed, resultGenerated, downloadInitiated, deleteInitiated, deleted).';

COMMENT ON COLUMN CPF_BATCH_JOBS.LAST_SCHEDULED_TIMESTAMP IS 'This is the timestamp when the most recent BATCH JOB REQUEST was scheduled to be processed for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.NOTIFICATION_URL IS 'This is the http, https, or mailto URL to be notified when the BATCH JOB has been completed and is ready for download.';

COMMENT ON COLUMN CPF_BATCH_JOBS.NUM_SUBMITTED_REQUESTS IS 'This is the number of requests which were submitted for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.NUM_SUBMITTED_GROUPS IS 'This is the number of groups which were submitted for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.GROUP_SIZE IS 'This is the number of requests per group.';

COMMENT ON COLUMN CPF_BATCH_JOBS.COMPLETED_REQUEST_RANGE IS 'This is the range of requests which have been completed successfully for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.COMPLETED_GROUP_RANGE IS 'This is the range of groups which have been completed successfully for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.FAILED_REQUEST_RANGE IS 'This is the range of requests which failed to be completed for the BATCH JOB.';

COMMENT ON COLUMN CPF_BATCH_JOBS.RESULT_DATA_CONTENT_TYPE IS 'This is the MIME content type the results of the BATCH JOB are to be returned in (e.g. text/csv, application/json, text/xml). Each business application has its own list of supported mime types.';

COMMENT ON COLUMN CPF_BATCH_JOBS.WHO_CREATED IS 'This is the database or web user that created the object.';

COMMENT ON COLUMN CPF_BATCH_JOBS.WHEN_CREATED IS 'This is the date that the object was created.';

COMMENT ON COLUMN CPF_BATCH_JOBS.WHO_UPDATED IS 'This is the database or web user that last updated the object.';

COMMENT ON COLUMN CPF_BATCH_JOBS.WHEN_UPDATED IS 'This is the date that the object was last updated.';
