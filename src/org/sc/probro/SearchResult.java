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

import java.util.*;
import org.json.*;

public class SearchResult extends BrokerData {

	public String id;
	public String response_type;
	public ArrayList<String> description;
	public ArrayList<String> accession;
	
	public SearchResult() {}
	
	public SearchResult(JSONObject obj) throws JSONException { 
		id = obj.getString("id");
		response_type = obj.getString("type");
		description = new ArrayList<String>();
		accession = new ArrayList<String>();
				
		JSONArray array = obj.getJSONArray("description");
		for(int i = 0; i < array.length(); i++) { 
			description.add(array.getString(i));
		}

		if(obj.has("accession")) { 
			array = obj.getJSONArray("accession");
			for(int i = 0; i < array.length(); i++) { 
				accession.add(array.getString(i));
			}
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
