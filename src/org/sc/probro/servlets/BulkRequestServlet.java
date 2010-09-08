package org.sc.probro.servlets;

import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.util.*;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.sc.probro.BrokerException;
import org.sc.probro.BrokerProperties;
import org.sc.probro.BulkRequestTable;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

public class BulkRequestServlet extends SkeletonDBServlet {
	
	public static final String[] headers = new String[] { 
		"Tracking ID",	
		"DB source",	
		"name",	
		"UniProtKB Ac",	
		"protein coordinates (beginning-end)",	
		"residue#, Modification",	
		"taxon ID",	
		"Organism",		
	};
	
	public BulkRequestServlet(BrokerProperties ps) { 
		super(ps);
	}

	private <T extends DBObject> Collection<T> load(T template, HttpServletResponse response) throws BrokerException { 
		LinkedList<T> reqs = new LinkedList<T>();

		try {
			Connection cxn = dbSource.getConnection();

			try {
				Constructor<T> constructor = (Constructor<T>) template.getClass().getConstructor(ResultSet.class);
				Statement stmt = cxn.createStatement();
				try { 
					ResultSet rs = stmt.executeQuery(template.queryString());
					try { 
						while(rs.next()) { 
							T req = constructor.newInstance(rs);
							reqs.add(req);
						}

					} finally { 
						rs.close();
					}

				} finally { 
					stmt.close();
				}

			} catch (InstantiationException e) {
				throw new BrokerException(e);
			} catch (IllegalAccessException e) {
				throw new BrokerException(e);
			} catch (InvocationTargetException e) {
				throw new BrokerException(e);
			} catch (NoSuchMethodException e) {
				throw new BrokerException(e);
			} catch(SQLException e) {
				throw new BrokerException(e);

			} finally { 
				cxn.close();
			}
		
		} catch (SQLException e) {
			throw new BrokerException(e);
		}

		return reqs;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try { 
			Request template = new Request();
			template.status = Request.RESPONSE_PENDING;
			Collection<Request> reqs = load(template, response);

			response.setContentType("text");
			response.setStatus(HttpServletResponse.SC_OK);
			
			PrintWriter writer = response.getWriter();
			printRow(writer, headers);

			/*
			"Tracking ID",	
			"DB source",	
			"name",	
			"UniProtKB Ac",	
			"protein coordinates (beginning-end)",	
			"residue#, Modification",	
			"taxon ID",	
			"Organism",		
			 */
			for(Request req : reqs) { 
				
				Metadata mdTemplate = new Metadata();
				mdTemplate.request_id = req.request_id;
				Collection<Metadata> mds = load(mdTemplate, response);
				if(mds == null) { return; }
				
				Map<String,String> metaMap = new TreeMap<String,String>();
				for(Metadata md : mds) { 
					if(!metaMap.containsKey(md.metadata_key)) { 
						metaMap.put(md.metadata_key, md.metadata_value);
					} else { 
						metaMap.put(md.metadata_key, String.format("%s|%s", metaMap.get(md.metadata_key), 
								md.metadata_value));
					}
				}
				
				printRow(writer, 
						req.getProvisionalTerm(),
						"?", 
						req.search_text, 
						metaMap.containsKey("uniprot") ? metaMap.get("uniprot") : "",
						metaMap.containsKey("coordinates") ? metaMap.get("coordinates") : "",
						metaMap.containsKey("modification") ? metaMap.get("modification") : "",
						metaMap.containsKey("taxon") ? metaMap.get("taxon") : "",
						metaMap.containsKey("organism") ? metaMap.get("organism") : ""
						);
			}
			

		} catch (BrokerException e) {
			handleException(response, e);
		}
	}
	
	private void printRow(PrintWriter writer, String... row) { 
		for(int i = 0; i < headers.length; i++) { 
			if(i > 0) { writer.print("\t"); }
			writer.print(i < row.length ? row[i] : "");
		}
		writer.println();		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String bulkResponse = request.getParameter("update");
		if(bulkResponse == null) {
			String msg = "No 'update' parameter given.";
			raiseException(response, HttpServletResponse.SC_BAD_REQUEST, msg);
			return;
		}
		
		bulkResponse = URLDecoder.decode(bulkResponse, "UTF-8");
		BulkRequestTable table = new BulkRequestTable(new StringReader(bulkResponse));

		try { 
			Connection cxn = dbSource.getConnection();
			try { 
				Statement stmt = cxn.createStatement();
				try { 
					for(int i = 0; i < table.getNumRows(); i++) {
						
						// retrieve the request line from the submitted bulk request table, 
						// and the corresponding request entry from the database...
						Request submittedReq = table.getRequest(i);
						Request dbReq = submittedReq.loadDBVersion(Request.class, stmt);
						
						//... and check to make sure that the bulk request didn't illegaly
						// update one of the values in the request.
						if(!submittedReq.isSubsetOf(dbReq)) {
							String msg = "Bulk request contained an illegal update of request " + submittedReq.request_id;
							raiseException(response, HttpServletResponse.SC_CONFLICT, msg);
							return;
						}
						
						// if everything is kosher, than get the updated status of the request,
						// change, and save.
						submittedReq.status = table.getNewStatus(i);
						stmt.executeUpdate(submittedReq.saveString());
					}
				} finally { 
					stmt.close();
				}

			} finally { 
				cxn.close();
			}
			
		} catch (SQLException e) {
			raiseInternalError(response, e);
			return;
		}

		//response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Not yet implemented.");
		response.sendRedirect("/");
	}
}
