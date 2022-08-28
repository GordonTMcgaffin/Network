package project_3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NAT {
    private static ConcurrentHashMap<String, String> NAT_TABLE = new ConcurrentHashMap<String, String>();
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<String, ClientHandler>();
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    public static byte[] MAC;

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        int threadAmount = Integer.parseInt(args[1]);
        MAC = genMAC();
        ServerSocket listener = null;
        try {
            System.out.println("[NAT]> Starting NAT-box. . .");
            listener = new ServerSocket(port);

        } catch (IOException e) {
            System.out.println("[NAT]> Socket setup error");
            System.exit(0);
        }
        System.out.println("[NAT]> Waiting for clients. . .");
        Socket client;
        while (true) {
            try {
                client = listener.accept();
                System.out.println("[NAT]> Client connected");
                ClientHandler clientThread = new ClientHandler(client, NAT_TABLE, clients, MAC);
                clients.put(clientThread.IPString, clientThread);
                threadPool.execute(clients.get(clientThread.IPString));
            } catch (IOException ioe) {
                System.out.println("[NAT]> Error occurred while connecting client");
            }
        }


    }


    public static String IPtoString(int[] ip) {
        String IPString = "" + ip[0];
        for (int i = 1; i < ip.length; i++) {
            IPString = IPString + "." + ip[i];
        }
        return IPString;
    }

    public static byte[] genMAC() {
        Random rand = new Random();
        byte[] MAC = new byte[6];
        rand.nextBytes(MAC);

        System.out.println();
        return MAC;
    }

}

/*

    IP header for ICMP
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |   4   | Start of data |       0       |    Total Length       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |         Identification (0)    |Flags(0)|  Fragment Offset(0)  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  Time to Live |        1      |         Header Checksum       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                       Source Address                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Destination Address                        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+



    ICMP
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             unused                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Internet Header + 64 bits of Original Data Datagram      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+



    Type

        3

   Code

      0 = net unreachable;

      1 = host unreachable;

      2 = protocol unreachable;

      3 = port unreachable;

      4 = fragmentation needed and DF set;

      5 = source route failed.

    DHCP
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     op(1)(0)  |   htype(1)(0) |     6 bytes   |   hops (1)(0) |
   +---------------+---------------+---------------+---------------+
   |                            xid (4)(0)                         |
   +-------------------------------+-------------------------------+
   |           secs (2)(0)         |           flags (2)(0)        |
   +-------------------------------+-------------------------------+
   |                        Client IP address                      |
   +---------------------------------------------------------------+
   |                   your (client) IP address                    |
   +---------------------------------------------------------------+
   |         IP address of next server to use in bootstrap (0)     |
   +---------------------------------------------------------------+
   |                   Relay agent IP address                      |
   +---------------------------------------------------------------+
   |                                                               |
   |                  Client hardware address (MAC)                |
   |                                                               |
   |                                                               |
   +---------------------------------------------------------------+
 */
