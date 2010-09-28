package org.sc.probro;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class Ontology extends BrokerData {

	public String id; 
	public String ontology_name;
	public User maintainer;
	
	public ArrayList<OntologyField> fields;
	
	public Ontology() { }
	
	public Ontology(JSONObject obj) throws JSONException { 
		this(obj.getString("href"), obj);
	}
	
	public Ontology(String id, JSONObject obj) throws JSONException { 
		this.id = id;
		ontology_name = obj.getString("ontology_name");
		maintainer = new User(obj.getJSONObject("maintainer"));
		
		fields = new ArrayList<OntologyField>();
		JSONArray fieldArray = obj.getJSONArray("fields");
		for(int i = 0; i < fieldArray.length(); i++) { 
			fields.add(new OntologyField(fieldArray.getJSONObject(i)));
		}
	}
	
	public String toString() { return ontology_name; }
	
	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();
		
		obj.key("href"); stringJSONLink(obj);
		obj.key("ontology_name").value(ontology_name);
		obj.key("maintainer"); maintainer.stringJSONLink(obj);
		
		obj.key("fields"); obj.array();
		for(OntologyField field : fields) { 
			field.stringJSON(obj);
		}
		obj.endArray();
		
		obj.endObject();		
	}
	
	public void stringJSONLink(JSONStringer obj) throws JSONException { 
		obj.object()
			.key("href").value(id)
			.key("text").value(ontology_name)
			.endObject();
	}
}
