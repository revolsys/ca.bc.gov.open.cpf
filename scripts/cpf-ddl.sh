#!/bin/bash
INSTANCE=$1
PASSWORD=$2
if [ ! -z $PASSWORD ]; then
  PASSWORD=/$PASSWORD
fi
sqlplus cpf$PASSWORD@$INSTANCE @cpf-ddl-all.sql