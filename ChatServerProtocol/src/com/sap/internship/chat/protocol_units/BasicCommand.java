package com.sap.internship.chat.protocol_units;

/**
 * Enumeration that contains all allowed commands for the chat-server communication.
 * 
 */
public enum BasicCommand {
	REGISTER("Register: user <username>\r\n", "user [a-z0-9_-]{3,15} .*"),
	ALL("Message all users: send_all <single line message>\r\n", "send_all .*"),
	TO("Message specific user: send_to <username> <single line message>\r\n", "send_to [a-z0-9_-]{3,15} .*"),
	LIST("List currently connected clients: list (no arguments needed)", "list"),
	QUIT("Quit: bye (only one argument needed)", "bye"),
	HELP("List available commands: help (no arguments needed)", "help");
	
	private static final BasicCommand[] registerAvailableCommands = {REGISTER, QUIT};
	private static final BasicCommand[] chatAvailableCommands = {ALL, TO, LIST, QUIT, HELP};
	
	private String usage;
	private String regex;
	

	private BasicCommand(String usage, String regex){
		this.usage = usage;
		this.regex = regex;
	}

	
//	/**
//	 * Checks if there is a command equivalent to the given string.
//	 * @param command
//	 * @return true if there is an Enum representation of the given string
//	 */
//	public static boolean contains(String command){
//		for (BasicCommand cmd : BasicCommand.values()) {
//			if(command.toUpperCase().equals(cmd.toString())){
//				return true;
//			}
//		}
//		return false;
//	}
	
	public static String userCommandsHelp() {
		StringBuilder result = new StringBuilder();
		for (BasicCommand basicCommand : BasicCommand.values()) {
			result.append(basicCommand.usage+"\n");
		}
		return result.toString();
	}

	public String getRegex() {
		return regex;
	}
	
	public static BasicCommand[] getChatAvailableCommands() {
		return chatAvailableCommands;
	}
	
	public static BasicCommand[] getRegisterAvailableCommands() {
		return registerAvailableCommands;
	}
}