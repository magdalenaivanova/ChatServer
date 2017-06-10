package com.sap.internship.chat.server;

import java.util.List;
import java.util.Map;

import com.sap.internship.chat.protocol_units.BasicCommand;
import com.sap.internship.chat.protocol_units.ChatProtocol;
import com.sap.internship.chat.protocol_units.ChatServerProtocolException;
import com.sap.internship.chat.protocol_units.ServerResponse;

/**
 * Executes user commands according to {@link ChatProtocol}
 */
public class ClientCommandsExecutor {
	private String userName;
	private ChatServer server;
	private ClientTask clientTask;
	
	/**
	 * Constructs new {@link ClientCommandsExecutor} for the given {@link ClientTask}
	 * @param clientProcess
	 * 			Client whose commands to execute. 
	 */
	public ClientCommandsExecutor(ClientTask clientTask) {
		this.clientTask = clientTask;
		this.server = clientTask.getServer();
	}

	/**
	 * Executes user command.
	 * 
	 * @param command
	 *            The command to be executed
	 * @param userInput
	 *            User's input. Depending on the command, generates the
	 *            message to be sent according to {@link ChatProtocol}.
	 * @throws ChatServerProtocolException 
	 */
	public void executeBasicCommand(BasicCommand command, String userInput) throws ChatServerProtocolException {
		switch (command) {
			case ALL: {
				String msg = ChatProtocol.generateMessage(BasicCommand.ALL, userInput, userName);					
				sendMessageToAll(msg);
			} break;
			case TO: {
				String msg = ChatProtocol.generateMessage(BasicCommand.TO, userInput, userName);
				String receiver = ChatProtocol.extractMessageReceiver(userInput);
				sendMessageToUser(receiver, msg);
			} break;
			case LIST:
				listAllClients();
				break;
			case QUIT:
				clientTask.disconnectFromServer();
				break;
			case HELP:
				this.clientTask.getOutputToClient().println(BasicCommand.userCommandsHelp());
				break;
			default:
				sendMessageToUser(userName, "Invalid input or command not available in current context");
				break;
		}
	}
	
	/**
	 * Sets client's user name and adds him to currently connected clients.
	 * 
	 * @return true If the user register within specified time. If he doesn't he
	 *         receives a warning message that he'll be disconnected.
	 */
	public boolean register(String userName) {
		Map<String,ClientTask> clients = server.getConnectedUsers();
		synchronized (clients) {
			if(clients.containsKey(userName)) {
				return false;
			}
			clients.put(userName, clientTask);
		}
		
		List<ClientTask> loggedOn = server.getLoggedOnClients();
		synchronized (loggedOn) {
			loggedOn.remove(clientTask);
		}
		
		this.userName = userName;
		clientTask.setLogedIn(true);
		clientTask.setUserName(userName);
		return true;
	}
	
	
	/**
	 * Sends all registered client's user names to client.
	 */
	public void listAllClients() {
		Map<String, ClientTask> clients = server.getConnectedUsers();
		StringBuilder clientsList = new StringBuilder();
		clientsList.append("List of Chat Server's clients:\n");
		synchronized (clients) {
			for (String client : clients.keySet()) {
				clientsList.append(client+"\n");
			}
		}
		sendMessageToUser(userName, clientsList.toString());
	}

	/**
	 * Sends the passed message to all registered chat clients.
	 * 
	 * @param msg
	 *            The message to be sent.
	 */
	//FIXME Message
	public boolean sendMessageToAll(String msg) {
		Map<String, ClientTask> clients = server.getConnectedUsers();
		synchronized (clients) {			
			if (clients.isEmpty()) {
				return false;
			}
			
			for (String user : clients.keySet()) {
				if (!userName.equals(user)) {
					clients.get(user).getOutputToClient().println(msg);
				}
			}
		}
		return true;
	}

	/**
	 * Sends <code>message</code> to client with user name <i>receiver</i>.
	 * 
	 * @param receiver
	 *            User name of connected chat client.
	 * @param message
	 *            The message to be sent.
	 */
	public boolean sendMessageToUser(String receiver, String msg) {
		Map<String, ClientTask> clients = server.getConnectedUsers();
		synchronized (clients) {			
			ClientTask user = clients.get(receiver);
			if (user != null) {
				user.getOutputToClient().println(msg);
				sendSuccessResponse(ServerResponse.SEND_TO.getSuccessMessage(receiver), userName);
				return true;
			}
		}
		sendErrorResponse(ServerResponse.SEND_TO.getErrorMessage(receiver), userName);
		return false;
	}

	private void sendSuccessResponse(String response, String userName) {
		Map<String, ClientTask> clients = server.getConnectedUsers();
		synchronized (clients) { 
			clients.get(userName).getOutputToClient().println(response);
		}
		
	}

	private void sendErrorResponse(String response, String userName) {
		Map<String, ClientTask> clients = server.getConnectedUsers();
		synchronized (clients) { 
			clients.get(userName).getOutputToClient().println(response);
		}
	}
}