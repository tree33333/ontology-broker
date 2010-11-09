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

import java.util.Map;
import java.lang.reflect.*;

import org.json.*;
import org.sc.probro.data.DBObject;
import org.sc.probro.exceptions.BrokerException;

public abstract class BrokerData extends ReflectedObject implements JSONString {
	
	public abstract void stringJSON(JSONStringer obj) throws JSONException;

	public String toJSONString() {
		JSONStringer obj = new JSONStringer();
		try { 
			stringJSON(obj);
		} catch(JSONException e) { 
			throw new IllegalStateException(e);
		}
		return obj.toString();
	}
	
	public static <T extends BrokerData> void stringJSONArray(JSONStringer stringer, T... values) throws JSONException {  
		stringer.array();
		for(int i = 0; i < values.length; i++) { 
			values[i].stringJSON(stringer);
		}
		stringer.endArray();
	}
	
	public String writeHTMLLink() { 
		try {
			Field idField = getClass().getField("id");
			
			String href = String.valueOf(idField.get(this));
			String text = toString();
			
			return String.format("<a href=\"%s\">%s</a>", href, text);

		} catch (SecurityException e) {
			throw new UnsupportedOperationException(e.getMessage());
		} catch (NoSuchFieldException e) {
			throw new UnsupportedOperationException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException(e.getMessage());
		}
	}
	
	public String writeHTMLRowHeader() { 
		StringBuilder html = new StringBuilder();

		html.append(String.format("<tr class=\"header_%s\">", getClass().getSimpleName().toLowerCase())); 
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				String name = f.getName();
				
				if(name.equals("id")) { 
					name = "link";
				}

				/*
				if(DBObject.isSubclass(f.getType(), BrokerData.class)) { 
					try {
						name = ((BrokerData)f.get(this)).writeHTMLLink();
					} catch (IllegalAccessException e) {
						throw new UnsupportedOperationException(e.getMessage());
					}
				}
				*/
				
				html.append(String.format("<th>%s</th>", name));
			}
		}
		html.append(String.format("</tr>")); 

		return html.toString();
	}
	
	public String writeHTMLObject(boolean asRow) {
		StringBuilder html = new StringBuilder();
		if(asRow) { 
			html.append(String.format("<tr class=\"obj_%s\">", getClass().getSimpleName().toLowerCase())); 
		} else {  
			html.append(String.format("<table class=\"obj_%s\">", getClass().getSimpleName().toLowerCase()));
		}
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				String name = f.getName();
				try {
					Object objValue = f.get(this);
					String value = String.valueOf(objValue);

					if(name.equals("id")) { 
						value = String.format("<a href=\"%s?format=html\">%s</a>", value, "link");
					} else if(objValue != null && objValue instanceof BrokerData) { 
						value = ((BrokerData)objValue).writeHTMLLink();
					}

					if(!asRow) { 
						html.append(String.format("<tr class=\"field_%s\">", name.toLowerCase())); 
						html.append(String.format("<td>%s</td>", name));
					}  
					html.append(String.format("<td class=\"value_%s\">%s</td>", name.toLowerCase(), value));
					if(!asRow) { 
						html.append("</tr>"); 
					} 

				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		if(asRow) { 
			html.append(String.format("</tr>")); 
		} else {  
			html.append("</table>");
		}
		return html.toString();
	}

	public void setFromJSON(JSONObject obj, Broker broker, UserCredentials user) throws BrokerException { 
		
		Map<String,Field> fields = getFieldMap();
		for(String fieldName : fields.keySet()) { 
			Field field = fields.get(fieldName);
			Class type = field.getType();

			try { 
				if(obj.has(fieldName)) { 
					if(isSubclass(type, BrokerData.class)) {
						
						if(isSubclass(type, User.class)) { 
							field.set(this, broker.checkUser(user, obj.get(fieldName).toString()));
							
						} else if (isSubclass(type, Ontology.class)) { 
							field.set(this, broker.checkOntology(user, obj.get(fieldName).toString()));
							
						} else { 
							throw new IllegalArgumentException(String.format(
									"Unsupported type %s for field %s in %s in setFromJSON()",
									type.getSimpleName(), fieldName, getClass().getSimpleName()));
						}

					} else { 
						Object value = obj.get(fieldName);
						String valString = value != null ? value.toString() : null;
						setFromString(fieldName, valString);
					}
				}
			} catch(JSONException e) { 
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new BrokerException(e);
			}
		}
	}
	
	public void setFromRequestParameters(Map<String,String[]> params) { 
		throw new UnsupportedOperationException("BrokerData.setFromRequestParameters");
	}
}
