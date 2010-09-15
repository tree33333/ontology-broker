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
import org.sc.probro.data.BrokerModel;
import org.sc.probro.data.DBModelException;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.DBObjectMissingException;
import org.sc.probro.data.MetadataObject;
import org.sc.probro.data.ProvisionalTermObject;
import org.sc.probro.data.RequestObject;

/**
 * Cleared for BrokerModel usage.
 * 
 * @author tdanford
 *
 */
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

	/*
	 * "Tracking ID",	
	 * "DB source",	
	 * "name",	
	 * "UniProtKB Ac",	
	 * "protein coordinates (beginning-end)",	
	 * "residue#, Modification",	
	 * "taxon ID",	
	 * "Organism",		
	 */

	public BulkRequestServlet(BrokerProperties ps) { 
		super(ps);
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try { 
			try { 
				BrokerModel model = getBrokerModel();
				try { 

					RequestObject template = new RequestObject();
					template.status = RequestObject.RESPONSE_PENDING;
					Collection<RequestObject> reqs = model.listLatestRequests();

					response.setContentType("text");
					response.setStatus(HttpServletResponse.SC_OK);

					PrintWriter writer = response.getWriter();
					printRow(writer, headers);

					for(RequestObject req : reqs) { 

						Collection<MetadataObject> mds = model.getMetadata(req);
						if(mds == null) { return; }

						Map<String,String> metaMap = new TreeMap<String,String>();
						for(MetadataObject md : mds) { 
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
				} finally { 
					model.close();
				}
			} catch(DBModelException e) { 
				throw new BrokerException(e);
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

		try { 

			String bulkResponse = request.getParameter("update");
			if(bulkResponse == null) {
				String msg = "No 'update' parameter given.";
				raiseException(response, HttpServletResponse.SC_BAD_REQUEST, msg);
				return;
			}

			bulkResponse = URLDecoder.decode(bulkResponse, "UTF-8");
			BulkRequestTable table = new BulkRequestTable(new StringReader(bulkResponse));

			try { 
				BrokerModel model = getBrokerModel();
				try {

					Map<Integer,String> errorMap = new TreeMap<Integer,String>();

					Map<Integer,ProvisionalTermObject> terms = new TreeMap<Integer,ProvisionalTermObject>();
					Map<Integer,RequestObject> submitted = new TreeMap<Integer,RequestObject>();
					Map<Integer,RequestObject> dbRequests = new TreeMap<Integer,RequestObject>();
					Map<Integer,Collection<MetadataObject>> mds = new TreeMap<Integer,Collection<MetadataObject>>();

					for(int i = 0; i < table.getNumRows(); i++) {
						// retrieve the request line from the submitted bulk request table, 
						// and the corresponding request entry from the database...
						RequestObject submittedReq = table.getRequest(i);
						ProvisionalTermObject term = model.getProvisionalTerm(submittedReq.provisional_term); 
						Collection<MetadataObject> submittedMetadata = table.getMetadata(i);
						RequestObject dbReq = model.getLatestRequest(term);

						//... and check to make sure that the bulk request didn't illegaly
						// update one of the values in the request.
						String error = model.checkRequestChange(dbReq, submittedReq); 
						if(error != null) { 
							errorMap.put(i, error);
						} else { 

							submitted.put(i, submittedReq);
							dbRequests.put(i, dbReq);
							mds.put(i, submittedMetadata);
							terms.put(i, term);
						}
					}

					if(!errorMap.isEmpty()) { 
						throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, errorMap.toString());
					}

					model.startTransaction();
					try { 
						for(Integer i : submitted.keySet()) { 
							model.updateRequest(terms.get(i), submitted.get(i), mds.get(i));
						}

						model.commitTransaction();

					} catch(DBModelException e) { 
						model.rollbackTransaction();
						throw e;
					}
				} finally { 
					model.close();
				}
				
			} catch (DBModelException e) {
				throw new BrokerException(e);
			} catch(DBObjectMissingException e) { 
				throw new BrokerException(e);
			}

			//response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Not yet implemented.");
			response.sendRedirect("/");

		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
