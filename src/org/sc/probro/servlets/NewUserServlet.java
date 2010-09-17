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

public class NewUserServlet extends BrokerServlet {

	public NewUserServlet(BrokerProperties props) {
		super(props);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();

			String contentType = getContentType(request);
			String content = null;

			String userID = request.getRequestURI();

			Broker broker = getBroker(); 

			User user = broker.checkUser(creds, userID);

			if(contentType.equals(CONTENT_TYPE_JSON)) { 
				content = user.toJSONString();

			} else if (contentType.equals(CONTENT_TYPE_HTML)) { 
				content = user.writeHTMLObject(false);
			}

			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(contentType);
			response.getWriter().println(content);

		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
