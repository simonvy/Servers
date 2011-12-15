package common.net;

import java.io.Serializable;

// asynchronous procedure call
public class APC implements Serializable {
	
	private String functionName;
	private Object[] parameters;
	
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
	
	public String getFunctionName() {
		return this.functionName;
	}
	
	public void setParameters(Object[] params) {
		this.parameters = params;
	}
	
	public Object[] getParameters() {
		return this.parameters;
	}
}
