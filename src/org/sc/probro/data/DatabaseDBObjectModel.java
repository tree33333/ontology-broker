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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.sql.DataSource;

import org.eclipse.jetty.util.log.Log;

public class DatabaseDBObjectModel implements DBObjectModel {
	
	private Connection cxn;
	
	public DatabaseDBObjectModel(DataSource source) throws DBModelException { 
		try {
			cxn = source.getConnection();
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}
	
	public Connection getConnection() { 
		return cxn;
	}

	public <T extends DBObject> int count(Class<? extends T> cls, T template) throws DBModelException {
		try {
			String tableName = template.getTableName();
			Preparation prep = new Preparation(
					cxn.prepareStatement(String.format(
							"select count(*) from %s%s",
							tableName,
							template.prepareSQLTemplateWhereClause())),
					template.getTemplateFields());
			try { 
				prep.setFromObject(template);
				ResultSet rs = prep.stmt.executeQuery();
				try { 
					if(!rs.next()) { throw new DBModelException("No 'count' returned."); }
					int count = rs.getInt(1);
					return count;
					
				} finally { 
					rs.close();
				}
				
			} finally { 
				prep.close();
			}
			
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public <T extends DBObject> Collection<T> load(Class<? extends T> cls, T template)
	throws DBModelException {
		return load(cls, template, null, null);
	}
	
	public <T extends DBObject> Collection<T> load(Class<? extends T> cls, T template, String order, Integer limit)
		throws DBModelException {
		try {
			String tableName = template.getTableName();
			Constructor<T> constructor = (Constructor<T>)template.getClass().getConstructor();
			String queryString = String.format(
					"select * from %s%s",
					tableName,
					template.prepareSQLTemplateWhereClause());
			
			if(order != null) { queryString += " ORDER BY " + order; }
			if(limit != null) { queryString += " LIMIT " + limit; }
			
			Log.info(queryString);
			
			Preparation prep = new Preparation(
					cxn.prepareStatement(queryString),
					template.getTemplateFields());
			try {
				prep.setFromObject(template);
				ResultSet rs = prep.stmt.executeQuery();
				try {
					LinkedList<T> returned = new LinkedList<T>();
					while(rs.next()) { 
						T newInst = constructor.newInstance();
						newInst.setFromResultSet(rs);
						returned.add(newInst);
					}
					return returned;
					
				} finally { 
					rs.close();
				}
				
			} finally { 
				prep.close();
			}
			
		} catch (InstantiationException e) {
			throw new DBModelException(e);
		} catch (IllegalAccessException e) {
			throw new DBModelException(e);
		} catch (InvocationTargetException e) {
			throw new DBModelException(e);
		} catch (NoSuchMethodException e) {
			throw new DBModelException(e);
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public <T extends DBObject> T loadOnly(Class<? extends T> cls, T template) throws DBModelException, DBObjectMissingException {
		try {
			String tableName = template.getTableName();
			Constructor<T> constructor = (Constructor<T>)template.getClass().getConstructor();
			String queryString = String.format(
					"select * from %s%s", tableName, template.prepareSQLTemplateWhereClause());
			
			Log.info(queryString);
			
			Preparation prep = new Preparation(
					cxn.prepareStatement(queryString),
					template.getTemplateFields());
			try {
				prep.setFromObject(template);
				ResultSet rs = prep.stmt.executeQuery();
				try {
					T returned = null;
					if(rs.next()) { 
						returned = constructor.newInstance();
						returned.setFromResultSet(rs);
					} else { 
						throw new DBObjectMissingException(String.format(
								"No matching %s found: \"%s\"", cls.getSimpleName(), queryString));
					}
					return returned;
					
				} finally { 
					rs.close();
				}
				
			} finally { 
				prep.close();
			}
			
		} catch (InstantiationException e) {
			throw new DBModelException(e);
		} catch (IllegalAccessException e) {
			throw new DBModelException(e);
		} catch (InvocationTargetException e) {
			throw new DBModelException(e);
		} catch (NoSuchMethodException e) {
			throw new DBModelException(e);
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public void rollbackTransaction() throws DBModelException {
		try {
			cxn.rollback();
			cxn.setAutoCommit(true);
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public void startTransaction() throws DBModelException {
		try {
			cxn.setAutoCommit(false);
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public void commitTransaction() throws DBModelException {
		try {
			cxn.commit();
			cxn.setAutoCommit(true);
			
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public <T extends DBObject> void create(Class<? extends T> cls, T obj) throws DBModelException {
		try {
			Constructor constructor = cls.getConstructor();
			String tableName = obj.getTableName();
			String[] autoGen = obj.getAutoGeneratedFields();
			
			String insertStatement = String.format(
					"insert into %s (%s) values (%s)",
					tableName, 
					obj.prepareSQLInsertFieldsClause(),
					obj.prepareSQLInsertValuesClause()); 
			
			Log.info(insertStatement);
			Log.info(obj.toString());
			
			Preparation prep = new Preparation(
					cxn.prepareStatement(insertStatement, 
							Statement.RETURN_GENERATED_KEYS),
					obj.getAllFields(autoGen));
			
			try {
				prep.setFromObject(obj);
				prep.stmt.executeUpdate();

				ResultSet rs = prep.stmt.getGeneratedKeys();
				try {
					if(rs.next()) { 
						for(int i = 0; i < autoGen.length; i++) { 
							Field f = cls.getField(autoGen[i]);
							
							//Object keyValue = rs.getObject(i+1);
							Integer keyValue = rs.getInt(i+1);
							
							f.set(obj, keyValue);
						}
						
					} else { 
						throw new DBModelException("No object created.");
					}
					
				} finally { 
					rs.close();
				}
				
			} finally { 
				prep.close();
			}
			
		} catch (NoSuchFieldException e) {
			throw new DBModelException(e);
		} catch (IllegalAccessException e) {
			throw new DBModelException(e);
		} catch (NoSuchMethodException e) {
			throw new DBModelException(e);
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public void delete(DBObject obj) throws DBModelException {
		throw new UnsupportedOperationException("No 'delete' supported yet.");
	}

	public void update(DBObject obj) throws DBModelException {
		try {
			String tableName = obj.getTableName();
			String[] autoGen = obj.getKeyFields();
			
			StringBuilder autoGenSelector = new StringBuilder();
			for(String ag : autoGen) { 
				if(autoGenSelector.length() > 0) { autoGenSelector.append(" AND "); }
				autoGenSelector.append(String.format("%s=?", ag));
			}
			
			ArrayList<Field> fields = new ArrayList<Field>(); 
			
			for(Field f : obj.getAllFields(autoGen)) { 
				fields.add(f);
			}
			
			for(String agName : autoGen) { 
				fields.add(obj.getClass().getField(agName));
			}
			
			String updateStatement = String.format(
					"update %s set %s where %s",
					tableName, 
					obj.prepareSQLUpdateFieldsClause(),
					autoGenSelector.toString()); 
			
			Log.info(updateStatement);
			
			Preparation prep = new Preparation(
					cxn.prepareStatement(updateStatement), 
					fields.toArray(new Field[0]));
			
			try {
				prep.setFromObject(obj);
				if(prep.stmt.executeUpdate() != 1) { 
					throw new DBModelException("UPDATE failed.");
				}

			} finally { 
				prep.close();
			}
			
		} catch (NoSuchFieldException e) {
			throw new DBModelException(e);
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

	public void close() throws DBModelException {
		try {
			cxn.close();
			cxn=null;
		} catch (SQLException e) {
			throw new DBModelException(e);
		}
	}

}
