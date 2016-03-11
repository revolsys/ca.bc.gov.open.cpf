CREATE TABLE CPF_APPLICATION_STATISTICS (
  APPLICATION_STATISTIC_ID bigint NOT NULL,
  BUSINESS_APPLICATION_NAME character varying(255) NOT NULL,
  START_TIMESTAMP timestamp without time zone NOT NULL,
  DURATION_TYPE character varying(10) NOT NULL,
  STATISTIC_VALUES character varying(4000) NOT NULL,
  
  CONSTRAINT APPLICATION_STATISTICS_PK PRIMARY KEY (APPLICATION_STATISTIC_ID)
);


COMMENT ON TABLE CPF_APPLICATION_STATISTICS IS 'The APPLICATION STATISTIC object represents a collection of statistic values for a business application during a time peroid hour, day, month, year). The statistics are stored as a JSON map of keys/values to allow additional statistics to be added in the future. The statistic values are processed by the application and are not intended to be queried using SQL.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.APPLICATION_STATISTIC_ID IS 'This is the unique key for the APPLICATION STATISTIC.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.BUSINESS_APPLICATION_NAME IS 'This is the name of the business application the statistic was created for.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.START_TIMESTAMP IS 'This is the timestamp of the start of the duration the statistic was created for.'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.DURATION_TYPE IS 'This is the type of duration (hour, day, month, year).'
;

COMMENT ON COLUMN CPF_APPLICATION_STATISTICS.STATISTIC_VALUES IS 'This is the JSON encoded object of statistic name/values (e.g. {''jobsSumbitted'': 1, ''jobsCompleted'':11}.'
;

CREATE SEQUENCE cpf_as_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
