package org.sc.probro.exceptions;

import javax.servlet.http.HttpServletResponse;

public class BadRequestException extends BrokerException {
	public BadRequestException(String description) {
		super(HttpServletResponse.SC_BAD_REQUEST, description);
	}
}
