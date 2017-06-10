package com.sap.internship.chat.protocol_units;

/**
 * Takes care of the input to the server: - Checks if the commands are allowed
 * and with right number of arguments - In case of sending message: -- Extracts
 * message's receiver (if it's for specific user). -- Generates the message that
 * is being sent to other chat client(s).
 */
public class ChatProtocol {
	private static final String SEND_TO_COMMAND = "send_to";
	private static final String SEND_ALL_COMMAND = "send_all";
	/**
	 * Extracts user's message from the input.
	 * 
	 * @param command
	 *            Depending on which command the user executes the message is
	 *            extracted differently.
	 * @param userInput
	 *            User's input.
	 * @return The message to be sent to other user or users.
	 * @throws ChatServerProtocolException 
	 */
	private static String extractMessage(BasicCommand command, String userInput) throws ChatServerProtocolException {
		switch (command) {
		case ALL:
			return userInput.substring(SEND_ALL_COMMAND.length());
		case TO:
			return userInput.substring(extractMessageReceiver(userInput).length() + SEND_TO_COMMAND.length());
		default:
			throw new IllegalArgumentException("Unexpected command: " + command);
		}
	}

	/**
	 * Extracts the user name of message's receiver from given user input.
	 * 
	 * @param userInput
	 *            User's input from which the receiver of the message is
	 *            extracted.
	 * @return user name of the receiver of the message
	 * @throws ChatServerProtocolException 
	 */
	public static String extractMessageReceiver(String userInput) throws ChatServerProtocolException {
		if(userInput.matches(BasicCommand.TO.getRegex())) {
			return userInput.split(" ")[1];
		}
		throw new ChatServerProtocolException("send_to command arguments mismatch!");
	}

	public static BasicCommand getRegisterCommand(String input) throws ChatServerProtocolException {
		for (BasicCommand command : BasicCommand.getRegisterAvailableCommands()) {
			if(input.matches(command.getRegex())) {
				return command;
			}
		}
		throw new ChatServerProtocolException("Invalid command! Please, type 'help' for more information.");
	}
	
	public static BasicCommand getChatCommand(String input) throws ChatServerProtocolException {
		for (BasicCommand command : BasicCommand.getChatAvailableCommands()) {
			if(input.matches(command.getRegex())) {
				return command;
			}
		}
		throw new ChatServerProtocolException("Invalid command! Type 'register <username> ' to register in the chat. Type 'bye' to close the program.");
	}
	
	public static boolean isValidIpAddress(String address) {
		return address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || address.equals("localhost");
	}
	
	public static boolean isValidServerPort(String port) {
		try {
			Integer.parseInt(port);
		}
		catch (NumberFormatException nfExc) {
			return false;
		}
		return true;
	}

	public static boolean isValidChatCommand(String input) {
		for (BasicCommand command : BasicCommand.getChatAvailableCommands()) {
			if(input.matches(command.getRegex())) {
				return true;
			}
		}
		return false;
	}

	public static String generateMessage(BasicCommand command, String userInput, String userName) throws ChatServerProtocolException {
		return "300 msg_from" + userName + extractMessage(command, userInput);
	}

	public static boolean isValidRegisterCommand(String input) {
		for (BasicCommand command : BasicCommand.getRegisterAvailableCommands()) {
			if(input.matches(command.getRegex())) {
				return true;
			}
		}
		return false;
	}

}