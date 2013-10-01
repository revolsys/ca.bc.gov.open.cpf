DO language plpgsql $$
DECLARE
  C INTEGER;
BEGIN
  SELECT COUNT(*) INTO C FROM pg_roles WHERE rolname = 'cpf_user';
  IF C = 0 THEN
    EXECUTE 'CREATE ROLE cpf_user NOLOGIN';
    RAISE INFO 'Role cpf_user created';
  ELSE
    RAISE INFO 'Role cpf_user already exists';
  END IF;
  
  SELECT COUNT(*) INTO C FROM pg_roles WHERE rolname = 'cpf_web_proxy';
  IF C = 0 THEN
    EXECUTE 'CREATE ROLE cpf_web_proxy NOLOGIN';
    RAISE INFO 'Role cpf_web_proxy created';
  ELSE
    RAISE INFO 'Role cpf_web_proxy already exists';
  END IF;
  
  SELECT COUNT(*) INTO C FROM pg_roles WHERE rolname = 'cpf_viewer';
  IF C = 0 THEN
    EXECUTE 'CREATE ROLE cpf_viewer NOLOGIN';
    RAISE INFO 'Role cpf_viewer created';
  ELSE
    RAISE INFO 'Role cpf_viewer already exists';
  END IF;
  
  SELECT COUNT(*) INTO C FROM pg_roles WHERE rolname = 'cpf';
  IF C = 0 THEN
    EXECUTE 'CREATE USER cpf PASSWORD ''cpf_2009'' CREATEDB';
    RAISE INFO 'User CPF created';
  ELSE
    RAISE INFO 'User CPF already exists';
  END IF;
  
  SELECT COUNT(*) INTO C FROM pg_roles WHERE rolname = 'proxy_cpf_web';
  IF C = 0 THEN
    EXECUTE 'CREATE USER proxy_cpf_web PASSWORD ''cpf_2009'' IN ROLE CPF_WEB_PROXY';
    RAISE INFO 'User PROXY_CPF_WEB created';
  ELSE
    RAISE INFO 'User PROXY_CPF_WEB already exists';
  END IF;
END
$$;
GRANT CPF_USER TO PROXY_CPF_WEB;
GRANT CPF_USER TO cpf_web_proxy;
