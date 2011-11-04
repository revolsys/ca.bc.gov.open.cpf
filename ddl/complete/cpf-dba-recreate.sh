#!/bin/bash
INSTANCE=$1

sqlplus system@$INSTANCE @cpf-dba-recreate.sql
sqlplus cpf/cpf_2009@$INSTANCE @cpf-ddl-all.sql
