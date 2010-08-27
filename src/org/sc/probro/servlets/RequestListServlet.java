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
import org.sc.probro.lucene.PROIndexer;

public class RequestListServlet extends SkeletonDBServlet {
	
	private String tableName;
	private String luceneIndexPath;
	private PROIndexer indexer;
	
    public RequestListServlet(BrokerProperties ps) {
    	super(ps);
    	tableName = "REQUESTS";
    	luceneIndexPath = ps.getLuceneIndex();
    }
    
    public void init() throws ServletException { 
    	super.init();
    	if(indexer == null) { 
    		try {
				indexer = new PROIndexer(new File(luceneIndexPath));
			} catch (IOException e) {
				Log.warn(e);
				throw new ServletException(e);
			}
    	}
    }
    
    public void destroy() { 
    	super.destroy();
    	try {
			indexer.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
    }

    /* 
     * Adds a new Request item.
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringWriter stringer = new StringWriter();
        JSONWriter json = new JSONWriter(stringer);
        System.out.println("doPost()");
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
					json.key(name).value(value);
					obj.setFromString(name, value);
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
			obj.response_code = Request.RESPONSE_PENDING;
			json.key("response_code").value(String.valueOf(obj.response_code));				
			
	        json.endObject();

	        //
	        // In this next block, we insert the corresponding Request object into the database, 
	        // retrieve its (newly-created) request_id, and then use that to insert all the metadata
	        // objects into the db as well.
	        // 
	        Connection cxn = dbSource.getConnection();
	        Statement stmt = cxn.createStatement();
	        String insertString = obj.insertString();
	        
	        Log.debug(insertString);
	        
	        stmt.execute(insertString, Statement.RETURN_GENERATED_KEYS);
	        
	        ResultSet generated = stmt.getGeneratedKeys();
	        if(generated.next()) { obj.request_id = generated.getInt(1); }

	        Log.debug(String.format("REQUEST_ID: %d", obj.request_id));
	        
	        generated.close();
	        
	        // Insert the associated metadata items.
	        for(Metadata m : metadatas) { 
	        	m.request_id = obj.request_id;
	        	
	        	if(stmt.executeUpdate(m.insertString()) > 0) { 
	        		String msg = String.format("Inserted Metadata: %s", m.toString());
	        		Log.debug(msg);
	        		
	        	} else { 
	        		String msg = String.format("Could not insert Metadata: %s", m.toString());
	        		Log.warn(msg);
	        	}
	        }
	        
	        stmt.close();
	        cxn.close();
	        
	        // Add the request to the Lucene index, so that it will satisfy future queries.
	        indexer.addQuery(obj, metadatas);

	        response.sendRedirect("/");
	        
        } catch (JSONException e) {
        	raiseInternalError(response, e);
			return;

        } catch (SQLException e) {
        	raiseInternalError(response, e);
			return;
		}
    }
    
    private static String TYPE_JSON = "application/json";
    private static String TYPE_HTML = "text/html";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	String contentType = TYPE_JSON;
    	String type = request.getParameter("format");
    	if(type != null && URLDecoder.decode(type, "UTF-8").toLowerCase().equals("html")) { 
    		contentType = TYPE_HTML;
    	}

        StringWriter stringer = new StringWriter();
        JSONWriter json = new JSONWriter(stringer);

        try {
        	Connection cxn = dbSource.getConnection();
        	Statement stmt = cxn.createStatement();
        	
        	Request obj = new Request();
        	
        	Enumeration paramNames = request.getParameterNames();
        	StringBuilder restriction = new StringBuilder();
        	while(paramNames.hasMoreElements()) { 
        		String paramName = (String)paramNames.nextElement();
        		try {
					Field paramField = Request.class.getField(paramName);
					int mod = paramField.getModifiers();
					if(paramField != null && Modifier.isPublic(mod) && !Modifier.isStatic(mod)) { 
						String value = URLDecoder.decode(request.getParameter(paramName), "UTF-8");
						obj.setFromString(paramName, value);
					}

        		} catch (NoSuchFieldException e) {
        			Log.warn(e);
        			// do nothing.
				}
        	}

        	String query = obj.queryString();
        	System.out.println(String.format("query: %s", query));
        	ResultSet rs = stmt.executeQuery(query);
        	
        	if(contentType.equals(TYPE_JSON)) { 
        		json.array();
        	
        	} else if(contentType.equals(TYPE_HTML)) { 
        		stringer.write("<table>\n");
        		stringer.write(obj.writeHTMLRowHeader());
        		stringer.write("\n");
        	}
			
			while(rs.next()) {
				//T res = resultConstructor.newInstance(rs);
				Request res = new Request(rs);

				if(contentType.equals(TYPE_JSON)) { 
					res.writeJSONObject(json);
				
				} else if (contentType.equals(TYPE_HTML)) { 
					stringer.write(res.writeHTMLObject(true));
	        		stringer.write("\n");
				}
			}
				
        	if(contentType.equals(TYPE_JSON)) { 
        		json.endArray();
        	
        	} else if(contentType.equals(TYPE_HTML)) { 
        		stringer.write("</table>");
        	}
			
			rs.close();
			stmt.close();
			cxn.close();

	    	response.setContentType(contentType);
	        response.setStatus(HttpServletResponse.SC_OK);
	        response.getWriter().println(stringer.toString());

        } catch (JSONException e) {
        	raiseInternalError(response, e);
			return;
		} catch (SQLException e) {
        	raiseInternalError(response, e);
			return;
		}
    }
}
