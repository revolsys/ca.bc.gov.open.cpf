CREATE TABLE CPF_USER_GROUPS (
  USER_GROUP_ID                   NUMBER(19)          NOT NULL,
  MODULE_NAME                     VARCHAR2(255)    NOT NULL,
  USER_GROUP_NAME                 VARCHAR2(255)    NOT NULL,
  DESCRIPTION                     VARCHAR2(4000),
  ACTIVE_IND                      NUMBER(1,0)    NOT NULL,
  WHO_CREATED                     VARCHAR2(255)    NOT NULL,
  WHEN_CREATED                    TIMESTAMP       NOT NULL,
  WHO_UPDATED                     VARCHAR2(255)    NOT NULL,
  WHEN_UPDATED                    TIMESTAMP       NOT NULL,
  
  CONSTRAINT USER_GROUPS_PK PRIMARY KEY (USER_GROUP_ID),
  
  CONSTRAINT USER_GROUPS_USER_GROUP_NAME_UK UNIQUE (USER_GROUP_NAME)
);

-- Sequence

CREATE SEQUENCE CPF_UG_SEQ;

-- Grants

GRANT DELETE, INSERT, SELECT, UPDATE ON CPF_USER_GROUPS TO CPF_USER;

GRANT SELECT ON CPF_UG_SEQ TO CPF_USER;

-- Comments

COMMENT ON TABLE CPF_USER_GROUPS IS 'USER GROUP represents a named group of CPF or external users that can be granted permissions via USER GROUP PERMISSION. The members of the group are defined in the USER GROUP ACCOUNT XFREF.';

COMMENT ON COLUMN CPF_USER_GROUPS.USER_GROUP_ID IS 'USER GROUP ID is a unique surrogate identifier for the object USER GROUP.';

COMMENT ON COLUMN CPF_USER_GROUPS.MODULE_NAME IS 'MODULE NAME contains the name of the CPF application module the USER GROUP was created for.';

COMMENT ON COLUMN CPF_USER_GROUPS.USER_GROUP_NAME IS 'This is the name of the SECURITY GROUP.';

COMMENT ON COLUMN CPF_USER_GROUPS.DESCRIPTION IS 'This is a description of the security group.';

COMMENT ON COLUMN CPF_USER_GROUPS.ACTIVE_IND IS 'This is the true (1), false (0) indicator if the object is active or has been deleted.';

COMMENT ON COLUMN CPF_USER_GROUPS.WHO_CREATED IS 'This is the database or web user that created the object.';

COMMENT ON COLUMN CPF_USER_GROUPS.WHEN_CREATED IS 'This is the date that the object was created.';

COMMENT ON COLUMN CPF_USER_GROUPS.WHO_UPDATED IS 'This is the database or web user that last updated the object.';

COMMENT ON COLUMN CPF_USER_GROUPS.WHEN_UPDATED IS 'This is the date that the object was last updated.';
