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
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.sc.probro.BrokerProperties;

public class ResetServlet extends BrokerServlet {
	
	public ResetServlet(BrokerProperties props) { 
		super(props);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try { 
			Connection cxn = dbSource.getConnection();
			StringBuilder sb = new StringBuilder();
			
			Log.warn("Resetting database...");

			try {
				cxn.setAutoCommit(false);
				
				Statement stmt = cxn.createStatement();
				try {
					int numMetadata = stmt.executeUpdate("delete from metadatas");
					int numTerms = stmt.executeUpdate("delete from provisionalterms");
					int numRequests = stmt.executeUpdate("delete from requests");
					int numUsers = stmt.executeUpdate("delete from users where user_id != 1");
					int numOntologies = stmt.executeUpdate("delete from ontologys where ontology_id != 1");
					
					sb.append(String.format("<p>Deleted %d Metadata rows</p>", numMetadata));
					sb.append(String.format("<p>Deleted %d ProvisionalTerm rows</p>", numTerms));
					sb.append(String.format("<p>Deleted %d Request rows</p>", numRequests));
					sb.append(String.format("<p>Deleted %d User rows</p>", numUsers));
					sb.append(String.format("<p>Deleted %d Ontology rows</p>", numOntologies));

					Log.info("Completed reset.");
					
				} finally { 
					stmt.close();
				}
				
				cxn.commit();
				cxn.setAutoCommit(true);

			} finally {
				if(!cxn.getAutoCommit()) {
					Log.warn("Rolling back commit.");
					cxn.rollback(); 
				}
				cxn.close();
			}
			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(sb.toString());
			
		} catch(SQLException e) { 
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
}
