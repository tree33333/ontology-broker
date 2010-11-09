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

import java.util.*;
import java.io.*;
import java.net.URLEncoder;
import java.util.regex.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONStringer;
import org.sc.probro.*;
import org.sc.probro.data.*;
import org.sc.probro.exceptions.*;

public class OntologyFieldServlet extends BrokerServlet {

	public OntologyFieldServlet(BrokerProperties props) {
		super(props);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { 
	
		try { 
			String ontologyID = (String)request.getAttribute("ontology_id");
			String fieldName = (String)request.getAttribute("field_name");
			String fieldDescription = (String)request.getAttribute("field_description");
			String fieldMetadataKey = (String)request.getAttribute("metdata_key");
			
			if(ontologyID == null || fieldName == null || fieldDescription == null || fieldMetadataKey == null) { 
				throw new BadRequestException("Missing metadata attribute.");
			}
			
			UserCredentials user = new UserCredentials();
			
			Broker broker = getBroker();
			try { 
				Ontology ont = broker.checkOntology(user, ontologyID);
				
				OntologyField newField = new OntologyField();
				newField.name = fieldName;
				newField.description = fieldDescription;
				newField.metadata_key = fieldMetadataKey;
				
				broker.addOntologyField(user, ont, newField);
				
				//response.setStatus(HttpServletResponse.SC_OK);
				response.sendRedirect(ontologyID);
				
			} finally { 
				broker.close();
			}
 		
		} catch(BrokerException e) { 
			this.handleException(response, e);
			return;
		}
		
	}

}
