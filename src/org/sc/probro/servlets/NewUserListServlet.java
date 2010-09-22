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

public class NewUserListServlet extends BrokerServlet {

	public NewUserListServlet(BrokerProperties props) {
		super(props);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();
			String contentType = getContentType(request);
			String content = null;
			
			Broker broker = getBroker();
			
			String ontologyID = null;
			User[] users = broker.listUsers(creds, ontologyID);

			if(contentType.equals(CONTENT_TYPE_JSON)) { 
				JSONStringer stringer = new JSONStringer();			
				try { 
					stringer.object();
					stringer.key("vals");
					
					BrokerData.stringJSONArray(stringer, users);
					
					stringer.endObject();
					
				} catch(JSONException e) { 
					throw new BrokerException(e);
				}
				content = stringer.toString(); 

			} else if (contentType.equals(CONTENT_TYPE_HTML)) { 
				StringWriter stringer = new StringWriter();
				PrintWriter writer = new PrintWriter(stringer);

				User t = new User();
				writer.println("<table>");
				writer.println(t.writeHTMLRowHeader());
				for(User user : users) { 
					writer.println(user.writeHTMLObject(true));
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
