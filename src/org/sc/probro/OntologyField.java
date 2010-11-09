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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Each Ontology comes with its own metadata requirements: additional pieces of information, associated
 * with request, which must (or may) be supplied by the requesting user and which are used to fill the 
 * ontology-specific columns of the bulk request table.  
 * 
 * The OntologyField represents such an association of a column name with a bulk request 
 * column for an Ontology. Each field consists of: 
 * <ol><li><tt>name</tt>, 
 * the name of the field, corresponding to a column in the bulk request table,</li>
 * <li><tt>description</tt>, 
 * the description of the field, a {@link java.lang.String} suitable for display to a user, and </li>
 * <li><tt>metadata_key</tt>, 
 * the corresponding metadata key associated with a request which is used to fulfill this column 
 * in the bulk request table.</li>
 * </ol> 
 * 
 * @author Timothy Danford
 *
 */
public class OntologyField extends BrokerData {

	public String name, description, metadata_key;
	
	public OntologyField() { }
	
	public OntologyField(JSONObject obj) throws JSONException { 
		name = obj.getString("field_name");
		description = obj.getString("field_description");
		metadata_key = obj.getString("field_metadata_key");
	}
	
	public OntologyField(String id, JSONObject obj) throws JSONException { 
		name = obj.getString("field_name");
		description = obj.getString("field_description");
		metadata_key = obj.getString("field_metadata_key");
	}
	
	public String toString() { return name; }
	
	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();
		
		obj.key("field_name").value(name);
		obj.key("field_description").value(description);
		obj.key("field_metadata_key").value(metadata_key);
		
		obj.endObject();		
	}
	
}
