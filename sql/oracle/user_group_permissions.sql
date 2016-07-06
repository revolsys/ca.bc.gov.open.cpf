CREATE TABLE CPF_USER_GROUP_PERMISSIONS (
  USER_GROUP_PERMISSION_ID        NUMBER(19)       NOT NULL,
  USER_GROUP_ID                   NUMBER(19)       NOT NULL,
  MODULE_NAME                     VARCHAR2(255)    NOT NULL,
  RESOURCE_CLASS                  VARCHAR2(255)    NOT NULL,
  RESOURCE_ID                     VARCHAR2(255)    NOT NULL,
  ACTION_NAME                     VARCHAR2(255)    NOT NULL,
  ACTIVE_IND                      NUMBER(1,0)      NOT NULL,
  WHO_CREATED                     VARCHAR2(255)    NOT NULL,
  WHEN_CREATED                    TIMESTAMP        NOT NULL,
  WHO_UPDATED                     VARCHAR2(255)    NOT NULL,
  WHEN_UPDATED                    TIMESTAMP        NOT NULL,
  CONSTRAINT USER_GROUP_PERMISSIONS_PK PRIMARY KEY (USER_GROUP_PERMISSION_ID),
  CONSTRAINT USER_GROUP_PERMISSIONS_FK FOREIGN KEY (USER_GROUP_ID) REFERENCES CPF.CPF_USER_GROUPS(USER_GROUP_ID) ON DELETE CASCADE
);

CREATE INDEX CPF_UGP_UG_FK_I ON CPF.CPF_USER_GROUP_PERMISSIONS (USER_GROUP_ID);

-- Sequence

CREATE SEQUENCE CPF_UGP_SEQ;

-- Grants

GRANT DELETE, INSERT, SELECT, UPDATE ON CPF_USER_GROUP_PERMISSIONS TO CPF_USER;

GRANT SELECT ON CPF_UGP_SEQ TO CPF_USER;

-- Comments

COMMENT ON TABLE CPF_USER_GROUP_PERMISSIONS IS 'USER GROUP P ERMISSION represents a permission for a member of a USER GROUP to perfom an action on a given resource for a module.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.USER_GROUP_PERMISSION_ID IS 'USER GROUP PERMISSION ID is a unique surrogate identifier for the object USER GROUP PERMISSION.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.MODULE_NAME IS 'MODULE NAME contains the name of the CPF application module the USER GROUP PERMISSION was created for.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.USER_GROUP_ID IS 'USER GROUP ID is a unique surrogate identifier for the object USER GROUP.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.RESOURCE_CLASS IS 'This is the classification of the RESOURCE NAME. For example URL (web page), JavaClass, JavaObject.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.RESOURCE_ID IS 'This is the unqiue id of the resource the permission applies to. For example a url, java class name, java class name and object identifier.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.ACTION_NAME IS 'This is the name of the action the user is granted permission to perform (e.g. any, edit, view).';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.ACTIVE_IND IS 'This is the true (1), false (0) indicator if the object is active or has been deleted.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHO_CREATED IS 'This is the database or web user that created the object.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHEN_CREATED IS 'This is the date that the object was created.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHO_UPDATED IS 'This is the database or web user that last updated the object.';

COMMENT ON COLUMN CPF_USER_GROUP_PERMISSIONS.WHEN_UPDATED IS 'This is the date that the object was last updated.';
