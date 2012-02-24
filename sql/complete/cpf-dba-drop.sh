#!/bin/bash
INSTANCE=$1

sqlplus system@$INSTANCE @cpf-dba-drop.sql
