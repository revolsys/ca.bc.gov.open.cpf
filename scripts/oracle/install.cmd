@echo off
set DIR=%~dp0%
set PROJECT=cpf
cd %DIR%

IF NOT EXIST db.properties (
  echo ERROR: Config file db.properties does not exist, copy and edit sample-db.properties
  pause
  exit /b
)
COPY %PROJECT%-dba-all.sql %TEMP%\%PROJECT%-dba-all.sql > NUL
for /F "tokens=1,2 delims==" %%i in (db.properties) do (
  set %%i=%%j
  cscript //nologo replace.vbs %%i %%j %PROJECT%-dba-all.sql
)

if "" == "%DB_NAME%" (
  set DB_NAME=%PROJECT%
)

REM --- DBA Scripts -----
sqlplus SYSTEM@%DB_NAME% @/tmp/%PROJECT%-dba-all.sql 2>&1 | tee %PROJECT%-dba.log
if %ERRORLEVEL% NEQ 0 (
  echo ERROR: sqlplus executed with error code %ERRORLEVEL%
  pause
  exit /b
)
CALL :getcommandoutput FINDSTR "ORA-" %PROJECT%-dba.log
IF NOT "" == "%CommandOutput%" (
  echo ERROR: Error running %PROJECT%-dba-all.sql, see above or log file %PROJECT%-dba.log
  pause
  exit /b
)

CALL :getcommandoutput sqlplus -SL %PROJECT%/%OWNER_PASSWORD%@srv @%PROJECT%-ddl-check-tables-exist.sql
IF "  0" == "%CommandOutput%" (
  GOTO create
) 

set /p DROP_DB=WARN: Do you want to drop the existing database including all data (YES/NO)?
IF NOT "%DROP_DB%" == "YES" GOTO canceldropdb

echo INFO: Dropping existing tables and sequences
sqlplus -SL %PROJECT%/%OWNER_PASSWORD%@%DB_NAME% @%PROJECT%-ddl-drop.sql 2>&1 > %PROJECT%-ddl-drop.log
CALL :getcommandoutput FINDSTR "ORA-" %PROJECT%-ddl-drop.log
IF "" == "%CommandOutput%" (
  echo INFO: Tables and sequences dropped
) ELSE (
  echo ERROR: Unable to delete tables check %PROJECT%-ddl-drop.log
  pause
  exit /b
)

:create
  echo INFO: Creating tables and sequences
  sqlplus -SL %PROJECT%/%OWNER_PASSWORD%@%DB_NAME% @%PROJECT%-ddl-all.sql 2>&1 > %PROJECT%-ddl.log
  CALL :getcommandoutput FINDSTR "ORA-" %PROJECT%-ddl.log
  IF "" == "%CommandOutput%" (
    echo INFO: Tables and sequences created
  ) ELSE (
    echo ERROR: Unable to create tables check %PROJECT%-ddl.log
  )
  
pause
exit /b

rem SubRoutine GetCommandOutput
:getcommandoutput
  set CommandOutput=
  for /f "tokens=*" %%a in (
    '%*'
  ) do (
    set CommandOutput=%%a
  )
exit /b

:canceldropdb
  echo ERROR: Database deletion cancelled by user input
  pause
  exit /b

  