package org.sc.probro;

import java.util.Map;
import java.lang.reflect.*;

import org.json.*;
import org.sc.probro.data.DBObject;

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

	public void setFromJSON(JSONObject obj) { 
		
	}
	
	public void setFromRequestParameters(Map<String,String[]> params) { 
		
	}
}
