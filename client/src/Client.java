import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Client {
    private Socket socket;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private ArrayBlockingQueue<Message> inQueue;
    private ArrayBlockingQueue<Message> outQueue;
    private Thread receiveMessageThread;
    private Thread sendMessageThread;

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

    public void send(Message msg)
    {
        try {
            outQueue.put(msg);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

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
