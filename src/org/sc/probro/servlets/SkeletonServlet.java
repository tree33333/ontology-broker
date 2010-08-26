package org.sc.probro.servlets;

import java.util.*;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.sc.probro.data.Request;

public abstract class SkeletonServlet extends HttpServlet {
	
	public SkeletonServlet() {
    	
    }
 
	public static Map<String,String[]> decodedParams(HttpServletRequest request) { 
		Map<String,ArrayList<String>> pmap = new LinkedHashMap<String,ArrayList<String>>();
		
		Enumeration paramEnum = request.getParameterNames();
		while(paramEnum.hasMoreElements()) { 
			String paramName = (String)paramEnum.nextElement();
			if(!pmap.containsKey(paramName)) { pmap.put(paramName, new ArrayList<String>()); }
			try {
				String param = URLDecoder.decode(request.getParameter(paramName), "UTF-8");
				pmap.get(paramName).add(param);
			} catch (UnsupportedEncodingException e) {
				Log.warn(e);
				throw new IllegalArgumentException(paramName);
			}
		}
		
		Map<String,String[]> params = new LinkedHashMap<String,String[]>();
		for(String key : pmap.keySet()) { 
			params.put(key, pmap.get(key).toArray(new String[0]));
		}
		return params;
	}
}
