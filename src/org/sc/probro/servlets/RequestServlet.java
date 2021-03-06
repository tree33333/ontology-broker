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
import java.net.URLDecoder;
import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sc.probro.*;
import org.sc.probro.data.*;
import org.sc.probro.exceptions.*;

public class RequestServlet extends BrokerServlet {

	public RequestServlet(BrokerProperties props) {
		super(props);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials user = new UserCredentials();

			String contentType = getContentType(request);
			String content = null;

			String requestID = request.getRequestURI();

			Broker broker = getBroker();
			try { 

				Request req = new Request();
				
				if(contentType.equals(CONTENT_TYPE_JSON)) {
					BufferedReader reader = request.getReader();
					int read = -1;
					StringBuilder sb = new StringBuilder();
					while((read = reader.read()) != -1) { 
						sb.append((char)read);
					}
					content = sb.toString();
					req.setFromJSON(new JSONObject(content), broker, user);

				} else if (contentType.equals(CONTENT_TYPE_FORM)) {
					Map<String,String[]> params = decodedParams(request);
					req.setFromParameters(params);

				} else { 
					throw new BadRequestException(String.format("Illegal POST content type: %s", contentType));
				}
				
				broker.update(user, requestID, req);

			} catch (JSONException e) {
				throw new BadRequestException(content);
			} finally { 
				broker.close();
			}

			response.sendRedirect("/");

		} catch(BrokerException e) { 
			handleException(response, e);
		}		
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();

			String contentType = getContentType(request);
			String content = null;

			String requestID = request.getRequestURI();
			Log.info(String.format("Request for \"%s\"", requestID));
			
			Request req = null;

			Broker broker = getBroker();
			try { 
				req = broker.check(creds, requestID);

				if(contentType.equals(CONTENT_TYPE_JSON)) { 
					content = req.toJSONString();

				} else if (contentType.equals(CONTENT_TYPE_HTML)) { 
					content = req.writeHTMLObject(false);
				}
			} finally { 
				broker.close();
			}

			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(contentType);
			response.getWriter().println(content);

		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
