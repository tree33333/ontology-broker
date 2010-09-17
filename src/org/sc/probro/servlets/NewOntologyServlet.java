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
import org.sc.probro.data.*;
import org.sc.probro.exceptions.*;

public class NewOntologyServlet extends BrokerServlet {

	public NewOntologyServlet(BrokerProperties props) {
		super(props);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();

			String contentType = getContentType(request);
			String content = null;

			String ontologyID = request.getRequestURI();

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
