package org.sc.probro.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sc.probro.BrokerProperties;
import org.sc.probro.lucene.ProteinSearcher;

public class TextQueryServlet extends SkeletonServlet {
	
	private String luceneIndexPath;
	private ProteinSearcher searcher;
	
	public TextQueryServlet(BrokerProperties props) { 
		searcher = null;
		luceneIndexPath = props.getLuceneIndex();
	}
	
	public void init() throws ServletException { 
		if(searcher == null) { 
			try {
				searcher = new ProteinSearcher(new File(luceneIndexPath));
			} catch (IOException e) {
				throw new ServletException(e);
			}
		}
	}
	
	public void destroy() {
		if(searcher != null) {
			System.out.println("Shutting down Lucene index...");
			try {
				searcher.close();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	private String renderHitAsHTML(JSONObject obj) throws JSONException { 
		StringBuilder sb = new StringBuilder();
		sb.append("<tr>");
		sb.append(String.format("<td>%s</td>", obj.get("id").toString()));
		sb.append(String.format("<td>%s</td>", obj.get("type").toString()));

		sb.append("<td><table>");
		JSONArray array = null;
		if(obj.has("description")) { 
			array = obj.getJSONArray("description");
			for(int i = 0; array != null && i < array.length(); i++) { 
				sb.append(String.format("<tr><td>%s</td></tr>", array.get(i).toString()));
			}
		}
		sb.append("</table></td>");

		sb.append("<td><table>");
		if(obj.has("accession")) { 
			array = obj.getJSONArray("accession");
			for(int i = 0; array != null && i < array.length(); i++) { 
				sb.append(String.format("<tr><td>%s</td></tr>", array.get(i).toString()));
			}
		}
		sb.append("</table></td>");

		sb.append("</tr>");
		return sb.toString();
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		String query = request.getParameter("search");
		String format = request.getParameter("format");
		if(query == null) { 
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No request given.");
			return;
		}
		
		if(format==null) { format = "json"; }
		
		String contentType = "application/json";
		if(format.equals("html")) { contentType = "text/html"; }
		
		query = URLDecoder.decode(query, "UTF-8");
		
		try {
			JSONArray hits = searcher.evaluate(query);
			
			response.setContentType(contentType);
			response.setStatus(HttpServletResponse.SC_OK);

			if(format.equals("json")) { 
				response.getWriter().println(hits.toString());
				
			} else if(format.equals("html")) {
				
				PrintWriter pw = response.getWriter();
				pw.println("<table>");
				pw.println("<tr><th>ID</th><th>Type</th><th>Descriptions</th><th>Accessions</th></tr>");
				
				for(int i = 0; i < hits.length(); i++) { 
					JSONObject hit = (JSONObject)hits.get(i);
					String htmlHit = renderHitAsHTML(hit);
					pw.println(htmlHit);
				}
				pw.println("</table>");
				
			} else { 
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown format: " + format);
				return;				
			}
			
		} catch (JSONException e) {
			e.printStackTrace(System.err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

}