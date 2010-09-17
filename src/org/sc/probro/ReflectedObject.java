package org.sc.probro;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReflectedObject {

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
				throw new UnsupportedOperationException(type.getSimpleName());
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
