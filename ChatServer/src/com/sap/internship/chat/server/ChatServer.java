package com.sap.internship.chat.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.internship.chat.protocol_units.ChatProtocol;

/**
 * Chat server that accepts client connections and lets clients write
 * messages to each other. The clients can register or log in. Their data is
 * kept in database, which the server manages.
 * 
 * This class is responsible for: <br>
 * - Creating {@link ServerSocket} with port number provided by administrator.
 * <br>
 * - Creating administrator thread responsible for interaction with admin and
 * executing his commands. <br>
 * - Accepting client sockets. <br>
 * - Starting individual client threads responsible for client-server
 * interaction.
 */
public class ChatServer {
	private final static Logger LOGGER = Logger.getLogger(ChatServer.class.getName());
	
	/** The maximum number of threads that can be in the thread pool. */
	private final static int MAXIMUM_RUNNING_THREADS = 10;

	// Server's socket and bounded port
	private int serverPortNumber;
	private ServerSocket serverSocket;

	// Keeps all currently connected clients
	private Map<String, ClientTask> loggedInClients;
	private List<ClientTask> loggedOnClients;
	
	// Executes client tasks with MAXIMUM_RUNNING_THREADS threads
	private ExecutorService clientTasksExecutor;

	/**
	 * {@code true} if: <br>
	 * - Server socket is still open and accepting connections. <br>
	 * - IO streams are still open. <br>
	 * - Tasks executor is still running.
	 */
	private Boolean isRunning;

	/**
	 * {@code false} if the server is still accepting connections and handling
	 * client requests
	 */
	private boolean isClosed;
	/**
	 * When the server socket accepts a new socket connection we first check if
	 * the <b>{@code activeConnections}</b> variable is less than
	 * {@link #MAXIMUM_RUNNING_THREADS}. If it is we start a new client thread,
	 * add the client to {@code chatClients} map and add him to the chat room.
	 * If it isn't then we just close the connection.
	 */
	private AtomicInteger activeConnections;

	// Constructs a ChatServer object.
	public ChatServer() {
		isRunning = false;
		isClosed = true;
	}

	/**
	 * Creates {@link ServerSocket} and starts waiting for connections on it.
	 * Might catch {@link IOException} if socket fails to open or connection
	 * fails while running. Finally shuts down the server - closing the socket
	 * and all client socket connections.
	 * 
	 * @param args
	 *            command line arguments. No problem if no arguments are
	 *            provided.
	 * @throws ChatServerException
	 *             if error occurs while setting up the chat server.
	 */
	public void startRunning(String... args) throws ChatServerException {
		// throws ChatServerException
		try {
			setupServer(args);
			waitForConnections();
		} finally {
			stopAcceptingConnections();
			stopTasksExecutor();			
		}
	}

	/**
	 * Creates {@link ServerSocket} and assigns it the port number passed to the
	 * constructor. Creates thread pool with {@link #MAXIMUM_RUNNING_THREADS}
	 * threads.
	 * 
	 * @throws ChatServerException If server socket or connection to database couldn't be created.
	 */
	private void setupServer(String... args) throws ChatServerException {
		LOGGER.setLevel(Level.ALL);
		setServerPort(args);

		try {
			serverSocket = new ServerSocket(serverPortNumber);
			this.isClosed = false;
		} catch (IOException ioExc) {
			throw new ChatServerException("[SERVER SETUP] Failed to create server socket.", ioExc);
		}

		this.loggedInClients = new HashMap<>(MAXIMUM_RUNNING_THREADS);
		this.loggedOnClients = new ArrayList<>();
		this.activeConnections = new AtomicInteger(0);
		this.clientTasksExecutor = Executors.newFixedThreadPool(MAXIMUM_RUNNING_THREADS);

		this.isRunning = true;
	}

	/**
	 * Sets valid server port to server from admin input.
	 * 
	 * @param args
	 *            command line arguments that should be checked before wanting
	 *            another input
	 */
	private void setServerPort(String[] args) {
		if (args.length == 1 && ChatProtocol.isValidServerPort(args[0])) {
			serverPortNumber = Integer.parseInt(args[0]);
			return;
		}
		
		Scanner sc = new Scanner(System.in);
		String port = null;
		System.out.println("Please enter valid port number:");
		while ((port = sc.nextLine()) != null) {
			
			if (!ChatProtocol.isValidServerPort(port)) {
				System.out.println("Please enter valid port number:");
				continue;
			}
			serverPortNumber = Integer.parseInt(port);
			break;
		}
		sc.close();
	}

