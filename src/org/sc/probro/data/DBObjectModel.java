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

import java.util.Collection;

public interface DBObjectModel {

	public <T extends DBObject> 
	Collection<T> load(Class<? extends T> cls, T template) 
	throws DBModelException;

	public <T extends DBObject> 
		Collection<T> load(Class<? extends T> cls, T template, String order, Integer limit) 
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
	
	public <T extends DBObject> void create(Class<? extends T> cls, T obj) throws DBModelException;
	public void update(DBObject obj) throws DBModelException;
	public void delete(DBObject obj) throws DBModelException;
	
	public void close() throws DBModelException;
}
