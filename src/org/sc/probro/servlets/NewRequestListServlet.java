package org.sc.probro.servlets;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sc.probro.*;
import org.sc.probro.exceptions.*;

public class NewRequestListServlet extends BrokerServlet {

	public NewRequestListServlet(BrokerProperties props) {
		super(props);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();
			
			Broker broker = getBroker();
	
			Request req = parseRequest(request);
			
			broker.update(creds, request.getRequestURI(), req);

		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try { 
			UserCredentials creds = new UserCredentials();
			
			String contentType = getContentType(request);
			
			Broker broker = getBroker();
			
			String ontologyID = getOptionalParam(request, "ontology_id", String.class);
			
			Request[] requests = broker.listRequests(creds, ontologyID);
			
			String content = formatRequests(requests, contentType);

			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(contentType);
			response.getWriter().println(content);
			
		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
	
	public static String formatRequestsAsJSON(Request[] reqs) throws BrokerException {
		JSONStringer stringer = new JSONStringer();			
		try { 
			BrokerData.stringJSONArray(stringer, reqs);
		} catch(JSONException e) { 
			throw new BrokerException(e);
		}
		
		return stringer.toString();
	}
	
	public static String formatRequestsAsHTML(Request[] reqs) throws BrokerException { 
		StringWriter stringer = new StringWriter();
		PrintWriter writer = new PrintWriter(stringer);

		Request t = new Request();
		writer.println("<table>");
		writer.println(t.writeHTMLRowHeader());
		
		for(Request req : reqs) { 
			writer.println(req.writeHTMLObject(true));
		}
		writer.println("</table>");
		
		return stringer.toString();
	}
	
	public static String formatRequests(Request[] reqs, String contentType) throws BrokerException { 
		if(contentType.equals(CONTENT_TYPE_JSON)) { 
			return formatRequestsAsJSON(reqs);
			
		} else if (contentType.equals(CONTENT_TYPE_HTML)) { 
			return formatRequestsAsHTML(reqs);
			
		} else { 
			throw new BadRequestException(contentType);
		}
	}
	
	public static Request parseRequest(HttpServletRequest httpReq) throws BrokerException { 
		String contentType = httpReq.getContentType();
		if(contentType == null || contentType.equals(CONTENT_TYPE_JSON)) { 
			return parseRequestFromJSONString(httpReq);
			
		} else if (contentType.equals(CONTENT_TYPE_FORM)) { 
			return parseRequestFromForm(httpReq);
			
		} else { 
			throw new BadRequestException(String.format("Unrecognized Content-Type: %s", String.valueOf(contentType)));
		}
	}

	public static Request parseRequestFromForm(HttpServletRequest httpReq) throws BrokerException { 
		Request req = new Request();
		req.setFromParameters(decodedParams(httpReq));
		return req;
	}

	public static Request parseRequestFromJSONString(HttpServletRequest httpReq) throws BrokerException { 
		Request req = new Request();
		StringBuilder sb = new StringBuilder();
		try { 
			Reader r = httpReq.getReader();
			int rchar = -1;
			while((rchar = r.read()) != -1) { 
				sb.append((char)rchar);
			}
			JSONObject obj = new JSONObject(sb.toString());
			req.setFromJSON(obj);
		} catch(IOException e) { 
			throw new BrokerException(e);
		} catch (JSONException e) {
			throw new BrokerException(e);
		}
		return req;
	}
}
