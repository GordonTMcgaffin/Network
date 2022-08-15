import static java.lang.System.out;
import java.io.*;
import java.nio.file.*;
import java.net.*;

public final class Receiver {

    private static long fileSz;
    private static long bytesWritten;
    private static double progress;

    public static void receive(int port, int socketTimeout, String dest)
        throws Exception
    {
        bytesWritten = 0L;
        progress = 0.0;
        try (ServerSocket serverSocket = new ServerSocket(port);
                Socket socket = serverSocket.accept();
                ObjectOutputStream socketOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream socketIn = new ObjectInputStream(socket.getInputStream())) {

            // Get basic information from sender.
            String protocol = (String) socketIn.readObject();
            String filename = (String) socketIn.readObject();
            Path filePath = Path.of(dest, filename);
            long fileSz = (long) socketIn.readObject();
            int blastSz = (int) socketIn.readObject();
            int packetSz = (int) socketIn.readObject();

            if (protocol.equals("TCP")) {
                // Receive with TCP.
                try (BufferedOutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(filePath))) {
                    byte[] data = new byte[packetSz];
                    int bytesRead = 0;
                    while ((bytesRead = socketIn.read(data, 0, packetSz)) != -1) {
                        fileOut.write(data, 0, bytesRead);
                        bytesWritten += bytesRead;
                        progress = bytesWritten/((double) fileSz);
                    }
                }
            } else {
                // Receive with RBUDP.
                try (DatagramSocket datagramSocket = new DatagramSocket(port);
                        BufferedOutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(filePath))) {

                    datagramSocket.setSoTimeout(socketTimeout);

                    DatagramPacket[] packets = new DatagramPacket[blastSz];
                    boolean[] received = new boolean[blastSz];
                    int nReceived = 0;

                    while (true) {
                        // Send ACK and write data if possible.
                        socketOut.writeObject(nReceived);
                        if (nReceived == blastSz) {
                            for (int i = 0; i < blastSz; i++) {
                                byte[] data = packets[i].getData();
                                fileOut.write(data, 4, readShortFromBytes(data, 2));
                            }
                            // Update progress.
                            bytesWritten += (packetSz - 4) * blastSz;
                            progress = bytesWritten/((double) fileSz);
                            // Reset packets and received list.
                            packets = new DatagramPacket[blastSz];
                            received = new boolean[blastSz];
                            nReceived = 0;
                        } else {
                            socketOut.writeObject(received);
                        }
                        // Try to receive all packets.
                        try {
                            for (int i = 0; i < blastSz; i++) {
                                DatagramPacket packet = new DatagramPacket(new byte[packetSz], packetSz);
                                datagramSocket.receive(packet);
                                int seqNum = readShortFromBytes(packet.getData(), 0);
                                if (!received[seqNum]) {
                                    packets[seqNum] = packet;
                                    received[seqNum] = true;
                                    nReceived++;
                                }
                            }
                        } catch (SocketTimeoutException stoe) {
                            // Socket timed out. Continue.
                        }
                    }
                }
            }
        }
    }

    private static int readShortFromBytes(byte[] data, int start)
    {
        return ((data[start] & 0xff) << 8) + (data[start+1] & 0xff);
    }

    public static double getProgress()
    {
        return progress;
    }
}
