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

import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.json.*;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.exceptions.GoneException;

import java.io.*;
import java.util.*;

public class SupervisorServlet extends SkeletonServlet {
	
	private File file = new File("data/supervisor.js");  // TODO: this shouldn't be hard coded.
	private JSONObject supervisor;
	
	public SupervisorServlet() { 
		supervisor = null;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 

		try {
			if(supervisor == null) { 
				if(!file.exists()) { 
					throw new GoneException("supervisor data file was not configured by broker administrator.");
				}
				
				try { 
					BufferedReader br = new BufferedReader(new FileReader(file));
					StringBuilder sb = new StringBuilder();
					int charInt; 
					while((charInt = br.read()) != -1) { 
						sb.append((char)charInt);
					}
					
					supervisor = new JSONObject(sb.toString());
				} catch(IOException e) { 
					throw new GoneException("supervisor.js file was incorrectly configured (unreadable) by administator.");
				} catch (JSONException e) {
					throw new BrokerException(e);
				}
			}
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(CONTENT_TYPE_JSON);
			response.getWriter().println(supervisor.toString());
			
		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
}
