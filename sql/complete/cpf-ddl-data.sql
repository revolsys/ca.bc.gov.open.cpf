
GRANT CPF_USER TO PROXY_CPF_WEB 
/

INSERT INTO cpf.cpf_user_accounts (user_account_id, user_account_class, user_name, consumer_key, consumer_secret, active_ind, disabled_ind, disabled_comment, who_created, when_created, who_updated, when_updated) VALUES
  (cpf.cpf_ua_seq.nextval, 'http://open.gov.bc.ca/cpf/SystemUser', 'http://open.gov.bc.ca/cpf/SystemUser/cpf', 'cpf', 'cpf2009', 1, 0, NULL, user, sysdate, user, sysdate);
INSERT INTO cpf.cpf_user_accounts (user_account_id, user_account_class, user_name, consumer_key, consumer_secret, active_ind, disabled_ind, disabled_comment, who_created, when_created, who_updated, when_updated) VALUES 
  (cpf.cpf_ua_seq.nextval, 'http://gov.bc.ca/cpf', 'cpfadmin', 'cpfadmin', 'cpf2009', 1, 0, NULL, user, sysdate, user, sysdate);