	/**
	 * Waits for new client connections to accept. If there are
	 * {@link #MAXIMUM_RUNNING_THREADS} connected the server declines client's
	 * request and sends him a warning message for disconnection, otherwise new
	 * client thread is started.
	 * 
	 * @throws IOException
	 */
	private void waitForConnections() throws ChatServerException {
		while (isRunning()) {
			Socket connection = null;

			try {
				connection = serverSocket.accept();
			} catch (IOException ioException) {
				stopAcceptingConnections();
				stopTasksExecutor();
				throw new ChatServerException("[ACCEPTING CLIENTS] Server socket failed to accept client connections. ", ioException);
			}

			if (isClosed) {
				return;
			}

			if (activeConnections.get() < MAXIMUM_RUNNING_THREADS) {
				activeConnections.incrementAndGet();
				ClientTask clientTask = new ClientTask(connection, this, new Timestamp(new Date().getTime()));
				clientTasksExecutor.execute(clientTask);
				
				synchronized (loggedOnClients) {
					loggedOnClients.add(clientTask);
				}
				continue;
			}

			try (PrintWriter outputToClient = new PrintWriter(connection.getOutputStream(), true)) {
				outputToClient.println("Chat room full - try again later. :)");
			} catch (IOException ioExc) {
				// FIXME exception logging?
				ChatServer.logException(new IOException("[NEW CONNECTION ESTABLISHMENT] Failed to send warning message to user...", ioExc));
			}

			try {
				connection.close();
			} catch (IOException ioExc) {
				// FIXME exception logging?
				ChatServer.logException(new IOException("[NEW CONNECTION ESTABLISHMENT] Failed to close client connection. ", ioExc));
			}
		}
	}

	/**
	 * Closes {@link ServerSocket} if it's open.
	 * 
	 * @throws IOException
	 *             if it fails to close the socket.
	 */
	void stopAcceptingConnections() {
		if (!isRunning || isClosed) {
			return;
		}

		Socket dummySocket;
		try {
			isClosed = true;
			dummySocket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
			dummySocket.close();
		} catch (IOException ioExc) {
			ChatServer.logException(ioExc);
		}

		try {
			serverSocket.close();
		} catch (IOException ioExc) {
			LOGGER.log(Level.FINEST,"[SERVER] Couldn't close ServerSocket. ", ioExc);
		}
	}

	/**
	 * Shuts down {@link ExecutorService} and sets {@link isRunning} to
	 * {@code false}.
	 */
	void stopTasksExecutor() {
		if (isRunning()) {
			isRunning = false;
			clientTasksExecutor.shutdownNow();
		}
	}

	/**
	 * Decrements {@link #activeConnections}.
	 */
	public void removeActiveConnection() {
		activeConnections.decrementAndGet();
	}

	// ---------------------GETTERS AND SETTERS---------------------

	/**
	 * @return <b>{@code true}</b> if the server is currently running and
	 *         accepting client connection.
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Returns {@link Map} with chat clients that are currently connected to the
	 * server.
	 * 
	 * @return {@link HashMap} containing all the connected clients - their
	 *         associated runnable values and user names as keys.
	 */
	public Map<String, ClientTask> getConnectedUsers() {
		return loggedInClients;
	}

	public List<ClientTask> getLoggedOnClients() {
		return loggedOnClients;
	}
	
	/**
	 * Returns the executor service used by the server to execute client
	 * threads.
	 * 
	 * @return ExecutorService that executes client tasks.
	 */
	public ExecutorService getExecutorService() {
		return this.clientTasksExecutor;
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setClosed(boolean closed) {
		isClosed = closed;
	}

	public static void logException(Throwable t) {
		System.out.println(getStackTrace(t));
	}
	
	private static String getStackTrace(Throwable t) {
		String msg = "";
		if (t instanceof SQLException) {
			SQLException sqlExc = (SQLException) t;
			msg = msg + "SQLState: " + sqlExc.getSQLState() + "\n";
			msg = msg + "Error Code: " + sqlExc.getErrorCode() + "\n";
			msg = msg + "Message: " + sqlExc.getMessage() + "\n";
		}
		StringWriter stringWriter = new StringWriter();
		t.printStackTrace(new PrintWriter(stringWriter, true));
		return msg + stringWriter.toString();
	}

	public static void main(String[] args) {
		ChatServer myChatServer = new ChatServer();
		try {
			myChatServer.startRunning(args);
		} catch (ChatServerException exc) {
			ChatServer.logException(exc);
		}
	}
}