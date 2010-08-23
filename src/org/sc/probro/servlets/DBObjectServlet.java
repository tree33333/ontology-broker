package org.sc.probro.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.io.StringWriter;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

import java.sql.*;
import java.util.regex.*;
import java.util.*;

public class DBObjectServlet<T extends DBObject> extends SkeletonDBServlet {
	
	private Class<T> objectClass;
	private String tableName;
	private Constructor<T> blankConstructor, resultConstructor;
	private Field idField;
	
	public DBObjectServlet(BrokerProperties ps, Class<T> cls, String idFieldName) {
		super(ps);
		objectClass = cls;
		try {
			blankConstructor = objectClass.getConstructor();
			resultConstructor = objectClass.getConstructor(ResultSet.class);
			tableName = (objectClass.getSimpleName() + "S").toUpperCase();
			idField = objectClass.getField(idFieldName);
			
		} catch (NoSuchMethodException e) {
			e.printStackTrace(System.err);
			throw new IllegalArgumentException(cls.getCanonicalName());
		} catch (NoSuchFieldException e) {
			e.printStackTrace(System.err);
			throw new IllegalArgumentException(cls.getCanonicalName());
		}
	}
	
	private boolean update(T req, HttpServletResponse response) throws IOException { 
		try {
			Connection cxn = dbSource.getConnection();
			try { 
				Statement stmt = cxn.createStatement();
				try {
					stmt.executeUpdate(req.saveString());
					return true;
					
				} finally { 
					stmt.close();
				}
				
			} finally { 
				cxn.close();
			}
		
		} catch (SQLException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}		

		return false;
	}

	private T load(T template, HttpServletResponse response) throws IOException { 
		try {
			Connection cxn = dbSource.getConnection();
			try { 
				Statement stmt = cxn.createStatement();
				try {
					String query = String.format(template.queryString());
					ResultSet rs = stmt.executeQuery(query);
					try { 
						if(rs.next()) { 
							return resultConstructor.newInstance(rs);
						} else { 
							response.sendError(HttpServletResponse.SC_BAD_REQUEST, template.toString());
							return null;
						}
						
					} catch (InstantiationException e) {
						e.printStackTrace(System.err);
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
						return null;

					} catch (IllegalAccessException e) {
						e.printStackTrace(System.err);
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
						return null;

					} catch (InvocationTargetException e) {
						e.printStackTrace(System.err);
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
						return null;

					} finally { 
						rs.close();
					}
					
				} finally { 
					stmt.close();
				}
				
			} finally { 
				cxn.close();
			}
		
		} catch (SQLException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return null;
		}		
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String responseType = "application/json";
		
		if(request.getParameter("format") != null) { 
			String responseParameter = request.getParameter("format").toLowerCase();
			if(responseParameter.equals("html")) { 
				responseType = "text/html";
			}
		}
		
		int requestID = -1;
		String path = request.getRequestURI();
		Pattern p = Pattern.compile("^/[^/]+/(\\d+)[^\\d]?.*$");
		Matcher m = p.matcher(path);
		if(!m.matches()) { 
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, path);
			return;
		}
		requestID = Integer.parseInt(m.group(1));

		T template = null;
		try {
			template = blankConstructor.newInstance();
			idField.set(template, requestID);

		} catch (InstantiationException e1) {
			e1.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
			return;
		} catch (IllegalAccessException e1) {
			e1.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
			return;
		} catch (InvocationTargetException e1) {
			e1.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
			return;
		}

		T loaded = load(template, response);
		
		if(loaded != null) { 
			if(responseType.equals("application/json")) {
				StringWriter stringer = new StringWriter();
				JSONWriter writer = new JSONWriter(stringer);
				
				try {
					loaded.writeJSONObject(writer);
										
					response.setContentType(responseType);
					response.setStatus(HttpServletResponse.SC_OK);
					response.getWriter().println(stringer.toString());
					
				} catch (JSONException e) {
					e.printStackTrace(System.err);
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
							e.getMessage());
					return;
				}
				
			} else if (responseType.equals("text/html")) { 

				response.setContentType(responseType);
				response.setStatus(HttpServletResponse.SC_OK);
				
				PrintWriter printer = response.getWriter();
				printer.println(loaded.writeHTMLObject(false));
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		int requestID = -1;
		String path = request.getRequestURI();
		Pattern p = Pattern.compile("^/[^/]+/(\\d+)[^\\d]?.*$");
		Matcher m = p.matcher(path);
		if(!m.matches()) { 
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, path);
			return;
		}
		requestID = Integer.parseInt(m.group(1));
		
		String update = request.getParameter("update");
		if(update != null) {
			update = URLDecoder.decode(update, "UTF-8");
			try {
				JSONObject obj = new JSONObject(update);
				Iterator<String> keyItr = obj.keys();
				T req = blankConstructor.newInstance();
				
				while(keyItr.hasNext()) { 
					String key = keyItr.next();
					Field f = Request.class.getField(key);
					int mod = f.getModifiers();
					if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
						f.set(req, obj.get(key));
					}
				}
				
				if(update(req, response)) { 
					response.sendRedirect(path);
				}
				
			} catch (JSONException e) {
				e.printStackTrace(System.err);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				return;

			} catch (NoSuchFieldException e) {
				e.printStackTrace(System.err);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				return;

			} catch (IllegalAccessException e) {
				e.printStackTrace(System.err);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				return;

			} catch (InstantiationException e) {
				e.printStackTrace(System.err);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				return;
				
			} catch (InvocationTargetException e) {
				e.printStackTrace(System.err);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				return;
			}
			
		} else { 
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'update' parameter sent.");
			return;
		}
	}

}