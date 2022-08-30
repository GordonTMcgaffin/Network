package project_3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NAT {
    //Client IP:PORT | NATIP:Client assigned PORT
    private static ConcurrentHashMap<String, String> NAT_TABLE = new ConcurrentHashMap<String, String>();
    //Client IP | Client thread
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<String, ClientHandler>();
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    public static byte[] MAC;

    public static void main(String[] args) {

        int port = Integer.parseInt(args[0]);
        int threadAmount = Integer.parseInt(args[1]);
        boolean[] portPool = new boolean[threadAmount];
        Arrays.fill(portPool, false);
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
                ClientHandler clientThread = new ClientHandler(client, NAT_TABLE, clients, MAC, port, portPool);
                if (clientThread.IPString.split("\\.")[0].equals("10"))
                    NAT_TABLE.put(clientThread.IPString + ":" + clientThread.PortString, "127.0.0.1" + ":" + clientThread.clientPort);
                System.out.println("NATTABLE");
                for (String key : NAT_TABLE.keySet()) {
                    System.out.println(key + " | " + NAT_TABLE.get(key));
                }
                threadPool.execute(clientThread);
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

    public static String genPort() {
        Random random = new Random();
        String port = random.nextInt(99) + "" + random.nextInt(99);
        if (port.equals("9090")) port = genPort();
        return port;
    }

}

/*

    Ethernet header
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                          MAC Destination                      |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           | MAC source |    Length    |                Data               |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+



    IP header
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |      Start of data           |             Total Length       |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |  Time to Live   |              Protocol                       |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                       Source Address                          |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |                    Destination Address                        |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

            protocol
                1 - ICMP
                6 - TCP
                17 - UDP



    ICMP
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |                    Code                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             Data                              |
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

 Type

      8 for echo message;

      0 for echo reply message.

   Code

      0

   Type

      15 for information request message;

      16 for information reply message.

   Code

      0

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
