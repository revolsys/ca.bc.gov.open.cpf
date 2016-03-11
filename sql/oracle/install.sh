#!/bin/bash
DIR=`dirname $0`
PROJECT=cpf
cd $DIR
SED="sed -i"
if [ `uname` == "Darwin" ]; then
  SED="sed -i ''"
fi
if [ -e ${DIR}/db.properties ]; then
  cp ${PROJECT}-dba-all.sql /tmp/${PROJECT}-dba-all.sql
  for line in `cat db.properties`; do
    var=`echo ${line} | awk -F = '{print $1'}`;
    value=`echo ${line} | awk -F = '{print $2'}`;
    export ${var}=${value}
    export escappedValue=${value//\//\\/}
    $SED "s/${var}/${escappedValue}/g" /tmp/${PROJECT}-dba-all.sql
  done
  if [ -z "${DB_NAME}" ]; then
    export DB_NAME=${PROJECT}
  fi

  sqlplus SYSTEM@${DB_NAME} @/tmp/${PROJECT}-dba-all.sql 2>&1 | tee ${PROJECT}-dba.log

  ERRORS=`grep ORA- ${PROJECT}-dba.log`
  if [ -n "$ERRORS" ]; then
    echo ERROR: CANNOT CREATE THE DATABASE
  else
    RESULT=`sqlplus -SL ${PROJECT}/${CPF_PASSWORD}@${DB_NAME} @${PROJECT}-ddl-check-tables-exist.sql`
    if [ "  0" != "$RESULT" ]; then
      echo "WARN: Do you want to drop the existing ${PROJECT} database including all data (YES/NO)?"
      read DROP_DB
      if [ "$DROP_DB" == "YES" ]; then
        echo INFO: Dropping existing tables and sequences
        sqlplus -SL ${PROJECT}/${CPF_PASSWORD}@${DB_NAME} @${PROJECT}-ddl-drop.sql 2>&1 > ${PROJECT}-ddl-drop.log
        RESULT=`grep ORA- ${PROJECT}-ddl-drop.log`
        if [ -n "$RESULT" ]; then
          echo ERROR: Unable to delete tables check ${PROJECT}-ddl-drop.log
          exit
        else
          echo INFO: Tables and sequences dropped
        fi
      else
        echo ERROR: Table and sequence deletion cancelled by user input
        exit
      fi
    fi
    echo INFO: Creating tables and sequences
    sqlplus -SL ${PROJECT}/${CPF_PASSWORD}@${DB_NAME} @${PROJECT}-ddl-all.sql 2>&1 > ${PROJECT}-ddl.log
    RESULT=`grep ORA- ${PROJECT}-ddl.log`
    if [ -n "$RESULT" ]; then
      echo ERROR: Unable to create tables check ${PROJECT}-ddl.log
    else
      echo INFO: Tables and sequences created
    fi
  fi
else
  echo ERROR: Config file ${DIR}/db.properties does not exist, copy and edit ${DIR}/sample-db.properties
  exit
fi
