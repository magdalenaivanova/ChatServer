package com.sap.internship.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.Map;

import com.sap.internship.chat.protocol_units.BasicCommand;
import com.sap.internship.chat.protocol_units.ChatProtocol;
import com.sap.internship.chat.protocol_units.ChatServerProtocolException;
import com.sap.internship.chat.protocol_units.ServerResponse;

/**
 * The server executes ClienTask for each connected client.
 * Keeps all the information about the client: user name, time of connection,
 * socket and IO streams.
 */
public class ClientTask implements Runnable {

	/**
	 * If the client doesn't send anything for <code>CONNECTION_TIMEOUT</code>ms
	 * the connection will be closed.
	 */
	private static final int CONNECTION_TIMEOUT = 300_000; // 5min
	private static final int LOG_ON_TIMEOUT = 10000; // 10 seconds

	private ChatServer chatServer;
	// client's user name; null if the client is not logged in
	private String userName;

	// Needed for current client session
	private int sessionID;
	private Timestamp timeOfConnection;
	private BufferedReader inputFromClient;
	private PrintWriter outputToClient;
	private Socket clientConnection;

	// Client commands executors
	private ClientCommandsExecutor commandsExecutor;

	/** True if there is an active socket connection to the server. */
	private boolean isConnected;
	/** True if the client has assigned user name. */
	private boolean isLoggedIn;

	/**
	 * Constructs new client {@link Runnable} that executes all client tasks on
	 * the server. Sets socket's timeout to {@link #CONNECTION_TIMEOUT}.
	 * 
	 * @param connection
	 *            Client's socket.
	 * @param server
	 *            Reference to the server that will run the task.
	 * @param connectionStartTime
	 *            The time of connection establishment.
	 */
	public ClientTask(Socket connection, ChatServer server, Timestamp connectionStartTime) {
		this.clientConnection = connection;
		this.chatServer = server;
		this.timeOfConnection = connectionStartTime;
	}

	/*
	 * Opens client's I/O streams. If the client register successfully then
	 * starts to handle his input commands until the connection is closed.
	 */
	@Override
	public void run() {
		try {
			setup();
			register();
			startNewSession();
			handleClientInput();

		} catch (ClientTimeOutException e) {
			System.out.println("Client tried to login/register but exceeded timeout... disconnecting...");
		} catch (IOException ioException) {
			ChatServer.logException(ioException);
		} finally {
			disconnectFromServer();
		}
	}

	/**
	 * Adds client's IP and time of connection to database. Notifies server's
	 * administrator, other connected clients and current client for successful
	 * connection.
	 * Sets client's connection timeout.
	 */
	private void startNewSession() {
		outputToClient.println(ServerResponse.REGISTER.getSuccessMessage(userName));
		setClientConnectionTimeout(CONNECTION_TIMEOUT);
	}

	/**
	 * Lets the user choose between registering or logging in into the chat
	 * server.
	 * 
	 * @throws IOException
	 *             If server fails to read client's input.
	 * @throws ClientTimeOutException
	 */
	private void register() throws ClientTimeOutException, IOException {
		String commandLine = null;
		setClientConnectionTimeout(LOG_ON_TIMEOUT);

		while ((commandLine = inputFromClient.readLine()) != null && isConnected()) {
			
			if(!ChatProtocol.isValidRegisterCommand(commandLine)) {
				this.outputToClient.println(ServerResponse.INVALID_COMMAND.getErrorMessage());
			}
			BasicCommand command = null;
			try {
				command = ChatProtocol.getRegisterCommand(commandLine);
				commandsExecutor.executeBasicCommand(command, commandLine);
			} catch (ChatServerProtocolException exc) {
				//TODO Log exception
				exc.printStackTrace();
			}
			if (isLoggedIn) {
				break;
			}
		}
	}
	

