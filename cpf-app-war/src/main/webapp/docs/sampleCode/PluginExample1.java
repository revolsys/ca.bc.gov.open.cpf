package ca.bc.gov.open.cpf.plugin.PluginExample1;

import ca.bc.gov.open.cpf.plugin.api.AllowedValues;
import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResponseField;

@BusinessApplicationPlugin(name = "PluginExample1", version = "1.0.0")
public class PluginExample1 {

  private int inputFld1;

  private int intputFld2 = 50000;

  private int responseFld1;

  private int responseFld2;
	
  @Required
  @RequestParameter
  public void setInputFld1(final String inputfield1) {
    this.inputfield1 = inputfield1;
  }

  @AllowedValues( {
    "1000000", "500000", "250000", "125000", "50000", "25000", "20000",
    "10000", "5000", "2500", "2000", "1250", "1000", "500"
  })
  @JobParameter
  @RequestParameter
  public void setInputField2(final int intputFld2) {
    this.intputFld2 = intputFld2;
  }

  @ResponseField
  public String getResponseFld1() {
    return responseFld1;
  }

  @ResponseField
  public int getResponseFld2() {
    return responseFld21;
  }

  public void execute() {
    responseFld1 = doSomething(inputFld1, inputFld2);
    if (responseFld1 <= 0) {
      throw new IllegalArgumentException("invalid inputFld1");
    } else {
      responseFld2 = doSomethingElse(inputFld1, inputFld2);
      if (responseFld2 <= 0) {
        throw new IllegalArgumentException("invalid inputFld2");
      }
    }
  }
	
	private int doSomething(final int input1, final int input2) {
		return input1 * input2;
	}

	private int doSomethingElse(final int input1, final int input2) {
		return input1 - input2;
	}
}
