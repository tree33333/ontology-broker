package org.sc.probro.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

public abstract class DBObject {
	
	public Field[] getAllFields(String... omit) { 
		Set<String> omitFields = omit.length == 0 ? null : 
			new TreeSet<String>(Arrays.asList(omit));
		Map<String,Field> fs = getFieldMap();
		ArrayList<Field> flist = new ArrayList<Field>();
		for(String fname : fs.keySet()) {
			if(omitFields==null || !omitFields.contains(fname)) { 
				flist.add(fs.get(fname)); 
			}
		}
		return flist.toArray(new Field[0]);
	}
	
	public Field[] getTemplateFields(String... omit) { 
		Set<String> omitFields = omit.length == 0 ? null : 
			new TreeSet<String>(Arrays.asList(omit));
		Map<String,Field> fs = getFieldMap();
		ArrayList<Field> flist = new ArrayList<Field>();
		for(String fname : fs.keySet()) {
			if(omitFields==null || !omitFields.contains(fname)) {
				try {
					Object value = fs.get(fname).get(this);
					if(value != null) { 
						flist.add(fs.get(fname));
					}
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return flist.toArray(new Field[0]);		
	}
	
	public String prepareSQLWhereClause(String... omit) {
		
		Field[] fs = getAllFields(omit);
		StringBuilder clause = new StringBuilder();
		
		for(int i = 0; i < fs.length; i++) { 
			String fieldName = fs[i].getName();
			
			if(clause.length() > 0) { clause.append(" AND "); }
			clause.append(String.format("%s=?", fieldName));
		}
		return clause.toString();
	}
	
	public String prepareSQLTemplateWhereClause(String... omit) { 

		Field[] fs = getTemplateFields(omit);
		StringBuilder clause = new StringBuilder();
		for(int i = 0; i < fs.length; i++) { 
			String fieldName = fs[i].getName();
			if(clause.length() > 0) { clause.append(" AND "); }
			clause.append(String.format("%s=?", fieldName));
		}
		return clause.length() > 0 ? String.format(" WHERE %s", clause.toString()) : "";
	}
	
	public String prepareSQLInsertValuesClause(String... omit) {
		Field[] fs = getAllFields(getAutoGeneratedFields());
		StringBuilder clause = new StringBuilder();
		for(int i = 0; i < fs.length; i++) { 
			String fieldName = fs[i].getName();
			if(clause.length() > 0) { clause.append(", "); }
			clause.append(String.format("?"));
		}
		return clause.toString();		
	}
	
	public String prepareSQLInsertFieldsClause(String... omit) {
		Field[] fs = getAllFields(getAutoGeneratedFields());
		StringBuilder clause = new StringBuilder();
		for(int i = 0; i < fs.length; i++) { 
			String fieldName = fs[i].getName();
			if(clause.length() > 0) { clause.append(", "); }
			clause.append(String.format("%s", fieldName));
		}
		return clause.toString();		
	}
	
	public String prepareSQLUpdateFieldsClause(String... omit) {
		Field[] fs = getAllFields(getAutoGeneratedFields());
		StringBuilder clause = new StringBuilder();
		for(int i = 0; i < fs.length; i++) { 
			String fieldName = fs[i].getName();
			if(clause.length() > 0) { clause.append(", "); }
			clause.append(String.format("%s=?", fieldName));
		}
		return clause.toString();		
	}
	
	public String[] getAutoGeneratedFields() { return new String[0]; }
	public String[] getKeyFields() { return new String[0]; }
	
	public static boolean isSubclass(Class<?> c1, Class<?> c2) {
		return c2.isAssignableFrom(c1);
	}
	
	/**
	 * @deprecated 
	 * 
	 * @param str
	 * @return
	 */
	public static boolean cleanSQL(String str) { 
		int idx = -1;
		while((idx = str.indexOf("'")) != -1) { 
			if(idx == 0 || str.charAt(idx-1) != '\\') { 
				return false;
			}
		}
		return true;
	}
	
	public DBObject() {}
	
	public String getTableName() { 
		String className = getClass().getSimpleName();
		if(className.endsWith("Object")) { 
			className = className.substring(0, className.length()-6);
		}
		return className.toUpperCase() + "S";
	}
	
	private static String readClob(Clob c) throws SQLException { 
		StringBuilder sb = new StringBuilder();
		Reader r = c.getCharacterStream();
		int charInt = -1;
		try {
			while((charInt = r.read()) != -1) { 
				sb.append((char)charInt);
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		return sb.toString();
	}
	
	public DBObject(ResultSet rs) throws SQLException { 
		setFromResultSet(rs);
	}
	
	public void setFromResultSet(ResultSet rs) throws SQLException { 
		ResultSetMetaData data = rs.getMetaData();

		for(int i = 1; i <= data.getColumnCount(); i++) { 
			String columnName = data.getColumnName(i).toLowerCase();
			try {
				Field f = getClass().getField(columnName);
				int mod = f.getModifiers();
				
				if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
					Object value = rs.getObject(i);
					if(value instanceof Clob) { 
						Clob clob = (Clob)value;
						f.set(this, readClob(clob));
					} else if (value instanceof java.sql.Date) { 
						java.sql.Date sqlDate = (java.sql.Date)value;
						long time = sqlDate.getTime();
						java.util.Date date = new java.util.Date(time);
						f.set(this, date);
					} else {
						f.set(this, value);
					}
				}
				
			} catch (NoSuchFieldException e) {
				// do nothing.
				
			} catch (IllegalAccessException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public DBObject(JSONObject obj) throws SQLException {
		Iterator<String> keys = obj.keys();
		while(keys.hasNext()) { 
			String key = keys.next();
			try {
				Field f = getClass().getField(key);
				int mod = f.getModifiers();
				Class type = f.getType();
				
				if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
					if(isSubclass(type, String.class)) { 
						f.set(this, obj.getString(key));
					} else if (isSubclass(type, Integer.class)) { 
						f.set(this, obj.getInt(key));
					} else if (isSubclass(type, Double.class)) { 
						f.set(this, obj.getDouble(key));
					} else { 
						f.set(this, obj.get(key));
					}
				}
				
			} catch (NoSuchFieldException e) {
				e.printStackTrace(System.err);
				
			} catch (IllegalAccessException e) {
				e.printStackTrace(System.err);
				
			} catch (JSONException e) {
				e.printStackTrace(System.err);
			}
		}		
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
	
	public void setFromJSON(JSONObject obj) { 
		throw new RuntimeException("Unsuppported");
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
	
	public abstract boolean isAutoGenerated(String fieldName);
	public String getKey() { return null; }
	
	public String writeHTMLRowHeader() { 
		StringBuilder html = new StringBuilder();

		html.append(String.format("<tr class=\"header_%s\">", getClass().getSimpleName().toLowerCase())); 
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				String name = f.getName();
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
					String value = String.valueOf(f.get(this));

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
	
	protected void writeJSONObjectContents(JSONWriter json) throws JSONException { 
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				String name = f.getName();
				try {
					Object value = f.get(this);
					if(value != null) { 
						json.key(name).value(value);
					}
					
				} catch (IllegalAccessException e) {
					throw new JSONException(e);
				}
			}
		}		
	}
	
	/**
	 * Basically, isSubsetOf(x) is *almost* equivalent to !findMatchingFields(x).isEmpty(), 
	 * except that it's directional -- it takes 'nulls' as missing values, and so it won't signal 
	 * false for a null field in this object that has a value in the other.  
	 * 
	 * @param <T>
	 * @param other
	 * @return
	 */
	public <T extends DBObject> boolean isSubsetOf(T other) { 
		if(!isSubclass(other.getClass(), getClass())) { 
			throw new IllegalArgumentException(String.format("%s is not a subclass of %s",
					other.getClass().getSimpleName(),
					getClass().getSimpleName()));
		}

		for(Field f : getClass().getFields()) {
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				try {
					Object thisValue = f.get(this), thatValue = f.get(other);
					if(thisValue != null) {
						if(thatValue == null || !thisValue.equals(thatValue)) { 
							return false;
						}
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		
		return true;
	}
	
	public void setFromDBObject(DBObject obj) {
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
	
	/**
	 * Returns the set of field names for which the values whose values differ between this object
	 * and the object given as an argument.  The argument object must be a subclass of the class of 
	 * 'this'.  Two values are said to 'differ' if one is 'null' when the other isn't, or if both
	 * values are non-null but equals() (when called on one value, given the other value as an argument) 
	 * returns false.
	 * 
	 * @param <T>
	 * @param other  The other object to compare to. 
	 * @throws IllegalArgumentException if the argument object is not a subclass of the class of 'this'.
	 * @return
	 */
	public <T extends DBObject> Set<String> findMismatchingFields(T other) {
		if(!isSubclass(other.getClass(), getClass())) { 
			throw new IllegalArgumentException(String.format("%s is not a subclass of %s",
					other.getClass().getSimpleName(),
					getClass().getSimpleName()));
		}

		Set<String> mismatches = new TreeSet<String>();

		for(Field f : getClass().getFields()) {
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				try {
					Object thisValue = f.get(this), thatValue = f.get(other);
					if(thisValue != null || thatValue != null) { 
						if(thisValue == null || thatValue == null || !thisValue.equals(thatValue)) { 
							mismatches.add(f.getName());
						}
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		
		return mismatches;
	}
	
	public void writeJSONObject(JSONWriter json) throws JSONException { 
		json.object();
		writeJSONObjectContents(json);
		json.endObject();
	}
}
