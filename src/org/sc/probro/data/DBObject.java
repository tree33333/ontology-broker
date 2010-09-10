package org.sc.probro.data;

import java.text.DateFormat;
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
			if(clause.length() > 0) { clause.append(" AND "); }
			clause.append(String.format("%s=?", fieldName));
		}
		return clause.toString();		
	}
	
	public String[] getAutoGeneratedFields() { return new String[0]; }
	public String[] getKeyFields() { return new String[0]; }
	
	public boolean checkExists(DBObjectModel model) throws DBModelException { 
		return model.count(getClass(), this) > 0;
	}
	
	/**
	 * @deprecated
	 * 
	 * @param cxn
	 * @return
	 * @throws SQLException
	 */
	public boolean checkExists(Connection cxn) throws SQLException { 
		String countQuery = countString();
		Statement stmt = cxn.createStatement();
		try { 
			ResultSet rs = stmt.executeQuery(countQuery);
			try { 
				rs.next();
				int count = rs.getInt(1);
				return count > 0;
			} finally { 
				rs.close();
			}
		} finally { 
			stmt.close();
		}
	}
	
	/**
	 * @deprecated 
	 * 
	 * @param v
	 * @return
	 */
	public static String asSQL(Object v) {
		if(v == null) { 
			return "NULL";
		} else if(v instanceof String) { 
			return String.format("'%s'", v.toString().replace(";", "\\;").replace("'", "\\'"));
		} else if(v instanceof java.util.Date) { 
			Date d = (Date)v;
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			return String.format("'%s'", df.format(d));
		} else { 
			return v.toString();
		}
	}
	
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
					} else { 
						f.set(this, value);
					}
				}
				
			} catch (NoSuchFieldException e) {
				e.printStackTrace(System.err);
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
			} else { 
				throw new UnsupportedOperationException(type.getSimpleName());
			}
			
		} catch (NoSuchFieldException e) {
			//e.printStackTrace(System.err);
			throw new IllegalArgumentException(fieldName);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(fieldName);
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

	/**
	 * @deprecated 
	 * 
	 * @param <T>
	 * @param cls
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public <T extends DBObject> T loadDBVersion(Class<T> cls, Statement stmt) throws SQLException { 
		if(!isSubclass(cls, getClass())) { 
			throw new IllegalArgumentException(String.format(
				"%s is not a subclass of %s", cls.getSimpleName(), getClass().getSimpleName()));
		}
		
		if(getKey() == null) { throw new IllegalArgumentException("Can't load matching object without a key."); }
		
		T db = null;
		try {
			Field keyField = getClass().getField(getKey());
			int keyMod = keyField.getModifiers();
			if(!Modifier.isPublic(keyMod) || Modifier.isStatic(keyMod)) { 
				throw new IllegalArgumentException(String.format("Key field %s must be public and non-static", 
						getKey()));
			}
			Object keyValue = keyField.get(this);
			
			String tableName = getClass().getSimpleName().toUpperCase() + "S";
			Constructor<T> constructor = cls.getConstructor(ResultSet.class);
			String query = String.format("select * from %s where %s=%s",
					tableName, 
					getKey(), 
					DBObject.asSQL(keyValue));

			ResultSet rs = stmt.executeQuery(query);
			try { 
				if(rs.next()) { 
					db = constructor.newInstance(rs);
				}
			} finally { 
				rs.close();
			}
			
		} catch (InstantiationException e) {
			e.printStackTrace(System.err);
		} catch (InvocationTargetException e) {
			e.printStackTrace(System.err);
		} catch (NoSuchMethodException e) {
			e.printStackTrace(System.err);
		} catch (NoSuchFieldException e) {
			e.printStackTrace(System.err);
		} catch (IllegalAccessException e) {
			e.printStackTrace(System.err);
		}
		
		return db;
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
	
	/**
	 * Creates an SQL string, representing the SQL update statement that must be made to 
	 * <em>update</em> this object in a database; this update statement can be executed assuming
	 * the object itself is already present as a row in the appropriate table (otherwise, the
	 * insertString() method should be used instead).
	 * 
	 *  @deprecated
	 * 
	 * @return
	 */
	public String saveString() { 
		StringBuilder sb = new StringBuilder();
		String tableName = getClass().getSimpleName().toUpperCase() + "S";
		
		String autoName = null;
		Integer autoValue = null;
		
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
				if(isAutoGenerated(f.getName())) { 
					if(autoName != null) { 
						throw new IllegalStateException("More than one auto-generated field isn't supported.");
					}
					if(!isSubclass(f.getType(), Integer.class)) { 
						throw new IllegalStateException("Non-integer auto-generated fields aren't supported.");						
					}
					
					autoName = f.getName();
					try {
						autoValue = (Integer)f.get(this);
						if(autoValue == null) { 
							throw new IllegalStateException("NULL auto-generated fields aren't allowed.");
						}

					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					
				} else { 
					Object value = null;
					try {
						value = f.get(this);
						if(value != null) { 
							if(sb.length() > 0) { sb.append(", "); }

							sb.append(String.format("%s=%s", f.getName(), asSQL(value)));
						}

					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		if(autoName == null) { 
			throw new IllegalStateException("Must have at least one key...");
		}
		
		return String.format("UPDATE %s SET %s WHERE %s=%d",
				tableName, sb.toString(), autoName, autoValue);
	}
	
	/**
	 * @deprecated 
	 * 
	 * @return
	 */
	public String insertString() { 
		String tableName = getClass().getSimpleName().toUpperCase() + "S";
		StringBuilder fields = new StringBuilder();
		StringBuilder values = new StringBuilder();
		
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(!isAutoGenerated(f.getName()) && Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				try {
					Object value = f.get(this);
					String name = f.getName();
					
					if(fields.length() > 0) { 
						fields.append(", ");
						values.append(", ");
					}
					
					fields.append(name);
					values.append(asSQL(value));
					
				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		
		String fieldString = fields.toString(), valueString = values.toString();
		return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, fieldString, valueString);
	}	
	
	public <T extends DBObject> Collection<T> loadByKey(String myFieldName, Class<T> cls, String otherFieldName, Statement stmt) throws SQLException { 
		LinkedList<T> keyed = new LinkedList<T>();
		
		try {
			Field myField = getClass().getField(myFieldName);
			Object myValue = myField.get(this);
			if(myValue == null) { throw new IllegalArgumentException(String.format("%s has null value", myFieldName)); }
			
			Field otherField = cls.getField(otherFieldName);
			Constructor<T> constructor = cls.getConstructor();
			Constructor<T> resultConstructor = cls.getConstructor(ResultSet.class);
			
			T template = constructor.newInstance();
			otherField.set(template, myValue);
			
			String queryString = template.queryString();
			ResultSet rs = stmt.executeQuery(queryString);
			while(rs.next()) {
				keyed.add(resultConstructor.newInstance(rs));
			}
			rs.close();
			
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		
		return keyed;
	}
	
	/**
	 * @deprecated 
	 * 
	 * @return
	 */
	public String countString() { 
		String tableName = getClass().getSimpleName().toUpperCase() + "S";
		StringBuilder where = new StringBuilder();
		
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				try {
					Object value = f.get(this);
					if(value != null) { 
						String name = f.getName();

						if(where.length() > 0) { 
							where.append(" AND ");
						}

						where.append(String.format("%s=%s", name, asSQL(value)));
					}
					
				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		
		String whereString = where.toString();
		return String.format("SELECT count(*) FROM %s %s", tableName, 
				(whereString.length() > 0 ? String.format("WHERE %s", whereString) : ""));		
	}
	
	/**
	 * @deprecated 
	 * 
	 * @return
	 */
	public String queryString() { 
		String tableName = getClass().getSimpleName().toUpperCase() + "S";
		StringBuilder where = new StringBuilder();
		
		for(Field f : getClass().getFields()) { 
			int mod = f.getModifiers();
			if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
				try {
					Object value = f.get(this);
					if(value != null) { 
						String name = f.getName();

						if(where.length() > 0) { 
							where.append(" AND ");
						}

						where.append(String.format("%s=%s", name, asSQL(value)));
					}
					
				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		
		String whereString = where.toString();
		return String.format("SELECT * FROM %s %s", tableName, 
				(whereString.length() > 0 ? String.format("WHERE %s", whereString) : ""));
	}
}
