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
import org.sc.probro.BrokerException;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.*;
import org.sc.probro.lucene.PROIndexer;

public class RequestListServlet extends SkeletonDBServlet {
	
	private String tableName;
	private String luceneIndexPath;
	
    public RequestListServlet(BrokerProperties ps) {
    	super(ps);
    	tableName = "REQUESTS";
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
        StringWriter stringer = new StringWriter();
        JSONWriter json = new JSONWriter(stringer);
        
        Log.debug("RequestListServlet.doPost()");

        Pattern metadataKey = Pattern.compile("^metadata_(.*)$");
        
        LinkedList<Metadata> metadatas = new LinkedList<Metadata>();
        
        /*
    	 * The following fields are auto-generated: 
    	request_id       
    	
    	 * The following fields are filled in the by the user: 
		search_text      
		context          // not required.
		user_id          
		provenance       
		date_submitted  
		ontology_id   
    	 
    	 * The following fields are filled in by the system (but not "auto" generated)  
		ontology_term    // blank at first.
		response_code    
         */
        
        try {
        	//
            // This first block reads in the parameters from the POST, and turns them into 
            // (a) fields of the Request, or 
            // (b) new Metadata objects associated with the request.
        	//
			Map paramMap = request.getParameterMap();

			Request obj = new Request();
			
			json.object();
			
			for(Object nameobj : paramMap.keySet()) {
				String name = nameobj.toString();
				Object valueObj = paramMap.get(name);
				
				String value = 
					valueObj.getClass().isArray() ? 
					Array.get(valueObj, 0).toString() : valueObj.toString();

				System.out.println(String.format("\t%s: %s", name, String.valueOf(value)));
				
				Matcher metadataMatcher = metadataKey.matcher(name);
				if(metadataMatcher.matches()) { 
					String keyName = metadataMatcher.group(1);
					String metaValue = value.trim();

					// we want to ignore blank metadata values.
					if(metaValue.length() > 0) { 
						Metadata m = new Metadata();
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
			
			if(obj.user_id == null) { 
				obj.user_id = 1;
				json.key("user_id").value(String.valueOf(obj.user_id));				
			}
			
			obj.ontology_term = null;
			
			if(obj.status == null) {
				obj.status = Request.RESPONSE_PENDING;
				json.key("response_code").value(String.valueOf(obj.status));
			}
			
	        json.endObject();

	        //
	        // In this next block, we insert the corresponding Request object into the database, 
	        // retrieve its (newly-created) request_id, and then use that to insert all the metadata
	        // objects into the db as well.
	        // 
	        Connection cxn = dbSource.getConnection();
	        cxn.setAutoCommit(false);
	        
	        Statement stmt = cxn.createStatement();
	        
	        Preparation prep = obj.savePreparation(cxn);
	        prep.setFromObject(obj);
	        
	        prep.stmt.executeUpdate();
	        
	        ResultSet generated = prep.stmt.getGeneratedKeys();
	        if(generated.next()) { obj.request_id = generated.getInt(1); }

	        Log.info(String.format("CREATED: request_id=%d", obj.request_id));
	        
	        generated.close();
	        
	        Date time = Calendar.getInstance().getTime();
	        
	        // Insert the associated metadata items.
	        for(Metadata m : metadatas) { 
	        	m.request_id = obj.request_id;
	        	m.created_by = obj.user_id;
	        	m.created_on = time;
	        	
	        	if(stmt.executeUpdate(m.insertString()) > 0) { 
	        		String msg = String.format("Inserted Metadata: %s", m.toString());
	        		Log.debug(msg);
	        		
	        	} else { 
	        		String msg = String.format("Could not insert Metadata: %s", m.toString());
	        		Log.warn(msg);
	        	}
	        }
	        
	        PROIndexer indexer = new PROIndexer(new File(luceneIndexPath));
	        try { 
	        	// Add the request to the Lucene index, so that it will satisfy future queries.
	        	indexer.addQuery(obj, metadatas);
	        } catch(IOException e) { 
	        	throw new BrokerException(e);
	        } finally { 
	        	indexer.close();
	        }
	        
	        cxn.commit();

	        prep.close();
	        stmt.close();
	        cxn.close();

	        response.sendRedirect("/");
	        
        } catch (JSONException e) {
        	raiseInternalError(response, e);
			return;
        } catch(BrokerException e) { 
        	handleException(response, e);
        	return;

        } catch (SQLException e) {
        	raiseInternalError(response, e);
			return;
		}
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	try { 
			Map<String,String[]> params = decodedParams(request);

			Request obj = new Request();
			obj.setFromParameters(params);

			String format = params.containsKey("format") ? params.get("format")[0] : "json";
			String contentType = contentTypeFromFormat(format, "html", "json");

			StringWriter stringer = new StringWriter();
			JSONWriter json = new JSONWriter(stringer);

			try {
				Connection cxn = dbSource.getConnection();
				try { // ends with cxn.close() 
					Statement stmt = cxn.createStatement();
					try {  // ends with stmt.close()

						if(obj.ontology_id != null) { 
							Ontology ontology = new Ontology();
							ontology.ontology_id = obj.ontology_id;
							if(!ontology.checkExists(cxn)) { 
								throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST,
										String.format("Unknown Ontology %d", obj.ontology_id));
							}
						}

						String query = obj.queryString();						
						Log.debug(String.format("query: %s", query));

						ResultSet rs = stmt.executeQuery(query);
						try {  // ends with rs.close()
							if(format.equals("json")) { 
								json.array();

							} else if(format.equals("html")) {

								stringer.write("<table>\n");
								stringer.write(obj.writeHTMLRowHeader());
								stringer.write("\n");
							}

							while(rs.next()) {
								//T res = resultConstructor.newInstance(rs);
								Request res = new Request(rs);

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
							rs.close();
						}
					} finally { 
						stmt.close();
					}
				} finally { 
					cxn.close();
				}

    			response.setContentType(contentType);
    			response.setStatus(HttpServletResponse.SC_OK);
    			response.getWriter().println(stringer.toString());

    		} catch (JSONException e) {
    			throw new BrokerException(e);
    		} catch (SQLException e) {
    			throw new BrokerException(e);
    		}

    	} catch(BrokerException e) { 
    		handleException(response, e);
    		return;
    	}
    }
}

