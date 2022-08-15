import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Arrays;

public class Receiver {
    static int packetSize;
    static int packetAmount;
    static int totalPackets;
    static int packetCheckSize;
    static double progress;
    static FileData fileInfo;
    static ObjectInputStream inStream;
    static ObjectOutputStream outStream;
    static ServerSocket TCPSocket;
    static Socket TCPSender;
    static DatagramSocket dataSocket;
    static String Dest;

    static float nano = 1000000000;
    static int maxSequenceAmount = 65500;


    public static void receive(int port, int socketTimeout, String dest) {
        System.out.println(dest);
        Dest = dest;

        packetSize = 65000;

        //set up sockets
        try {
            TCPSocket = new ServerSocket(port);
            dataSocket = new DatagramSocket(port);
            dataSocket.setSoTimeout(socketTimeout);
            TCPSender = TCPSocket.accept();
            inStream = new ObjectInputStream(TCPSender.getInputStream());
            outStream = new ObjectOutputStream(TCPSender.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        runServer();
    }

    public static void runServer() {

        while (true) {
            fileInfo = initSender();
            UDPReceive();
            TCPReceive();
        }
    }

    public static void closeServer() throws IOException {
        inStream.close();
        outStream.close();
        TCPSocket.close();
        TCPSender.close();
        dataSocket.close();
    }

    public static double getProgress() {
        return progress;
    }

    public static void TCPReceive() {
        BufferedOutputStream FOut = initFile();
        try {
            FOut.write((byte[]) inStream.readObject());
            FOut.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void UDPReceive() {
        BufferedOutputStream FOut = initFile();
        int[] receivedPackets = new int[packetAmount];
        //Status: 0 = not received; 1 = received
        Arrays.fill(receivedPackets, 0);
        byte[] fileBytes = new byte[fileInfo.fileSize];
        DatagramPacket receivePacket;
        int receivedPacketNumber = 0;
        totalPackets = 0;
        for (int packetSet = 0; packetSet < packetAmount; packetSet += packetCheckSize) {
            boolean complete = false;
            while (!complete) {
                int[] missingPackets = new int[packetCheckSize];
                Arrays.fill(missingPackets, -1);
                //Create a list of missing packets
                int m = 0;
                if (!((packetSet + packetCheckSize) > packetAmount)) {
                    for (int p = packetSet; p < packetSet + packetCheckSize; p++) {
                        if (receivedPackets[p] == 0) {
                            missingPackets[m] = p;
                            m++;
                        }
                    }
                } else {
                    for (int p = packetSet; p < packetAmount; p++) {
                        if (receivedPackets[p] == 0) {
                            missingPackets[m] = p;
                            m++;
                        }
                    }
                }
                //Send request list to sender for resending
                try {
                    outStream.writeObject(missingPackets);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //if there are no more packets for this packet set, then break
                if (missingPackets[0] == -1) {
                    break;
                }
                for (int j = m; j > 0; j--) {
                    byte[] receivedPacketBytes = new byte[packetSize + 4];
                    receivePacket = new DatagramPacket(receivedPacketBytes, receivedPacketBytes.length);
                    try {
                        dataSocket.receive(receivePacket);
                        receivedPacketBytes = receivePacket.getData();
                        receivedPacketNumber = packetSet + ((receivedPacketBytes[0] & 0xff) << 16) + ((receivedPacketBytes[1] & 0xff) << 8) + (receivedPacketBytes[2] & 0xff);
                        //Check if the packet is a late packet and hasn't already been received
                        if (!(receivedPackets[receivedPacketNumber] == 1)) {
                            if (receivedPacketBytes[3] == 1) {
                                //EOF
                                System.arraycopy(receivedPacketBytes, 4,
                                        fileBytes, (receivedPacketNumber) * (packetSize), fileInfo.fileSize - (packetAmount - 1) * (packetSize));
                            } else {
                                //Not EOF
                                System.arraycopy(receivedPacketBytes, 4, fileBytes, (receivedPacketNumber) * (packetSize), packetSize);
                            }
                            receivedPackets[receivedPacketNumber] = 1;
                            totalPackets++;
                            progress = (double) totalPackets / (double) packetAmount;
                        }//------------------------
                    } catch (IOException e) {
                        break;
                    }

                }

            }
        }
        try {
            FOut.write(fileBytes);
            FOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileData initSender() {
        FileData fileInfo = null;
        try {
            fileInfo = (FileData) inStream.readObject();
            RecieverInfo recieverInfo = new RecieverInfo(packetSize);
            outStream.writeObject(recieverInfo);
        } catch (IOException e) {
            runServer();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return fileInfo;
    }

    public static BufferedOutputStream initFile() {
        int fileSize = fileInfo.fileSize;
        packetAmount = (int) fileSize / packetSize + 1;
        packetCheckSize = Math.min(maxSequenceAmount, packetAmount);
        Path path = Path.of(Dest + fileInfo.fileName);
        BufferedOutputStream FOut;
        try {
            FOut = new BufferedOutputStream(Files.newOutputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return FOut;
    }
}
