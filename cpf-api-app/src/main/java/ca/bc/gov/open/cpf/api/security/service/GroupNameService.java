package ca.bc.gov.open.cpf.api.security.service;

import java.util.List;

import com.revolsys.data.record.Record;

public interface GroupNameService {

  List<String> getGroupNames(Record userAccount);
}
