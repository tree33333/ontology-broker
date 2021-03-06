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

import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sc.probro.exceptions.BadRequestException;
import org.sc.probro.exceptions.BrokerException;

public class Metadata extends BrokerData {

	public String key, value;
	
	public String created_on;
	
	public User created_by;
	
	public Metadata() { }
	
	public Metadata(JSONObject obj) throws JSONException { 
		key = obj.getString("metadata_key");
		value = obj.getString("metadata_value");
		created_by = new User(obj.getJSONObject("created_by"));
		created_on = obj.getString("created_on");
	}
	
	public void stringJSON(JSONStringer obj) throws JSONException { 
		obj.object();
		
		obj.key("metadata_key").value(key);
		obj.key("metadata_value").value(value);
		
		obj.key("created_by");
		created_by.stringJSONLink(obj);
		
		obj.key("created_on").value(created_on);

		obj.endObject();		
	}
	
	public void setFromJSON(JSONObject obj, Broker broker, UserCredentials user) throws BrokerException { 
		super.setFromJSON(obj, broker, user);
		try { 
			if(obj.has("metadata_key")) { 
				key = obj.getString("metadata_key");
			}
			if(obj.has("metadata_value")) { 
				value = obj.getString("metadata_value");
			}
			
			if(key == null || value == null) {
				BadRequestException except = new BadRequestException(obj.toString());
				Log.warn(except);
				throw except;
			}
		} catch(JSONException e) { 
			throw new BrokerException(e);
		}
	}
}
