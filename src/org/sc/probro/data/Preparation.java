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
	
	public static String[] splitArgList(String argList) { 
		String[] array = argList.split(",");
		for(int i = 0; i < array.length; i++) { array[i] = array[i].trim(); }
		return array;
	}
	
	public Preparation(PreparedStatement ps, String argList, Class<? extends DBObject> cls) { 
		this(ps, cls, splitArgList(argList));
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
		assert stmt != null : "Null Statement";
		
		for(int i = 0; i < fields.length; i++) { 
			Class type = fields[i].getType();
			Object value;
			try {
				value = fields[i].get(obj);
				
				if(DBObject.isSubclass(type, String.class)) {
					if(clobs.contains(i)) { 
						String str = (String)value;
						if(str == null) { 
							//stmt.setAsciiStream(i+1, null);
							stmt.setNull(i+1, java.sql.Types.CLOB);
						} else { 
							InputStream is = new ByteArrayInputStream(str.getBytes());
							stmt.setAsciiStream(i+1, is);
						}
					} else { 
						if(value == null) { 
							stmt.setNull(i+1, java.sql.Types.VARCHAR);
						} else { 
							stmt.setString(i+1, (String)value);
						}
					}

				} else if (DBObject.isSubclass(type, Integer.class)) {
					Integer ivalue = (Integer)value;
					if(ivalue == null) { 
						stmt.setNull(i+1, java.sql.Types.INTEGER);
					} else { 
						stmt.setInt(i+1, ivalue);
					}

				} else if (DBObject.isSubclass(type, Double.class)) {
					if(value == null) { 
						stmt.setNull(i+1, java.sql.Types.DOUBLE);
					} else { 
						stmt.setDouble(i+1, (Double)value);
					}

				} else if (DBObject.isSubclass(type, Boolean.class)) {
					if(value == null) { 
						stmt.setNull(i+1, java.sql.Types.BOOLEAN);
					} else { 
						stmt.setBoolean(i+1, (Boolean)value);
					}

				} else if (DBObject.isSubclass(type, java.util.Date.class)) {
					java.util.Date d = (java.util.Date)value;
					if(d==null) { 
						stmt.setNull(i+1, java.sql.Types.DATE);
					} else { 
						stmt.setDate(i+1, new java.sql.Date(d.getTime()));
					}

				}
			} catch (IllegalAccessException e) {
				e.printStackTrace(System.err);
			}
		}
	}
}
