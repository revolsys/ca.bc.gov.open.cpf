@echo off
set DIR=%~dp0%
set PROJECT=cpf
cd %DIR%

IF NOT EXIST db.properties (
  echo ERROR: Config file db.properties does not exist, copy and edit sample-db.properties
  pause
  exit /b
)
COPY cpf-dba-all.sql %TEMP%\cpf-dba-all.sql > NUL
for /F "tokens=1,2 delims==" %%i in (db.properties) do (
  set %%i=%%j
  cscript //nologo replace.vbs %%i %%j cpf-dba-all.sql
)

if "" == "%DB_NAME%" (
  set DB_NAME=cpf
)

REM --- DBA Scripts -----
sqlplus SYSTEM@%DB_NAME% @%TEMP%\cpf-dba-all.sql
if %ERRORLEVEL% NEQ 0 (
  echo ERROR: sqlplus executed with error code %ERRORLEVEL%
  pause
  exit /b
)
CALL :getcommandoutput FINDSTR "ORA-" cpf-dba.log
IF NOT "" == "%CommandOutput%" (
  echo ERROR: Error running cpf-dba-all.sql, see above or log file cpf-dba.log
  pause
  exit /b
)

CALL :getcommandoutput sqlplus -S -L cpf/%CPF_PASSWORD%@%DB_NAME% @cpf-ddl-check-tables-exist.sql
IF "  0" == "%CommandOutput%" (
  GOTO create
) 

set /p DROP_DB=WARN: Do you want to drop the existing database including all data (YES/NO)?
IF NOT "%DROP_DB%" == "YES" GOTO canceldropdb

echo INFO: Dropping existing tables and sequences
sqlplus -S -L cpf/%CPF_PASSWORD%@%DB_NAME% @cpf-ddl-drop.sql
CALL :getcommandoutput FINDSTR "ORA-" cpf-ddl-drop.log
IF "" == "%CommandOutput%" (
  echo INFO: Tables and sequences dropped
) ELSE (
  echo ERROR: Unable to delete tables check cpf-ddl-drop.log
  pause
  exit /b
)

:create
  echo INFO: Creating tables and sequences
  sqlplus -S -L  cpf/%CPF_PASSWORD%@%DB_NAME% @cpf-ddl-all.sql
  CALL :getcommandoutput FINDSTR "ORA-" cpf-ddl.log
  IF "" == "%CommandOutput%" (
    echo INFO: Tables and sequences created
  ) ELSE (
    echo ERROR: Unable to create tables check cpf-ddl.log
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
