package org.sc.probro.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sc.probro.Broker;
import org.sc.probro.BrokerProperties;
import org.sc.probro.SearchResult;
import org.sc.probro.UserCredentials;
import org.sc.probro.exceptions.BadRequestException;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.lucene.ProteinSearcher;

public class TextQueryServlet extends BrokerServlet {
	
	private String luceneIndexPath;
	
	public TextQueryServlet(BrokerProperties props) { 
		super(props);
		luceneIndexPath = props.getLuceneIndex();
	}
	
	public void init() throws ServletException { 
		super.init();
	}
	
	public void destroy() {
		super.destroy();
	}

	private String renderHitAsHTML(SearchResult obj) { 
		StringBuilder sb = new StringBuilder();
		sb.append("<tr>");
		sb.append(String.format("<td>%s</td>", obj.id));
		sb.append(String.format("<td>%s</td>", obj.response_type));

		sb.append("<td><table>");
		for(String desc : obj.description) { 
			sb.append(String.format("<tr><td>%s</td></tr>", desc));				
		}
		sb.append("</table></td>");

		sb.append("<td><table>");
		for(String acc : obj.accession) { 
			sb.append(String.format("<tr><td>%s</td></tr>", acc));				
		}
		sb.append("</table></td>");

		sb.append("</tr>");
		return sb.toString();
	}
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		try {
			String searchTerm = getRequiredParam(request, "search", String.class);
			String contentType = getContentType(request);
			String[] ontologies = getOptionalParam(request, "ontology_id", String[].class);
			UserCredentials user = new UserCredentials();
			
			Broker broker = getBroker();
			try {
				SearchResult[] results = broker.query(user, searchTerm, ontologies);
				
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType(contentType);
				
				if(contentType.equals(CONTENT_TYPE_JSON)) {
					
					JSONStringer stringer = new JSONStringer();
					stringer.array();
					for(SearchResult res : results) { 
						res.stringJSON(stringer);
					}
					stringer.endArray();
					
					response.getWriter().println(stringer.toString());
					
				} else if (contentType.equals(CONTENT_TYPE_HTML)) {
					
					PrintWriter writer = response.getWriter();
					writer.println("<table>");
					writer.println("<tr>" +
							"<th>ID</th>" +
							"<th>Type</th>" +
							"<th>Descriptions</th>" +
							"<th>Accessions</th>" +
							"</tr>");
					for(SearchResult res : results) { 
						writer.println(renderHitAsHTML(res));
					}
					writer.println("</table>");
					
				} else { 
					throw new BadRequestException(String.format("Unsupported content type: %s", contentType));
				}
				
			} catch(JSONException e) { 
				throw new BrokerException(e);
				
			} finally { 
				broker.close();
			}
			

		} catch (BrokerException e) {
			handleException(response, e);
			return;
		}

	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	
		handleException(response, 
				new BrokerException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
						String.format("POST not allowed.")));
	}

}
