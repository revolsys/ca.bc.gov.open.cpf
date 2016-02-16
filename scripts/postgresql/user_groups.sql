CREATE TABLE cpf_user_groups (
  user_group_id bigint NOT NULL,
  module_name character varying(255) NOT NULL,
  user_group_name character varying(255) NOT NULL,
  description character varying(4000),
  active_ind numeric(1,0) NOT NULL,
  WHO_CREATED character varying(255) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(255) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL,
  
  CONSTRAINT user_groups_pk PRIMARY KEY (user_group_id),
  CONSTRAINT user_groups_user_group_name_uk UNIQUE (user_group_name)
);

CREATE SEQUENCE cpf_ug_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
