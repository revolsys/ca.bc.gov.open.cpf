package ca.bc.gov.open.cpf.api.security.service;

import java.util.List;

import com.revolsys.gis.data.model.DataObject;

public interface GroupNameService {

  List<String> getGroupNames(DataObject userAccount);
}
