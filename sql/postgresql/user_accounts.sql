CREATE TABLE cpf_user_accounts (
  user_account_id bigint NOT NULL,
  user_account_class character varying(255) NOT NULL,
  user_name character varying(255) NOT NULL,
  consumer_key character varying(36) NOT NULL,
  consumer_secret character varying(36) NOT NULL,
  active_ind numeric(1,0) NOT NULL,
  WHO_CREATED character varying(255) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(255) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL,
  
  CONSTRAINT user_accounts_pk PRIMARY KEY (user_account_id)
);

CREATE SEQUENCE cpf_ua_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
