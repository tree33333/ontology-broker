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
	
	public User(String id, String name) { 
		this.id = id;
		this.user_name = name;
	}
	
	public User(String id, JSONObject obj) throws JSONException {
		this.id = id;
		user_name = obj.getString("user_name");
	}
	
	public String toString() { return user_name; }

	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();
		obj.key("user_name").value(user_name);	
		obj.key("href"); stringJSONLink(obj);
		obj.endObject();		
	}
	
	public void stringJSONLink(JSONStringer stringer) throws JSONException { 
		stringer.object();
		stringer.key("href").value(id);
		stringer.key("text").value(user_name);
		stringer.endObject();
	}
}
