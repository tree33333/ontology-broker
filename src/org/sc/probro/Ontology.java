package org.sc.probro;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class Ontology extends BrokerData {

	public String id; 
	public String name;
	
	public Ontology() { }
	
	public Ontology(JSONObject obj) throws JSONException { 
		id = obj.getString("href");
		name = obj.getString("text");
	}
	
	public Ontology(String id, JSONObject obj) throws JSONException { 
		this.id = id;
		name = obj.getString("name");
	}
	
	public String toString() { return name; }
	
	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();
		obj.key("name").value(name);
		obj.key("href"); stringJSONLink(obj);
		obj.endObject();		
	}
	
	public void stringJSONLink(JSONStringer obj) throws JSONException { 
		obj.object()
			.key("href").value(id)
			.key("text").value(name)
			.endObject();
	}
}
