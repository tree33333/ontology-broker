package org.sc.probro.servlets;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
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
			UserCredentials user = new UserCredentials();
			
			Broker broker = getBroker();
			try { 
				Request req = parseRequest(broker, user, request);
				String term = broker.request(user, req.search_text, req.context, req.provenance, req.ontology, req.metadata);

				Log.info(String.format("Request created %s for (%s,%s,%s,%s)", 
						term, req.search_text, req.context, req.provenance, req.metadata.toString()));

				response.sendRedirect(term);
				
			} finally { 
				broker.close();
			}
			
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
			stringer.object();
			
			stringer.key("vals");
			BrokerData.stringJSONArray(stringer, reqs);
			
			stringer.endObject();
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
	
	public static Request parseRequest(Broker broker, UserCredentials user, HttpServletRequest httpReq) throws BrokerException { 
		String contentType = httpReq.getContentType();
		if(contentType == null || contentType.equals(CONTENT_TYPE_JSON)) { 
			return parseRequestFromJSONString(httpReq);
			
		} else if (contentType.equals(CONTENT_TYPE_FORM)) { 
			return parseRequestFromForm(broker, user, httpReq);
			
		} else { 
			throw new BadRequestException(String.format("Unrecognized Content-Type: %s", String.valueOf(contentType)));
		}
	}

	public static Request parseRequestFromForm(Broker broker, UserCredentials user, HttpServletRequest httpReq) throws BrokerException { 
		Request req = new Request();
		
		Map<String,String[]> params = decodedParams(httpReq);
		req.setFromParameters(params);

		Log.info(String.format("Setting up Request from parameters %s", params.keySet().toString()));
		
		String creator_id = params.get("creator_id")[0];
		String modified_by_id = params.get("modified_by")[0];
		String ontology_id = params.get("ontology_id")[0];
		
		req.creator = broker.checkUser(user, creator_id);
		req.modified_by = broker.checkUser(user, modified_by_id);
		req.ontology = broker.checkOntology(user, ontology_id);
		
		req.metadata = new ArrayList<Metadata>();
		Pattern metadataPattern = Pattern.compile("metadata_(.*)");
		
		for(String key : params.keySet()) { 
			Matcher metaMatcher = metadataPattern.matcher(key);
			
			if(metaMatcher.matches()) { 
				String kk = metaMatcher.group(1);
				String vv = params.get(key)[0].trim();

				if(vv.length() > 0) { 
					Metadata meta = new Metadata();

					meta.key = kk;
					meta.value = vv;
					meta.created_by = req.modified_by;
					meta.created_on = req.date_submitted;

					req.metadata.add(meta); 
				}
			}
		}
		
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
