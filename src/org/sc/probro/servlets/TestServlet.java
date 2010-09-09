package org.sc.probro.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;
import org.sc.probro.BrokerException;

public class TestServlet extends SkeletonServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try { 
			try {
				JSONObject obj = this.getLocalJSON(request, "/states?format=json");
				
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/json");
				response.getWriter().println(obj.toString());
				
			} catch (JSONException e) {
				throw new BrokerException(e);
			}
		} catch(BrokerException e) { 
			handleException(response, e);
			return;
		}
	}
}
