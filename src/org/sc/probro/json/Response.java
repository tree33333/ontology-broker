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
package org.sc.probro.json;

import java.util.*;
import java.lang.reflect.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tdanford.json.schema.SchemaEnv;

public class Response<T extends ValidatedJSON> {

	private ArrayList<T> values;
	
	public Response(Collection<T> vs) { 
		values = new ArrayList<T>(vs);
	}
	
	public Response() { 
		values= new ArrayList<T>();
	}
	
	public Response(Class<T> cls, String schemaName, JSONArray array) { 
		this();
		try {
			Constructor<T> constructor = cls.getConstructor(String.class, JSONObject.class);
			for(int i = 0; i < array.length(); i++) { 
				JSONObject obj = array.getJSONObject(i);
				T value = constructor.newInstance(schemaName, obj);
				values.add(value);
			}
			
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (JSONException e) {
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public int size() { return values.size(); }
	public T value(int i) { return values.get(i); }
	
	public boolean validate(SchemaEnv env) { 
		for(T v : values) { 
			if(!v.validate(env)) { 
				return false;
			}
		}
		return true;
	}
}
