#!/bin/bash
INSTANCE=$1
USER=$2
sqlplus $USER@$INSTANCE @cpf-dba-all.sql
