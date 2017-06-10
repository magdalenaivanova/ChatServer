package com.sap.internship.chat.server;

public class ClientTimeOutException extends Exception {

	private static final long serialVersionUID = 1L;

	public ClientTimeOutException() {
		super();
	}

	public ClientTimeOutException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ClientTimeOutException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClientTimeOutException(String message) {
		super(message);
	}

	public ClientTimeOutException(Throwable cause) {
		super(cause);
	}
}
