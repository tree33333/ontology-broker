package org.sc.probro.servlets;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.json.JSONException;
import org.json.JSONWriter;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.Request;

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
			e.printStackTrace(System.err);
		} catch (InstantiationException e) {
			e.printStackTrace(System.err);
		} catch (IllegalAccessException e) {
			e.printStackTrace(System.err);
		} catch (InvocationTargetException e) {
			e.printStackTrace(System.err);
		} catch (NoSuchFieldException e) {
			e.printStackTrace(System.err);
		}
    }

    /* 
     * Adds a new Request item.
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringWriter stringer = new StringWriter();
        JSONWriter json = new JSONWriter(stringer);
        System.out.println("doPost()");
        
        try {
			Map paramMap = request.getParameterMap();

			DBObject obj = blankConstructor.newInstance();
			
			json.object();
			
			for(Object nameobj : paramMap.keySet()) {
				String name = nameobj.toString();
				Object valueObj = paramMap.get(name);
				
				String value = 
					valueObj.getClass().isArray() ? 
					Array.get(valueObj, 0).toString() : valueObj.toString();

				System.out.println(String.format("\t%s: %s", name, String.valueOf(value)));

				json.key(name).value(value);
				obj.setFromString(name, value);
			}

	        json.endObject();
	        
	        Connection cxn = dbSource.getConnection();
	        Statement stmt = cxn.createStatement();
	        String insertString = obj.insertString();
	        System.out.println(insertString);
	        stmt.executeUpdate(insertString);
	        stmt.close();
	        cxn.close();

	        System.out.println(stringer.toString());
	        response.setStatus(HttpServletResponse.SC_OK);
	        response.setContentType("text");
	        response.getWriter().println(stringer.toString());
	        
        } catch (JSONException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;

        } catch (SQLException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;

        } catch (InstantiationException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		
        } catch (IllegalAccessException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;

		} catch (InvocationTargetException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		//response.sendRedirect("/");

    }
    
    private static String TYPE_JSON = "application/json";
    private static String TYPE_HTML = "text/html";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	String contentType = TYPE_JSON;
    	String type = request.getParameter("format");
    	if(type != null && URLDecoder.decode(type, "UTF-8").toLowerCase().equals("html")) { 
    		contentType = TYPE_HTML;
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
				}
        	}

        	String query = obj.queryString();
        	System.out.println(String.format("query: %s", query));
        	ResultSet rs = stmt.executeQuery(query);
        	
        	if(contentType.equals(TYPE_JSON)) { 
        		if(keyField != null) {
        			json.object();
        		} else { 
        			json.array();
        		}
        	
        	} else if(contentType.equals(TYPE_HTML)) { 
        		stringer.write("<table>\n");
        		stringer.write(obj.writeHTMLRowHeader());
        		stringer.write("\n");
        	}
			
			while(rs.next()) {
				T res = resultConstructor.newInstance(rs);

				if(contentType.equals(TYPE_JSON)) {
					
					if(keyField != null) {
						String keyValue = String.valueOf(keyField.get(res));
						json.key(keyValue);
					}
					
					res.writeJSONObject(json);
				
				} else if (contentType.equals(TYPE_HTML)) { 
					stringer.write(res.writeHTMLObject(true));
	        		stringer.write("\n");
				}
			}
				
        	if(contentType.equals(TYPE_JSON)) { 
        		if(keyField != null) {
        			json.endObject();
        		} else { 
        			json.endArray();
        		}
        	
        	} else if(contentType.equals(TYPE_HTML)) { 
        		stringer.write("</table>");
        	}
			
			rs.close();
			stmt.close();
			cxn.close();

	    	response.setContentType(contentType);
	        response.setStatus(HttpServletResponse.SC_OK);
	        response.getWriter().println(stringer.toString());

        } catch (JSONException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		} catch (SQLException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		} catch (InstantiationException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		} catch (IllegalAccessException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		} catch (InvocationTargetException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
    }
}
