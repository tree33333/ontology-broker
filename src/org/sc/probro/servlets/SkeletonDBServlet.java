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
package org.sc.probro.servlets;

import java.util.*;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.sc.probro.BrokerProperties;
import org.sc.probro.BrokerStart;
import org.sc.probro.data.BrokerModel;
import org.sc.probro.data.DBModelException;
import org.sc.probro.data.DBObjectModel;
import org.sc.probro.data.DatabaseDBObjectModel;
import org.sc.probro.data.RequestObject;

public abstract class SkeletonDBServlet extends SkeletonServlet {
	
	protected DataSource dbSource = null;
	private String dbPath;

	public SkeletonDBServlet(BrokerProperties props) {
    	dbPath = props.getDBPath();
    }
    
    public void init() throws ServletException {
    	super.init();
    	if(dbSource == null) {
    		EmbeddedDataSource derbySource = new EmbeddedDataSource();
    		derbySource.setDatabaseName(dbPath);
    		
    		dbSource = derbySource;
    	}
    }
    
    public DBObjectModel getDBObjectModel() throws DBModelException { 
    	return new DatabaseDBObjectModel(dbSource);
    }
    
    public BrokerModel getBrokerModel() throws DBModelException { 
    	return new BrokerModel(BrokerStart.getURLPrefix(), getDBObjectModel());
    }
}
