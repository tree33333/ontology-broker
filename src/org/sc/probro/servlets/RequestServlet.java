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
import org.sc.probro.BrokerException;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

import java.sql.*;
import java.util.regex.*;
import java.util.*;

public class RequestServlet extends SkeletonDBServlet {
	
	public RequestServlet(BrokerProperties ps) { 
		super(ps);
	}

	private boolean createStatusChange(Connection cxn, Request req, 
			int updater_id, int oldStatus, int newStatus, String comment) throws BrokerException {
		
		if(!req.status.equals(oldStatus)) { 
			throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, 
					String.format("old_status=%d does not match request existing request.status=%d", 
							oldStatus, req.status));
		}
		
		try { 
			java.sql.Date date = new java.sql.Date(Calendar.getInstance().getTime().getTime());
			
			PreparedStatement ps = cxn.prepareStatement("insert into statusupdates " +
					"(request_id, updated_on, updated_by, old_status, new_status, comment) values " +
					"(?, ?, ?, ?, ?, ?)");
			
			try {
				ps.setInt(1, req.request_id);
				ps.setDate(2, date);
				ps.setInt(3, updater_id);
				ps.setInt(4, oldStatus);
				ps.setInt(5, newStatus);
				ps.setString(6, comment);
				
				if(ps.executeUpdate() != 1) { 
					throw new BrokerException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							String.format("Could not create STATUSUPDATES entry"));
				}
				
				
			} finally { 
				ps.close();
			}
			
		} catch(SQLException e) { 
			throw new BrokerException(e);
		}
		
		return true;
	}
	
	private boolean updateMetadata(Connection cxn, Request req, Collection<String[]> pairs) throws BrokerException {

		try {
			PreparedStatement findStmt = 
				cxn.prepareStatement(
						"select metadata_id, metadata_key, metadata_value from metadatas " +
				"where request_id=? and metadata_key=? and metadata_value=?");
			PreparedStatement insertStmt = 
				cxn.prepareStatement(
						"insert into metadatas (request_id, created_on, created_by, " +
				"metadata_key, metadata_value) values (?, ?, ?, ?, ?)");

			java.util.Date date = Calendar.getInstance().getTime();

			try {
				for(String[] pair : pairs) { 
					findStmt.setInt(1, req.request_id);
					findStmt.setString(2, pair[0]);
					findStmt.setString(3, pair[1]);
					ResultSet rs = findStmt.executeQuery();
					try {
						if(!rs.next()) { 
							insertStmt.setInt(1, req.request_id);
							insertStmt.setDate(2, new java.sql.Date(date.getTime()));
							insertStmt.setInt(3, req.user_id);
							insertStmt.setString(4, pair[0]);
							insertStmt.setString(5, pair[1]);

							if(insertStmt.executeUpdate() != 1) { 
								throw new BrokerException(
										HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
										String.format("Couldn't create metadata %s/%s", pair[0], pair[1]));
							}

						}

					} finally { 
						rs.close();
					}
				}

			} finally { 
				findStmt.close();
				insertStmt.close();
			}


		} catch (SQLException e) {
			throw new BrokerException(e);
		}		

		return true;
	}

	private boolean updateRequest(Connection cxn, Request req) throws BrokerException { 
		try {
			Statement stmt = cxn.createStatement();
			try {
				stmt.executeUpdate(req.saveString());
				return true;

			} finally { 
				stmt.close();
			}

		} catch(SQLException e) { 
			throw new BrokerException(e);
		}
	}

	private Collection<Metadata> loadMetadata(Request req) throws BrokerException { 
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
				throw new BrokerException(e);
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
		
		try { 

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
			Collection<Metadata> metadata = loadMetadata(loaded);

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

		} catch(BrokerException e) { 
			handleException(response, e);
			return;
		}
	}
	
	private static String OLD_STATUS_KEY = "old_status";
	private static String NEW_STATUS_KEY = "new_status";


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
		
		Pattern metadataPattern = Pattern.compile("metadata_(.*)");
		
		LinkedList<String[]> metadataPairs = new LinkedList<String[]>();
		int oldStatus = -1, newStatus = -1;
		
		try {
			Map<String,String[]> params = decodedParams(request);
			JSONObject obj = new JSONObject();
			
			for(String paramName : params.keySet()) {
				Matcher metadataMatcher = metadataPattern.matcher(paramName);

				String[] parray = params.get(paramName);

				try { 
					if(metadataMatcher.matches()) {
						String keyName = metadataMatcher.group(1);
						for(int i =0; i < parray.length; i++) { 
							metadataPairs.add(new String[] { keyName, parray[i] });
						}
					} else if (paramName.equals(OLD_STATUS_KEY)) {
						oldStatus = Integer.parseInt(parray[0]);
						obj.put("status", newStatus);

					} else if (paramName.equals(NEW_STATUS_KEY)) { 
						newStatus = Integer.parseInt(parray[0]);

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
					throw new BrokerException(e);
				}
			}
			
			if(newStatus == -1) { 
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, "No new status given");
			}
			
			Iterator<String> keyItr = obj.keys();
			Request req = new Request();

			while(keyItr.hasNext()) { 
				String key = keyItr.next();
				Field f = Request.class.getField(key);
				int mod = f.getModifiers();
				
				// This 'if' doesn't check the .isAutoGenerated() method, because 
				// we *want* to set the request_id field.  
				if(Modifier.isPublic(mod) && 
					!Modifier.isStatic(mod)) {
					
					req.setFromString(key, String.valueOf(obj.get(key)));
				}
			}
			
			if(req.user_id == null) {
				Log.warn(obj.toString());
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, "No 'user_id' supplied.");
			}
			
			req.status = newStatus;

			Connection cxn = dbSource.getConnection();
			try { 
				cxn.setAutoCommit(false);
				
				int updater = req.user_id;
				String comment = null;


				if(updateRequest(cxn, req) && 
					updateMetadata(cxn, req, metadataPairs) && 
					createStatusChange(cxn, req, updater, oldStatus, newStatus, comment)) {
					
					cxn.commit();
					cxn.setAutoCommit(true);
					response.sendRedirect(path);
				}
			} finally { 
				if(!cxn.getAutoCommit()) { 
					cxn.rollback();
				}
				cxn.close();
			}

		} catch (JSONException e) {
			raiseInternalError(response, e);
			return;
			
		} catch(SQLException e) { 
			raiseInternalError(response, e);
			return;

		} catch (NoSuchFieldException e) {
			raiseInternalError(response, e);
			return;

		} catch(BrokerException e) { 
			handleException(response, e);
			return;
			
		} catch(RuntimeException re) { 
			handleException(response, new BrokerException(re));
			return;
		}
	}
}
