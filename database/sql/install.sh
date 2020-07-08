#!/bin/bash
DIR=`dirname $0`
cd ${DIR}
PROJECT=cpf
SED="sed -i"
if [ `uname` == "Darwin" ]; then
  SED="sed -i ''"
fi
if [ -e ${DIR}/db.properties ]; then
  cp cpf-dba-all.sql /tmp/cpf-dba-all.sql
  for line in `cat db.properties`; do
    var=`echo ${line} | awk -F = '{print $1'}`;
    value=`echo ${line} | awk -F = '{print $2'}`;
    export ${var}=${value}
    export escappedValue=${value//\//\\/}
    $SED "s/${var}/${escappedValue}/g" /tmp/cpf-dba-all.sql
  done

  PSQL_CONNECT=""

  if [ -z "${DB_NAME}" ]; then
    export DB_NAME=cpf
  fi

  if [ -n "${DB_HOSTNAME}" ]; then
    export PSQL_CONNECT="${PSQL_CONNECT} -h ${DB_HOSTNAME}"
  fi

  if [ -n "${DB_PORT}" ]; then
    export PSQL_CONNECT="${PSQL_CONNECT} -p ${DB_PORT}"
  fi

  psql ${PSQL_CONNECT} -U postgres -d postgres -f /tmp/cpf-dba-all.sql > cpf-dba.log 2>&1
  if [ "$?" != "0" ]; then
    echo ERROR: Psql executed with error code $?
    rm /tmp/cpf-dba-all.sql
    exit
  fi
  cat /tmp/cpf-dba-all.sql
  RESULT=`grep -e "(FATAL|ERROR)" cpf-dba.log`
  cat ${DIR}/cpf-dba.log
  if [ -n "${RESULT}" ]; then
    echo "ERROR: Error running cpf-dba-all.sql, see above or log file cpf-dba.log"
    exit
  fi

  RESULT=`psql ${PSQL_CONNECT} -q -U postgres -d postgres --tuples-only --command "SELECT spcname FROM pg_tablespace WHERE spcname = 'cpf';"`
  if [ "$?" != "0" ]; then
    echo ERROR: Psql executed with error code $?
    exit
  fi
  if [ " cpf" == "${RESULT}" ]; then
    echo INFO: Tablespace cpf already exists
  else
    echo CREATE TABLESPACE cpf OWNER cpf LOCATION \'${TABLESPACE_DIR}\' | psql ${PSQL_CONNECT} -U postgres -d postgres
    if [ "$?" != "0" ]; then
      echo ERROR: Psql executed with error code $?
      exit
    fi
    echo INFO: Created tablespace cpf
  fi

  RESULT=`psql ${PSQL_CONNECT} -q -U postgres -d postgres --tuples-only --command "SELECT datname FROM pg_database WHERE datname = '${DB_NAME}';"`
  if [ "$?" != "0" ]; then
    echo ERROR: Psql executed with error code $?
    exit
  fi
  if [ " ${DB_NAME}" == "${RESULT}" ]; then
    echo "WARN: Do you want to drop the existing database including all data (YES/NO)?"
    read DROP_DB
    if [ "${DROP_DB}" == "YES" ]; then
      dropdb ${PSQL_CONNECT} -U postgres ${DB_NAME}
      if [ "$?" != "0" ]; then
        echo ERROR: Cannot delete database
        exit
      fi
    else
      echo ERROR: Database deletion cancelled by user input
      exit
    fi
  fi

  createdb ${PSQL_CONNECT} --username postgres --template postgres --tablespace cpf --owner=cpf ${DB_NAME}

  if [ "$?" != "0" ]; then
    echo ERROR: CANNOT CREATE THE DATABASE
  else
    PGPASSWORD=${CPF_PASSWORD}
    echo INFO: Creating tables and sequences
    psql ${PSQL_CONNECT} -U cpf -d ${DB_NAME} -f cpf-ddl-all.sql > cpf-ddl.log 2>&1
    if [ "$?" != "0" ]; then
      echo ERROR: Psql executed with error code $?
      exit
    fi
    RESULT=`grep -E "(FATAL|ERROR)" cpf-ddl.log`
    if [ -n "${RESULT}" ]; then
      cat cpf-ddl.log
      echo "ERROR: Error running cpf-ddl-all.sql, see above or log file cpf-ddl.log"
      exit
    else
      echo INFO: Tables and sequences created
    fi

  fi
else
  echo ERROR: Config file ${DIR}/db.properties does not exist, copy and edit ${DIR}/sample-db.properties
  exit
fi
