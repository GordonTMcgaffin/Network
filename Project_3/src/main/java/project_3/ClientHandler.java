package project_3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    public int clientPort;
    private byte[] Port;
    private int[] IP;
    public String IPString;
    public String PortString;
    private byte[] NATMAC;
    private int NATPort;
    private Socket client;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private ConcurrentHashMap<String, String> NATTable;
    private ConcurrentHashMap<String, ClientHandler> clients;
    private boolean[] portPool;

    /**
     * Client handler constructor. Sets up all input and output streams for the provided socket.
     * The constructor Generates an IP address for each client and communicates it to the client using DHCP
     *
     * @param clientSocket the socket connected to the client
     * @param NAT_TABLE    a table containing all pairs of local ip:port address and global:port address
     * @param clientList   a hashmap containing a list of all clients and their sockets
     * @param NMAC         NAT MAC address
     * @param NPort        NAT port that has been assigned for to the client for port forwarding
     * @param portPool     the available ports left to assign to clients
     */
    public ClientHandler(Socket clientSocket,
                         ConcurrentHashMap<String, String> NAT_TABLE,
                         ConcurrentHashMap<String, ClientHandler> clientList,
                         byte[] NMAC,
                         int NPort, boolean[] portPool) {

        this.client = clientSocket;
        this.NATTable = NAT_TABLE;
        this.clients = clientList;
        this.NATMAC = NMAC;
        this.NATPort = NPort;
        this.Port = new byte[2];
        this.portPool = portPool;


        int location;

        try {
            outStream = new ObjectOutputStream(client.getOutputStream());
            inStream = new ObjectInputStream(client.getInputStream());
            location = (int) inStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        //The port assigned to listen to a client for port forwarding
        clientPort = -1;
        if (location == 0) {
            int p;
            for (p = 0; p < portPool.length; p++) if (!portPool[p]) break;
            portPool[p] = true;
            clientPort = 9000 + p;
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
            byte[] DHCP = (byte[]) inStream.readObject();
            Port[0] = DHCP[35];
            Port[1] = DHCP[36];

            DHCP[21] = (byte) IP[0];
            DHCP[22] = (byte) IP[1];
            DHCP[23] = (byte) IP[2];
            DHCP[24] = (byte) IP[3];

            DHCP[29] = NATMAC[0];
            DHCP[30] = NATMAC[1];
            DHCP[31] = NATMAC[2];
            DHCP[32] = NATMAC[3];
            DHCP[33] = NATMAC[4];
            DHCP[34] = NATMAC[5];

            outStream.writeObject(DHCP);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        PortString = (Port[0] & 0xff) + "" + (Port[1] & 0xff);
        clients.put(IPString + ":" + PortString, this);
        System.out.println("[Thread: " + IPString + ":" + PortString + "]> Client connected");
    }

    /**
     * Starts when thread is executed. Continuously listens for
     * incoming packets from client, translates and routes the packets to their specified locations
     */
    @Override
    public void run() {
        byte[] packet;
        while (true) {
            try {

                packet = (byte[]) inStream.readObject();


                String sourceIP = (packet[25] & 0xff) + "." + (packet[26] & 0xff) + "." + (packet[27] & 0xff) + "." + (packet[28] & 0xff);
                String sourcePort = "";


                String destIP = (packet[29] & 0xff) + "." + (packet[30] & 0xff) + "." + (packet[31] & 0xff) + "." + (packet[32] & 0xff);
                String destPort = "";

                if ((packet[24] & 0xff) == 1) {

                    if ((packet[36] & 0xff) < 10) {
                        sourcePort = (packet[35] & 0xff) + "0" + (packet[36] & 0xff);
                    } else {
                        sourcePort = (packet[35] & 0xff) + "" + (packet[36] & 0xff);
                    }

                    if ((packet[38] & 0xff) < 10) {
                        int portNum = ((packet[37] & 0xff) * 100) + (packet[38] & 0xff);
                        destPort = "" + portNum;
                    } else {
                        destPort = (packet[37] & 0xff) + "" + (packet[38] & 0xff);
                    }
                    //ICMP packet
                    if ((packet[33] & 0xff) == 15) {
                        //Client sent a list request
                        System.out.println("[Thread: " + IPString + "]> Sending client list to " + sourceIP + ":" + sourcePort);
                        String clientList = "Clients:";
                        if ((packet[25] & 0xff) == 10) {
                            //Request was made by internal client
                            //Generate list of local and global clients
                            for (String ip : clients.keySet()) {
                                clientList += "--> " + ip + "\n";
                            }
                        } else {
                            //Request was made by external client
                            //Generate list of global ip + local ports
                            for (String ip : NATTable.values()) {
                                clientList += "--> " + ip + "\n";
                            }
                        }
                        byte[] response = genICMP("127.0.0.1:9090", NATMAC, 16, 1, clientList, 0, destIP + ":" + destPort);
                        outStream.writeObject(response);
                    } else if ((packet[33] & 0xff) == 8) {
                        //Client sent an echo request
                        System.out.println("[Thread: " + IPString + "]> Sending Echo from " + sourceIP + ":" + sourcePort + " to " + destIP + ":" + destPort);
                        if ((packet[25] & 0xff) == 10) {
                            //local client is sending an echo request
                            if ((packet[29] & 0xff) == 10) {
                                //request is being sent to internal client
                                try {
                                    clients.get(destIP + ":" + destPort).outStream.writeObject(packet);
                                } catch (NullPointerException npe) {
                                    // Send ICMP back with a no route found error
                                    System.out.println("[Thread: " + IPString + "]> Client " + sourceIP + ":" + sourcePort + " is attempting to use unused route " + destIP + ":" + destPort);
                                    clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 3, destIP + ":" + destPort));
                                }
                            } else {
                                //request is being sent to external client
                                try {
                                    clients.get(destIP + ":" + destPort).outStream.writeObject(localToGlobal(packet, sourceIP, sourcePort, 25, 35));
                                } catch (NullPointerException npe) {
                                    // Send ICMP back with a no route found error
                                    System.out.println("[Thread: " + IPString + "]> Client " + sourceIP + ":" + sourcePort + " is attempting to use unused route " + destIP + ":" + destPort);
                                    clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 3, destIP + ":" + destPort));
                                }
                            }
                        } else {
                            //External client is sending an echo request
                            if ((packet[29] & 0xff) == 123) {
                                //External client is trying to send to external client, drop the packet

                                System.out.println("[Thread: " + IPString + "]> External client " + sourceIP + ":" + sourcePort + " is attempting to send packet to external client " + destIP + ":" + destPort);
                                clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 5, destIP + ":" + destPort));

                            } else {
                                //External client is sending to internal client
                                try {
                                    clients.get(globalIPTToLocalIP(destIP, destPort)).outStream.writeObject(globalToLocal(packet, destIP, destPort, 29, 37));
                                } catch (NullPointerException npe) {
                                    // Send ICMP back with a no route found error
                                    System.out.println("[Thread: " + IPString + "]> Client " + sourceIP + ":" + sourcePort + " is attempting to use unused route " + destIP + ":" + destPort);
                                    clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 3, destIP + ":" + destPort));
                                }
                            }
                        }
                    } else if ((packet[33] & 0xff) == 0) {
                        //Client is sending an echo response
                        System.out.println("[Thread: " + IPString + "]> Sending Echo response from " + sourceIP + ":" + sourcePort + " to " + destIP + ":" + destPort);
                        if ((packet[25] & 0xff) == 10) {
                            //local client is sending echo response
                            if ((packet[29] & 0xff) == 123) {
                                //local client is sending echo response to external client
                                //translate ip address;
                                clients.get(destIP + ":" + destPort).outStream.writeObject(localToGlobal(packet, sourceIP, sourcePort, 25, 35));
                            } else {
                                //local client is sending to local client
                                clients.get(destIP + ":" + destPort).outStream.writeObject(packet);
                            }
                        } else {
                            //external client is sending echo response
                            if ((packet[29] & 0xff) == 127) {
                                //external client is sending echo response to interanl client
                                //translate global ip to local ip
                                clients.get(globalIPTToLocalIP(destIP, destPort)).outStream.writeObject(globalToLocal(packet, destIP, destPort, 29, 37));
                            } else {
                                //Packet drop
                                //This might not be needed
                                clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 5, destIP + ":" + destPort));
                            }
                        }
                    }

                } else if ((packet[24] & 0xff) == 6 || (packet[24] & 0xff) == 17) {

                    if ((packet[34] & 0xff) < 10) {
                        sourcePort = (packet[33] & 0xff) + "0" + (packet[34] & 0xff);
                    } else {
                        sourcePort = (packet[33] & 0xff) + "" + (packet[34] & 0xff);

                    }
                    if ((packet[36] & 0xff) < 10) {
                        int portNum = ((packet[35] & 0xff) * 100) + (packet[36] & 0xff);
                        destPort = "" + portNum;
                    } else {
                        destPort = (packet[35] & 0xff) + "" + (packet[36] & 0xff);
                    }

                    //TCP/UDP
                    if ((packet[24] & 0xff) == 6) {
                        System.out.println("[Thread: " + IPString + "]> Sending TCP packet from " + sourceIP + ":" + sourcePort + " to " + destIP + ":" + destPort);
                    } else if ((packet[24] & 0xff) == 17) {
                        System.out.println("[Thread: " + IPString + "]> Sending UDP packet from " + sourceIP + ":" + sourcePort + " to " + destIP + ":" + destPort);
                    }
                    if ((packet[25] & 0xff) == 10) {
                        //local is sending TCP/UDP packet
                        if ((packet[29] & 0xff) == 10) {
                            try {
                                clients.get(destIP + ":" + destPort).outStream.writeObject(packet);
                            } catch (NullPointerException npe) {
                                // Send ICMP back with a no route found error
                                System.out.println("[Thread: " + IPString + "]> Client " + sourceIP + ":" + sourcePort + " is attempting to use unused route " + destIP + ":" + destPort);
                                clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 3, destIP + ":" + destPort));
                            }
                        } else {
                            //local is sending to external client
                            //translate local ip to global ip

                            try {
                                clients.get(destIP + ":" + destPort).outStream.writeObject(localToGlobal(packet, sourceIP, sourcePort, 25, 33));
                            } catch (NullPointerException npe) {
                                // Send ICMP back with a no route found error
                                System.out.println("[Thread: " + IPString + "]> Client " + sourceIP + ":" + sourcePort + " is attempting to use unused route " + destIP + ":" + destPort);
                                clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 3, destIP + ":" + destPort));
                            }
                        }
                    } else {
                        //external
                        if ((packet[29] & 0xff) == 127) {
                            //external is sending TCP to local ip
                            //translate global ip to local ip
                            try {

                                clients.get(globalIPTToLocalIP(destIP, destPort)).outStream.writeObject(globalToLocal(packet, destIP, destPort, 29, 35));
                            } catch (NullPointerException npe) {
                                System.out.println("[Thread: " + IPString + "]> Client " + sourceIP + ":" + sourcePort + " is attempting to use unused route " + destIP + ":" + destPort);
                                clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 3, destIP + ":" + destPort));
                            }
                        } else {
                            //drop packet
                            System.out.println("[Thread: " + IPString + "]> Client " + sourceIP + ":" + sourcePort + " is attempting to use unused route " + destIP + ":" + destPort);
                            clients.get(sourceIP + ":" + sourcePort).outStream.writeObject(genICMP("127.0.0.1:9090", NATMAC, 3, 1, null, 5, destIP + ":" + destPort));
                        }
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

    /**
     * Translates global IP address to local IP address using the NATTABLE
     *
     * @param destIP   the IP of the receiving client
     * @param destPort the port of the receiving client
     * @return the local IP assigned to the global IP
     */
    public String globalIPTToLocalIP(String destIP, String destPort) {
        String localAddr = "";
        for (String key : NATTable.keySet()) {
            if ((destIP + ":" + destPort).equals(NATTable.get(key))) {
                localAddr = key;
                break;
            }
        }
        return localAddr;
    }

    /**
     * Translates the packets contents, namely it translates sender's IP and sender's Port
     * from a local address to the globally assigned address
     *
     * @param packet     the packet being sent with a local address
     * @param sourceIP   the sender's IP address
     * @param sourcePort the sender's port
     * @param ipPos      the position of the sender's IP address in the packet
     * @param portPos    the position of the sender's port in the packet
     * @return the packet with the local address translated to global address
     */
    public byte[] localToGlobal(byte[] packet, String sourceIP, String sourcePort, int ipPos, int portPos) {

        String globalAddr = NATTable.get(sourceIP + ":" + sourcePort);
        if (!globalAddr.equals("")) {

            String[] senderInfo = globalAddr.split(":");
            String[] senderIP = senderInfo[0].split("\\.");

            packet[ipPos] = (byte) (Integer.parseInt(senderIP[0]));
            packet[ipPos + 1] = (byte) (Integer.parseInt(senderIP[1]));
            packet[ipPos + 2] = (byte) (Integer.parseInt(senderIP[2]));
            packet[ipPos + 3] = (byte) (Integer.parseInt(senderIP[3]));

            packet[portPos] = (byte) (Integer.parseInt(senderInfo[1].substring(0, 2)));
            packet[portPos + 1] = (byte) (Integer.parseInt(senderInfo[1].substring(2, 4)));
        }
        return packet;
    }

    /**
     * Translates the packets contents, namely it translates receiver's IP and receiver's Port
     * from a globally assigned address to the client's local address
     *
     * @param packet   the packet being sent with a local address
     * @param destIP   the IP of the receiver
     * @param destPort the port of the receiver
     * @param ipPos    the position of the receiver's IP address in the packet
     * @param portPos  the position of the receiver's port in the packet
     * @return the packet with the global address translated to the local address
     */
    public byte[] globalToLocal(byte[] packet, String destIP, String destPort, int ipPos, int portPos) {

        String localAddr = "";
        for (String key : NATTable.keySet()) {
            if ((destIP + ":" + destPort).equals(NATTable.get(key))) {
                localAddr = key;
                break;
            }
        }
        if (!localAddr.equals("")) {

            String[] receiverInfo = localAddr.split(":");
            String[] receiverIP = receiverInfo[0].split("\\.");

            packet[ipPos] = (byte) (Integer.parseInt(receiverIP[0]));
            packet[ipPos + 1] = (byte) (Integer.parseInt(receiverIP[1]));
            packet[ipPos + 2] = (byte) (Integer.parseInt(receiverIP[2]));
            packet[ipPos + 3] = (byte) (Integer.parseInt(receiverIP[3]));

            packet[portPos] = (byte) (Integer.parseInt(receiverInfo[1].substring(0, 2)));
            packet[portPos + 1] = (byte) (Integer.parseInt(receiverInfo[1].substring(2, 4)));

        } else {
            //Packet drop
        }
        return packet;
    }

    /**
     * If a client disconnects then this function is run to close all input stream, output stream, remove the
     * client form the list of clients and remove any NAT table entries related to that client. The function then
     * closes the client socket
     *
     * @throws IOException
     */
    public void exit() throws IOException {
        clients.remove(IPString + ":" + PortString);
        NATTable.remove(IPString + ":" + PortString);
        if (clientPort != -1)
            portPool[clientPort - 9000] = false;
        outStream.close();
        inStream.close();
        client.close();
    }

    /**
     * Translates the given IP to a string
     *
     * @param ip the ip to be translated
     * @return a string version of the ip given
     */
    public static String IPtoString(int[] ip) {
        String IPString = "" + ip[0];
        for (int i = 1; i < ip.length; i++) {
            IPString = IPString + "." + ip[i];
        }
        return IPString;
    }

    /**
     * Generates an IP address for the client and checks if the generated IP is not already in use
     *
     * @param head the first number in the IP address, this is used to indicate if the client is local or global
     * @return the assigned IP address as an int array
     */
    public int[] genIP(int head) {
        int[] ip = new int[4];
        boolean free = false;
        for (int b = 0; b < 255; b++) {
            for (int c = 0; c < 255; c++) {
                for (int d = 0; d < 255; d++) {
                    free = true;
                    for (String addr : clients.keySet()) {
                        if (addr.split(":")[0].equals(head + "." + b + "." + c + "." + d)) {
                            free = false;
                            break;
                        }
                    }
                    if (free) {
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

    /**
     * Generates a minimal ICMP packet using the information provided
     *
     * @param NATIP    the IP of the NAT
     * @param MAC      the MAC address of the sender
     * @param type     the ICMP type
     * @param protocol the IP protocol
     * @param data     if the ICMP is a type 16 packet then it will contain information requested by the client
     * @param code     the ICMP code
     * @param destData the information of the receiver
     * @return the ICMP packet
     * @throws IOException
     */
    public byte[] genICMP(String NATIP, byte[] MAC, int type, int protocol, String data, int code, String destData) throws IOException {

        byte[] preamble = new byte[7];
        for (int i = 0; i < 7; i++) {
            preamble[i] = (byte) (i % 2);
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        String[] sourceInfo = NATIP.split(":");
        String[] sourceIPString = sourceInfo[0].split("\\.", 4);

        String[] destInfo = destData.split(":");
        String[] destIPString = destInfo[0].split("\\.", 4);

        //Ethernet
        byteStream.write(preamble);// Preamble: 7 bytes : 0
        byteStream.write((byte) 171); //stf 1 byte : 7
        byteStream.write(MAC); // MAC destination : 6 bytes : 8
        byteStream.write(NATMAC); // MAC source : 6 bytes: 14
        byteStream.write((byte) 25);// Length : 1 byte : 20

        //TCP
        byteStream.write((byte) 12);//Start of data : 1 byte : 21
        byteStream.write((byte) 12);//Total length : 1 byte : 22
        byteStream.write((byte) 2); //Time to live : 1 byte : 23
        byteStream.write((byte) protocol); //Protocol : 1 byte : 24
        byteStream.write((byte) Integer.parseInt(sourceIPString[0]));
        byteStream.write((byte) Integer.parseInt(sourceIPString[1]));
        byteStream.write((byte) Integer.parseInt(sourceIPString[2]));
        byteStream.write((byte) Integer.parseInt(sourceIPString[3]));//Source IP : 4 bytes : 25
        byteStream.write((byte) IP[0]);
        byteStream.write((byte) IP[1]);
        byteStream.write((byte) IP[2]);
        byteStream.write((byte) IP[3]);//Dest IP : 4 bytes; 29


        //ICMP
        byteStream.write((byte) type);//33
        byteStream.write((byte) code);//34

        byteStream.write(Port);//Source port : 2 byte : 35
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(0, 2))));
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(2, 4))));//Dest port : 2 bytes : 36


        if (type == 16)
            byteStream.write(data.getBytes());//36

        return byteStream.toByteArray();
    }
}
