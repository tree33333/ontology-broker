package org.sc.probro.servlets;

import javax.servlet.http.HttpServletRequest;

import org.sc.probro.Broker;
import org.sc.probro.BrokerProperties;
import org.sc.probro.BrokerStart;
import org.sc.probro.LocalBroker;
import org.sc.probro.data.DBModelException;
import org.sc.probro.exceptions.BrokerException;

public class BrokerServlet extends SkeletonDBServlet {

	public BrokerServlet(BrokerProperties props) {
		super(props);
	}
	
	public Broker getBroker() throws BrokerException { 
		try {
			return new LocalBroker(BrokerStart.getURLPrefix(), getBrokerModel());
		} catch (DBModelException e) {
			throw new BrokerException(e);
		}
	}
	
	public String getContentType(HttpServletRequest request) throws BrokerException {
		String fmt = getOptionalParam(request, "format", String.class);
		return fmt != null ? contentTypeFromFormat(fmt, "json", "html") : CONTENT_TYPE_JSON;
	}
	

}
