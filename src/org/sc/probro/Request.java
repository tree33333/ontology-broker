package org.sc.probro;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONStringer;

public class Request extends BrokerData {
	
	public String id;  // provisional_term
	
	public String ontology_term;
	
	public String search_text;
	public String context;
	public String comment;
	public String status;
	public String provenance;
	
	public User creator;
	public User modified_by;
	public String date_submitted;
	public Ontology ontology;
	public ArrayList<Metadata> metadata;
	
	public Request() {}
	
	public Request(JSONObject obj) throws JSONException { 
		id = obj.getString("provisional_term");
		ontology_term = obj.getString("ontology_term");
		search_text = obj.getString("search_text");
		context = obj.getString("context");
		comment = obj.getString("comment");
		status = obj.getString("status");
		provenance = obj.getString("provenance");
		date_submitted = obj.getString("date_submitted");
		
		creator = new User(obj.getJSONObject("creator"));
		modified_by = new User(obj.getJSONObject("modified_by"));
		ontology = new Ontology(obj.getJSONObject("ontology"));
		
		metadata = new ArrayList<Metadata>();
		JSONArray marray = obj.getJSONArray("metadata");
		
		for(int i = 0; i < marray.length(); i++) { 
			metadata.add(new Metadata(marray.getJSONObject(i)));
		}
	}
	
	public String toString() { return id; }

	public void stringJSON(JSONStringer stringer) throws JSONException { 
		
		stringer.object();
		
		stringer.key("provisional_term").value(id);
		stringer.key("ontology_term").value(ontology_term);
		stringer.key("search_text").value(search_text);
		stringer.key("context").value(context);
		stringer.key("comment").value(comment);
		stringer.key("provenance").value(provenance);
		stringer.key("status").value(status);
		
		stringer.key("creator");
		creator.stringJSONLink(stringer);
		
		stringer.key("modified_by");
		modified_by.stringJSONLink(stringer);
		
		stringer.key("date_submitted").value(date_submitted);
		
		stringer.key("ontology");
		ontology.stringJSONLink(stringer);
		
		stringer.key("metadata");
		stringer.array();
		for(Metadata m : metadata) { 
			m.stringJSON(stringer);
		}
		stringer.endArray();
		
		stringer.endObject();
		
	}
}
