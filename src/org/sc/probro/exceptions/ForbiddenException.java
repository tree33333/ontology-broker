package org.sc.probro.exceptions;

import javax.servlet.http.HttpServletResponse;

public class ForbiddenException extends BrokerException {
	public ForbiddenException(String description) {
		super(HttpServletResponse.SC_FORBIDDEN, description);
	}
}
