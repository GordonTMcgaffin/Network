import java.io.*;
import java.net.*;
import java.time.LocalTime;

public class Sender {
    static int packetSize;
    static int packetAmount;

    static Socket dataSocket;
    static ObjectOutputStream outStream;
    static ObjectInputStream inStream;
    static DatagramSocket socket;
    static InetAddress address;

    public static void main(String[] args) {

        //Initialise communication with receiver

        try {
            dataSocket = new Socket("127.0.0.1", 9090);
            outStream = new ObjectOutputStream(dataSocket.getOutputStream());
            inStream = new ObjectInputStream(dataSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String input = "";
        while (!input.equals("exit")) {
            //File to transfer
            System.out.print("Please enter file name > ");
            BufferedReader ip =
                    new BufferedReader(new InputStreamReader(System.in));

            try {
                input = ip.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (input.equals("exit")) {

                close();
            }

            File file = new File(input);
            System.out.println("Sending file: " + file.getPath());
            byte[] fileBytes = initFileTransfer(file);
            packetAmount = (int) file.length() / packetSize + 1;

            try {
                socket = new DatagramSocket();
                address = InetAddress.getByName("127.0.0.1");
            } catch (SocketException | UnknownHostException e) {
                throw new RuntimeException(e);
            }
            int packetNumber = 0;
            UDPSend(fileBytes);
            TCPSend(fileBytes);
        }
    }

    public static void close() {
        try {
            dataSocket.close();
            outStream.close();
            inStream.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

    public static void TCPSend(byte[] fileBytes) {
        System.out.println("Sending file via TCP...");
        try {
            outStream.writeObject(LocalTime.now());
            outStream.writeObject(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("File sent");
    }

    public static void UDPSend(byte[] fileBytes) {

        System.out.println("Sending files via UDP...");
        try {
            outStream.writeObject(LocalTime.now());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        boolean complete = false;

        while (!complete) {
            int[] request;
            try {
                request = (int[]) inStream.readObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }


            if (request[0] == -1) {
                complete = true;
            } else {

                DatagramPacket sendPacket;
                boolean EOF = false;
                for (int r = 0; r < request.length; r++) {
                    if (request[r] != -1) {
                        byte[] packetBytes = new byte[packetSize + 4];
                        packetBytes[0] = (byte) (request[r] >> 16);
                        packetBytes[1] = (byte) (request[r] >> 8);
                        packetBytes[2] = (byte) (request[r]);

                        int PacketNumber = ((packetBytes[0] & 0xff) << 16) + ((packetBytes[1] & 0xff) << 8) + (packetBytes[2] & 0xff);

                        if ((request[r] * packetSize + packetSize) >= fileBytes.length) {
                            //We have reached end of file
                            EOF = true;
                            //The one in the third byte indicates EOF
                            packetBytes[3] = 1;
                        } else {
                            EOF = false;
                            packetBytes[3] = 0;
                        }

                        if (EOF) {
                            System.arraycopy(fileBytes, request[r] * packetSize, packetBytes, 4, fileBytes.length - (packetAmount - 1) * packetSize);
                        } else {
                            System.arraycopy(fileBytes, request[r] * packetSize, packetBytes, 4, packetSize);
                        }
                        sendPacket = new DatagramPacket(packetBytes, packetBytes.length, address, 9090);
                        try {
                            socket.send(sendPacket);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        System.out.println("File sent");
    }

    public static byte[] initFileTransfer(File file) {


        FileData fileInfo = new FileData(file.getName(), (int) file.length());

        try {
            //Send file info to receiver
            System.out.println("Sending file info...");
            outStream.writeObject(fileInfo);
            System.out.println("File info sent");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            //Receive ack
            System.out.println("Waiting for receiver info...");
            RecieverInfo recieverInfo = (RecieverInfo) inStream.readObject();
            packetSize = recieverInfo.packetSize;
            System.out.println("Receiver info received");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        FileInputStream FIn = null;
        try {
            FIn = new FileInputStream(file);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        byte fileBytes[] = new byte[(int) file.length()];
        try {
            FIn.read(fileBytes);
            FIn.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileBytes;
    }

}
