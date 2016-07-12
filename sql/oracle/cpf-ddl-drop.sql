SPOOL cpf-ddl-drop.log
BEGIN
  FOR tab IN (SELECT table_name FROM all_tables WHERE owner = 'CPF') LOOP
    EXECUTE IMMEDIATE 'DROP TABLE CPF.' || tab.table_name || ' CASCADE CONSTRAINTS PURGE';
  END LOOP;

  FOR seq IN (SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'CPF') LOOP
    EXECUTE IMMEDIATE 'DROP SEQUENCE CPF.' || seq.sequence_name;
  END LOOP;
END;
/
exit