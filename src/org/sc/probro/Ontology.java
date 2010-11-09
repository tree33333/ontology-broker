/*
	Copyright 2010 Massachusetts General Hospital

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
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
	
	public int hashCode() { return id.hashCode(); }

	public boolean equals(Object obj) { 
		return (obj instanceof Ontology) && 
			id.equals(((Ontology)obj).id);
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
