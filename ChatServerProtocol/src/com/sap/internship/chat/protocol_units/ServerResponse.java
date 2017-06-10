package com.sap.internship.chat.protocol_units;

public enum ServerResponse {
	REGISTER("200 ok %s successfully registerred", "100 err %s already taken!", "200 ok [a-z0-9_-]{3,15} successfully registerred", "100 err [a-z0-9_-]{3,15} already taken!"),
	SEND_TO("200 ok message to %s sent successfully.", "100 err %s does not exists!", "200 ok message to [a-z0-9_-]{3,15} sent successfully.", "100 err [a-z0-9_-]{3,15} does not exists!"),
	SEND_ALL("200 ok message sent successfully.", "100 err server error!"),
	LIST("200 ok .*", "100 err server error!"),
	SEND_FILE_TO("200 file transferred sucessfully", "100 server transfer error!"),
	INVALID_COMMAND("404 Invalid command!");
	
	private String successMessageRegex;
	private String errorMessageRegex;
	private String successMessageFormat;
	private String errorMessageFormat;
	
	private ServerResponse(String errorMessageRegex) {
		this.errorMessageRegex = errorMessageRegex;
	}
	
	private ServerResponse(String successMessageFormat, String errorMessageFormat, String successMessageRegex, String errorMessageRegex) {
		this.successMessageFormat = successMessageFormat;
		this.errorMessageFormat = errorMessageFormat;
		this.successMessageRegex = successMessageRegex;
		this.errorMessageRegex = errorMessageRegex;
	}
	
	private ServerResponse(String successMessageRegex, String errorMessageRegex) {
		this.successMessageRegex = successMessageRegex;
		this.errorMessageRegex = errorMessageRegex;
	}
	
	public String getSuccessMessageRegex() {
		return successMessageRegex;
	}

	
	public String getErrorMessageRegex() {
		return errorMessageRegex;
	}

	public String getSuccessMessage(String userName) {
		return String.format(successMessageFormat, userName);
	}
	
	public String getErrorMessage() {
		return errorMessageRegex;
	}
	
	public String getErrorMessage(String userName) {
		return errorMessageFormat!=null ? String.format(errorMessageFormat, userName) : errorMessageRegex;
	}
}