package ca.bc.gov.open.cpf.api.domain;

import com.revolsys.io.PathName;

public interface BatchJobStatusChange extends Common {
  PathName BATCH_JOB_STATUS_CHANGE = PathName.newPathName("/CPF/CPF_BATCH_JOB_STATUS_CHANGE");

  String BATCH_JOB_STATUS_CHANGE_ID = "BATCH_JOB_STATUS_CHANGE_ID";

  String BATCH_JOB_ID = "BATCH_JOB_ID";

  String JOB_STATUS = "JOB_STATUS";
}
