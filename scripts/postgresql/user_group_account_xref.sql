CREATE TABLE cpf_user_group_account_xref (
  user_account_id bigint NOT NULL,
  user_group_id bigint NOT NULL,
  WHO_CREATED character varying(255) NOT NULL,
  WHEN_CREATED timestamp without time zone NOT NULL,
  
  CONSTRAINT user_group_account_xref_pk PRIMARY KEY (user_group_id, user_account_id),
  CONSTRAINT user_group_xref_account_fk FOREIGN KEY (user_account_id) REFERENCES CPF.cpf_user_accounts(user_account_id),
  CONSTRAINT user_account_xref_group_fk FOREIGN KEY (user_group_id) REFERENCES CPF.cpf_user_groups(user_group_id)
);
