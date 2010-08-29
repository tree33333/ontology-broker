package org.sc.probro;

import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.json.*;

public class BrokerException extends Exception {
	
	public static Map<Integer,String> ERROR_NAMES; 
	
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
	
	private int code; 
	private String name, description;
	private Throwable throwable;
	
	public BrokerException(Throwable t) { 
		this(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t);
	}

	public BrokerException(int code, String description) { 
		super(description);
		this.code = code;
		name = ERROR_NAMES.get(code);
		this.description = description;
		throwable = null;
	}
	
	public BrokerException(int code, Throwable t) { 
		super(t.getMessage(), t);
		this.code = code;
		this.description = t.getMessage();
		this.name = ERROR_NAMES.get(code);
		throwable = t;
	}
	
	public int getCode() { return code; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	
	public Throwable getThrowable() { return throwable; }
	
	public boolean isFromThrowable() { return throwable != null;  }
	
	public boolean isServerError() { 
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR == code;
	}
	
	public BrokerException format(Object... args) { 
		return new BrokerException(code, String.format(description, args));
	}

	public JSONObject asJSON() { 
		JSONObject obj = new JSONObject();
		try { 
			obj.put("error_code", code);
			obj.put("error_name", name);
			obj.put("error_description", description);
		} catch(JSONException e) { 
			throw new IllegalStateException(e);
		}
		
		return obj;
	}
}
