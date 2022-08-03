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
    {
        try {
            socket = new Socket(host, port);
            inStream = new ObjectInputStream(socket.getInputStream());
            outStream = new ObjectOutputStream(socket.getOutputStream());
            inQueue = new ArrayBlockingQueue<Message>(16);
            outQueue = new ArrayBlockingQueue<Message>(16);
        } catch (ConnectException ce) {
            System.err.println("Could not connect to server at host:" + host +
                               ", port:" + port + ".");
            System.exit(1);
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
                        return;
                    } catch (EOFException eofe) {
                        System.err.println("Server disconnected.");
                        System.exit(1);
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
            sendMessageThread.interrupt();
            receiveMessageThread.interrupt();
            outStream.close();
            inStream.close();
            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
