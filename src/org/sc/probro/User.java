package org.sc.probro;

import org.json.*;

public class User extends BrokerData {

	public String id;
	public String user_name;
	
	public User() { 
	}
	
	public User(JSONObject obj) throws JSONException { 
		id = obj.getString("href");
		user_name = obj.getString("text");
	}
	
	public User(String id, JSONObject obj) throws JSONException {
		this.id = id;
		user_name = obj.getString("user_name");
	}

	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();
		obj.key("user_name").value(user_name);			
		obj.endObject();		
	}
	
	public void stringJSONLink(JSONStringer stringer) throws JSONException { 
		stringer.object();
		stringer.key("href").value(id);
		stringer.key("text").value(user_name);
		stringer.endObject();
	}
}
