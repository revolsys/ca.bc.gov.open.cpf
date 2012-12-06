package ca.bc.gov.open.cpf.api.security;

public interface CpfMethodSecurityExpressions {
  String ADMIN = "hasRole('ROLE_ADMIN')";

  String ADMIN_SECURITY = "hasRole('ROLE_ADMIN_SECURITY')";

  String ADMIN_MODULE_ADMIN_FOR_MODULE = "hasRole('ROLE_ADMIN_MODULE_' + #moduleName + '_ADMIN')";

  String ADMIN_MODULE_ANY_FOR_MODULE = "hasRoleRegex('ROLE_ADMIN_MODULE_' + #moduleName + '.*')";

  String ADMIN_MODULE_ANY = "hasRoleRegex('ROLE_ADMIN_MODULE_.*')";

  String ADMIN_MODULE_ANY_ADMIN = "hasRoleRegex('ROLE_ADMIN_MODULE_.*_ADMIN')";

  String ADMIN_MODULE_SECURITY_FOR_MODULE = "hasRole('ROLE_ADMIN_MODULE_' + #moduleName + '_SECURITY')";

  String ADMIN_OR_ADMIN_FOR_MODULE = ADMIN + " or "
    + ADMIN_MODULE_ADMIN_FOR_MODULE;

  String ADMIN_OR_MODULE_ANY_ADMIN_EXCEPT_SECURITY = ADMIN + " or "
    + ADMIN_MODULE_ANY_ADMIN;

  String ADMIN_OR_ADMIN_SECURITY_OR_ANY_MODULE_ADMIN = ADMIN + " or "
    + ADMIN_SECURITY + " or " + ADMIN_MODULE_ANY;

  String ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS = ADMIN + " or "
    + ADMIN_SECURITY + " or " + ADMIN_MODULE_ANY_FOR_MODULE;

  String FILTER_ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS = ADMIN + " or "
    + ADMIN_SECURITY + " or "
    + "hasRoleRegex('ROLE_ADMIN_MODULE_' + filterObject.name + '.*')";

  String ADMIN_OR_ADMIN_SECURITY = ADMIN + " or " + ADMIN_SECURITY;

  String ADMIN_OR_ANY_ADMIN_FOR_MODULE = ADMIN + " or "
    + ADMIN_MODULE_ANY_FOR_MODULE;

  String ADMIN_OR_MODULE_ADMIN = ADMIN + " or " + ADMIN_MODULE_ANY;
}
