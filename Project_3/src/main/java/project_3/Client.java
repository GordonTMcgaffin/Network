package project_3;

import java.io.*;
import java.net.Socket;
import java.util.Random;

public class Client {

    private static Socket serverSocket;
    private static ObjectOutputStream outStream;
    private static ObjectInputStream inStream;
    private static Thread receiveThread;
    private static Thread sendThread;
    private static byte[] MAC;
    private static int[] IP;
    private static byte[] Port;
    private static String PortString;
    private static String IPString;
    private static String MACString;
    private static byte[] NATMAC;
    private static String host;

    /**
     * Sets up connection to NAT, including sending DHCP packets to request an IP address to communicate with the NAT.
     * Sets up two threads, one for receiving incoming packets and another for sending packets.
     *
     * @param args
     */
    public static void main(String args[]) {
        host = args[0];
        int port = Integer.parseInt(args[1]);
        int location = Integer.parseInt(args[2]); // location = 0 means that the client is a local connection. Location = 1 means that the client is external.

        MAC = genMAC();

        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("[NAT]> Connecting to NAT. . . ");

        try {
            serverSocket = new Socket(host, port);
            outStream = new ObjectOutputStream(serverSocket.getOutputStream());
            inStream = new ObjectInputStream(serverSocket.getInputStream());
            outStream.writeObject(location);

            IP = new int[4];
            NATMAC = new byte[6];
            Port = genPort();

            outStream.writeObject(genDHCP());
            byte[] DHCP = (byte[]) inStream.readObject();

            IP[0] = (DHCP[21] & 0xff);
            IP[1] = (DHCP[22] & 0xff);
            IP[2] = (DHCP[23] & 0xff);
            IP[3] = (DHCP[24] & 0xff);

            NATMAC[0] = DHCP[29];
            NATMAC[1] = DHCP[30];
            NATMAC[2] = DHCP[31];
            NATMAC[3] = DHCP[32];
            NATMAC[4] = DHCP[33];
            NATMAC[5] = DHCP[34];

            PortString = (Port[0] & 0xff) + "" + (Port[1] & 0xff);
            IPString = IPtoString(IP);
            MACString = MACtoString(MAC);

            System.out.println("[NAT]> Connection established");
            System.out.println();
            System.out.println("===================================");

            System.out.println("[NAT]> Client IP: " + IPString);
            System.out.println("[NAT]> Client MAC: " + MACString);
            System.out.println("[NAT]> Client Port: " + PortString);

            System.out.println("===================================");
            System.out.println();

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[NAT]> Connection to NAT failed ");
            System.exit(0);
        }

        /**
         * Thread used for receiving and processing packets.
         * The thread displays relevant information about the packets to the client
         */
        receiveThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] packet = (byte[]) inStream.readObject();
                        byte[] messageBytes;
                        String sourceIP = (packet[25] & 0xff) + "." + (packet[26] & 0xff) + "." + (packet[27] & 0xff) + "." + (packet[28] & 0xff);
                        String sourcePort = "";

