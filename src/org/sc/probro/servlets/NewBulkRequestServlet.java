package org.sc.probro.servlets;

import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.util.*;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.sc.probro.Broker;
import org.sc.probro.BrokerProperties;
import org.sc.probro.BulkRequestTable;
import org.sc.probro.BulkResponseTable;
import org.sc.probro.Metadata;
import org.sc.probro.Request;
import org.sc.probro.UserCredentials;
import org.sc.probro.data.BrokerModel;
import org.sc.probro.data.DBModelException;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.DBObjectMissingException;
import org.sc.probro.data.MetadataObject;
import org.sc.probro.data.ProvisionalTermObject;
import org.sc.probro.data.RequestObject;
import org.sc.probro.exceptions.BrokerException;

/**
 * Cleared for BrokerModel usage.
 * 
 * @author tdanford
 *
 */
public class NewBulkRequestServlet extends BrokerServlet {
	
	public NewBulkRequestServlet(BrokerProperties ps) { 
		super(ps);
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try { 
			String ontology_id = getOptionalParam(request, "ontology_id", String.class);
			UserCredentials user = new UserCredentials();
			
			Broker broker = getBroker();
			try {
				BulkRequestTable table = broker.listRequestsInBulk(user, ontology_id);
				
				response.setContentType("text");
				response.setStatus(HttpServletResponse.SC_OK);
				table.printTable(response.getWriter());
				
			} finally { 
				broker.close();
			}

		} catch (BrokerException e) {
			handleException(response, e);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try { 
			String bulkResponse = request.getParameter("update");
			if(bulkResponse == null) {
				String msg = "No 'update' parameter given.";
				raiseException(response, HttpServletResponse.SC_BAD_REQUEST, msg);
				return;
			}

			bulkResponse = URLDecoder.decode(bulkResponse, "UTF-8");
			BulkResponseTable table = new BulkResponseTable(bulkResponse);

			UserCredentials creds = null;

			Broker broker = getBroker();
			try { 
				broker.respondInBulk(creds, table);
			} finally { 
				broker.close();
			}

			//response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Not yet implemented.");
			response.sendRedirect("/");

		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
