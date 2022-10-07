package Client;

import p2p.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static Socket serverSocket;
    public static ObjectInputStream inStream;
    public static ObjectOutputStream outStream;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(6);

    public Thread sendThread;
    public Thread receiveThread;
    public static void main(String[] args){
        System.out.println("Client --> Starting client . . .");
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        try {
            serverSocket = new Socket(host,port);
            System.out.println("Client --> Connected to server");
            outStream = new ObjectOutputStream(serverSocket.getOutputStream());
            inStream = new ObjectInputStream(serverSocket.getInputStream());
            ServerSender serverSenderThread = new ServerSender(outStream,inStream);
            ServerReceiver serverReceiverThread = new ServerReceiver(inStream);


            BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
            String message;
            Message sendMessage;
            Message receiveMessage;
            boolean exit = false;
            boolean isUnique = false;
            while(!isUnique){
                System.out.print("Enter nickname> ");

                try {
                    message = inputReader.readLine();
                    sendMessage = new Message(1,message);
                    outStream.writeObject(sendMessage);

                    receiveMessage = (Message) inStream.readObject();
                    if (receiveMessage.type == 6){
                        System.out.println("Client --> Username taken");
                    }else{
                        System.out.println("User name set as " + message);
                        isUnique = true;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            threadPool.execute(serverSenderThread);
            threadPool.execute(serverReceiverThread);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
