package org.sc.probro.servlets;

import java.util.*;
import java.io.*;
import java.net.URLEncoder;
import java.util.regex.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sc.probro.*;
import org.sc.probro.data.*;
import org.sc.probro.exceptions.*;

public class OntologyServlet extends BrokerServlet {

	public OntologyServlet(BrokerProperties props) {
		super(props);
	}
	
	private static Pattern bulkRequestURIPattern = Pattern.compile("^(.*)/ontology/(.*)/bulk-requests(.*)$");
	private static Pattern metadataURIPattern = Pattern.compile("^(.*/ontology/.*)/metadata(.*)$");
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
	
		try { 
			String ontologyID = request.getRequestURI();
			Matcher metadataMatcher = metadataURIPattern.matcher(ontologyID);

			if(metadataMatcher.matches()) { 
				ontologyID = metadataMatcher.group(1);
				
				request.setAttribute("ontology_id", ontologyID);

				String fieldName = getRequiredParam(request, "field_name", String.class);
				String fieldDescription = getRequiredParam(request, "field_description", String.class);
				String fieldMetadataKey = getRequiredParam(request, "metadata_key", String.class);
				
				request.setAttribute("field_name", fieldName);
				request.setAttribute("field_description", fieldDescription);
				request.setAttribute("metadata_key", fieldMetadataKey);
				
				request.getRequestDispatcher("/ontologyfields").forward(request, response);
				
			} else { 
				throw new BadRequestException("'metadata' not present in ontology URI");
			}
		
		} catch(BrokerException e) { 
			this.handleException(response, e);
			return;
		}
		
	}

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();

			String contentType = getContentType(request);
			String content = null;

			String ontologyID = request.getRequestURI();
			
			Matcher bulkRequestMatcher = bulkRequestURIPattern.matcher(ontologyID);
			
			if(bulkRequestMatcher.matches()) {
				
				String ontologyURI = 
					bulkRequestMatcher.group(1) + "/ontology/" + 
					bulkRequestMatcher.group(2);
				
				Log.info(String.format("Ontology URI: \"%s\"", ontologyURI));
				
				String newRequestURI = String.format("%s/bulk-requests?ontology_id=%s",
						bulkRequestMatcher.group(1),
						URLEncoder.encode(ontologyURI, "UTF-8"));
				
				Log.info(String.format("New Request URI: \"%s\"", newRequestURI));
				
				RequestDispatcher dispatcher = request.getRequestDispatcher(newRequestURI);
				
				dispatcher.forward(request, response);
				return;
			}

			Broker broker = getBroker(); 

			Ontology ont = broker.checkOntology(creds, ontologyID);

			if(contentType.equals(CONTENT_TYPE_JSON)) { 
				content = ont.toJSONString();

			} else if (contentType.equals(CONTENT_TYPE_HTML)) { 
				content = ont.writeHTMLObject(false);
			}

			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(contentType);
			response.getWriter().println(content);

		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
