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

import javax.servlet.http.HttpServletRequest;

import org.sc.probro.Broker;
import org.sc.probro.BrokerProperties;
import org.sc.probro.BrokerStart;
import org.sc.probro.LocalBroker;
import org.sc.probro.data.DBModelException;
import org.sc.probro.exceptions.BrokerException;

public class BrokerServlet extends SkeletonDBServlet {
	
	private BrokerProperties props;

	public BrokerServlet(BrokerProperties props) {
		super(props);
		this.props = props;
	}
	
	public Broker getBroker() throws BrokerException { 
		try {
			return new LocalBroker(props, getBrokerModel());
		} catch (DBModelException e) {
			throw new BrokerException(e);
		}
	}
	
	public String getContentType(HttpServletRequest request) throws BrokerException {
		String fmt = getOptionalParam(request, "format", String.class);
		return fmt != null ? contentTypeFromFormat(fmt, "json", "html") : CONTENT_TYPE_JSON;
	}
	

}
