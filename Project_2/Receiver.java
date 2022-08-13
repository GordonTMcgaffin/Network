import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.Arrays;

public class Receiver {
    static int packetSize;
    static int packetAmount;
    static FileData fileInfo;
    static int totalPackets;
    static ObjectInputStream inStream;
    static ObjectOutputStream outStream;
    static ServerSocket TCPSocket;
    static Socket TCPSender;
    static DatagramSocket dataSocket;
    static String TCPDest;
    static String UDPDest;
    static float nano = 1000000000;

    static int maxSequenceAmount = 65500;
    static int packetCheckSize;

    public static void main(String[] args) {

        BufferedReader ip =
                new BufferedReader(new InputStreamReader(System.in));
        try {
            System.out.print("Please enter file packet transfer size > ");
            packetSize = Integer.parseInt(ip.readLine());
            System.out.print("Please enter TCP file download location > ");
            TCPDest = ip.readLine();
            System.out.print("Please enter UDP file download location > ");
            UDPDest = ip.readLine();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            TCPSocket = new ServerSocket(9090);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        try {
            dataSocket = new DatagramSocket(9090);
            dataSocket.setSoTimeout(500);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        runServer();
    }

    public static void runServer() {


        //Init sockets
        System.out.println("Waiting for Sender");
        try {
            TCPSender = TCPSocket.accept();
            inStream = new ObjectInputStream(TCPSender.getInputStream());
            outStream = new ObjectOutputStream(TCPSender.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Client connected");

        while (true) {
            try {
                fileInfo = initSender();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            UDPReceive(dataSocket);
            TCPReceive();

            System.out.println("Both Files received");
        }
    }

    public static void closeServer() throws IOException {
        inStream.close();
        outStream.close();
        TCPSocket.close();
        TCPSender.close();
        dataSocket.close();
    }

    public static void TCPReceive() {
        System.out.println("Starting TCP connection...");
        FileOutputStream FOut = initFile(TCPDest);
        float finalTime = 0;
        try {
            LocalTime startTime = (LocalTime) inStream.readObject();
            byte[] fileBytes = (byte[]) inStream.readObject();
            FOut.write(fileBytes);
            LocalTime endTime = LocalTime.now();
            finalTime = ((float) endTime.getMinute() * 60 + (float) endTime.getSecond() + (float) endTime.getNano() / nano) - ((float) startTime.getMinute() * 60 + (float) startTime.getSecond() + (float) startTime.getNano() / nano);
            FOut.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        System.out.println("File transfer completed in: " + finalTime + "seconds");

    }

    public static void UDPReceive(
            DatagramSocket socket) {

        System.out.println("Starting UDP connection...");
        LocalTime startTime;
        try {
            startTime = (LocalTime) inStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        FileOutputStream FOut = initFile(UDPDest);
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

                try {
                    outStream.writeObject(missingPackets);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (missingPackets[0] == -1) {
                    break;
                }
                for (int j = m; j > 0; j--) {

                    byte[] receivedPacketBytes = new byte[packetSize + 4];
                    receivePacket = new DatagramPacket(receivedPacketBytes, receivedPacketBytes.length);
                    try {
                        socket.receive(receivePacket);
                        receivedPacketBytes = receivePacket.getData();
                        receivedPacketNumber = packetSet + ((receivedPacketBytes[0] & 0xff) << 16) + ((receivedPacketBytes[1] & 0xff) << 8) + (receivedPacketBytes[2] & 0xff);
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

                            System.out.println("Total packets received: " + 100 * ((float) totalPackets / (float) packetAmount) + "%");


                        } else {
                            break;
                        }
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
        LocalTime endTime = LocalTime.now();
        float finalTime = ((float) endTime.getMinute() * 60 + (float) endTime.getSecond() + (float) endTime.getNano() / nano) - ((float) startTime.getMinute() * 60 + (float) startTime.getSecond() + (float) startTime.getNano() / nano);
        System.out.println("File transfer completed in: " + finalTime + "seconds");
    }

    public static FileData initSender() throws IOException {
        FileData fileInfo = null;
        try {
            fileInfo = (FileData) inStream.readObject();
        } catch (IOException e) {
            System.out.println("Client disconnected");
            //closeServer();
            runServer();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        RecieverInfo recieverInfo = new RecieverInfo(packetSize);
        outStream.writeObject(recieverInfo);

        return fileInfo;
    }

    public static FileOutputStream initFile(String location) {
        int fileSize = fileInfo.fileSize;
        packetAmount = (int) fileSize / packetSize + 1;


        packetCheckSize = Math.min(maxSequenceAmount, packetAmount);

        File f = new File(location + fileInfo.fileName);
        FileOutputStream FOut;
        try {
            FOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return FOut;
    }
}
