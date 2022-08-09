import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class Sender {

    public static BufferedInputStream bin;

    public static void main (String[] args) throws IOException, ClassNotFoundException {

        File f = new File(args[0]);
        int fileTransferSize = Integer.parseInt(args[1]);

        Socket dataSocket = new Socket("127.0.0.1",9090);
        FileData fileInfo = new FileData(f.getName(),(int)f.length(),fileTransferSize);
        ObjectOutputStream outStream = new ObjectOutputStream(dataSocket.getOutputStream());
        outStream.writeObject(fileInfo);


        ObjectInputStream inStream = new ObjectInputStream(dataSocket.getInputStream());
        FileData ack = (FileData) inStream.readObject();
        dataSocket.close();

        System.out.println("Sending file...");
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName("127.0.0.1");


        //Get file and convert it to bytes and store it in a byte array
        FileInputStream FIn = new FileInputStream(f);
        byte fileBytes[] = new byte[(int) f.length()];
        FIn.read(fileBytes);
        FIn.close();

        //Split the packet up into smaller chunks to send;
        int packetNumber = 0;
        boolean EOF = false;
        DatagramPacket sendPacket;
        for(int i = 0; i < fileBytes.length; i += fileTransferSize){
            packetNumber++;
            // We add three bytes, two for the packet number and one to indicate EOF
            byte[] packetBytes = new byte[fileTransferSize + 3];

            //We store the packet number over two bytes in case there is a large amount of packets
            //The first packet contains the first 8 bits of the packet number and the second byte contains the last 8 bits
            packetBytes[0] = (byte)(packetNumber >> 8);
            packetBytes[1] = (byte)(packetNumber);

            if((i+fileTransferSize)>= fileBytes.length){
                //We have reached end of file
                EOF = true;
                //The one in the third byte indicates EOF
                packetBytes[2] = 1;
            }else{
                EOF = false;
                packetBytes[2] = 0;
            }

            if(EOF){
                System.arraycopy(fileBytes,i,packetBytes,3,fileBytes.length-i);
            }else{
                System.arraycopy(fileBytes,i,packetBytes,3,fileTransferSize);
            }
            sendPacket = new DatagramPacket(packetBytes, packetBytes.length,address,9090);
            socket.send(sendPacket);
            System.out.println("Packet " + packetNumber + " has been sent. EOF: " + EOF);
        }
        System.out.println("File sent");
    }
}
