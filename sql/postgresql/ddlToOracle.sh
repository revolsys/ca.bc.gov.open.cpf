#!/bin/bash
SED="sed -i"
if [ `uname` == "Darwin" ]; then
  SED="sed -i ''"
fi
for file in \
  application_statistics.sql \
  batch_job_status_change.sql \
  batch_job_files.sql \
  batch_job_results.sql \
  batch_jobs.sql \
  config_properties.sql \
  user_accounts.sql \
  user_group_account_xref.sql \
  user_group_permissions.sql \
  user_groups.sql \
  cpf-ddl-data.sql \
; do
  cp $file ../oracle/
  sed -i '' "s/VARCHAR/VARCHAR2/g" ../oracle/$file
  sed -i '' 's/BIGINT/NUMBER\(19\)/g' ../oracle/$file
  sed -i '' 's/INTEGER/NUMBER\(10\)/g' ../oracle/$file
  sed -i '' 's/SMALLINT/NUMBER\(5\)/g' ../oracle/$file
  sed -i '' 's/OID/BLOB/g' ../oracle/$file
  sed -i '' 's/TEXT/CLOB/g' ../oracle/$file
  sed -i '' 's/NUMERIC/NUMBER/g' ../oracle/$file
  sed -i '' 's/current_user/USER/g' ../oracle/$file
  sed -i '' 's/now[(][)]/SYSDATE/g' ../oracle/$file
  sed -i '' 's/nextval[(].//g' ../oracle/$file
  sed -i '' 's/_seq.[)]/_seq.NEXTVAL/g' ../oracle/$file
done
