package org.sc.probro.exceptions;

import javax.servlet.http.HttpServletResponse;

public class ConflictException extends BrokerException {
	public ConflictException(String description) {
		super(HttpServletResponse.SC_CONFLICT, description);
	}
}
