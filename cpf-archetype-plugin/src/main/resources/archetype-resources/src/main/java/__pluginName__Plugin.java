package $package;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

@BusinessApplicationPlugin(name = "${pluginName}", version = "${version}")
public class ${pluginName}Plugin {
  private String resultAttribute;
  
  private String parameter;
  
  @ResultAttribute
  public String getResultAttribute() {
    // TODO Replace this method with on method for each response field
    return resultAttribute;
  }

  @JobParameter
  @RequestParameter
  public void setParameter(String parameter) {
    // TODO Replace this method with one method for each parameter
    this.parameter = parameter;
  }

  public void execute() {
    // TODO Replace this with code to perform the function of your plugin
    this.resultAttribute = parameter;
  }
}