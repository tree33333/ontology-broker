package org.sc.probro.exceptions;

import javax.servlet.http.HttpServletResponse;

public class GoneException extends BrokerException {
	public GoneException(String description) {
		super(HttpServletResponse.SC_GONE, description);
	}
}
