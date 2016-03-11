insert into cpf.cpf_config_properties (config_property_id, environment_name, module_name, component_name, property_name, property_value, who_created, when_created, who_updated, when_updated)
values (cpf.cpf_cp_seq.nextval, 'default', 'mapTile', 'moduleConfig', 'mavenModuleId', 'ca.bc.gov.open.cpf:cpf-plugins-maptile:3.0.0-SNAPSHOT', user, sysdate, user, sysdate);
insert into cpf.cpf_config_properties (config_property_id, environment_name, module_name, component_name, property_name, property_value, who_created, when_created, who_updated, when_updated)
values (cpf.cpf_cp_seq.nextval, 'default', 'mapTile', 'moduleConfig', 'enabled', 'true', user, sysdate, user, sysdate);
insert into cpf.cpf_config_properties (config_property_id, environment_name, module_name, component_name, property_name, property_value, who_created, when_created, who_updated, when_updated)
values (cpf.cpf_cp_seq.nextval, 'default', 'sample', 'moduleConfig', 'mavenModuleId', 'ca.bc.gov.open.cpf:cpf-plugins-sample:3.0.0-SNAPSHOT', user, sysdate, user, sysdate);
insert into cpf.cpf_config_properties (config_property_id, environment_name, module_name, component_name, property_name, property_value, who_created, when_created, who_updated, when_updated)
values (cpf.cpf_cp_seq.nextval, 'default', 'sample', 'moduleConfig', 'enabled', 'true', user, sysdate, user, sysdate);
commit