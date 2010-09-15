package org.sc.probro.exceptions;

import javax.servlet.http.HttpServletResponse;

public class RequestEntityTooLargeException extends BrokerException {
	public RequestEntityTooLargeException(String description) {
		super(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, description);
	}
}
