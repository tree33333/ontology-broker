package org.sc.probro.servlets;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONWriter;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.Request;

/**
 * Converted to using the Jetty Logging system.
 * 
 * @author tdanford
 *
 * @param <T>
 */
public class DBObjectListServlet<T extends DBObject> extends SkeletonDBServlet {
	
	private Class<T> objectClass;
	private Field keyField;
	private Constructor<T> blankConstructor, resultConstructor;
	private String tableName;
	
    public DBObjectListServlet(BrokerProperties ps, Class<T> cls) {
    	super(ps);
    	objectClass = cls;
    	try {
			blankConstructor = objectClass.getConstructor();
			resultConstructor = objectClass.getConstructor(ResultSet.class);
			tableName = objectClass.getSimpleName().toUpperCase() + "S";
			
			T template = blankConstructor.newInstance();
			String keyName = template.getKey();
			if(keyName != null) { 
				keyField = objectClass.getField(keyName);
			} else { 
				keyField = null;
			}
			
		} catch (NoSuchMethodException e) {
			Log.warn(e);
		} catch (InstantiationException e) {
			Log.warn(e);
		} catch (IllegalAccessException e) {
			Log.warn(e);
		} catch (InvocationTargetException e) {
			Log.warn(e);
		} catch (NoSuchFieldException e) {
			Log.warn(e);
		}
    }

    /* 
     * Adds a new Request item.
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringWriter stringer = new StringWriter();
        JSONWriter json = new JSONWriter(stringer);
        
        try {
			Map paramMap = request.getParameterMap();

			DBObject obj = blankConstructor.newInstance();
			
			json.object();
			StringBuilder info = new StringBuilder();
			
			for(Object nameobj : paramMap.keySet()) {
				String name = nameobj.toString();
				Object valueObj = paramMap.get(name);
				
				String value = 
					valueObj.getClass().isArray() ? 
					Array.get(valueObj, 0).toString() : valueObj.toString();

				info.append(String.format("%s=%s ", name, String.valueOf(value)));

				json.key(name).value(value);
				obj.setFromString(name, value);
			}
			Log.debug(info.toString());

	        json.endObject();
	        
	        Connection cxn = dbSource.getConnection();
	        Statement stmt = cxn.createStatement();
	        String insertString = obj.insertString();
	        
	        //System.out.println(insertString);
	        Log.debug(insertString);
	        
	        stmt.executeUpdate(insertString);
	        stmt.close();
	        cxn.close();

	        Log.debug(stringer.toString());
	        
	        response.setStatus(HttpServletResponse.SC_OK);
	        response.setContentType("text");
	        response.getWriter().println(stringer.toString());
	        
        } catch (JSONException e) {
			raiseInternalError(response, e);
			return;

        } catch (SQLException e) {
			raiseInternalError(response, e);
			return;

        } catch (InstantiationException e) {
			raiseInternalError(response, e);
			return;
		
        } catch (IllegalAccessException e) {
			raiseInternalError(response, e);
			return;

		} catch (InvocationTargetException e) {
			raiseInternalError(response, e);
			return;
		}
    }
    
    private static Set<String> SUPPORTED_CONTENT_TYPES =
    	new TreeSet<String>(Arrays.asList(CONTENT_TYPE_HTML, CONTENT_TYPE_JSON));

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	Map<String,String[]> params = decodedParams(request);
    	
    	String contentType = decodeResponseType(params, CONTENT_TYPE_JSON);
    	if(contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType)) { 
			String msg = String.format("format %s not supported", params.get("format")[0]);
			Log.warn(msg);
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, msg);
			return;    		
    	}

        StringWriter stringer = new StringWriter();
        JSONWriter json = new JSONWriter(stringer);

        try {
        	Connection cxn = dbSource.getConnection();
        	Statement stmt = cxn.createStatement();
        	
        	T obj = blankConstructor.newInstance();
        	
        	Enumeration paramNames = request.getParameterNames();
        	StringBuilder restriction = new StringBuilder();
        	while(paramNames.hasMoreElements()) { 
        		String paramName = (String)paramNames.nextElement();
        		try {
					Field paramField = objectClass.getField(paramName);
					int mod = paramField.getModifiers();
					if(paramField != null && Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
						String value = URLDecoder.decode(request.getParameter(paramName), "UTF-8");
						obj.setFromString(paramName, value);
					}

        		} catch (NoSuchFieldException e) {
        			// do nothing.
        			Log.debug(e);
				}
        	}

        	String query = obj.queryString();
        	
        	//System.out.println(String.format("query: %s", query));
        	Log.debug(String.format("query: %s", query));
        	
        	ResultSet rs = stmt.executeQuery(query);
        	
        	if(contentType.equals(CONTENT_TYPE_JSON)) { 
        		if(keyField != null) {
        			json.object();
        		} else { 
        			json.array();
        		}
        	
        	} else if(contentType.equals(CONTENT_TYPE_HTML)) { 
        		stringer.write("<table>\n");
        		stringer.write(obj.writeHTMLRowHeader());
        		stringer.write("\n");
        	}
			
			while(rs.next()) {
				T res = resultConstructor.newInstance(rs);

				if(contentType.equals(CONTENT_TYPE_JSON)) {
					
					if(keyField != null) {
						String keyValue = String.valueOf(keyField.get(res));
						json.key(keyValue);
					}
					
					res.writeJSONObject(json);
				
				} else if (contentType.equals(CONTENT_TYPE_HTML)) { 
					stringer.write(res.writeHTMLObject(true));
	        		stringer.write("\n");
				}
			}
				
        	if(contentType.equals(CONTENT_TYPE_JSON)) { 
        		if(keyField != null) {
        			json.endObject();
        		} else { 
        			json.endArray();
        		}
        	
        	} else if(contentType.equals(CONTENT_TYPE_HTML)) { 
        		stringer.write("</table>");
        	}
			
			rs.close();
			stmt.close();
			cxn.close();

	    	response.setContentType(contentType);
	        response.setStatus(HttpServletResponse.SC_OK);

	        Log.debug(stringer.toString());
	        response.getWriter().println(stringer.toString());

        } catch (JSONException e) {
         	raiseInternalError(response, e);
         	return;
		} catch (SQLException e) {
         	raiseInternalError(response, e);
			return;
		} catch (InstantiationException e) {
         	raiseInternalError(response, e);
			return;
		} catch (IllegalAccessException e) {
         	raiseInternalError(response, e);
			return;
		} catch (InvocationTargetException e) {
         	raiseInternalError(response, e);
			return;
		}
    }
}
