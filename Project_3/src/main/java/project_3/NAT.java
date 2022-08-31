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

    /**
     * Main function used to set up the NAT server
     *
     * @param args contains values for port and thread/client amount
     */
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

    /**
     * Generates a random MAC address for the NAT router
     *
     * @return The NAT's MAC address
     */
    public static byte[] genMAC() {
        Random rand = new Random();
        byte[] MAC = new byte[6];
        rand.nextBytes(MAC);
        return MAC;
    }
}
