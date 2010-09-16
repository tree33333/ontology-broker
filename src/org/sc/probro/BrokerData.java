package org.sc.probro;

import org.json.*;

public abstract class BrokerData implements JSONString {
	
	public abstract void stringJSON(JSONStringer obj) throws JSONException;

	public String toJSONString() {
		JSONStringer obj = new JSONStringer();
		try { 
			stringJSON(obj);
		} catch(JSONException e) { 
			throw new IllegalStateException(e);
		}
		return obj.toString();
	}
}
