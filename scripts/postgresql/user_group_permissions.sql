CREATE TABLE cpf_user_group_permissions (
  user_group_permission_id bigint NOT NULL,
  module_name character varying(255) NOT NULL,
  resource_class character varying(255) NOT NULL,
  resource_id character varying(255) NOT NULL,
  action_name character varying(255) NOT NULL,
  active_ind numeric(1,0) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  WHO_CREATED character varying(30) NOT NULL,
  WHEN_UPDATED timestamp without time zone NOT NULL,
  WHO_UPDATED character varying(30) NOT NULL,
  user_group_id bigint NOT NULL,
  
  CONSTRAINT user_group_permissions_pk PRIMARY KEY (user_group_permission_id),
  CONSTRAINT user_group_permissions_fk FOREIGN KEY (user_group_id) REFERENCES CPF.cpf_user_groups(user_group_id) ON DELETE CASCADE
);

CREATE SEQUENCE cpf_ugp_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
