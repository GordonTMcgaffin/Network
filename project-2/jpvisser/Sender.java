/**
 * @file    Sender.java
 * @brief   Send files via TCP or RBUDP.
 * @author  G. Mcgaffin (23565608@sun.ac.za)
 * @author  J. P. Visser (21553416@sun.ac.za)
 * @date    2022-08-16
 */

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static java.lang.System.out;

/**
 * The {@code Sender} class provides a function for sending files, either by TCP
 * or RBUDP.
 */
public final class Sender {

    /* --- Static Methods --------------------------------------------------- */

    /**
     * Sends a file via TCP or RBUDP.
     * 
     * @param  filename  complete name of a file
     * @param  host  IP address of the device to send to
     * @param  port  port to send to
     * @param  protocol  TCP or RBUDP
     * @param  blastSz  how many packets to send at once when sending via RBUDP
     * @param  packetSz  size of the packets to send (number of bytes)
     * @throws Exception  if anything goes wrong
     */
    public static void send(String filename, String host, int port,
            String protocol, int blastSz, int packetSz)
        throws Exception
    {
        try (Socket socket = new Socket(host, port);
                ObjectInputStream socketIn =
                new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream socketOut =
                new ObjectOutputStream(socket.getOutputStream())) {

            // Send basic information to the receiver.
            socketOut.writeObject(protocol);
            Path filePath = Path.of(filename);
            socketOut.writeObject(filePath.getFileName().toString());
            socketOut.writeObject(Files.size(filePath));
            socketOut.writeObject(blastSz);
            socketOut.writeObject(packetSz);

            if (protocol.equals("TCP")) {
                // Send with TCP.
                try (BufferedInputStream fileIn =
                        new BufferedInputStream(
                            Files.newInputStream(filePath))) {
                    byte[] data = new byte[packetSz];
                    int bytesRead = 0;
                    while ((bytesRead = fileIn.read(data, 0, packetSz)) != -1) {
                        socketOut.write(data, 0, bytesRead);
                    }
                }
            } else {
                // Send with RBUDP.
                try (DatagramSocket datagramSocket = new DatagramSocket();
                        BufferedInputStream fileIn =
                        new BufferedInputStream(
                            Files.newInputStream(filePath))) {
                    InetAddress ip = InetAddress.getByName(host);
                    int bytesRead = 0;
                    while (bytesRead != -1) {
                        // Create packets.
                        DatagramPacket[] packets = new DatagramPacket[blastSz];
                        for (int i = 0; i < blastSz; i++) {
                            byte[] data = new byte[packetSz];
                            bytesRead = fileIn.read(data, 4, packetSz - 4);
                            writeShortToBytes(data, 0, i);
                            writeShortToBytes(data, 2,
                                    (bytesRead == -1) ? 0 : bytesRead);
                            packets[i] =
                                new DatagramPacket(data, packetSz, ip, port);
                        }
                        // While the receiver reports missing packets, resend.
                        while (((int) socketIn.readObject()) != blastSz) {
                            boolean[] received =
                                (boolean[]) socketIn.readObject();
                            for (int i = 0; i < blastSz; i++) {
                                if (!received[i]) {
                                    datagramSocket.send(packets[i]);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Writes a short integer (the lower two bytes of an integer) to byte array.
     *
     * @param b  byte array to write to
     * @param start  position in the array to start writing to (inclusive)
     * @param num  short integer to write to the byte array
     */
    private static void writeShortToBytes(byte[] b, int start, int num)
    {
        b[start]   = (byte) (num >> 8);
        b[start+1] = (byte) num;
    }
}
