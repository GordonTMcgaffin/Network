package p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public class Server {

    private static ConcurrentHashMap<String, ClientHandler> clientList = new ConcurrentHashMap<String, ClientHandler>();
    private static LinkedBlockingQueue<String> clients = new LinkedBlockingQueue<>();
    private static ConcurrentHashMap<String, ClientHandler> fileList = new ConcurrentHashMap<>();
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    public static void main(String[] args){
        System.out.println("Server --> Starting server . . .");
        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket;
        Socket clientSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Server --> Waiting for clients");

        while(true){

            try {
                clientSocket = serverSocket.accept();
                System.out.println("Server --> Client connected");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ClientHandler clientThread = new ClientHandler(clientSocket,clientList, fileList,clients);
            threadPool.execute(clientThread);
        }


    }

}
