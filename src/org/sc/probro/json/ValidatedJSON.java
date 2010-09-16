package org.sc.probro.json;

import org.json.JSONObject;

import tdanford.json.schema.SchemaEnv;

public class ValidatedJSON extends JSONObject {
	
	private String schemaName;

	public ValidatedJSON(String schemaName, JSONObject obj) { 
		super(obj, JSONObject.getNames(obj));
		this.schemaName = schemaName;
	}
	
	public boolean validate(SchemaEnv env) { 
		return env.lookupType(schemaName).contains(this);
	}
}
