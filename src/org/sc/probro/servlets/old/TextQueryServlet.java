package org.sc.probro.servlets.old;

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
import org.sc.probro.BrokerProperties;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.lucene.ProteinSearcher;
import org.sc.probro.servlets.SkeletonServlet;

/**
 * 
 * @author tdanford
 * @deprecated
 *
 */
public class TextQueryServlet extends SkeletonServlet {
	
	private String luceneIndexPath;
	
	public TextQueryServlet(BrokerProperties props) { 
		luceneIndexPath = props.getLuceneIndex();
	}
	
	public void init() throws ServletException { 
		super.init();
	}
	
	public void destroy() {
		super.destroy();
	}

	private String renderHitAsHTML(JSONObject obj) throws BrokerException { 
		try { 
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
		} catch(JSONException e) { 
			throw new BrokerException(e);
		}
	}
	
	private static Set<String> SUPPORTED_CONTENT_TYPES = 
		new TreeSet<String>(Arrays.asList(CONTENT_TYPE_JSON, CONTENT_TYPE_HTML));

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		Map<String,String[]> params = decodedParams(request);

		if(!params.containsKey("search")) {
			String msg = "No search term given";
			Log.warn(msg);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
			return;
		}

		String query = params.get("search")[0];
		String contentType = decodeResponseType(params, CONTENT_TYPE_JSON);

		try {
			if(!SUPPORTED_CONTENT_TYPES.contains(contentType)) { 
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, 
						String.format("Unsupported content type: %s", contentType));
			}
			
			ProteinSearcher searcher = new ProteinSearcher(new File(luceneIndexPath));
			try { 

				JSONArray hits = searcher.evaluate(query);

				response.setContentType(contentType);
				response.setStatus(HttpServletResponse.SC_OK);

				if(contentType.equals(CONTENT_TYPE_JSON)) { 

					response.getWriter().println(hits.toString());

				} else if(contentType.equals(CONTENT_TYPE_HTML)) {

					PrintWriter pw = response.getWriter();
					pw.println("<table>");
					pw.println("<tr><th>ID</th><th>Type</th><th>Descriptions</th><th>Accessions</th></tr>");

					for(int i = 0; i < hits.length(); i++) { 
						JSONObject hit = (JSONObject)hits.get(i);
						String htmlHit = renderHitAsHTML(hit);
						pw.println(htmlHit);
					}
					pw.println("</table>");

				}

			} catch(JSONException e) { 
				throw new BrokerException(e);
			} finally { 
				searcher.close();
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
