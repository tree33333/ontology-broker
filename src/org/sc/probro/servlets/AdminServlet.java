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

import static java.lang.String.*;
import java.io.IOException;
import java.sql.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.sc.probro.BrokerProperties;

public class AdminServlet extends BrokerServlet {
	
	public AdminServlet(BrokerProperties props) { 
		super(props);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try { 
			Connection cxn = dbSource.getConnection();
			StringBuilder sb = new StringBuilder();

			try {
				Statement stmt = cxn.createStatement();
				try {
					sb.append(p(
							"# Users: %s",
							get(stmt.executeQuery("select count(*) from users"))));
					sb.append(p(
							"# Ontologies: %s",
							get(stmt.executeQuery("select count(*) from ontologys"))));
					sb.append(p(
							"# Terms: %s",
							get(stmt.executeQuery("select count(*) from provisionalterms"))));
					sb.append(p(
							"# Requests: %s",
							get(stmt.executeQuery("select count(*) from requests"))));
					sb.append(p(
							"# Metadata: %s",
							get(stmt.executeQuery("select count(*) from metadatas"))));
					
				} finally { 
					stmt.close();
				}
				
			} finally {
				cxn.close();
			}
			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(sb.toString());
			
		} catch(SQLException e) { 
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	private String p(String fmt, String... args) { 
		return format("<p>%s</p>", format(fmt, args));
	}
	
	private String get(ResultSet rs) throws SQLException { 
		try { 
			rs.next();
			return rs.getObject(1).toString();
			
		} finally { 
			rs.close();
		}
	}
}
