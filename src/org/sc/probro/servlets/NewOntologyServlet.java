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
import org.json.JSONStringer;
import org.sc.probro.*;
import org.sc.probro.data.*;
import org.sc.probro.exceptions.*;

public class NewOntologyServlet extends BrokerServlet {

	public NewOntologyServlet(BrokerProperties props) {
		super(props);
	}
	
	private static Pattern bulkRequestURIPattern = Pattern.compile("^(.*)/ontology/(.*)/bulk-requests(.*)$");
	
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
