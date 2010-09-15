package org.sc.probro.exceptions;

import javax.servlet.http.HttpServletResponse;

public class UnauthorizedException extends BrokerException {
	public UnauthorizedException(String description) {
		super(HttpServletResponse.SC_UNAUTHORIZED, description);
	}
}
