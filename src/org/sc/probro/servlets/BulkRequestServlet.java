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
import org.sc.probro.BulkTable;
import org.sc.probro.Metadata;
import org.sc.probro.Ontology;
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
public class BulkRequestServlet extends BrokerServlet {
	
	public BulkRequestServlet(BrokerProperties ps) { 
		super(ps);
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try { 
			Broker broker = getBroker();
			try {
				String ontology_id = getRequiredParam(request, "ontology_id", String.class);
				//String ontology_id = request.getRequestURI();
			
				Log.info(String.format("Creating bulk request file for ontology \"%s\"", ontology_id));
			
				UserCredentials user = new UserCredentials();
			
				BulkTable table = broker.listRequestsInBulk(user, ontology_id);
				
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
			
			String ontologyID = getRequiredParam(request, "ontology_id", String.class);

			UserCredentials creds = null;

			Broker broker = getBroker();
			try { 
				Ontology ontology = broker.checkOntology(creds, ontologyID);
				
				bulkResponse = URLDecoder.decode(bulkResponse, "UTF-8");
				BulkTable table = new BulkTable(ontology, bulkResponse);

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
