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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;


public class ReflectedObject {
	
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		Map<String,Field> fields = getFieldMap();
		for(String key : fields.keySet()) { 
			Field f = fields.get(key);
			sb.append(sb.length() > 0 ? "|" : "");
			try {
				sb.append(String.format("%s=%s", f.getName(), String.valueOf(f.get(this))));
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		return sb.toString();
	}

	public Map<String,Field> getFieldMap() { 
		Map<String,Field> map = new LinkedHashMap<String,Field>();
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(!Modifier.isStatic(mod) && Modifier.isPublic(mod)) { 
				map.put(f.getName(), f);
			}
		}
		return map;
	}
	
	public void setFromParameters(Map<String,String[]> params) {
		Map<String,Field> fields = getFieldMap();
		
		for(String key : params.keySet()) { 
			if(fields.containsKey(key) && params.get(key).length > 0) { 
				setFromString(key, params.get(key)[0]);
			}
		}
	}
	
	public void setFromReflectedObject(ReflectedObject obj) {
		Map<String,Field> otherFields = obj.getFieldMap();
		Map<String,Field> myFields = getFieldMap();
		
		for(String fieldName : otherFields.keySet()) { 
			if(myFields.containsKey(fieldName) && 
					isSubclass(otherFields.get(fieldName).getType(),
							myFields.get(fieldName).getType())) { 
				try {
					Object otherValue = otherFields.get(fieldName).get(obj);
					if(otherValue != null) { 
						myFields.get(fieldName).set(this, otherValue);
					}
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}
	
	public void setFromString(String fieldName, String value) { 
		try {
			Field f = getClass().getField(fieldName);
			int mod = f.getModifiers();
			if(!Modifier.isPublic(mod) || Modifier.isStatic(mod)) { 
				throw new IllegalArgumentException(fieldName);
			}
			
			Class type = f.getType();
			if(isSubclass(type, String.class)) {
				f.set(this, value);
			} else if (isSubclass(type, Integer.class)) { 
				f.set(this, Integer.parseInt(value));
			} else if (isSubclass(type, Double.class)) { 
				f.set(this, Double.parseDouble(value));
			} else if (isSubclass(type, java.util.Date.class)) {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				f.set(this, format.parse(value));
			} else {
				// Instead of throwing an error, we should do *nothing*
				// -- this will be handled by the caller instead.
				/*
				throw new UnsupportedOperationException(
						String.format("type %s cannot be set from string value \"%s\"", 
								type.getName(), value));
			 	*/
			}
			
		} catch (NoSuchFieldException e) {
			//e.printStackTrace(System.err);
			throw new IllegalArgumentException(fieldName);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(fieldName);
		} catch (ParseException e) {
			throw new IllegalArgumentException(fieldName, e);
		}
	}
	
	public static boolean isSubclass(Class<?> c1, Class<?> c2) {
		return c2.isAssignableFrom(c1);
	}
}
