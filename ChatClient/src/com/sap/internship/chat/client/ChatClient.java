package com.sap.internship.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.internship.chat.protocol_units.BasicCommand;
import com.sap.internship.chat.protocol_units.ChatProtocol;
import com.sap.internship.chat.protocol_units.ChatServerProtocolException;
import com.sap.internship.chat.protocol_units.ServerResponse;

/**
 * Client for Chat Server that implements {@link ChatProtocol} and validates
 * it's output towards it. This class is responsible for: <br>
 * - Launching client's program. <br>
 * - Get valid server IP address and port to connect to from user. <br>
 * - Process and validate user input by {@link ChatProtocol}. <br>
 * - Establishing connection with server. <br>
 * - Processing input from server.
 */
public class ChatClient {
	private static final Logger LOGGER = Logger.getLogger(ChatClient.class.getName());
	private static final String DEFAULT_SERVER = "localhost";
	private static final String DEFAULT_PORT = "53333";

	// Server's address and port to which the client connects.
	// Specified by the user or given default values if not so
	private String serverIP;
	private int serverPort;

	// ------Client session related------

	private Socket clientSocket;
	private PrintWriter outputToServer;
	private BufferedReader inputFromServer;
	private BufferedReader inputFromConsole;
	// Thread that reads input from the console and sends it to the server.
	private Thread writeToServer;
	// True if the connection is established.
	private boolean connected;
	// True if the client successfully logged on 
	private boolean loggedOn;

	/**
	 * Constructs new chat client that connects to chat server with IP
	 * <code>serverIP</code> and port number <code>port</code>.
	 * 
	 * @param serverIP
	 *            IP address of the chat server we want to connect
	 * @param port
	 *            server port
	 */
	public ChatClient() {
		inputFromConsole = new BufferedReader(new InputStreamReader(System.in));
	}

	/**
	 * Opens client's {@link Socket}. <br>
	 * Setups client's input and output streams to the server. <br>
	 * Opens user's console input stream and starts thread that reads this input
	 * and sends it to the server through the output stream.
	 * 
	 * @throws ClientException
	 *             if error occurred when staring the client program.
	 */
	public void startClientProgram(String... args) throws ClientException {
		if (args == null) {
			args = new String[] { DEFAULT_SERVER, DEFAULT_PORT };
		}
		try {
			connectToServer(args);
		} catch (IOException ioExc) {
			throw new ClientException("Failed to connect to server. ", ioExc);
		}

		try {
			register();
		} catch (IOException ioExc) {
			throw new ClientException("Failed to log in. ", ioExc);
		}

		if (!connected) {
			System.out.println("Program closed. :)");
			return;
		}

		if (loggedOn) {
			startOutputProceederThread();
			receiveInputFromServer();
		}
		disconnectFromServer();
	}

	/**
	 * Registers or logs in the user if there is connection to server. Validates
	 * user's command and credentials and if they are valid sends them to the
	 * server.
	 * 
	 * @throws IOException
	 *             if some IO error occurs while reading user's input.
	 * @throws ClientException 
	 */
	private void register() throws IOException, ClientException {
		String commandLine = null;
		System.out.println("Welcome to Chat Server!\n Type 'register <username> ' to register in the chat. Type 'bye' to close the program.");
		while ((commandLine = inputFromConsole.readLine()) != null) {
			BasicCommand command = null;
			try {				
				command = ChatProtocol.getRegisterCommand(commandLine);
			} catch (ChatServerProtocolException exc) {
				System.err.println(exc.getMessage());
				continue;
			}
			
			
			if (command.equals(BasicCommand.QUIT)) {
				return;
			}

			outputToServer.println(commandLine);
			String serverInput = null;
			while((serverInput = inputFromServer.readLine()) != null) {
				if(serverInput.matches(ServerResponse.REGISTER.getSuccessMessageRegex())) { // || serverInput.matches(ServerResponse.REGISTER.getErrorMessage())) {
					System.out.println(serverInput);
					break;
				}
				if(serverInput.matches(ServerResponse.REGISTER.getErrorMessageRegex()) || serverInput.matches(ServerResponse.INVALID_COMMAND.getErrorMessage())) {
					System.err.println(serverInput);
				} else {
					throw new ClientException("Invalid input from server! Disconnection required.");
				}
			}
		}

	}

	/**
	 * Opens: <br>
	 * - client socket <br>
	 * - output stream to server <br>
	 * - input stream from server <br>
	 * - input stream from console
	 * 
	 * @throws IOException
	 *             If socket or stream creation fails.
	 */
	private void connectToServer(String... args) throws IOException {
		try {
			try {
				clientSocket = new Socket(serverIP, serverPort);
			} catch (IOException ioExc) {
				throw new IOException("[CONNECTION SETUP] Failed to create socket.", ioExc);
			}

			try {
				OutputStream outputStream = clientSocket.getOutputStream();
				outputToServer = new PrintWriter(outputStream, true);
			} catch (IOException ioExc) {
				throw new IOException("[CONNECTION SETUP] Failed to setup output stream.", ioExc);
			}

			try {
				InputStream inputStream = clientSocket.getInputStream();
				inputFromServer = new BufferedReader(new InputStreamReader(inputStream));
			} catch (IOException ioExc) {
				throw new IOException("[CONNECTION SETUP] Failed to setup input stream.", ioExc);
			}
			connected = true;
		} catch (IOException ioExc) {
			disconnectFromServer();
			throw ioExc;
		}
	}

	/**
	 * Starts a thread for reading client's console input and sending it to the
	 * server through the output stream.
	 */
	private void startOutputProceederThread() {
		writeToServer = new Thread(new ClientOutputProceeder(this, outputToServer, inputFromConsole));
		writeToServer.start();
	}

	/**
	 * Closes: <br>
	 * - Client's socket <br>
	 * - Console input stream <br>
	 * - Input stream from server <br>
	 * - Output stream to server. <br>
	 * Might catch IOException while closing socket or some of the streams.
	 */
	synchronized void disconnectFromServer() {

		connected = false;
		if (inputFromServer != null) {
			try {
				inputFromServer.close();
			} catch (IOException ioExc) {
				LOGGER.log(Level.ALL, "[CONNECTION CLOSING] Failed to close server input stream.", ioExc);
			}
		}

		if (outputToServer != null) {
			outputToServer.close();
		}

		if (clientSocket != null) {
			try {
				clientSocket.close();
			} catch (IOException ioExc) {
				LOGGER.log(Level.ALL, "[CONNECTION CLOSING] Failed to close socket. ", ioExc);
			}
		}

		// WRONG WRONG WRONG WRONG WRONG WRONG
		System.exit(0);

		if (inputFromConsole != null) {
			try {
				inputFromConsole.close();
			} catch (IOException ioExc) {
				LOGGER.log(Level.ALL, "[CONNECTION CLOSING] Failed to close console input. ", ioExc);
			}
		}
	}

	/**
	 * Waits for input from the server, then displays it to the user Might catch
	 * exception while waiting for input if connection closes unexpectedly
	 * Finally closes clients's streams and socket
	 */
	private void receiveInputFromServer() {
		String message;
		try {
			while (isConnected() && (message = inputFromServer.readLine()) != null) {
				System.out.println(message);
			}
		} catch (IOException ioExc) {
			LOGGER.log(Level.ALL, "Failed to receive input from server. ", ioExc);
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public static void main(String[] args) {
		ChatClient client = new ChatClient();
		try {
			client.startClientProgram(args);
		} catch (ClientException exc) {
			LOGGER.log(Level.ALL, exc.getMessage(), exc.getCause());
		}
	}
}