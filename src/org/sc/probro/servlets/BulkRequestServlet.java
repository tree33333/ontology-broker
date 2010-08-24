package org.sc.probro.servlets;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sc.probro.BrokerProperties;
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

	private <T extends DBObject> Collection<T> load(T template, HttpServletResponse response) throws SQLException, IOException { 
		LinkedList<T> reqs = new LinkedList<T>();

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
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return null;
		} catch (InvocationTargetException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return null;
		} catch (NoSuchMethodException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return null;
		} finally { 
			cxn.close();
		}

		return reqs;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try { 
			Request template = new Request();
			template.response_code = Request.RESPONSE_PENDING;
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
			

		} catch (SQLException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
	}
	
	private void printRow(PrintWriter writer, String... row) { 
		for(int i = 0; i < headers.length; i++) { 
			if(i > 0) { writer.print("\t"); }
			writer.print(i < row.length ? row[i] : "");
		}
		writer.println();		
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

	}

}
