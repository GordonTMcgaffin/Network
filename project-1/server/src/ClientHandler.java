/**
 * @file    ClientHandler.java
 * @brief   {@code Runnable} that handles messages from/to a specific client
 * @author  G. Mcgaffin (23565608@sun.ac.za)
 * @date    2022-08-02
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Map.*;

/**
 * {@code ClientHandler} is a {@code Runnable} that communicates with a single
 * client, receiving messages from it, and sending messages to it.
 */
public class ClientHandler
    implements Runnable {

    /* --- Instance Variables ----------------------------------------------- */

    /** connection to the client */
    private Socket client;

    /** for receiving object data from the client */
    private ObjectInputStream inStream;

    /** for sending object data to the client */
    private ObjectOutputStream outStream;

    /** hashmap of all connected clients */
    private ConcurrentHashMap<String, ClientHandler> clientList;

    public String username;
    /* --- Constructors ----------------------------------------------------- */

    /**
     * Creates and returns a new {@code ClientHandler} instance.
     *
     * @param  clientSocket  connection to the client
     * @param  clients  hashmap of all connected clients
     * @throws Exception  if anything goes wrong in the process of opening the
     *                    object input and ouput streams
     */
    public ClientHandler(Socket clientSocket,
                         ConcurrentHashMap<String, ClientHandler> clients)
            throws Exception
    {
        this.client = clientSocket;
        this.clientList = clients;
        outStream = new ObjectOutputStream(client.getOutputStream());
        inStream = new ObjectInputStream(client.getInputStream());
    }

    /* --- Instance Methods ------------------------------------------------- */

    /** Runs in the background; handles incoming messages. */
    @Override
    public void run()
    {
        String message = "";
        Message recvMessage;
        Message sendMessage;

        try {
            while (!message.equals("exit")) {
                // This is what is received.
                recvMessage = (Message) inStream.readObject();
                message = recvMessage.content;
                
                // If the message sent to the server is "Username set" then the
                // server will check the username given by recvMessage.user and
                // set the thread's username variable if it is unique.
                if (recvMessage.content.equals("username_set")) {
                    if (clientList.containsKey(recvMessage.sender)) {
                        message = "duplicate";
                        sendMessage = new Message("Server", username, message);
                        outStream.writeObject(sendMessage);
                    } else {
                        message = "unique";
                        clientList.put(recvMessage.sender, this);
                        username = recvMessage.sender;
                        // Server sends a message back if the username is unique
                        // or not.
                        System.out.println("[Server] " + username + " has connected.");
                        System.out.println(printClients());
                        sendMessage = new Message("Server", username, message);
                        outStream.writeObject(sendMessage);
                        Shout("Server", username + " has connected.");
                        showClients();
                    }
                } else if (recvMessage.content.equals("list_all_clients")) {
                    sendMessage = new Message("Server", username,
                                              printClients());
                    outStream.writeObject(sendMessage);
                } else if (!recvMessage.receiver.equals("all")) {
                    if (!clientList.containsKey(recvMessage.receiver)) {
                        Message msg = new Message("Server", recvMessage.sender,
                                recvMessage.receiver + " is not available.");
                        clientList.get(recvMessage.sender)
                                  .outStream.writeObject(msg);
                    } else {
                        clientList.get(recvMessage.sender)
                                  .outStream.writeObject(recvMessage);
                        clientList.get(recvMessage.receiver)
                                  .outStream.writeObject(recvMessage);
                        System.out.println("[Server] Whisper message '" +
                                           recvMessage.content + "' from " +
                                           recvMessage.sender + " to " +
                                           recvMessage.receiver + ".");
                    }
                } else if (!recvMessage.content.equals("exit")) {
                    System.out.println("[Server] " + username + " sent '" +
                                       recvMessage.content + "' to all users.");
                    Shout(recvMessage.sender, recvMessage.content);
                }
            }
            endThread();
        } catch (IOException e) {
            endThread();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Cleans up, then stops the {@code Runnable}. */
    public void endThread()
    {
        if (username != null) {
            clientList.remove(username);
            Shout("Server", username + " has left.");
            showClients();
            try {
                System.out.println("[Server] " + username +
                                   " has disconnected.");
                outStream.close();
                inStream.close();
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns a {@code String} that lists all connected clients.
     *
     * @return {@code String} that lists all connected clients
     */
    private String printClients()
    {
        String onlineUsers = "online_users:";
        for (Entry<String, ClientHandler> aClient : clientList.entrySet()) {
            onlineUsers = onlineUsers + "\n" + aClient.getKey();
        }
        return onlineUsers;
    }

    /**
     * Sends a {@code String} that lists all connected clients to all clients.
     */
    private void showClients()
    {
        Shout("Server", printClients());
    }

    /**
     * Broadcasts a message to all clients.
     *
     * @param username  name of the broadcasting client
     * @param message  message to broadcast
     */
    private void Shout(String username, String message)
    {
        try {
            for (Entry<String, ClientHandler> aClient : clientList.entrySet()) {
                Message packet = new Message(username, "all", message);
                aClient.getValue().outStream.writeObject(packet);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }



}
