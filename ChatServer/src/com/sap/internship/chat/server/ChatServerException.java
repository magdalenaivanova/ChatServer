package com.sap.internship.chat.server;

/**
 * Server might get this exception if some internal error occurs and it must be terminated.
 */
public class ChatServerException extends Exception {

	private static final long serialVersionUID = 1L;

	public ChatServerException() {
		super();
	}

	public ChatServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ChatServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChatServerException(String message) {
		super(message);
	}

	public ChatServerException(Throwable cause) {
		super(cause);
	}

}
