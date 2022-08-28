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
                protocol = inputReader.readLine();
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


            IPString = IPtoString(IP);
            MACString = MACtoString(MAC);
            System.out.println("[NAT]> Connection established");
            System.out.println();
            System.out.println("===================================");

            System.out.println("[NAT]> Client IP: " + IPString);
            System.out.println("[NAT]> Client MAC: " + MACString);

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
                        byte[] message = (byte[]) inStream.readObject();
                        System.out.println("Received message");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println("[" + IPString + "]> Message received");

                }
            }

        };
        receiveThread.start();


        sendThread = new Thread() {
            @Override
            public void run() {
                String input = "";
                System.out.println("[" + IPString + "]> Please select a command");
                System.out.println("[" + IPString + "]> 0 = View client list");
                System.out.println("[" + IPString + "]> 1 = PING");
                System.out.println("[" + IPString + "]> 2 = Message");

                while (true) {

                    System.out.print("[" + IPString + "]> ");

                    try {
                        input = inputReader.readLine();
                        if (input.equals("0")) {
                            System.out.println("[" + IPString + "]> Available Clients");
                            //Request client list
                        } else if (input.equals("1")) {
                            System.out.println("[" + IPString + "]> Please enter <IP Address> <Ping amount>");
                            System.out.print("[" + IPString + "]> ");
                            String[] inputs = inputReader.readLine().split(" ");
                            //Send ping for the amount specified
                        } else if (input.equals("2")) {
                            System.out.println("[" + IPString + "]> Please enter <IP Address> <Message>");
                            System.out.print("[" + IPString + "]> ");
                            String[] inputs = inputReader.readLine().split(" ", 2);
                            byte[] packet = genTCPPacket(inputs, 6);

                            outStream.writeObject(packet);


//                          System.out.println("["+IPString+"]> Sending message: ");
//                          byte[] message = new byte[inputs[1].getBytes().length];
//                          System.out.println(inputs[1].getBytes().length);
//                          System.out.println(packet.length);
//                          System.arraycopy(packet, 28,message,0,inputs[1].getBytes().length);
//                          String str = new String(message);
//                          System.out.println("["+IPtoString(IP)+"]> " +str);
                            //Send message to specified address
                        }

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

        System.out.println();
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


    public static byte[] genTCPPacket(String[] data, int protocol) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] message = data[1].getBytes();
        int messageSize = message.length;

        byte ethernetFrameLength = (byte) (28 + messageSize - 1);

        byte ipFrameLength = (byte) (12 + messageSize - 1);

        String[] destIPString = data[0].split("\\.", 4);

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
        byteStream.write((byte) 10);//Source port : 1 byte : 22
        byteStream.write((byte) 10);//Dest port : 1 byte : 23
        byteStream.write((byte) 3);//Data off set : 1 byte : 24
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
