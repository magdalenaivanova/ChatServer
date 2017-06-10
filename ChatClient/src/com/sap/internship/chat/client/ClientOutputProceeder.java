package com.sap.internship.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.internship.chat.protocol_units.BasicCommand;
import com.sap.internship.chat.protocol_units.ChatProtocol;

/**
 * Runnable that reads client's console input, validates it through {@link ChatProtocol}
 * and if it's valid sends client's output to the server.
 */
public class ClientOutputProceeder implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(ClientOutputProceeder.class.getName());
	
	private ChatClient chatClient;
	private PrintWriter outputToServer;
	private BufferedReader inputFromConsole;
	
	private static final String QUIT_COMMAND = "quit";
	
	/**
	 * Constructs {@link Runnable} task that takes user input from console and sends it
	 * to the server while there is a connection.
	 * 
	 * @param client Reference to the client that starts the thread
	 * @param clientSocket Reference to client's socket
	 * @param outputToServer Client's output stream to the server
	 * @param inputFromConsole Client's input stream to the server
	 * Sets {@link #isConnected} to true 
	 */
	public ClientOutputProceeder(ChatClient client, PrintWriter outputToServer, BufferedReader inputFromConsole) {
		this.chatClient = client;
		this.outputToServer = outputToServer;
		this.inputFromConsole = inputFromConsole;
	}

	@Override
	public void run() {
		sendOutput();
	}
	
	/**
	 * Waits for input from the console while there is
	 * a connection to the server, then depending on it's value:
	 * <br>Sends it for processing to the server.
	 * <br>Processes it here if it's "help".
	 * <br>Might catch {@link IOException} if the connection to server is closed.
	 */
	private void sendOutput() {
		String message;
		try {
			while (chatClient.isConnected() && (message = inputFromConsole.readLine()) != null && chatClient.isConnected()) {
				if (!ChatProtocol.isValidChatCommand(message)) {
					continue;
				}
				outputToServer.println(message);
				
				if (message.equals(QUIT_COMMAND)) {
					break;
				}
			}
		} catch (IOException ioExc) {
			System.err.println("Failed to read user's input...");
			LOGGER.log(Level.ALL, "Failed to read user's input.", ioExc);
		} finally {
			chatClient.disconnectFromServer();
		}
	}
}