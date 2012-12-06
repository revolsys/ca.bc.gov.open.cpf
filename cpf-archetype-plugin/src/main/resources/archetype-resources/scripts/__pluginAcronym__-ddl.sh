#!/bin/bash
INSTANCE=$1
PASSWORD=$2
if [ ! -z $PASSWORD ]; then
  PASSWORD=/$PASSWORD
fi
sqlplus ${pluginAcronym}$PASSWORD@$INSTANCE @${pluginAcronym}-ddl-all.sql