package org.sc.probro.data;

public class DBModelException extends Exception {

	public DBModelException() {
	}

	public DBModelException(String message) {
		super(message);
	}

	public DBModelException(Throwable cause) {
		super(cause);
	}

	public DBModelException(String message, Throwable cause) {
		super(message, cause);
	}
}
