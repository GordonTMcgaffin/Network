import static java.lang.System.out;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;

public final class Sender {

    public static void send(String filename, String host, int port, String protocol, int blastSz, int packetSz)
        throws Exception
    {
        try (Socket socket = new Socket(host, port);
                ObjectInputStream socketIn = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream socketOut = new ObjectOutputStream(socket.getOutputStream())) {

            // Send basic information to the receiver.
            socketOut.writeObject(protocol);
            Path filePath = Path.of(filename);
            socketOut.writeObject(filePath.getFileName().toString());
            socketOut.writeObject(Files.size(filePath));
            socketOut.writeObject(blastSz);
            socketOut.writeObject(packetSz);

            if (protocol.equals("TCP")) {
                // Send with TCP.
                try (BufferedInputStream fileIn = new BufferedInputStream(Files.newInputStream(filePath))) {
                    byte[] data = new byte[packetSz];
                    int bytesRead = 0;
                    while ((bytesRead = fileIn.read(data, 0, packetSz)) != -1) {
                        socketOut.write(data, 0, bytesRead);
                    }
                }
            } else {
                // Send with RBUDP.
                try (DatagramSocket datagramSocket = new DatagramSocket();
                        BufferedInputStream fileIn = new BufferedInputStream(Files.newInputStream(filePath))) {
                    InetAddress ip = InetAddress.getByName(host);
                    int bytesRead = 0;
                    while (bytesRead != -1) {
                        // Create packets.
                        DatagramPacket[] packets = new DatagramPacket[blastSz];
                        for (int i = 0; i < blastSz; i++) {
                            byte[] data = new byte[packetSz];
                            bytesRead = fileIn.read(data, 4, packetSz - 4);
                            writeShortToBytes(data, 0, i);
                            writeShortToBytes(data, 2, (bytesRead == -1) ? 0 : bytesRead);
                            packets[i] = new DatagramPacket(data, packetSz, ip, port);
                        }
                        // While the receiver reports missing packets, resend.
                        while (((int) socketIn.readObject()) != blastSz) {
                            boolean[] received = (boolean[]) socketIn.readObject();
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

    private static void writeShortToBytes(byte[] data, int start, int num)
    {
        data[start]   = (byte) (num >> 8);
        data[start+1] = (byte) num;
    }
}
