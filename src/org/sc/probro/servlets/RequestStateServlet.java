package org.sc.probro.servlets;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

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

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
		try {
			Integer fromIdx = getOptionalParam(request, "from", Integer.class);
			String from = fromIdx != null ? STATES.backward(fromIdx) : null;
			
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
			
		} catch (BrokerException e) {
			handleException(response, e);
		}		
	}
}
