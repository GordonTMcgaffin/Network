/**
 * @file    Client.java
 * @brief   chat client
 * @author  J. P. Visser (21553416@sun.ac.za)
 * @date    2022-08-02
 */

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * The {@code Client} class connects to a server and provides an interface for
 * receiving and sending {@code Message} objects.
 */ 
public class Client {

    /* --- Instance Variables ----------------------------------------------- */

    /** for connecting to the server */
    private Socket socket;

    /** for receiving {@code Message} objects */
    private ObjectInputStream inStream;

    /** for sending {@code Message} objects */
    private ObjectOutputStream outStream;

    /** queue to hold messages that cannot be read immediately */
    private ArrayBlockingQueue<Message> inQueue;

    /** queue to hold messages that cannot be sent immediately */
    private ArrayBlockingQueue<Message> outQueue;

    /** for receiving messages in the background */
    private Thread receiveMessageThread;

    /** for sending messages in the background */
    private Thread sendMessageThread;

    /* --- Constructors ----------------------------------------------------- */

    /**
     * Creates and returns a new {@code Client} instance, connected to a server
     * identified with hostname or address {@code host} and port {@code port}.
     *
     * @param  host  name or address of the machine to connect to
     * @param  port  port on the server machine to connect to
     */ 
    public Client(String host, int port)
            throws IOException {
        try {
            socket = new Socket(host, port);
            inStream = new ObjectInputStream(socket.getInputStream());
            outStream = new ObjectOutputStream(socket.getOutputStream());
            inQueue = new ArrayBlockingQueue<Message>(16);
            outQueue = new ArrayBlockingQueue<Message>(16);
        } catch (ConnectException ce) {
            retryConnection(host,port);
            inStream = new ObjectInputStream(socket.getInputStream());
            outStream = new ObjectOutputStream(socket.getOutputStream());
            inQueue = new ArrayBlockingQueue<Message>(16);
            outQueue = new ArrayBlockingQueue<Message>(16);
        } catch (IOException e) {
            e.printStackTrace();
        }

        receiveMessageThread = new Thread() {
            @Override
            public void run()
            {
                while (true) {
                    try {
                        inQueue.put((Message) inStream.readObject());
                    } catch (SocketException se) {
                        Message msg = new Message("Server", "all",
                                "Server has gone offline. Please restart " +
                                "client to reconnect");
                        try {
                            inQueue.put(msg);
                        } catch (InterruptedException e) {
                            System.exit(0);
                        }
                        break;
                    } catch (EOFException eofe) {
                        Message msg = new Message("Server", "all",
                                "Server has gone offline. Please restart " +
                                "client to reconnect");
                        try {
                            inQueue.put(msg);
                        } catch (InterruptedException e) {
                            System.exit(0);
                        }
                        break;
                    } catch (IOException |
                             ClassNotFoundException |
                             InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        receiveMessageThread.start();

        sendMessageThread = new Thread() {
            @Override
            public void run()
            {
                while (true) {
                    try {
                        outStream.writeObject(outQueue.take());
                    } catch(SocketException se) {
                        Message msg = new Message("Server", "all",
                                "Server has gone offline. Please restart " +
                                "client to reconnect");
                        try {
                            inQueue.put(msg);
                        } catch (InterruptedException e) {
                            System.exit(1);
                        }
                        break;
                    } catch (InterruptedException ie) {
                        return;
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        };
        sendMessageThread.start();

    }

    /* --- Instance Methods ------------------------------------------------- */

    /**
     * Returns a {@code Message} from the incoming message queue.
     * Note that this method will block if the queue is empty, and unblock once
     * a new {@code Message} enters the queue.
     *
     * @return {@code Message} from the incoming message queue
     */
    public Message receive()
    {
        Message msg = null;
        try {
            msg = inQueue.take();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        return msg;
    }

    /**
     * If the client attempts to connect to a server that is not running, it will
     * periodically retry to connect to the server until the connection is successful
     *
     * @param host the ip address of the server to connect to
     * @param port the port on which t connect to the server
     */
    public void retryConnection(String host, int port) {
        boolean connected = false;

        while (!connected) {
            try {
                Thread.sleep(2000);
                socket = new Socket(host, port);
                connected = true;
            } catch (IOException e) {
                // Continue.
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Places a {@code Message} on the outgoing message queue to be delivered to
     * the server.
     * Note that this method can block.
     *
     * @param msg  {@code Message} to send
     */
    public void send(Message msg)
    {
        try {
            outQueue.put(msg);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Stops all background threads, closes all streams, and disconnects from
     * the server.
     */
    public void disconnect()
    {
        try {
            receiveMessageThread.interrupt();
            sendMessageThread.interrupt();
            outStream.close();
            inStream.close();
            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
