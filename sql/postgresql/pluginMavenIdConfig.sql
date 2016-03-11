insert into cpf.cpf_config_properties (config_property_id, environment_name, application_name, component_name, property_name, property_value, who_created, when_created, who_updated, when_updated)
values (nextval('cpf.cpf_cp_seq'), 'default', 'mapTile', 'moduleConfig', 'mavenModuleId', 'ca.bc.gov.open.cpf:cpf-plugins-maptile:3.0.0-SNAPSHOT', user, now(), user, now());
insert into cpf.cpf_config_properties (config_property_id, environment_name, application_name, component_name, property_name, property_value, who_created, when_created, who_updated, when_updated)
values (nextval('cpf.cpf_cp_seq'), 'default', 'mapTile', 'moduleConfig', 'enabled', 'true', user, now(), user, now());
