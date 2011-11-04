SPOOL ../log/bpf-dba.log
DECLARE
  CURSOR synonym_cursor IS
    SELECT
      SYNONYM_NAME
    FROM
      ALL_SYNONYMS
    WHERE
      SYNONYM_NAME LIKE 'BPF_%'
  ;
  num INTEGER := 0;
BEGIN
	FOR user_record IN (
	  SELECT
	    username
	  FROM
	    dba_users
	  WHERE username IN (
	    'BPF',
	    'PROXY_BPF_WEB'
	  )) LOOP
    EXECUTE IMMEDIATE 'DROP USER ' || user_record.username || ' CASCADE';
  END LOOP;
  
  FOR role_record IN (
    SELECT
      role
    FROM
      dba_roles
    WHERE role IN (
      'BPF',
      'PROXY_BPF_WEB'
    )) LOOP
    EXECUTE IMMEDIATE 'DROP ROLE ' || role_record.role;
  END LOOP;
 
  FOR syn IN synonym_cursor
  LOOP
    EXECUTE IMMEDIATE 'DROP PUBLIC SYNONYM ' || syn.SYNONYM_NAME;
  END LOOP;
END;
/