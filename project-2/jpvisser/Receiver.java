/**
 * @file    Receiver.java
 * @brief   Receive files via TCP or RBUDP.
 * @author  G. Mcgaffin (23565608@sun.ac.za)
 * @author  J. P. Visser (21553416@sun.ac.za)
 * @date    2022-08-16
 */

import java.io.*;
import java.nio.file.*;
import java.net.*;
import static java.lang.System.out;

/**
 * The {@code Receiver} class provides a function for receiving files, either by
 * TCP or RBUDP. It also provides a function that returns the progress of the
 * file transfer.
 */
public final class Receiver {

    /* --- Static Variables ------------------------------------------------- */

    /** for keeping track of transfer progress */
    private static double progress;

    /* --- Static Methods --------------------------------------------------- */

    /**
     * Receives a file via TCP or RBUDP.
     *
     * @param  port  port to listen to for incoming data
     * @param  socketTimeout  how long to wait for a {@code DatagramPacket} (in
     *                        milliseconds)
     * @param  dest  where to save the file
     * @throws Exception  if anything goes wrong
     */
    public static void receive(int port, int socketTimeout, String dest)
        throws Exception
    {
        long bytesWritten = 0L;
        progress = 0.0;
        try (ServerSocket serverSocket = new ServerSocket(port);
                Socket socket = serverSocket.accept();
                ObjectOutputStream socketOut =
                new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream socketIn =
                new ObjectInputStream(socket.getInputStream())) {

            // Get basic information from sender.
            String protocol = (String) socketIn.readObject();
            String filename = (String) socketIn.readObject();
            Path filePath = Path.of(dest, filename);
            long fileSz = (long) socketIn.readObject();
            int blastSz = (int) socketIn.readObject();
            int packetSz = (int) socketIn.readObject();

            if (protocol.equals("TCP")) {
                // Receive with TCP.
                try (BufferedOutputStream fileOut =
                        new BufferedOutputStream(
                            Files.newOutputStream(filePath))) {
                    byte[] data = new byte[packetSz];
                    int bytesRead = 0;
                    while ((bytesRead = socketIn.read(data, 0, packetSz))
                            != -1) {
                        fileOut.write(data, 0, bytesRead);
                        bytesWritten += bytesRead;
                        progress = bytesWritten/((double) fileSz);
                    }
                }
            } else {
                // Receive with RBUDP.
                try (DatagramSocket datagramSocket = new DatagramSocket(port);
                        BufferedOutputStream fileOut =
                        new BufferedOutputStream(
                            Files.newOutputStream(filePath))) {

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
                                fileOut.write(data, 4,
                                        readShortFromBytes(data, 2));
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
                                DatagramPacket packet = new DatagramPacket(
                                        new byte[packetSz], packetSz);
                                datagramSocket.receive(packet);
                                int seqNum =
                                    readShortFromBytes(packet.getData(), 0);
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

    /**
     * Reads a short integer from a byte array and returns it.
     *
     * @param  b  byte array to read from
     * @param  start  position to start reading from (inclusive)
     * @return short integer read from a byte array
     */
    private static int readShortFromBytes(byte[] b, int start)
    {
        return ((b[start] & 0xff) << 8) + (b[start+1] & 0xff);
    }

    /**
     * Returns the file transfer progress percentage as a number in [0, 1].
     *
     * @return file transfer progress
     */
    public static double getProgress()
    {
        return progress;
    }
}
