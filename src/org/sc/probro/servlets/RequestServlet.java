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
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

import java.sql.*;
import java.util.regex.*;
import java.util.*;

public class RequestServlet extends SkeletonDBServlet {
	
	public RequestServlet(BrokerProperties ps) { 
		super(ps);
	}
	
	private boolean updateRequest(Request req, HttpServletResponse response) throws IOException { 
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
			raiseInternalError(response, e);
			return false;
		}		
	}

	private Collection<Metadata> loadMetadata(Request req, HttpServletResponse response) throws IOException { 
		LinkedList<Metadata> metadataList = new LinkedList<Metadata>();

		if(req != null) { 
			try {
				Connection cxn = dbSource.getConnection();
				try { 
					Statement stmt = cxn.createStatement();
					try {
						String query = String.format("select * from METADATAS where request_id=%d", req.request_id);
						ResultSet rs = stmt.executeQuery(query);
						try { 
							while(rs.next()) { 
								metadataList.add(new Metadata(rs));
							}

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
				raiseInternalError(response, e);
				return null;
			}			
		}

		return metadataList;
	}

	private Request loadRequest(int requestID, HttpServletResponse response) throws IOException { 
		try {
			Connection cxn = dbSource.getConnection();
			try { 
				Statement stmt = cxn.createStatement();
				try {
					String query = String.format("select * from REQUESTS where request_id=%d", requestID);
					ResultSet rs = stmt.executeQuery(query);
					try { 
						if(rs.next()) { 
							return new Request(rs);
						} else { 
							String msg = String.format("Unknown Request: %d", requestID);
							raiseException(response, HttpServletResponse.SC_BAD_REQUEST, msg);
							return null;
						}
						
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
			raiseInternalError(response, e);
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
		Pattern p = Pattern.compile("^/request/(\\d+)[^\\d]?.*$");
		Matcher m = p.matcher(path);

		if(!m.matches()) { 
			raiseException(response, HttpServletResponse.SC_BAD_REQUEST, path);
			return;
		}
		requestID = Integer.parseInt(m.group(1));

		Request loaded = loadRequest(requestID, response);
 		Collection<Metadata> metadata = loadMetadata(loaded, response);
		
		if(loaded != null) { 
			if(responseType.equals("application/json")) {
				StringWriter stringer = new StringWriter();
				JSONWriter writer = new JSONWriter(stringer);
				
				try {
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
					
				} catch (JSONException e) {
					raiseInternalError(response, e);
					return;
				}
				
			} else if (responseType.equals("text/html")) { 

				response.setContentType(responseType);
				response.setStatus(HttpServletResponse.SC_OK);
				
				PrintWriter printer = response.getWriter();
				printer.println(loaded.writeHTMLObject(false));
				printer.println("<table>");
				Metadata header = new Metadata();
				printer.println(header.writeHTMLRowHeader());
				for(Metadata md : metadata) { 
					printer.println(md.writeHTMLObject(true));
				}
				printer.println("</table>");
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		int requestID = -1;
		String path = request.getRequestURI();
		Pattern p = Pattern.compile("^/request/(\\d+)[^\\d]?.*$");
		Matcher m = p.matcher(path);
		if(!m.matches()) { 
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, path);
			return;
		}
		requestID = Integer.parseInt(m.group(1));

		/*
		Reader reader = request.getReader();
		StringBuilder stringer = new StringBuilder();
		int c;
		while((c = reader.read()) != -1) { 
			stringer.append((char)c);
		}
		String submitted = URLDecoder.decode(stringer.toString(), "UTF-8");
		Log.info(submitted);
		*/
		
		Pattern metadataPattern = Pattern.compile("metadata_(.*)");
		LinkedList<String[]> metadataPairs = new LinkedList<String[]>();
		
		try {
			Map<String,String[]> params = decodedParams(request);
			JSONObject obj = new JSONObject();
			
			for(String paramName : params.keySet()) {
				Matcher metadataMatcher = metadataPattern.matcher(paramName);

				String[] parray = params.get(paramName);

				if(metadataMatcher.matches()) {
					String keyName = metadataMatcher.group(1);
					for(int i =0; i < parray.length; i++) { 
						metadataPairs.add(new String[] { keyName, parray[i] });
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
			}
			
			Iterator<String> keyItr = obj.keys();
			Request req = new Request();

			while(keyItr.hasNext()) { 
				String key = keyItr.next();
				Field f = Request.class.getField(key);
				int mod = f.getModifiers();
				if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
					//f.set(req, obj.get(key));
					req.setFromString(key, String.valueOf(obj.get(key)));
				}
			}

			if(updateRequest(req, response)) { 
				response.sendRedirect(path);
			}

		} catch (JSONException e) {
			raiseInternalError(response, e);
			return;

		} catch (NoSuchFieldException e) {
			raiseInternalError(response, e);
			return;

		}

	}

}
