CREATE TABLE CPF_USER_ACCOUNTS (
  USER_ACCOUNT_ID                 NUMBER(19)       NOT NULL,
  USER_ACCOUNT_CLASS              VARCHAR2(255)    NOT NULL,
  USER_NAME                       VARCHAR2(255)    NOT NULL,
  CONSUMER_KEY                    VARCHAR2(36)     NOT NULL,
  CONSUMER_SECRET                 VARCHAR2(36)     NOT NULL,
  ACTIVE_IND                      NUMBER(1,0)      NOT NULL,
  WHO_CREATED                     VARCHAR2(255)    NOT NULL,
  WHEN_CREATED                    TIMESTAMP        NOT NULL,
  WHO_UPDATED                     VARCHAR2(255)    NOT NULL,
  WHEN_UPDATED                    TIMESTAMP        NOT NULL,
  CONSTRAINT USER_ACCOUNTS_PK PRIMARY KEY (USER_ACCOUNT_ID) USING INDEX TABLESPACE CPF_NDX,
  CONSTRAINT USER_ACCOUNTS_CK_UK UNIQUE (CONSUMER_KEY) USING INDEX TABLESPACE CPF_NDX,
  CONSTRAINT USER_ACCOUNTS_UN_UAC_UK UNIQUE (USER_NAME,USER_ACCOUNT_CLASS) USING INDEX TABLESPACE CPF_NDX
);

-- Sequence

CREATE SEQUENCE CPF_UA_SEQ;

-- Grants

GRANT DELETE, INSERT, SELECT, UPDATE ON CPF_USER_ACCOUNTS TO CPF_USER;

GRANT SELECT ON CPF_UA_SEQ TO CPF_USER;

-- Comments

COMMENT ON TABLE CPF_USER_ACCOUNTS IS 'The USER ACCOUNTS represents a user account created for an internal or external user, for example a OpenID account, BC Government IDIR account, or email address.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.USER_ACCOUNT_ID IS 'This is the unique identifier for the USER ACCOUNT.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.USER_ACCOUNT_CLASS IS 'This is the classification for the USER NAME, for example http://openid.net/ or http://idir.bcgov/.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.USER_NAME IS 'This is the user name, for example an OpenID, IDIR account name, or email address.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.CONSUMER_KEY IS 'This is the system generated unique user identifier for the user account.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.CONSUMER_SECRET IS 'This is the system generated password/encryption key for the user account.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.ACTIVE_IND IS 'This is the true (1), false (0) indicator if the object is active or has been deleted.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHO_CREATED IS 'This is the database or web user that created the object.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHEN_CREATED IS 'This is the date that the object was created.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHO_UPDATED IS 'This is the database or web user that last updated the object.';

COMMENT ON COLUMN CPF_USER_ACCOUNTS.WHEN_UPDATED IS 'This is the date that the object was last updated.';
