package project_3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private int[] IP;
    public String IPString = "";
    private byte[] NATMAC;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private ConcurrentHashMap<String, String> NATTable;
    private ConcurrentHashMap<String, ClientHandler> clients;
    private Socket client;

    public ClientHandler(Socket clientSocket,
                         ConcurrentHashMap<String, String> NAT_TABLE,
                         ConcurrentHashMap<String, ClientHandler> clientList, byte[] NATMAC) {

        this.client = clientSocket;
        this.NATTable = NAT_TABLE;
        this.clients = clientList;
        this.NATMAC = NATMAC;
        int location;

        try {
            outStream = new ObjectOutputStream(client.getOutputStream());
            inStream = new ObjectInputStream(client.getInputStream());
            location = (int) inStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (location == 0) {
            //Internal client
            IP = genIP(10);
        } else {
            //External client
            IP = genIP(123);
        }

        IPString = IPtoString(IP);

        try {
            outStream.writeObject(IP);
            outStream.writeObject(NATMAC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[Thread: " + IPString + "]> setup complete");
    }

    @Override
    public void run() {
        byte[] packet;
        while (true) {
            try {

                packet = (byte[]) inStream.readObject();
                String sourceIP = (packet[17] & 0xff) + "." + (packet[18] & 0xff) + "." + (packet[19] & 0xff) + "." + (packet[20] & 0xff);
                String destIP = (packet[21] & 0xff) + "." + (packet[22] & 0xff) + "." + (packet[23] & 0xff) + "." + (packet[24] & 0xff);
                System.out.println("[Thread: " + IPString + "]> Sending packet from " + sourceIP + " to " + destIP);
                if ((packet[17] & 0xff) == 10) {
                    //local
                    try {
                        clients.get(destIP).outStream.writeObject(packet);
                    } catch (IOException ioe) {
                        // Send ICMP back with a no route found error
                        //outStream.writeObject();
                    }
                }
            } catch (IOException e) {
                System.out.println("[Thread: " + IPString + "]> Client " + IPString + " has disconnected");
                try {
                    exit();
                    break;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void exit() throws IOException {
        clients.remove(IPString);
        outStream.close();
        inStream.close();
        client.close();
    }

    public static String IPtoString(int[] ip) {
        String IPString = "" + ip[0];
        for (int i = 1; i < ip.length; i++) {
            IPString = IPString + "." + ip[i];
        }
        return IPString;
    }

    public int[] genIP(int head) {
        int[] ip = new int[4];
        for (int b = 0; b < 255; b++) {
            for (int c = 0; c < 255; c++) {
                for (int d = 0; d < 255; d++) {
                    if (!clients.containsKey("10." + b + "." + c + "." + d)) {
                        System.out.println("[NAT]> Assigned client ip " + "10." + b + "." + c + "." + d);
                        ip[0] = head;
                        ip[1] = b;
                        ip[2] = c;
                        ip[3] = d;
                        return ip;
                    }
                }
            }
        }
        return null;
    }
}
