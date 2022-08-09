import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Receiver {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        ServerSocket dataSocket = new ServerSocket(9090);
        Socket sender = dataSocket.accept();
        ObjectInputStream inStream = new ObjectInputStream(sender.getInputStream());
        FileData fileInfo = (FileData) inStream.readObject();

        System.out.println(fileInfo.fileName);
        String fileName = fileInfo.fileName;
        System.out.println(fileInfo.fileSize);
        int fileSize = fileInfo.fileSize;
        System.out.println(fileInfo.fileTransferSize);
        int fileTransferSize = fileInfo.fileTransferSize;

        ObjectOutputStream outStream = new ObjectOutputStream(sender.getOutputStream());
        outStream.writeObject(fileInfo);
        dataSocket.close();

        File f = new File(fileInfo.fileName);
        FileOutputStream FOut = new FileOutputStream(f);
        DatagramSocket socket = new DatagramSocket(9090);
        System.out.println("Ready for file");

        int packetNumber = 0;
        boolean EOF = false;
        DatagramPacket receivePacket;
        byte[] fileBytes = new byte[fileSize];
        for(int i = 0; i <= fileSize; i += fileTransferSize){
            packetNumber++;

            byte[] packetBytes = new byte[fileTransferSize + 3];


            receivePacket = new DatagramPacket(packetBytes,packetBytes.length);
            socket.receive(receivePacket);
            packetBytes = receivePacket.getData();
            packetNumber = (packetBytes[0] <<8)+ packetBytes[1];
            if(packetBytes[2] == 1){
                //EOF
                System.arraycopy(packetBytes,3,fileBytes,i,fileSize - i);
            }else{
                //Not EOF
                System.arraycopy(packetBytes,3,fileBytes,i,fileTransferSize);
            }

            System.out.println("Received packet "+ packetNumber);


        }

        FOut.write(fileBytes);

//        byte[] message = new byte[fileInfo.fileSize];
//        byte[] fileByteArray = new byte[fileInfo.fileSize];
//
//        DatagramPacket receivePacket = new DatagramPacket(message, message.length);
//        socket.receive(receivePacket);
//        message = receivePacket.getData();
//
//        System.arraycopy(message, 0, fileByteArray, 0, fileInfo.fileSize);
//
//        FOut.write(fileByteArray);

        System.out.println("File received");
    }
}
