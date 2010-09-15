package org.sc.probro.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.io.StringWriter;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sc.probro.BrokerException;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.DBModelException;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.DBObjectMissingException;
import org.sc.probro.data.DBObjectModel;
import org.sc.probro.data.MetadataObject;
import org.sc.probro.data.RequestObject;

import java.sql.*;
import java.util.regex.*;
import java.util.*;

/**
 * Updated to use DBObjectModel.
 * 
 * @author tdanford
 *
 * @param <T>
 */
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
			
			Log.info(String.format("DBObjectServlet started: %s", cls.getSimpleName()));
			
		} catch (NoSuchMethodException e) {
			Log.warn(e);
			throw new IllegalArgumentException(cls.getCanonicalName());
		} catch (NoSuchFieldException e) {
			Log.warn(e);
			throw new IllegalArgumentException(cls.getCanonicalName());
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

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

				DBObjectModel model = getDBObjectModel();
				try { 
					T loaded = model.loadOnly(objectClass, template);

					if(responseType.equals("application/json")) {
						StringWriter stringer = new StringWriter();
						JSONWriter writer = new JSONWriter(stringer);

						loaded.writeJSONObject(writer);

						response.setContentType(responseType);
						response.setStatus(HttpServletResponse.SC_OK);
						response.getWriter().println(stringer.toString());


					} else if (responseType.equals("text/html")) { 

						response.setContentType(responseType);
						response.setStatus(HttpServletResponse.SC_OK);

						PrintWriter printer = response.getWriter();
						printer.println(loaded.writeHTMLObject(false));
					}
				} finally { 
					model.close();
				}

			} catch (InstantiationException e1) {
				throw new BrokerException(e1);
			} catch (IllegalAccessException e1) {
				throw new BrokerException(e1);
			} catch (InvocationTargetException e1) {
				throw new BrokerException(e1);
			} catch (JSONException e) {
				throw new BrokerException(e);
			} catch (DBModelException e) {
				throw new BrokerException(e);
			} catch (DBObjectMissingException e) {
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			}
		} catch(BrokerException e) { 
			handleException(response, e);
			return;
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try { 
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
						Field f = RequestObject.class.getField(key);
						int mod = f.getModifiers();
						if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
							f.set(req, obj.get(key));
						}
					}
					
					DBObjectModel model = getDBObjectModel();
					try { 
						model.update(req);
					} finally { 
						model.close();
					}

					response.sendRedirect(path);

				} catch (JSONException e) {
					throw new BrokerException(e);
				} catch (NoSuchFieldException e) {
					throw new BrokerException(e);
				} catch (IllegalAccessException e) {
					throw new BrokerException(e);
				} catch (InstantiationException e) {
					throw new BrokerException(e);
				} catch (InvocationTargetException e) {
					throw new BrokerException(e);
				} catch (DBModelException e) {
					throw new BrokerException(e);
				}

			} else { 
				String msg = "No 'update' paramter sent.";
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, msg);
			}
			
			
		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}

}
