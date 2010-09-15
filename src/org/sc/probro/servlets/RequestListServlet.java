package org.sc.probro.servlets;

import java.io.File;
import java.io.IOException;
import java.util.regex.*;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

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
import org.sc.probro.data.*;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.lucene.PROIndexer;

/**
 * Cleared for DBObjectModel usage.
 * 
 * @author tdanford
 */
public class RequestListServlet extends SkeletonDBServlet {
	
	private String luceneIndexPath;
	
    public RequestListServlet(BrokerProperties ps) {
    	super(ps);
    	luceneIndexPath = ps.getLuceneIndex();
    }
    
    public void init() throws ServletException { 
    	super.init();
    }
    
    public void destroy() { 
    	super.destroy();
    }

    /* 
     * Adds a new Request item.
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	try { 
    		StringWriter stringer = new StringWriter();
    		JSONWriter json = new JSONWriter(stringer);

    		Log.debug("RequestListServlet.doPost()");

    		Pattern metadataKey = Pattern.compile("^metadata_(.*)$");

    		LinkedList<MetadataObject> metadatas = new LinkedList<MetadataObject>();

    		try {
    			//
    			// This first block reads in the parameters from the POST, and turns them into 
    			// (a) fields of the Request, or 
    			// (b) new Metadata objects associated with the request.
    			//
    			Map paramMap = request.getParameterMap();

    			RequestObject obj = new RequestObject();

    			json.object();

    			for(Object nameobj : paramMap.keySet()) {
    				String name = nameobj.toString();
    				Object valueObj = paramMap.get(name);

    				String value = 
    					valueObj.getClass().isArray() ? Array.get(valueObj, 0).toString() : valueObj.toString();

    					System.out.println(String.format("\t%s: %s", name, String.valueOf(value)));

    					Matcher metadataMatcher = metadataKey.matcher(name);
    					if(metadataMatcher.matches()) { 
    						String keyName = metadataMatcher.group(1);
    						String metaValue = value.trim();

    						// we want to ignore blank metadata values.
    						if(metaValue.length() > 0) { 
    							MetadataObject m = new MetadataObject();
    							m.metadata_key = keyName;
    							m.metadata_value = value;
    							metadatas.add(m);
    						}

    					} else { 
    						try {
    							Field f = obj.getClass().getField(name);
    							json.key(name).value(value);
    							obj.setFromString(name, value);

    						} catch (NoSuchFieldException e) {
    							// do nothing! 
    						}
    					}
    			}

    			// special case -- set the ontology_id to "1" (the default ontology) if 
    			// no ontology is specified.
    			if(obj.ontology_id == null) { 
    				obj.ontology_id = 1;
    				json.key("ontology_id").value(String.valueOf(obj.ontology_id));
    			}

    			if(obj.creator_id == null) { 
    				obj.creator_id = 1;
    				json.key("user_id").value(String.valueOf(obj.creator_id));				
    			}
    			
    			if(obj.modified_by == null) { 
    				obj.modified_by = obj.creator_id;
    			}

    			obj.ontology_term = null;

    			if(obj.status == null) {
    				obj.status = RequestObject.RESPONSE_PENDING;
    				json.key("response_code").value(String.valueOf(obj.status));
    			}

    			json.endObject();

    	        //
    	        // In this next block, we insert the corresponding Request object into the database, 
    	        // retrieve its (newly-created) request_id, and then use that to insert all the metadata
    	        // objects into the db as well.
    	        // 
    			BrokerModel model = getBrokerModel();
    			try { 
    				model.startTransaction();
    				try {
    					ProvisionalTermObject provisionalTerm = model.createNewRequest(obj, metadatas);

    			        PROIndexer indexer = new PROIndexer(new File(luceneIndexPath));
    			        try { 
    			        	// Add the request to the Lucene index, so that it will satisfy future queries.
    			        	indexer.addQuery(obj, metadatas);
    			        } finally { 
    			        	indexer.close();
    			        }

        				model.commitTransaction();
        				Log.debug("Committed Request/Metadata Transaction.");
        				
            			response.sendRedirect(String.format("/request/%s/", provisionalTerm.provisional_term));
    				
    				} catch(IOException e) {
    					model.rollbackTransaction();
    					throw new BrokerException(e);
    					
    				} catch(DBModelException e) { 
    					model.rollbackTransaction();
    					throw e;
    				}
    				
    			} finally {
    				model.close();
    			}

    		} catch (JSONException e) {
    			throw new BrokerException(e);

    		} catch (DBModelException e) {
    			throw new BrokerException(e);
    		}
    		
    		
    	} catch(BrokerException e) { 
    		handleException(response, e);
    	}
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	try { 
			Map<String,String[]> params = decodedParams(request);

			RequestObject obj = new RequestObject();
			obj.setFromParameters(params);

			String format = params.containsKey("format") ? params.get("format")[0] : "json";
			String contentType = contentTypeFromFormat(format, "html", "json");

			StringWriter stringer = new StringWriter();
			JSONWriter json = new JSONWriter(stringer);

			try {
				BrokerModel model = getBrokerModel();
				
				try { // ends with model.close() 

					if(obj.ontology_id != null) { 
						OntologyObject ontology = new OntologyObject();
						ontology.ontology_id = obj.ontology_id;
						
						if(!model.contains(ontology)) { 
							throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST,
									String.format("Unknown Ontology %d", obj.ontology_id));															
						}
					}

					if(format.equals("json")) { 
						json.array();

					} else if(format.equals("html")) {

						stringer.write("<table>\n");
						stringer.write(obj.writeHTMLRowHeader());
						stringer.write("\n");
					}

					Collection<RequestObject> requests = model.listLatestRequests();
					for(RequestObject res : requests) { 
						if(format.equals("json")) { 
							res.writeJSONObject(json);

						} else if (format.equals("html")) { 
							stringer.write(res.writeHTMLObject(true));
							stringer.write("\n");
						}
					}

					if(format.equals("json")) { 
						json.endArray();

					} else if(format.equals("html")) { 
						stringer.write("</table>");
					}

				} finally { 
					model.close();
				}

    			response.setContentType(contentType);
    			response.setStatus(HttpServletResponse.SC_OK);
    			response.getWriter().println(stringer.toString());

    		} catch (JSONException e) {
    			throw new BrokerException(e);
    		} catch(DBModelException e) { 
    			throw new BrokerException(e);
    		}

    	} catch(BrokerException e) { 
    		handleException(response, e);
    		return;
    	}
    }
}

