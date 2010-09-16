package org.sc.probro.exceptions;

import javax.servlet.http.HttpServletResponse;

public class InternalServerErrorException extends BrokerException {
	
	public InternalServerErrorException(String msg) { 
		super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
	}
	
	public InternalServerErrorException(Throwable t) {
		super(t);
	}
}
