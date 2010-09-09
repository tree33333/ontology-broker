package org.sc.probro.servlets;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sc.probro.BrokerException;
import org.sc.probro.Numbering;
import org.sc.probro.Pairing;
import org.sc.probro.RequestStateMachine;

public class RequestStateServlet extends SkeletonServlet {
	
	public static Numbering<String> STATES;
	public static RequestStateMachine MACHINE;
	
	static { 
		STATES = new Numbering<String>(
				"REDUNDANT",
				"FULFILLED",
				"PENDING", 
				"INCOMPLETE",
				"ERROR", 
				"ESCALATE",
				"ACCEPTED",
				"WITHDRAWN"
				);
		
		MACHINE = new RequestStateMachine();
		for(int i = 0; i < STATES.size(); i++) { 
			MACHINE.addState(STATES.backward(i));
		}
		
		MACHINE.addTransition("PENDING", "RESPOND-INCOMPLETE", "INCOMPLETE");
		MACHINE.addTransition("PENDING", "RESPOND-ERROR", "ERROR");
		MACHINE.addTransition("PENDING", "RESPOND-ESCALATE", "ESCALATE");
		MACHINE.addTransition("PENDING", "RESPOND-FULFILLED", "FULFILLED");
		MACHINE.addTransition("PENDING", "RESPOND-REDUNDANT", "REDUNDANT");
		MACHINE.addTransition("PENDING", "UPDATE-WITHDRAWN", "WITHDRAWN");

		MACHINE.addTransition("INCOMPLETE", "UPDATE-PENDING", "PENDING");
		MACHINE.addTransition("INCOMPLETE", "UPDATE-WITHDRAWN", "WITHDRAWN");
		MACHINE.addTransition("INCOMPLETE", "UPDATE-ESCALATE", "ESCALATE");

		MACHINE.addTransition("ERROR", "UPDATE-PENDING", "PENDING");
		MACHINE.addTransition("ERROR", "UPDATE-WITHDRAWN", "WITHDRAWN");
		MACHINE.addTransition("ERROR", "UPDATE-ESCALATE", "ESCALATE");

		MACHINE.addTransition("ESCALATE", "RESPOND-INCOMPLETE", "INCOMPLETE");
		MACHINE.addTransition("ESCALATE", "RESPOND-ERROR", "ERROR");
		MACHINE.addTransition("ESCALATE", "RESPOND-REDUNDANT", "REDUNDANT");
		MACHINE.addTransition("ESCALATE", "RESPOND-FUFLFILLED", "FULFILLED");
		MACHINE.addTransition("ESCALATE", "UPDATE-WITHDRAWN", "WITHDRAWN");

		MACHINE.addTransition("FULFILLED", "JUDGE-REJECT", "PENDING");
		MACHINE.addTransition("FULFILLED", "JUDGE-ACCEPT", "ACCEPTED");

		MACHINE.addTransition("REDUNDANT", "JUDGE-REJECT", "PENDING");
		MACHINE.addTransition("REDUNDANT", "JUDGE-ACCEPT", "ACCEPTED");		
	}
	
	public static Set<Integer> legalTransitions(Integer fidx) {
		String from = fidx != null ? STATES.backward(fidx) : null;
		Set<Integer> legal = new TreeSet<Integer>();
		for(Integer k = 0; k < MACHINE.size(); k++) { 
			String to = STATES.backward(k);
			if(from == null || MACHINE.isReachable(from, to)) { 
				legal.add(k);
			}
		}
		return legal;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			
			String format = getOptionalParam(request, "format", String.class);
			if(format == null) { format = "html"; }
			
			Log.info(String.format("RequestStateServlet: format=%s", format));
			
			Integer fromIdx = getOptionalParam(request, "from", Integer.class);
			String from = fromIdx != null ? STATES.backward(fromIdx) : null;
			
			Log.info(String.format("RequestStateServlet: from=%s", String.valueOf(from)));
			
			if(format.equals("html")) { 
			
				response.setStatus(HttpServletResponse.SC_OK);				
				response.setContentType("text/html");
				PrintWriter writer = response.getWriter();
				
				writer.println("<table>");
				
				for(int i =0; i < STATES.size(); i++) {
					String to = STATES.backward(i);
					if(from == null || MACHINE.isReachable(from, to)) { 
						writer.println(String.format("<tr><td>%d</td><td>%s</td></tr>", i, to));
					}
				}
				
				writer.println("</table>");
				
			} else if (format.equals("fieldset")) { 

				response.setStatus(HttpServletResponse.SC_OK);				
				response.setContentType("text/html");
				PrintWriter writer = response.getWriter();
				
				writer.println("<fieldset><legend>New Status</legend>");
				
				for(int i =0; i < STATES.size(); i++) {
					String to = STATES.backward(i);
					if(from == null || MACHINE.isReachable(from, to)) { 
						//writer.println(String.format("<tr><td>%d</td><td>%s</td></tr>", i, to));
						String field = 
							String.format("<input type=\"radio\" name=\"new_status\" value=\"%d\">%s</input>",
									i, to);
						writer.println(field);
					}
				}
				
				writer.println("</fieldset>");
				
			} else if(format.equals("json")) { 
				
				Log.info("RequestStateServlet: writing JSON response.");

				response.setStatus(HttpServletResponse.SC_OK);				
				response.setContentType("application/json");
				
				JSONObject top = new JSONObject();
				JSONArray array = new JSONArray();
				try {
					
					top.put("from", from);
					top.put("states", array);

					for(int i =0; i < STATES.size(); i++) {
						String to = STATES.backward(i);

						if(from == null || MACHINE.isReachable(from, to)) { 
							array.put(i);
						}
					}
				} catch(JSONException e) { 
					throw new BrokerException(e);
				}

				PrintWriter writer = response.getWriter();
				writer.println(top.toString());
				
				Log.info(String.format("RequestStateServlet: %s", top.toString()));

			} else { 
				throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, "Unknown 'format' value " + format);
			}
			
		} catch (BrokerException e) {
			Log.warn(e);
			Log.warn(String.format("RequestStateServlet.doGet() : %s", e.getMessage()));
			handleException(response, e);
		}		
	}
}
