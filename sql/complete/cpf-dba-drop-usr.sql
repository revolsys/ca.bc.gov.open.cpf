BEGIN
  FOR user_record IN (
    SELECT
      username
    FROM
      dba_users
    WHERE username IN (
      'CPF',
      'PROXY_CPF_WEB'
    )) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP USER ' || user_record.username || ' CASCADE';
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping ' || user_record.username || ' ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
    END;
  END LOOP;
  
  FOR role_record IN (
    SELECT
      role
    FROM
      dba_roles
    WHERE role IN (
      'CPF_USER',
      'CPF_VIEWER'
    )) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP ROLE ' || role_record.role;
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error dropping ' || role_record.role || ' ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
    END;
  END LOOP;
END;
/