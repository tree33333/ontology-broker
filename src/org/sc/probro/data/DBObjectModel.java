package org.sc.probro.data;

import java.util.Collection;

public interface DBObjectModel {

	public <T extends DBObject> 
		Collection<T> load(Class<? extends T> cls, T template) 
		throws DBModelException;
	
	public <T extends DBObject> 
		T loadOnly(Class<? extends T> cls, T template) 
		throws DBModelException, DBObjectMissingException;
	
	public <T extends DBObject>  
		int count(Class<? extends T> cls, T template) 
		throws DBModelException;
	
	
	public void startTransaction() throws DBModelException;
	public void commitTransaction() throws DBModelException;
	public void rollbackTransaction() throws DBModelException;
	
	public <T extends DBObject> T create(Class<? extends T> cls, T obj) throws DBModelException;
	public void update(DBObject obj) throws DBModelException;
	public void delete(DBObject obj) throws DBModelException;
	
	public void close() throws DBModelException;
}
