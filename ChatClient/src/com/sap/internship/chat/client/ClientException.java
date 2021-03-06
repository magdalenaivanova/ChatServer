package com.sap.internship.chat.client;

/**
 * Client gets this exception if some internal error occurs and it's program must be terminated. 
 */
public class ClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public ClientException() {
		super();
	}

	public ClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClientException(String message) {
		super(message);
	}

	public ClientException(Throwable cause) {
		super(cause);
	}
}
