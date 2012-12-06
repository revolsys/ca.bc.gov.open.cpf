#!/bin/bash
INSTANCE=$1

sqlplus system@$INSTANCE @${pluginAcronym}-dba-all.sql
