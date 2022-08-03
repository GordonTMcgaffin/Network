/**
 * @file    Server.java
 * @brief   chat server
 * @author  G. Mcgaffin (23565608@sun.ac.za)
 * @date    2022-08-02
 */

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * The {@code Server} class listens for new connections made by clients on a
 * specific port. When a client tries to connect to the port, the connection is
 * accepted and handed over to a {@code ClientHandler}, which will then be run
 * on a new thread.
 */
public class Server {

    /* --- Static Variables ------------------------------------------------- */

    /** port to listen to */
    private static final int PORT = 9090;

    /** hashmap of connected clients */
    private static ConcurrentHashMap<String, ClientHandler> clientsList =
        new ConcurrentHashMap<>();

    /** thread pool */
    private static ExecutorService threadPool =
        Executors.newFixedThreadPool(10);

    /* --- Main Method ------------------------------------------------------ */

    /**
     * Starts the {@code Server}.
     *
     * @param args  command-line arguments
     */
    public static void main(String[] args)
            throws Exception
    {
        // Set up main server.
        ServerSocket listener = new ServerSocket(PORT);
        System.out.println("[Server] Server started.");
        String command = "";
        ServerCommand inputThread = new ServerCommand(command, listener);
        threadPool.execute((inputThread));
        Socket client;

        while (!command.equals("exit")) {
            System.out.println("[Server] Waiting for clients...");
            // Wait for a client to connect.
            try {
                client = listener.accept();
            } catch(IOException e) {
                break;
            }
            // Set up client thread.
            ClientHandler clientThread = new ClientHandler(client, clientsList);
            // Execute client thread.
            threadPool.execute(clientThread);
            
        }
        // End all userHandler threads created before closing server
        for(Map.Entry<String, ClientHandler> aClient: clientsList.entrySet()){
            aClient.getValue().endThread();
        }
        System.out.println("[Server] Goodbye");
        System.exit(0);
    }
}
