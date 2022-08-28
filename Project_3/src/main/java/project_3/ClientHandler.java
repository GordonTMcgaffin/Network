package project_3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
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
    private int NATPort;
    private byte[] Port;
    public String PortString;
    private boolean[] portPool;
    public int clientPort;

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
        this.Port = genPort();
        this.portPool = portPool;

        int p;
        for (p = 0; p < portPool.length; p++) if (!portPool[p]) break;
        portPool[p] = true;
        clientPort = 9000 + p;

        PortString = (Port[0] & 0xff) + "" + (Port[1] & 0xff);

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
            outStream.writeObject(Port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clients.put(IPString, this);
        System.out.println("[Thread: " + IPString + "]> Client connected");
    }

    @Override
    public void run() {
        byte[] packet;
        while (true) {
            try {

                packet = (byte[]) inStream.readObject();

                String sourceIP = (packet[17] & 0xff) + "." + (packet[18] & 0xff) + "." + (packet[19] & 0xff) + "." + (packet[20] & 0xff);
                String sourcePort = "";


                String destIP = (packet[21] & 0xff) + "." + (packet[22] & 0xff) + "." + (packet[23] & 0xff) + "." + (packet[24] & 0xff);
                String destPort = "";


                if ((packet[16] & 0xff) == 1) {
                    if ((packet[28] & 0xff) < 10) {
                        sourcePort = (packet[27] & 0xff) + "0" + (packet[28] & 0xff);
                    } else {
                        sourcePort = (packet[27] & 0xff) + "" + (packet[28] & 0xff);
                    }
                    if ((packet[30] & 0xff) < 10) {
                        int portNum = ((packet[29] & 0xff) * 100) + (packet[30] & 0xff);
                        destPort = "" + portNum;
                    } else {
                        destPort = (packet[29] & 0xff) + "" + (packet[30] & 0xff);
                    }
                    //ICMP packet
                    if ((packet[25] & 0xff) == 15) {
                        //Client sent a list request
                        System.out.println("[Thread: " + IPString + "]> Sending client list from " + destIP + " to " + sourceIP);
                        String clientList = "Clients:";
                        if ((packet[17] & 0xff) == 10) {
                            //Request was made by internal client
                            //Generate list of local and global clients
                            for (String ip : NATTable.keySet()) {
                                clientList += "--> " + ip + "\n";
                            }
                        } else {
                            //Request was made by external client
                            //Generate list of global ip + local ports
                            for (String ip : NATTable.values()) {
                                clientList += "--> " + ip + "\n";
                            }
                        }
                        byte[] response = genICMP("127.0.0.1:9090", NATMAC, 16, 1, clientList);
                        outStream.writeObject(response);

                    } else if ((packet[25] & 0xff) == 8) {
                        //Client sent an echo request
                        System.out.println("[Thread: " + IPString + "]> Sending Echo from " + sourceIP + " to " + destIP);
                        if ((packet[17] & 0xff) == 10) {
                            //local client is sending an echo request
                            if ((packet[21] & 0xff) == 10) {
                                //request is being sent to internal client
                                clients.get(destIP).outStream.writeObject(packet);
                            } else {
                                //request is being sent to external client
                                //translate local source ip to global ip
                                System.out.println("Sending to " + destIP);
                                clients.get(destIP).outStream.writeObject(localToGlobal(packet, destIP, destPort, 21, 29));
                            }
                        } else {
                            //External client is sending an echo request
                            if ((packet[21] & 0xff) == 123) {
                                //External client is trying to send to external client, drop the packet
                                System.out.println("Packet dropped");
                            } else {
                                //External client is sending to internal client, translate global dest sip to local dest ip
                                clients.get(globalIPToLoaclIP(destIP, destPort)).outStream.writeObject(globalToLocal(packet, destIP, destPort, 21, 29));
                            }
                        }
                    } else if ((packet[25] & 0xff) == 0) {
                        //Client is sending an echo response
                        System.out.println("[Thread: " + IPString + "]> Sending Echo response from " + sourceIP + " to " + destIP);
                        if ((packet[17] & 0xff) == 10) {
                            //local client is sending echo response
                            if ((packet[21] & 0xff) == 123) {
                                //local client is sending echo response to external client
                                //translate ip address
                                clients.get(destIP).outStream.writeObject(localToGlobal(packet, destIP, destPort, 21, 29));
                            } else {
                                //local client is sending to local client
                                clients.get(destIP).outStream.writeObject(packet);
                            }
                        } else {
                            //external client is sending echo response
                            if ((packet[21] & 0xff) == 10) {
                                //external client is sending echo response to interanl client
                                //translate global ip to local ip

                                clients.get(destIP).outStream.writeObject(globalToLocal(packet, destIP, destPort, 21, 29));
                            } else {
                                //Packet drop
                            }
                        }
                    }

                } else if ((packet[16] & 0xff) == 6) {
                    if ((packet[26] & 0xff) < 10) {
                        sourcePort = (packet[25] & 0xff) + "0" + (packet[26] & 0xff);
                    } else {
                        sourcePort = (packet[25] & 0xff) + "" + (packet[26] & 0xff);
                    }
                    if ((packet[28] & 0xff) < 10) {
                        int portNum = ((packet[27] & 0xff) * 100) + (packet[28] & 0xff);
                        destPort = "" + portNum;
                    } else {
                        destPort = (packet[27] & 0xff) + "" + (packet[28] & 0xff);
                    }
                    //TCP/UDP
                    System.out.println("[Thread: " + IPString + "]> Sending TCP packet from " + sourceIP + " to " + destIP);
                    if ((packet[17] & 0xff) == 10) {
                        //local is sending TCP packet
                        if ((packet[21] & 0xff) == 10) {
                            try {
                                clients.get(destIP).outStream.writeObject(packet);
                            } catch (NullPointerException npe) {
                                // Send ICMP back with a no route found error
                                //outStream.writeObject();
                            }
                        } else {
                            //local is sending to external client
                            //translate local ip to global ip
                            //set up catch for if client doesn't exist
                            clients.get(destIP).outStream.writeObject(localToGlobal(packet, sourceIP, sourcePort, 21, 27));
                        }
                    } else {
                        //external
                        if ((packet[21] & 0xff) == 127) {
                            //external is sending TCP to local ip
                            //translate global ip to local ip
                            clients.get(globalIPToLoaclIP(destIP, destPort)).outStream.writeObject(globalToLocal(packet, destIP, destPort, 21, 27));
                        } else {
                            //drop packet
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

    public String globalIPToLoaclIP(String destIP, String destPort) {

        String localAddr = "";

        System.out.println(destIP + ":" + destPort);
        for (String key : NATTable.keySet()) {
            if ((destIP + ":" + destPort).equals(NATTable.get(key))) {
                localAddr = key;
                break;
            }
        }
        System.out.println(localAddr);
        return localAddr.split(":")[0];
    }

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
        } else {
            //Packet drop
        }

        return packet;
    }

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

    public void exit() throws IOException {
        clients.remove(IPString);
        NATTable.remove(IPString + ":" + PortString);
        portPool[clientPort - 9000] = false;
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
                    if (!clients.containsKey(head + "." + b + "." + c + "." + d)) {
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

    public byte[] genPort() {
        Random rand = new Random();
        byte[] port = new byte[2];
        port[0] = (byte) Math.floor(Math.random() * (99 - 10 + 1) + 10);
        port[1] = (byte) Math.floor(Math.random() * (99 - 10 + 1) + 10);
        return port;
    }

    public byte[] genICMP(String NATIP, byte[] MAC, int type, int protocol, String data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        String[] sourceInfo = NATIP.split(":");
        String[] sourceIPString = sourceInfo[0].split("\\.", 4);

        //Ethernet
        byteStream.write(MAC); // MAC destination : 6 bytes : 0
        byteStream.write(NATMAC); // MAC source : 6 bytes: 6
        byteStream.write((byte) 25);// Length : 1 byte : 12

        //TCP
        byteStream.write((byte) 12);//Start of data : 1 byte : 13
        byteStream.write((byte) 12);//Total length : 1 byte : 14
        byteStream.write((byte) 2); //Time to live : 1 byte : 15
        byteStream.write((byte) protocol); //Protocol : 1 byte : 16
        byteStream.write((byte) Integer.parseInt(sourceIPString[0]));
        byteStream.write((byte) Integer.parseInt(sourceIPString[1]));
        byteStream.write((byte) Integer.parseInt(sourceIPString[2]));
        byteStream.write((byte) Integer.parseInt(sourceIPString[3]));//Source IP : 4 bytes : 21
        byteStream.write((byte) IP[0]);
        byteStream.write((byte) IP[1]);
        byteStream.write((byte) IP[2]);
        byteStream.write((byte) IP[3]);//Dest IP : 4 bytes; 17


        //ICMP
        byteStream.write((byte) type);//22
        byteStream.write((byte) 0);//23
        byteStream.write(data.getBytes());

        return byteStream.toByteArray();
    }
}
