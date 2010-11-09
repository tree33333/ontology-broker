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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
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
import org.sc.probro.data.DBObject;
import org.sc.probro.data.RequestObject;
import org.sc.probro.exceptions.BrokerException;

import tdanford.json.schema.SchemaEnv;

public abstract class SkeletonServlet extends HttpServlet {
	
	protected SchemaEnv schemaEnv;
	
	public SkeletonServlet() {
    	
    }
	
    public void init() throws ServletException {
    	super.init();
    	schemaEnv = new SchemaEnv(new File("docs/json-schemas"));
    }
	
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String CONTENT_TYPE_HTML = "text/html";
	public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
	
	public String contentTypeFromFormat(String format, String... accept) throws BrokerException {
		Set<String> acc = new TreeSet<String>();
		for(String a : accept) { if(a != null) { acc.add(a); } }
		
		if(acc.size() > 0 && !acc.contains(format)) { 
			throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, String.format(
					"Unacceptable format '%s'", format));
		}
		
		if(format.equals("html")) { 
			return CONTENT_TYPE_HTML;
		} else if (format.equals("json")) { 
			return CONTENT_TYPE_JSON;
		} else if (format.equals("fieldset")) { 
			return CONTENT_TYPE_HTML;
		}
		
		throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, String.format(
				"Unknown format '%s'", format));
	}
	
	public JSONObject getLocalJSON(HttpServletRequest req, String path) throws ServletException, IOException, JSONException, BrokerException {		
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(path);
		DummyServletResponse response = new DummyServletResponse();
		dispatcher.include(req, response);

		if(response.getStatus() != HttpServletResponse.SC_OK) { 
			throw new BrokerException(response.getStatus(), response.getValue());
		}
		
		String value = response.getValue();
		Log.info(value);
		return new JSONObject(value);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		handleException(response, new BrokerException(HttpServletResponse.SC_FORBIDDEN, "Illegal operation."));
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		handleException(response, new BrokerException(HttpServletResponse.SC_FORBIDDEN, "Illegal operation."));
	}
	
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
	
	private <T> T decode(String[] undecoded, Class<T> type) throws BrokerException { 

		try {
			String[] decoded = new String[undecoded.length];
			for(int i = 0; i < undecoded.length; i++) { 
				decoded[i] = URLDecoder.decode(undecoded[i], "UTF-8");
			}
			
			if(type.isArray()) {
				Class inner = type.getComponentType();
				int len = undecoded.length;
				Object arrayValue = Array.newInstance(inner, len);
				for(int i = 0; i < len; i++) { 
					Array.set(arrayValue, i, decode(new String[] { undecoded[i] }, inner));
				}
				return (T)arrayValue;

			} else if(DBObject.isSubclass(type, String.class)) { 
				return (T)decoded[0];

			} else if (DBObject.isSubclass(type, Integer.class)) {
				int parsed = Integer.parseInt(decoded[0]);
				return (T)(new Integer(parsed));

			} else if (DBObject.isSubclass(type, Double.class)) {
				int parsed = Integer.parseInt(decoded[0]);
				return (T)(new Integer(parsed));

			} else if (DBObject.isSubclass(type, Boolean.class)) {
				String lower = decoded[0].toLowerCase();
				if(!lower.equals("true") && !lower.equals("1")) { 
					return (T)(Boolean.FALSE);
				} else { 
					return (T)(Boolean.TRUE);						
				}

			} else if (DBObject.isSubclass(type, JSONObject.class)) {
				return (T)(new JSONObject(decoded[0]));

			} else { 
				throw new IllegalArgumentException(type.getCanonicalName());
			}
			
		} catch (UnsupportedEncodingException e) {
			throw new BrokerException(e);
		} catch (JSONException e) {
			throw new BrokerException(e);
		}

	}
	
	public <T> T getOptionalParam(HttpServletRequest req, String name, Class<T> type) throws BrokerException { 
		String[] undecoded = req.getParameterValues(name);
		
		if(undecoded != null) { 
			return decode(undecoded, type);
		} else { 
			return null;
		}
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

class DummyServletResponse implements HttpServletResponse {

	private Map<String,Set<String>> headers;
	
	private String encoding;
	private String contentType;
	private Locale locale;
	private int status;
	private StringWriter stringer;
	private PrintWriter writer;
	
	public DummyServletResponse() { 
		headers = new LinkedHashMap<String,Set<String>>();
		status = 200;
		stringer = new StringWriter();
		writer = new PrintWriter(stringer);
		encoding = "UTF-8";
		contentType = "text/html";
		locale = Locale.getDefault();
	}
	
	private class StringOutputStream extends ServletOutputStream {
		public void write(int b) throws IOException {
			stringer.append((char)b);
		} 
	}
	
	public String getValue() { return stringer.toString(); }
	
	public int getStatus() { return status; }

	public void addCookie(Cookie arg0) {
		// do nothing.
	}

	public void addDateHeader(String key, long time) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		addHeader(key, format.format(new Date(time)));
	}

	public void addHeader(String key, String value) {
		if(!(headers.containsKey(key))) { headers.put(key, new LinkedHashSet<String>()); }
		headers.get(key).add(value);
	}

	public void addIntHeader(String key, int value) {
		addHeader(key, String.valueOf(value));
	}

	public boolean containsHeader(String key) {
		return headers.containsKey(key);
	}

	public String encodeRedirectURL(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public String encodeRedirectUrl(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public String encodeURL(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public String encodeUrl(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public void sendError(int status) throws IOException {
		this.status = status;
		writer.close();
		writer = null;
	}

	public void sendError(int status, String msg) throws IOException {
		writer.println(msg);
		sendError(status);
	}

	public void sendRedirect(String url) throws IOException {
		throw new RuntimeException();
	}

	public void setDateHeader(String key, long time) {
		if(headers.containsKey(key)) { headers.get(key).clear(); }
		addDateHeader(key, time);
	}

	public void setHeader(String key, String value) {
		if(headers.containsKey(key)) { headers.get(key).clear(); }
		addHeader(key, value);
	}

	public void setIntHeader(String key, int value) {
		if(headers.containsKey(key)) { headers.get(key).clear(); }
		addIntHeader(key, value);
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setStatus(int status, String msg) {
		this.status = status;
		writer.println(msg);
	}

	public void flushBuffer() throws IOException {
		writer.flush();
	}

	public int getBufferSize() {
		return 0;
	}

	public String getCharacterEncoding() {
		return encoding;
	}

	public String getContentType() {
		return contentType;
	}

	public Locale getLocale() {
		return locale;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		return new StringOutputStream();
	}

	public PrintWriter getWriter() throws IOException {
		return writer;
	}

	public boolean isCommitted() {
		return writer == null;
	}

	public void reset() {
		status = 200;
		stringer = new StringWriter();
		writer = new PrintWriter(stringer);
	}

	public void resetBuffer() {
		// do nothing.
	}

	public void setBufferSize(int arg0) {
		throw new UnsupportedOperationException();
	}

	public void setCharacterEncoding(String arg0) {
		encoding = arg0;
	}

	public void setContentLength(int len) {
		// do nothing.
	}

	public void setContentType(String arg0) {
		contentType = arg0;
	}

	public void setLocale(Locale arg0) {
		locale = arg0;
	} 
}