                        if ((packet[24] & 0xff) == 1) {

                            if ((packet[36] & 0xff) < 10) {
                                sourcePort = (packet[35] & 0xff) + "0" + (packet[36] & 0xff);
                            } else {
                                sourcePort = (packet[35] & 0xff) + "" + (packet[36] & 0xff);
                            }
                            //ICMP packet
                            if ((packet[33] & 0xff) == 16) {
                                //ICMP response
                                messageBytes = new byte[packet.length - 36];
                                System.arraycopy(packet, 36, messageBytes, 0, packet.length - 36);
                                String message = new String(messageBytes);

                                if (location == 0)
                                    System.out.println("[" + IPString + ":" + PortString + "]> Available Clients");
                                if (location == 1)
                                    System.out.println("[" + IPString + ":" + PortString + "]> Available routes");

                                if (message.split(":", 2).length > 1) System.out.println(message.split(":", 2)[1]);
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                            } else if ((packet[33] & 0xff) == 8) {
                                //ICMP echo
                                packet = genICMP(sourceIP + ":" + sourcePort, 0, 1);
                                outStream.writeObject(packet);
                            } else if ((packet[33] & 0xff) == 0) {
                                System.out.println("Client " + sourceIP + ":" + sourcePort + " is reachable");
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                            } else if ((packet[33] & 0xff) == 3) {
                                //ICMP error message
                                if ((packet[34] & 0xff) == 5)
                                    System.out.println("Address entered is unreachable");
                                if ((packet[34] & 0xff) == 3)
                                    System.out.println("Address entered is unreachable");
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                            }
                        } else if ((packet[24] & 0xff) == 6 || (packet[24] & 0xff) == 17) {
                            if ((packet[34] & 0xff) < 10) {
                                sourcePort = (packet[33] & 0xff) + "0" + (packet[34] & 0xff);
                            } else {
                                sourcePort = (packet[33] & 0xff) + "" + (packet[34] & 0xff);
                            }
                            //TCP/UDP message
                            messageBytes = new byte[packet.length - 38];
                            System.arraycopy(packet, 38, messageBytes, 0, packet.length - 38);
                            String message = new String(messageBytes);
                            System.out.println();
                            System.out.println("[" + sourceIP + ":" + sourcePort + "]> " + message);
                            System.out.print("[" + IPString + ":" + PortString + "]> ");

                        }

                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println(" Connection to NAT lost ");
                        System.exit(0);
                    }

                }
            }

        };
        receiveThread.start();

        /**
         *
         * Thread used for client input and packet sending
         */
        sendThread = new Thread() {
            @Override
            public void run() {
                String input = "";
                System.out.println("[" + IPString + ":" + PortString + "]> Please select a command number");
                System.out.println("[" + IPString + ":" + PortString + "]> 0 = View client list");
                System.out.println("[" + IPString + ":" + PortString + "]> 1 = PING");
                System.out.println("[" + IPString + ":" + PortString + "]> 2 = Message");
                System.out.print("[" + IPString + ":" + PortString + "]> ");
                while (true) {
                    try {
                        input = inputReader.readLine();
                        byte[] packet = null;
                        if (!input.equals("")) {
                            if (input.equals("0")) {
                                //Request client list
                                packet = genICMP(serverSocket.getLocalAddress().getHostAddress() + ":" + port, 15, 1);
                                outStream.writeObject(packet);
                            } else if (input.equals("1")) {
                                //Send ping for the amount specified
                                System.out.println("[" + IPString + ":" + PortString + "]> Please enter <IP Address:Port>");
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                                String inputs = inputReader.readLine().trim();
                                packet = genICMP(inputs, 8, 1);
                                outStream.writeObject(packet);
                            } else if (input.equals("2")) {
                                String protocol = "";
                                while (!(protocol.equals("TCP") || protocol.equals("UDP"))) {
                                    System.out.print("[NAT] Please select protocol (TCP/UDP)> ");
                                    try {
                                        protocol = inputReader.readLine().toUpperCase().trim();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (!(protocol.equals("TCP") || protocol.equals("UDP")))
                                        System.out.println("[NAT]> Please enter a valid protocol.");
                                }
                                //Send message using selected protocol
                                System.out.println("[" + IPString + ":" + PortString + "]> Please enter <IP Address:Port> <Message>");
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                                String[] inputs = inputReader.readLine().split(" ", 2);
                                if (protocol.equals("TCP")) packet = genMSGPacket(inputs, 6);
                                if (protocol.equals("UDP")) packet = genMSGPacket(inputs, 17);
                                outStream.writeObject(packet);
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                            } else {
                                System.out.println("[" + IPString + ":" + PortString + "]> " + input + " is not a valid command");
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                            }
                        } else {
                            System.out.print("[" + IPString + ":" + PortString + "]> ");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        sendThread.start();
    }

    /**
     * Generates a random MAC address
     *
     * @return a byte array containing the generated MAC address
     */
    public static byte[] genMAC() {
        Random rand = new Random();
        byte[] MAC = new byte[6];
        rand.nextBytes(MAC);
        return MAC;
    }

    /**
     * Translates the MAC address provided to a string
     *
     * @param MAC the MAC address to be translated
     * @return a string version of the MAC address
     */
    public static String MACtoString(byte[] MAC) {
        String MACString = String.format("%02x", MAC[1]);
        for (int i = 1; i < MAC.length; i++) {
            MACString = MACString + ":" + String.format("%02x", MAC[i]);
        }
        return MACString;
    }

    /**
     * Translates the IP address provided to a string
     *
     * @param ip the IP address to be translated
     * @return a string version of the IP address
     */
    public static String IPtoString(int[] ip) {
        String IPString = "" + ip[0];
        for (int i = 1; i < ip.length; i++) {
            IPString = IPString + "." + ip[i];
        }
        return IPString;
    }

    /**
     * Generates the port that the client is using to listen to the NAT
     *
     * @return a byte array containing the port
     */
    public static byte[] genPort() {
        byte[] port = new byte[2];
        port[0] = (byte) Math.floor(Math.random() * (99 - 10 + 1) + 10);
        port[1] = (byte) Math.floor(Math.random() * (99 - 10 + 1) + 10);
        return port;
    }

    /**
     * Generates an ICMP packet with the required data provided
     *
     * @param receiverAddr the address of the receiver
     * @param type         the ICMP type
     * @param protocol     the IP protocol
     * @return tha ICMP packet generated
     * @throws IOException
     */
    public static byte[] genICMP(String receiverAddr, int type, int protocol) throws IOException {

        byte[] preamble = new byte[7];
        for (int i = 0; i < 7; i++) {
            preamble[i] = (byte) (i % 2);
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        String[] destInfo = receiverAddr.split(":");
        String[] destIPString = destInfo[0].split("\\.", 4);

        //Ethernet
        byteStream.write(preamble);// Preamble: 7 bytes : 0
        byteStream.write((byte) 171); //stf 1 byte : 7
        byteStream.write(NATMAC); // MAC destination : 6 bytes : 8
        byteStream.write(MAC); // MAC source : 6 bytes: 14
        byteStream.write((byte) 25);// Length : 1 byte : 20

        //TCP
        byteStream.write((byte) 12);//Start of data : 1 byte : 21
        byteStream.write((byte) 12);//Total length : 1 byte : 22
        byteStream.write((byte) 2); //Time to live : 1 byte : 23
        byteStream.write((byte) protocol); //Protocol : 1 byte : 24
        byteStream.write((byte) IP[0]);
        byteStream.write((byte) IP[1]);
        byteStream.write((byte) IP[2]);
        byteStream.write((byte) IP[3]);//Source IP : 4 bytes; 25:28
        byteStream.write((byte) Integer.parseInt(destIPString[0]));
        byteStream.write((byte) Integer.parseInt(destIPString[1]));
        byteStream.write((byte) Integer.parseInt(destIPString[2]));
        byteStream.write((byte) Integer.parseInt(destIPString[3]));//Destination source : 4 bytes : 29:32

        //ICMP
        byteStream.write((byte) type);//33
        byteStream.write((byte) 0);//34

        byteStream.write(Port);//Source port : 2 byte : 35:36
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(0, 2))));
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(2, 4))));//Dest port : 2 bytes : 37:38

        return byteStream.toByteArray();
    }

    /**
     * Generates a TCP or UDP packet depending on which protocol is selected
     *
     * @param messageInfo contains the message being sent along with the receiver's address
     * @param protocol    the protocol used to send the data
     * @return a TCP or UDP packet
     * @throws IOException
     */
    public static byte[] genMSGPacket(String[] messageInfo, int protocol) throws IOException {

        byte[] preamble = new byte[7];
        for (int i = 0; i < 7; i++) {
            preamble[i] = (byte) (i % 2);
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] message = messageInfo[1].getBytes();

        String[] destInfo = messageInfo[0].split(":");
        String[] destIPString = destInfo[0].split("\\.", 4);

        int messageSize = message.length;
        byte ethernetFrameLength = (byte) (29 + messageSize - 1);
        byte ipFrameLength = (byte) (13 + messageSize - 1);


        //Ethernet header
        byteStream.write(preamble);// Preamble: 7 bytes : 0
        byteStream.write((byte) 171); //stf 1 byte : 7
        byteStream.write(NATMAC); // MAC destination : 6 bytes : 8
        byteStream.write(MAC); // MAC source : 6 bytes: 14
        byteStream.write(ethernetFrameLength);// Length : 1 byte : 20

        //IP header
        byteStream.write((byte) 12);//Start of data : 1 byte : 21
        byteStream.write(ipFrameLength);//Total length : 1 byte : 22
        byteStream.write((byte) 2); //Time to live : 1 byte : 23
        byteStream.write((byte) protocol); //Protocol : 1 byte : 24
        byteStream.write((byte) IP[0]);
        byteStream.write((byte) IP[1]);
        byteStream.write((byte) IP[2]);
        byteStream.write((byte) IP[3]);//Source IP : 4 bytes; 25
        byteStream.write((byte) Integer.parseInt(destIPString[0]));
        byteStream.write((byte) Integer.parseInt(destIPString[1]));
        byteStream.write((byte) Integer.parseInt(destIPString[2]));
        byteStream.write((byte) Integer.parseInt(destIPString[3]));//Destination source : 4 bytes : 29

        //TCP/UDP
        byteStream.write(Port);//Source port : 2 byte : 33
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(0, 2))));
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(2, 4))));//Dest port : 2 bytes : 35


        byteStream.write((byte) 3);//Data off set : 1 byte : 37
        byteStream.write(message);// data : 38
        return byteStream.toByteArray();
    }

    /**
     * Generates the request DHCP packet
     *
     * @return the DHCP packet
     * @throws IOException
     */
    public static byte[] genDHCP() throws IOException {

        String[] serverIP = host.split("\\.");
        byte[] preamble = new byte[7];
        for (int i = 0; i < 7; i++) {
            preamble[i] = (byte) (i % 2);
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        //Ethernet
        byteStream.write(preamble);// Preamble: 7 bytes : 0
        byteStream.write((byte) 171); //stf 1 byte : 7
        byteStream.write(NATMAC); // MAC destination : 6 bytes : 8
        byteStream.write(MAC); // MAC source : 6 bytes: 14
        byteStream.write((byte) 25);// Length : 1 byte : 20

        //DHCP
        byteStream.write((byte) 1);
        byteStream.write((byte) 1);
        byteStream.write((byte) 1);
        byteStream.write((byte) 1);//Source IP : 4 bytes; 21
        byteStream.write((byte) Integer.parseInt(serverIP[0]));
        byteStream.write((byte) Integer.parseInt(serverIP[1]));
        byteStream.write((byte) Integer.parseInt(serverIP[2]));
        byteStream.write((byte) Integer.parseInt(serverIP[3]));//Server IP : 4 bytes; 25
        byteStream.write(MAC);// Client MAC : 6 bytes; 29
        byteStream.write(Port);//Source port : 2 byte : 35:32

        return byteStream.toByteArray();
    }
}
