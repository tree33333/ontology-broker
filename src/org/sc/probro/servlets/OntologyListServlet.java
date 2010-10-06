package org.sc.probro.servlets;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONStringer;
import org.sc.probro.*;
import org.sc.probro.exceptions.*;

public class OntologyListServlet extends BrokerServlet {

	public OntologyListServlet(BrokerProperties props) {
		super(props);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();
			String contentType = getContentType(request);
			String content = null;
			
			Broker broker = getBroker();
			Ontology[] onts = broker.listOntologies(creds);

			if(contentType.equals(CONTENT_TYPE_JSON)) { 
				JSONStringer stringer = new JSONStringer();
				
				try { 
					stringer.object();

					stringer.key("vals");
					BrokerData.stringJSONArray(stringer, onts);

					stringer.endObject();
				} catch(JSONException e) { 
					throw new BrokerException(e);
				}
				
				content = stringer.toString(); 

			} else if (contentType.equals(CONTENT_TYPE_HTML)) { 
				StringWriter stringer = new StringWriter();
				PrintWriter writer = new PrintWriter(stringer);

				Ontology t = new Ontology();
				writer.println("<table>");
				writer.println(t.writeHTMLRowHeader());
				for(Ontology ont : onts) { 
					writer.println(ont.writeHTMLObject(true));
				}
				writer.println("</table>");
				
				content = stringer.toString();
			}

			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(contentType);
			response.getWriter().println(content);
			
		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
