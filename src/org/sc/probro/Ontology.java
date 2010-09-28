package org.sc.probro;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class Ontology extends BrokerData {

	public String id; 
	public String ontology_name;
	
	public Ontology() { }
	
	public Ontology(JSONObject obj) throws JSONException { 
		id = obj.getString("href");
		ontology_name = obj.getString("text");
	}
	
	public Ontology(String id, JSONObject obj) throws JSONException { 
		this.id = id;
		ontology_name = obj.getString("ontology_name");
	}
	
	public String toString() { return ontology_name; }
	
	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();
		obj.key("ontology_name").value(ontology_name);
		obj.key("href"); stringJSONLink(obj);
		obj.endObject();		
	}
	
	public void stringJSONLink(JSONStringer obj) throws JSONException { 
		obj.object()
			.key("href").value(id)
			.key("text").value(ontology_name)
			.endObject();
	}
}
