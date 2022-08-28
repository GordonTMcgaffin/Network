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
    private static String IPString;
    private static String MACString;
    private static byte[] NATMAC;
    private static byte[] Port;
    private static String PortString;

    public static void main(String args[]) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int location = Integer.parseInt(args[2]); // location = 0 means that the client is a local connection. Location = 1 means that the client is external.

        MAC = genMAC();

        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
        String protocol = "";
        while (!(protocol.equals("TCP") || protocol.equals("UDP"))) {
            System.out.print("[NAT] Please select protocol (TCP/UDP)> ");
            try {
                protocol = inputReader.readLine().trim();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!(protocol.equals("TCP") || protocol.equals("UDP")))
                System.out.println("[NAT]> Please enter a valid protocol.");
        }

        System.out.println("[NAT]> Connecting to NAT. . . ");

        try {
            serverSocket = new Socket(host, port);
            outStream = new ObjectOutputStream(serverSocket.getOutputStream());
            inStream = new ObjectInputStream(serverSocket.getInputStream());
            outStream.writeObject(location);

            IP = (int[]) inStream.readObject();
            NATMAC = (byte[]) inStream.readObject();
            Port = (byte[]) inStream.readObject();

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
            throw new RuntimeException(e);
        }

        receiveThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] packet = (byte[]) inStream.readObject();
                        byte[] messageBytes;
                        String sourceIP = (packet[17] & 0xff) + "." + (packet[18] & 0xff) + "." + (packet[19] & 0xff) + "." + (packet[20] & 0xff);
                        String sourcePort = "";
                        if ((packet[28] & 0xff) < 10) {
                            sourcePort = (packet[27] & 0xff) + "0" + (packet[28] & 0xff);
                        } else {
                            sourcePort = (packet[27] & 0xff) + "" + (packet[28] & 0xff);
                        }
                        byte[] sourceIPBytes = {packet[17], packet[18], packet[19], packet[20]};
                        String destIP = (packet[21] & 0xff) + "." + (packet[22] & 0xff) + "." + (packet[23] & 0xff) + "." + (packet[24] & 0xff);
                        byte[] destIPBytes = {packet[21], packet[22], packet[23], packet[24]};
                        if ((packet[16] & 0xff) == 1) {
                            //ICMP packet
                            if ((packet[25] & 0xff) == 16) {
                                //ICMP response
                                messageBytes = new byte[packet.length - 28];
                                System.arraycopy(packet, 27, messageBytes, 0, packet.length - 28);
                                String message = new String(messageBytes);
                                System.out.println();
                                System.out.println("[" + IPString + ":" + PortString + "]> Available Clients");
                                System.out.println(message.split(":", 2)[1]);
                                System.out.print("[" + IPString + "]> ");
                            } else if ((packet[25] & 0xff) == 8) {
                                //ICMP echo
                                packet = genICMP(sourceIP + ":" + sourcePort, 0, 1);
                                outStream.writeObject(packet);
                            } else if ((packet[25] & 0xff) == 0) {
                                System.out.println("Client " + sourceIP + " is reachable");
                                System.out.print("[" + IPString + ":" + PortString + "]> ");
                            }
                        } else if ((packet[16] & 0xff) == 6) {
                            //TCP message
                            messageBytes = new byte[packet.length - 30];
                            System.arraycopy(packet, 30, messageBytes, 0, packet.length - 30);
                            String message = new String(messageBytes);
                            System.out.println();
                            System.out.println("[" + sourceIP + ":" + (packet[27] & 0xff) + "" + (packet[28] & 0xff) + "]> " + message);
                            System.out.print("[" + IPString + ":" + PortString + "]> ");
                        }

                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                }
            }

        };
        receiveThread.start();


        sendThread = new Thread() {
            @Override
            public void run() {
                String input = "";
                System.out.println("[" + IPString + ":" + PortString + "]> Please select a command");
                System.out.println("[" + IPString + ":" + PortString + "]> 0 = View client list");
                System.out.println("[" + IPString + ":" + PortString + "]> 1 = PING");
                System.out.println("[" + IPString + ":" + PortString + "]> 2 = Message");
                System.out.print("[" + IPString + ":" + PortString + "]> ");
                while (true) {
                    try {
                        input = inputReader.readLine();
                        byte[] packet = null;
                        if (input.equals("0")) {
                            //Request client list
                            packet = genICMP(serverSocket.getLocalAddress().getHostAddress() + ":" + port, 15, 1);
                        } else if (input.equals("1")) {
                            //Send ping for the amount specified
                            System.out.println("[" + IPString + ":" + PortString + "]> Please enter <IP Address:Port> <Ping amount>");
                            System.out.print("[" + IPString + ":" + PortString + "]> ");
                            String[] inputs = inputReader.readLine().split(" ");
                            packet = genICMP(inputs[0], 8, 1);
                        } else if (input.equals("2")) {
                            //Send message using selected protocol
                            System.out.println("[" + IPString + ":" + PortString + "]> Please enter <IP Address:Port> <Message>");
                            System.out.print("[" + IPString + ":" + PortString + "]> ");
                            String[] inputs = inputReader.readLine().split(" ", 2);
                            packet = genMSGPacket(inputs, 6);
                            System.out.print("[" + IPString + ":" + PortString + "]> ");
                        }
                        outStream.writeObject(packet);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        sendThread.start();
    }

    public static byte[] genMAC() {
        Random rand = new Random();
        byte[] MAC = new byte[6];
        rand.nextBytes(MAC);
        return MAC;
    }

    public static String MACtoString(byte[] MAC) {
        String MACString = String.format("%02x", MAC[1]);
        for (int i = 1; i < MAC.length; i++) {
            MACString = MACString + ":" + String.format("%02x", MAC[i]);
        }
        return MACString;
    }

    public static String IPtoString(int[] ip) {
        String IPString = "" + ip[0];
        for (int i = 1; i < ip.length; i++) {
            IPString = IPString + "." + ip[i];
        }
        return IPString;
    }

    public static byte[] genICMP(String data, int type, int protocol) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        String[] destInfo = data.split(":");
        String[] destIPString = destInfo[0].split("\\.", 4);

        //Ethernet
        byteStream.write(NATMAC); // MAC destination : 6 bytes : 0
        byteStream.write(MAC); // MAC source : 6 bytes: 6
        byteStream.write((byte) 25);// Length : 1 byte : 12

        //TCP
        byteStream.write((byte) 12);//Start of data : 1 byte : 13
        byteStream.write((byte) 12);//Total length : 1 byte : 14
        byteStream.write((byte) 2); //Time to live : 1 byte : 15
        byteStream.write((byte) protocol); //Protocol : 1 byte : 16
        byteStream.write((byte) IP[0]);
        byteStream.write((byte) IP[1]);
        byteStream.write((byte) IP[2]);
        byteStream.write((byte) IP[3]);//Source IP : 4 bytes; 17
        byteStream.write((byte) Integer.parseInt(destIPString[0]));
        byteStream.write((byte) Integer.parseInt(destIPString[1]));
        byteStream.write((byte) Integer.parseInt(destIPString[2]));
        byteStream.write((byte) Integer.parseInt(destIPString[3]));//Destination source : 4 bytes : 24

        //ICMP
        byteStream.write((byte) type);//25
        byteStream.write((byte) 0);//26

        byteStream.write(Port);//Source port : 2 byte : 27
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(0, 2))));
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(2, 4))));//Dest port : 2 bytes : 29

        return byteStream.toByteArray();
    }

    public static byte[] genMSGPacket(String[] data, int protocol) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] message = data[1].getBytes();

        String[] destInfo = data[0].split(":");
        String[] destIPString = destInfo[0].split("\\.", 4);

        int messageSize = message.length;
        byte ethernetFrameLength = (byte) (29 + messageSize - 1);
        byte ipFrameLength = (byte) (13 + messageSize - 1);


        //Ethernet header
        byteStream.write(NATMAC); // MAC destination : 6 bytes : 0
        byteStream.write(MAC); // MAC source : 6 bytes: 6
        byteStream.write(ethernetFrameLength);// Length : 1 byte : 12

        //IP header
        byteStream.write((byte) 12);//Start of data : 1 byte : 13
        byteStream.write(ipFrameLength);//Total length : 1 byte : 14
        byteStream.write((byte) 2); //Time to live : 1 byte : 15
        byteStream.write((byte) protocol); //Protocol : 1 byte : 16
        byteStream.write((byte) IP[0]);
        byteStream.write((byte) IP[1]);
        byteStream.write((byte) IP[2]);
        byteStream.write((byte) IP[3]);//Source IP : 4 bytes; 17
        byteStream.write((byte) Integer.parseInt(destIPString[0]));
        byteStream.write((byte) Integer.parseInt(destIPString[1]));
        byteStream.write((byte) Integer.parseInt(destIPString[2]));
        byteStream.write((byte) Integer.parseInt(destIPString[3]));//Destination source : 4 bytes : 21

        //TCP
        byteStream.write(Port);//Source port : 2 byte : 25
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(0, 2))));
        byteStream.write((byte) (Integer.parseInt(destInfo[1].substring(2, 4))));//Dest port : 2 bytes : 27


        byteStream.write((byte) 3);//Data off set : 1 byte : 29
        byteStream.write(message);
        return byteStream.toByteArray();
    }


    /*
    Headers to be included for each packet that is sent.

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

            padding is to ensure the header ends on a 32 bit boundary

    TCP header
                       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                       |          Source Port          |       Destination Port        |
                       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                       |      Data                  |                                  |
                       |      Offset                |                -                 |
                       |    Where the data begins   |                                  |
                       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                       |                             data                              |
                       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+



    UDP header
                     +--------+--------+--------+--------+
                     |     Source      |   Destination   |
                     |      Port       |      Port       |
                     +--------+--------+--------+--------+
                     |  Header + data  |                 |
                     |     Length      |        -        |
                     +--------+--------+--------+--------+
                     |               data                |
                     +--------+--------+--------+--------+
     */

}
