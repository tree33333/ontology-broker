/*
	Copyright 2010 Massachusetts General Hospital

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
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

public class UserListServlet extends BrokerServlet {

	public UserListServlet(BrokerProperties props) {
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
