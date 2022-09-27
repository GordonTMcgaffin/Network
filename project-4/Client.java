import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.System.err;

public final class Client {

    public static final int DEFAULT_SERVER_PORT = 9090;

    private InetAddress serverAddress;
    private int serverPort;
    private final String id;
    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    public Client(InetAddress serverAddress, int serverPort, String id)
    {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.id = id;
    }

    public void connect()
        throws IOException
    {
        socket = new Socket(serverAddress, serverPort);
        in = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        send(new Message().setReceiver("server").setText(id));
    }

    public boolean isConnected()
    {
        return (socket != null && socket.isConnected() && !socket.isClosed());
    }

    public void send(Message m)
    {
        m.setSender(id);
        try {
            out.writeObject(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Message receive()
        throws EOFException
    {
        Message m = null;
        try {
            m = (Message) in.readObject();
        } catch (EOFException eofe) { 
            try {
                socket.close();
            } catch (IOException ioe) { 
                ioe.printStackTrace();
            }
            throw eofe; 
        } catch (SocketException se) { 
            // IGNORE: Connection closed from client side.
        } catch (Exception e) { 
            e.printStackTrace();
        }
        return m;
    }

    public static InetAddress getLocalHost()
    {
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localhost;
    }

    public static String generateRandomID()
    {
        return getLocalHost().getHostName() + "-" +
            ThreadLocalRandom.current().nextInt(10000, 100000);
    }

    @Override
    public String toString()
    {
        return id;
    }

    public void close()
    {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            // IGNORE
        }
    }
}
