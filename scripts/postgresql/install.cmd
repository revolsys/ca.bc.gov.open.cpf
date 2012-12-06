@echo off
set DIR=%~dp0%
set PROJECT=cpf
cd %DIR%

IF NOT EXIST db.properties (
  echo "ERROR: Config file db.properties does not exist, copy and edit sample-db.properties
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


psql -h %DB_HOSTNAME% -p %DB_PORT% -U postgres -d postgres -f %TEMP%\%PROJECT%-dba-all.sql > %PROJECT%-dba.log 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo "ERROR: Psql executed with error code "%ERRORLEVEL%"
  pause
  exit /b
)
CALL :getcommandoutput FINDSTR "FATAL" %PROJECT%-dba.log
set RESULT=%CommandOutput%
CALL :getcommandoutput FINDSTR "ERROR" %PROJECT%-dba.log
set RESULT=%RESULT%%CommandOutput%
TYPE "%PROJECT%-dba.log"
IF NOT "" == "%RESULT%" (
  echo "ERROR: Error running %PROJECT%-dba-all.sql, see above or log file %PROJECT%-dba.log"
  pause
  exit /b
)


CALL :getcommandoutput psql -q -h %DB_HOSTNAME% -p %DB_PORT% -U postgres -d postgres --tuples-only --command "SELECT spcname FROM pg_tablespace WHERE spcname = '%PROJECT%';"
if %ERRORLEVEL% NEQ 0 (
  echo "ERROR: Psql executed with error code "%ERRORLEVEL%"
  pause
  exit /b
)
IF "%PROJECT%" == "%CommandOutput%" (
  echo INFO: Tablespace %PROJECT% already exists
) ELSE (
  echo CREATE TABLESPACE %PROJECT% OWNER %PROJECT% LOCATION '%TABLESPACE_DIR%' | psql -h %DB_HOSTNAME% -p %DB_PORT% -U postgres -d postgres
  if %ERRORLEVEL% NEQ 0 (
    echo "ERROR: Psql executed with error code "%ERRORLEVEL%"
    pause
    exit /b
  )
  echo INFO: Created tablespace %PROJECT%
)

CALL :getcommandoutput psql -q -U postgres -d postgres --tuples-only --command "SELECT datname FROM pg_database WHERE datname = '%DB_NAME%';"
IF NOT "%CommandOutput%" == "%DB_NAME%" GOTO createdb

set /p DROP_DB=WARN: Do you want to drop the existing database including all data (YES/NO)?
IF NOT "%DROP_DB%" == "YES" GOTO canceldropdb

dropdb -h %DB_HOSTNAME% -p %DB_PORT% -U postgres %DB_NAME%
if %ERRORLEVEL% NEQ 0 (
  echo ERROR: Cannot delete database
  pause
  exit /b
)

:createdb
createdb -h %DB_HOSTNAME% -p %DB_PORT% -U postgres --template template_postgis --tablespace cpf --owner=cpf %DB_NAME%

IF %ERRORLEVEL% NEQ 0 (
  echo ERROR: CANNOT CREATE THE DATABASE
) ELSE (
  echo INFO: Creating tables
  psql -U cpf -d %DB_NAME% -f cpf-ddl-all.sql > cpf-ddl.log 2>&1
  if %ERRORLEVEL% NEQ 0 (
    echo "ERROR: Psql executed with error code "%ERRORLEVEL%"
    pause
    exit /b
  )
  CALL :getcommandoutput FINDSTR "FATAL" %PROJECT%-ddl.log
  set RESULT=%CommandOutput%
  CALL :getcommandoutput FINDSTR "ERROR" %PROJECT%-ddl.log
  set RESULT=%RESULT%%CommandOutput%
  if "%RESULT%" == "" (
    echo INFO: Tables and sequences created
  ) ELSE (
    TYPE "%PROJECT%-ddl.log"
    echo "ERROR: Error running %PROJECT%-ddl-all.sql, see above or log file %PROJECT%-ddl.log"
  )
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