	/**
	 * Creates the input and output streams from client's socket. Sets
	 * {@link #isConnected} to <code>true</code>.
	 * 
	 * @throws IOException
	 *             if one of the streams' creation fails. If so, it doesn't
	 *             close the other stream or client's socket.
	 */
	private void setup() throws IOException {
		this.isConnected = true;
		try {
			InputStream inputStream = clientConnection.getInputStream();
			inputFromClient = new BufferedReader(new InputStreamReader(inputStream));
		} catch (IOException ioException) {
			throw new IOException("[SERVER | CLIENT THREAD] Failed to setup input stream. ", ioException);
		}

		try {
			OutputStream outputStream = clientConnection.getOutputStream();
			outputToClient = new PrintWriter(outputStream, true);
		} catch (IOException ioException) {
			throw new IOException("[SERVER | CLIENT THREAD] Failed to setup output stream. ", ioException);
		}
		commandsExecutor = new ClientCommandsExecutor(this);
	}

	/**
	 * Takes care of client's input while the client is connected to the server.
	 * Validates the input and proceeds the client request.
	 * 
	 * @throws IOException
	 *             If the socket closes.
	 */
	private void handleClientInput() throws IOException {
		String userInput;
		while ((userInput = inputFromClient.readLine()) != null && isConnected()) {
			BasicCommand command;
			try {
				command = ChatProtocol.getChatCommand(userInput);
				commandsExecutor.executeBasicCommand(command, userInput);
			} catch (ChatServerProtocolException exc) {
				this.outputToClient.println(exc.getMessage());
			}
		}
	}

	/**
	 * Closes client's socket and streams if there's still connection. <br>
	 * Removes client from server's client map if he's registered.
	 */
	void disconnectFromServer() {
		if (isConnected()) {
			isConnected = false;
			chatServer.removeActiveConnection();
			this.closeConnection();
		}

		if (isLoggedIn()) {
			Map<String, ClientTask> chatClients = chatServer.getConnectedUsers();
			synchronized (chatClients) {
				chatServer.getConnectedUsers().remove(userName);
			}
		}
	}

	/**
	 * Closes client's I/O streams and socket.
	 */
	private void closeConnection() {

		if (outputToClient != null) {
			outputToClient.println("200 Disconnected from the server.");
			outputToClient.close();
		}

		if (clientConnection != null) {
			try {
				clientConnection.close();
			} catch (IOException e) {
				System.err.println("[CLIENT CONNECTION CLOSING] Failed to close" + userName + "'s socket. " + e.getMessage());
			}
		}
		

		if (inputFromClient != null) {
			try {
				inputFromClient.close();
			} catch (IOException e) {
				System.err.println("[CLIENT CONNECTION CLOSING] Failed to close" + userName + "'s input stream. "
						+ e.getMessage());
			}
		}

	}

	/**
	 * @return true if the client is being added to server's map of entries
	 */
	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public void setLogedIn(boolean isLogedIn) {
		this.isLoggedIn = isLogedIn;
	}

	public int getSessionID() {
		return sessionID;
	}

	void setSessionID(int id) {
		this.sessionID = id;
	}

	/**
	 * @return client's user name, which is the key to server's map
	 */
	public String getUserName() {
		return userName;
	}

	void setUserName(String userName) {
		this.userName = userName;
		System.out.println("-" + userName + "-" + this.userName + "-");
	}

	/**
	 * @return true if the client is still connected to the server
	 */
	private boolean isConnected() {
		return isConnected;
	}

	/**
	 * @return client's socket
	 */
	Socket getConnection() {
		return clientConnection;
	}

	ChatServer getServer() {
		return chatServer;
	}

	/**
	 * @return Time of connection to the server
	 */
	Timestamp getConnectionTime() {
		return timeOfConnection;
	}

	/**
	 * @return The output stream to client.
	 */
	public PrintWriter getOutputToClient() {
		return outputToClient;
	}

	/**
	 * @return The input stream from client.
	 */
	public BufferedReader getInputFromClient() {
		return inputFromClient;
	}

	void setClientConnectionTimeout(int timeout) {
		try {
			clientConnection.setSoTimeout(timeout);
		} catch (SocketException e) {
			// If exception is caught no connection timeout is set.
			System.err.println("[CLIENT THREAD] Error setting connection timeout. " + e.getMessage());
		}
	}
}