package org.sc.probro.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.*;
import java.io.StringWriter;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sc.probro.BrokerException;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.BrokerModel;
import org.sc.probro.data.DBModelException;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.DBObjectMissingException;
import org.sc.probro.data.DBObjectModel;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.ProvisionalTerm;
import org.sc.probro.data.Request;
import org.sc.probro.data.StatusUpdate;

import java.sql.*;
import java.util.regex.*;
import java.util.*;

public class RequestServlet extends SkeletonDBServlet {
	
	public RequestServlet(BrokerProperties ps) { 
		super(ps);
	}
	
	public static String requestURLPattern = "^/request/([\\da-z]+)[^\\da-z]?.*$"; 

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String responseType = "application/json";
		try { 

			try { 
				if(request.getParameter("format") != null) { 
					String responseParameter = request.getParameter("format").toLowerCase();
					if(responseParameter.equals("html")) { 
						responseType = "text/html";
					}
				}

				String path = request.getRequestURI();
				Pattern p = Pattern.compile(requestURLPattern);
				Matcher m = p.matcher(path);

				if(!m.matches()) { 
					throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, path);
				}
				String provisional_id = m.group(1);

				BrokerModel model = getBrokerModel();
				try { 

					Log.info(String.format("provisional_term=%s", provisional_id));
					
					ProvisionalTerm provisionalTerm = model.getProvisionalTerm(provisional_id);
					Log.info(String.format("found provisionalterms.provisional_id=%d", provisionalTerm.provisional_id));

					Request loaded = model.getLatestRequest(provisionalTerm);
					Log.info(String.format("found requests.request_id=%d", loaded.request_id));
					
					Collection<Metadata> metadata = model.getMetadata(loaded);

					if(responseType.equals("application/json")) {
						StringWriter stringer = new StringWriter();
						JSONWriter writer = new JSONWriter(stringer);

						writer.object();

						writer.key("request");
						loaded.writeJSONObject(writer);

						writer.key("metadata");
						writer.array();
						for(Metadata md : metadata) { 
							md.writeJSONObject(writer);
						}
						writer.endArray();

						writer.endObject();

						response.setContentType(responseType);
						response.setStatus(HttpServletResponse.SC_OK);
						response.getWriter().println(stringer.toString());

					} else if (responseType.equals("text/html")) { 

						response.setContentType(responseType);
						response.setStatus(HttpServletResponse.SC_OK);

						PrintWriter printer = response.getWriter();
						printer.println(loaded.writeHTMLObject(false));

						printer.println("<table>");
						Metadata mdheader = new Metadata();
						printer.println(mdheader.writeHTMLRowHeader());
						for(Metadata md : metadata) { 
							printer.println(md.writeHTMLObject(true));
						}
						printer.println("</table>");

					}

				} finally { 
					model.close();
				}

			} catch(DBModelException e) { 
				throw new BrokerException(e);
			} catch(JSONException e) { 
				throw new BrokerException(e);
			} catch (DBObjectMissingException e) {
				throw new BrokerException(HttpServletResponse.SC_GONE, e.getMessage());
			}

		} catch(BrokerException e) { 
			handleException(response, e);
			return;
		}
	}

	private static Pattern metadataPattern = Pattern.compile("metadata_(.*)");
	
	public LinkedList<String[]> readMetadataFromParams(Map<String,String[]> params) {
		LinkedList<String[]> mdList = new LinkedList<String[]>();
		Matcher m = null;
		for(String key : params.keySet()) {
			m = metadataPattern.matcher(key);
			if(m.matches()) { 
				String metadataKey = m.group(1), metadataValue = params.get(key)[0];
				mdList.add(new String[] { metadataKey, metadataValue });
			}
		}
		return mdList;
	}
	
	public LinkedList<String[]> readMetadataFromJSON(JSONObject obj) { 
		LinkedList<String[]> mdList = new LinkedList<String[]>();
		if(obj.has("metadata")) { 
			try {
				JSONArray array = obj.getJSONArray("metadata");
				for(int i = 0; i < array.length(); i++) {
					JSONObject md = array.getJSONObject(i);
					String key = md.getString("metadata_key");
					String value = md.getString("metadata_value");
					mdList.add(new String[] { key, value });
				}
				
			} catch (JSONException e) {
				// do nothing.
			}
		}
		return mdList;
	}
	
	public Request readRequestFromParams(Map<String,String[]> params) { 
		Request req = new Request();
		req.setFromParameters(params);
		return req;
	}
	
	public Request readRequestFromJSON(JSONObject obj) { 
		Request req = new Request();
		req.setFromJSON(obj);
		return req;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try { 
			int requestID = -1;
			String path = request.getRequestURI();
			Pattern p = Pattern.compile(requestURLPattern);
			Matcher m = p.matcher(path);
			if(!m.matches()) { 
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, path);
				return;
			}
			String provisional_id = m.group(1);

			Pattern metadataPattern = Pattern.compile("metadata_(.*)");

			LinkedList<Metadata> metadatas = new LinkedList<Metadata>();

			try {
				BrokerModel model = getBrokerModel();
				try { 

					ProvisionalTerm provisionalTerm = model.getProvisionalTerm(provisional_id);
					Request dbReq = model.getLatestRequest(provisionalTerm);

					JSONObject obj = new JSONObject();
					Map<String,String[]> params = decodedParams(request);
					
					for(String paramName : params.keySet()) {
						Matcher metadataMatcher = metadataPattern.matcher(paramName);

						String[] parray = params.get(paramName);
						try { 
							if(metadataMatcher.matches()) {
								String keyName = metadataMatcher.group(1);
								for(int i =0; i < parray.length; i++) { 
									if(parray[i].length() > 0) {
										Metadata md = new Metadata();
										md.metadata_key = keyName;
										md.metadata_value = parray[i];
										metadatas.add(md);
									}
								}

							} else { 
								if(parray.length > 1) { 
									for(int i = 0; i < parray.length; i++) { 
										obj.append(paramName, parray[i]);
									}
								} else if(parray.length == 1) { 
									obj.put(paramName, parray[0]);
								}
							}
						} catch(NumberFormatException e) { 
							throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
						}
					}
					
					Request newReq = new Request();
					
					newReq.setFromDBObject(dbReq);
					newReq.request_id = null;
					newReq.parent_request = dbReq.request_id;

					/**
					 * This block could probably be replaced with setFromJSON(), no? 
					 */
					Iterator<String> keyItr = obj.keys();
					while(keyItr.hasNext()) { 
						String key = keyItr.next();
						Field f = Request.class.getField(key);
						int mod = f.getModifiers();

						// This 'if' doesn't check the .isAutoGenerated() method, because 
						// we *want* to set the request_id field.  
						if(Modifier.isPublic(mod) && 
							!Modifier.isStatic(mod)) {

							newReq.setFromString(key, String.valueOf(obj.get(key)));
						}
					}
										
					String error = model.checkRequestChange(dbReq, newReq);
					if(error != null) {
						throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, error);
					}
					
					model.startTransaction();
					try { 
						model.updateRequest(provisionalTerm, newReq, metadatas);
						model.commitTransaction();
						
					} catch(DBModelException e) { 
						model.rollbackTransaction();
						throw e;
					}

					response.sendRedirect("/");
				} finally { 
					model.close();
				}

			} catch (JSONException e) {
				throw new BrokerException(e);

			} catch (NoSuchFieldException e) {
				throw new BrokerException(e);

			} catch(RuntimeException re) { 
				throw new BrokerException(re);

			} catch (DBModelException e) {
				throw new BrokerException(e);

			} catch (DBObjectMissingException e) {
				throw new BrokerException(e);
			}
		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
