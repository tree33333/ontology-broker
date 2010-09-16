package org.sc.probro;

import java.util.*;
import org.json.*;

public class SearchResult extends BrokerData{

	public String id;
	public String response_type;
	public ArrayList<String> description;
	public ArrayList<String> accession;
	
	public SearchResult() {}
	
	public SearchResult(JSONObject obj) throws JSONException { 
		id = obj.getString("id");
		response_type = obj.getString("response_type");
		description = new ArrayList<String>();
		accession = new ArrayList<String>();
				
		JSONArray array = obj.getJSONArray("description");
		for(int i = 0; i < array.length(); i++) { 
			description.add(array.getString(i));
		}
		
		array = obj.getJSONArray("accession");
		for(int i = 0; i < array.length(); i++) { 
			accession.add(array.getString(i));
		}
	}

	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();

		obj.key("id").value(id);
		obj.key("response_type").value(response_type);

		obj.key("description").array();
		for(String v : description) { obj.value(v); }
		obj.endArray();

		obj.key("accession").array();
		for(String v : accession) { obj.value(v); }
		obj.endArray();

		obj.endObject();
	}
}
