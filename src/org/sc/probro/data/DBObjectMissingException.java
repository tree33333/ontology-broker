package org.sc.probro.data;

public class DBObjectMissingException extends Exception {

	public DBObjectMissingException() {
	}

	public DBObjectMissingException(String message) {
		super(message);
	}

	public DBObjectMissingException(Throwable cause) {
		super(cause);
	}

	public DBObjectMissingException(String message, Throwable cause) {
		super(message, cause);
	}
}
