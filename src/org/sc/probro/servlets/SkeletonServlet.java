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
import org.sc.probro.BrokerException;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.Request;

public abstract class SkeletonServlet extends HttpServlet {
	
	public SkeletonServlet() {
    	
    }
	
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String CONTENT_TYPE_HTML = "text/html";
	
	public void handleException(HttpServletResponse response, BrokerException e) throws IOException {
		if(e.isFromThrowable()) { 
			Log.warn(e.getThrowable());
		}
		Log.warn(e.getDescription());
		response.sendError(e.getCode(), e.asJSON().toString());
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
				.key("error_name").value(BrokerException.ERROR_NAMES.get(errorCode))
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
	
	public <T> T getRequiredParam(HttpServletRequest req, String name, Class<T> type) throws BrokerException { 
		T value = getOptionalParam(req, name, type);
		
		if(value == null) { 
			throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, 
					String.format("Missing required parameter: %s", name));
		}
		
		return value;
	}
	
	public <T> T getOptionalParam(HttpServletRequest req, String name, Class<T> type) throws BrokerException { 
		T param = null;
		
		String undecoded = req.getParameter(name);
		if(undecoded != null) { 
			try {
				String decoded = URLDecoder.decode(undecoded, "UTF-8");
				
				if(DBObject.isSubclass(type, String.class)) { 
					return (T)decoded;
					
				} else if (DBObject.isSubclass(type, Integer.class)) {
					int parsed = Integer.parseInt(decoded);
					return (T)(new Integer(parsed));

				} else if (DBObject.isSubclass(type, Double.class)) {
					int parsed = Integer.parseInt(decoded);
					return (T)(new Integer(parsed));

				} else if (DBObject.isSubclass(type, Boolean.class)) {
					String lower = decoded.toLowerCase();
					if(!lower.equals("true") && !lower.equals("1")) { 
						return (T)(Boolean.FALSE);
					} else { 
						return (T)(Boolean.TRUE);						
					}
				
				} else if (DBObject.isSubclass(type, JSONObject.class)) {
					return (T)(new JSONObject(decoded));
					
				} else { 
					throw new IllegalArgumentException(type.getCanonicalName());
				}
			
			} catch (UnsupportedEncodingException e) {
				throw new BrokerException(e);
				
			} catch(NumberFormatException e) { 
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, e);
				
			} catch (JSONException e) {
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, e);
			}
		}
		
		return param;
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
