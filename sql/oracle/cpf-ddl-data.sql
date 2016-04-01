
INSERT INTO cpf.cpf_user_accounts (user_account_id, user_account_class, user_name, consumer_key, consumer_secret, active_ind, who_created, when_created, who_updated, when_updated) VALUES 
  (cpf.cpf_ua_seq.NEXTVAL, 'CPF', 'cpftest', 'cpftest', 'cpftest', 1,USER, SYSDATE, USER, SYSDATE);

-- CPF_ADMIN
INSERT INTO cpf.cpf_user_accounts (user_account_id, user_account_class, user_name, consumer_key, consumer_secret, active_ind, who_created, when_created, who_updated, when_updated) VALUES 
  (cpf.cpf_ua_seq.NEXTVAL, 'CPF', 'cpf_admin', 'cpf_admin', '4dm1n157r470r', 1,USER, SYSDATE, USER, SYSDATE);
INSERT INTO cpf.cpf_user_groups (USER_GROUP_ID, MODULE_NAME, USER_GROUP_NAME, DESCRIPTION, ACTIVE_IND, WHO_CREATED, WHEN_CREATED, WHO_UPDATED, WHEN_UPDATED) VALUES 
  (cpf.cpf_ug_seq.NEXTVAL, 'ADMIN', 'ADMIN', 'Global administrator', '1', USER, SYSDATE, USER, SYSDATE);
INSERT INTO CPF.CPF_USER_GROUP_ACCOUNT_XREF (USER_GROUP_ID, USER_ACCOUNT_ID, WHO_CREATED, WHEN_CREATED)
SELECT USER_GROUP_ID, USER_ACCOUNT_ID, USER, SYSDATE
FROM CPF.CPF_USER_GROUPS G, CPF.CPF_USER_ACCOUNTS A
WHERE G.USER_GROUP_NAME = 'ADMIN' AND A.USER_ACCOUNT_CLASS = 'CPF' AND A.USER_NAME = 'cpf_admin';

-- CPF_WORKER
INSERT INTO cpf.cpf_user_accounts (user_account_id, user_account_class, user_name, consumer_key, consumer_secret, active_ind, who_created, when_created, who_updated, when_updated) VALUES
  (cpf.cpf_ua_seq.NEXTVAL, 'CPF', 'cpf_worker', 'cpf_worker', 'cpf_w0rk3r', 1, USER, SYSDATE, USER, SYSDATE);
INSERT INTO cpf.cpf_user_groups (USER_GROUP_ID, MODULE_NAME, USER_GROUP_NAME, DESCRIPTION, ACTIVE_IND, WHO_CREATED, WHEN_CREATED, WHO_UPDATED, WHEN_UPDATED) VALUES 
  (cpf.cpf_ug_seq.NEXTVAL, 'WORKER', 'WORKER', 'CPF worker process', '1', USER, SYSDATE, USER, SYSDATE);
INSERT INTO cpf.CPF_USER_GROUP_ACCOUNT_XREF (USER_GROUP_ID, USER_ACCOUNT_ID, WHO_CREATED, WHEN_CREATED)
SELECT USER_GROUP_ID, USER_ACCOUNT_ID, USER, SYSDATE
FROM CPF.CPF_USER_GROUPS G, CPF.CPF_USER_ACCOUNTS A
WHERE G.USER_GROUP_NAME = 'WORKER' AND A.USER_ACCOUNT_CLASS = 'CPF' AND A.USER_NAME = 'cpf_worker';
