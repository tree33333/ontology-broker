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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sc.probro.BrokerProperties;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.lucene.BiothesaurusSearcher;
import org.sc.probro.lucene.ProteinSearcher;

/**
 * Cleared for <tt>BrokerModel</tt> usage.
 * 
 * This servlet is relatively disconnected from the rest of the app -- no other servlets contain explicit
 * dependencies on it, aside from BrokerStart.  
 * 
 * This is a simple interface to the Biothesaurus-based Protein index.  It is run under the url <tt>/proteins</tt>
 * and is (normally) accessed either programmatically or from the query-proteins.html static HTML form.
 * 
 * @author tdanford
 *
 */
public class BiothesaurusQueryServlet extends SkeletonServlet {
	
	private File biothesaurusIndex;
	private File uniprotMappingFile;
	
	public BiothesaurusQueryServlet(BrokerProperties props) { 
		biothesaurusIndex = props.getProteinIndexDir();
		uniprotMappingFile = props.getUniprotMappingFile();
	}
	
	public void init() throws ServletException { 
		super.init();
	}
	
	public void destroy() {
		super.destroy();
	}
	
	private JSONArray renderDocumentsAsHits(BiothesaurusSearcher searcher, 
			Collection<Document> docs) throws BrokerException {
		
		JSONArray array = new JSONArray();
		
		int i = 0;
		for(Document doc : docs) { 
			JSONObject obj = new JSONObject();
			
			try {
				String id = doc.getField("protein-id").stringValue();
				obj.put("id", id);
				obj.put("type", "protein");
				
				obj.put("accession", new JSONArray());
				obj.put("description", new JSONArray());
				
				for(org.apache.lucene.document.Field f : doc.getFields("description")) { 
					obj.append("description", f.stringValue());
				}
				
				Collection<String> pros = searcher.convertToPRO(doc);
				if(pros != null) { 
					for(String pro : pros) { 
						obj.append("accession", pro);
					}
				}
				
			} catch (JSONException e) {
				throw new BrokerException(e);
			}

			array.put(obj);
		}
		
		return array;
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
			String msg = "No 'search' parameter given";
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
			
			BiothesaurusSearcher searcher = new BiothesaurusSearcher(biothesaurusIndex, uniprotMappingFile);
			try {
				
				Collection<Document> docs = searcher.search(searcher.createQuery(query));
				JSONArray hits = renderDocumentsAsHits(searcher, docs);

				response.setContentType(contentType);
				response.setStatus(HttpServletResponse.SC_OK);

				if(contentType.equals(CONTENT_TYPE_JSON)) { 

					response.getWriter().println(hits.toString());

				} else if(contentType.equals(CONTENT_TYPE_HTML)) {

					PrintWriter pw = response.getWriter();
					pw.println("<table>");
					pw.println("<tr><th>ID</th><th>Type</th><th>Descriptions</th><th>PRO</th></tr>");

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
}
