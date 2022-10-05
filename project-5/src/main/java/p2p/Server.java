package p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class Server {

    private static ConcurrentHashMap<String, ClientHandler> clientList = new ConcurrentHashMap<>();

    public static void main(String[] args){

        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket;
        Socket clientSocket;
        try {
            serverSocket = new ServerSocket(port);
            clientSocket = new Socket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while(true){

            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ClientHandler clientThread = new ClientHandler(clientSocket,clientList);
        }


    }

}
