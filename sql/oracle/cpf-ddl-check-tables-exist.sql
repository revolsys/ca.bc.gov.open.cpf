set heading off
set feedback off
set pagesize 0
set termout on
set trimout on
set trimspool on
set recsep off
set linesize 100
set colsep ""
column num justify left
column num format 9
SELECT count(*) "num" FROM USER_TABLES,USER_SEQUENCES;
/
exit
