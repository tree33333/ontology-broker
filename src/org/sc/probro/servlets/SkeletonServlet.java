package org.sc.probro.servlets;

import java.util.*;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
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
	
	public static Map<Integer,String> ERROR_NAMES;

	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String CONTENT_TYPE_HTML = "text/html";
	
	static { 
		ERROR_NAMES = new TreeMap<Integer,String>();
		ERROR_NAMES.put(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
		ERROR_NAMES.put(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
		ERROR_NAMES.put(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
		ERROR_NAMES.put(HttpServletResponse.SC_GONE, "Gone");
		ERROR_NAMES.put(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request Entity Too Large");
		ERROR_NAMES.put(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
		ERROR_NAMES.put(HttpServletResponse.SC_CONFLICT, "Conflict");
		ERROR_NAMES.put(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
		ERROR_NAMES.put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
	}
	
	public static void raiseInternalError(HttpServletResponse response, Throwable t) throws IOException { 
		StringWriter stringer = new StringWriter();
		JSONWriter writer = new JSONWriter(stringer);

		int errorCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		int httpStatusCode = errorCode;

		try {
			String msg = t.getMessage();
			
			writer
				.object()
				.key("error_code").value(errorCode)
				.key("error_name").value(ERROR_NAMES.get(errorCode))
				.key("error_description").value(msg)
				.endObject();

			String errorMessage = stringer.toString();
			Log.warn(t);
			Log.warn(errorMessage);
			response.sendError(httpStatusCode, errorMessage);			
			
		} catch (JSONException e) {
			Log.warn(e);
			Log.warn(t);
			response.sendError(httpStatusCode, t.getMessage());
		}		
	}
	
	public static void raiseInternalError(HttpServletResponse response, String msg) throws IOException { 
		raiseException(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
	}

	public static void raiseException(HttpServletResponse response, int errorCode, String msg) throws IOException {
		raiseException(response, errorCode, errorCode, msg);
	}

	public static void raiseException(HttpServletResponse response, int httpStatusCode, int errorCode, String msg) throws IOException {
		StringWriter stringer = new StringWriter();
		JSONWriter writer = new JSONWriter(stringer);
		
		try {
			writer
				.object()
				.key("error_code").value(errorCode)
				.key("error_name").value("")
				.key("error_description").value(msg)
				.endObject();

			String errorMessage = stringer.toString();
			Log.warn(errorMessage);
			response.sendError(httpStatusCode, errorMessage);			
			
		} catch (JSONException e) {
			Log.warn(e);
			Log.warn(msg);
			response.sendError(httpStatusCode, msg);
		}
	}
	
	public static String decodeResponseType(HttpServletRequest request, String defaultType) { 
		return decodeResponseType(decodedParams(request), defaultType);
	}
	
	public static String decodeResponseType(Map<String,String[]> params, String defaultType) { 
    	String contentType = defaultType;

    	if(params.containsKey("format")) {
    		String format = params.get("format")[0].toLowerCase();
    	
    		if(format.equals("html")) { 
    			contentType = CONTENT_TYPE_HTML;

    		} else if (format.equals("json")) { 
    			contentType = CONTENT_TYPE_JSON;
        		
    		} else { 
    			return null;
    		}
    	}
    	
    	return contentType;
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
