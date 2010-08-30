package org.sc.probro.data;

import java.io.*;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;

public class Preparation {

	public PreparedStatement stmt;
	public Field[] fields;
	public Set<Integer> clobs;
	
	public Preparation(PreparedStatement ps, Field... fs) { 
		stmt = ps;
		fields = fs.clone();
		clobs = new TreeSet<Integer>();
	}
	
	public Preparation(PreparedStatement ps, Class<? extends DBObject> cls, String... fieldNames) { 
		stmt = ps;
		clobs = new TreeSet<Integer>();
		ArrayList<Field> fs = new ArrayList<Field>();
		for(String fname : fieldNames) { 
			try {
				Field f = cls.getField(fname);
				int mod = f.getModifiers();
				if(!Modifier.isPublic(mod) || Modifier.isStatic(mod)) { 
					throw new IllegalArgumentException(fname);
				}
				
				fs.add(f);
				
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException(e);
			}
		}
		fields = fs.toArray(new Field[0]);
	}
	
	public void addClob(int clobIdx) { 
		clobs.add(clobIdx);
	}
	
	public void close() throws SQLException { 
		stmt.close();
	}

	public void setFromObject(DBObject obj) throws SQLException { 
		for(int i = 0; i < fields.length; i++) { 
			Class type = fields[i].getType();
			Object value;
			try {
				value = fields[i].get(obj);

				if(DBObject.isSubclass(type, String.class)) {
					if(clobs.contains(i)) { 
						String str = (String)value;
						if(str == null) { 
							stmt.setAsciiStream(i+1, null);
						} else { 
							InputStream is = new ByteArrayInputStream(str.getBytes());
							stmt.setAsciiStream(i+1, is);
						}
					} else { 
						stmt.setString(i+1, (String)value);
					}

				} else if (DBObject.isSubclass(type, Integer.class)) {
					stmt.setInt(i+1, (Integer)value);

				} else if (DBObject.isSubclass(type, Double.class)) {
					stmt.setDouble(i+1, (Double)value);

				} else if (DBObject.isSubclass(type, Boolean.class)) {
					stmt.setBoolean(i+1, (Boolean)value);

				} else if (DBObject.isSubclass(type, java.util.Date.class)) {
					java.util.Date d = (java.util.Date)value;
					if(d != null) { 
						stmt.setDate(i+1, new java.sql.Date(d.getTime()));
					} else { 
						stmt.setDate(i+1, null);
					}

				}
			} catch (IllegalAccessException e) {
				e.printStackTrace(System.err);
			}
		}
	}
}
