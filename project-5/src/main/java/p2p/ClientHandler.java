package p2p;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    public ConcurrentHashMap<String, ClientHandler> fileList;
    public Socket clientSocket;
    public ObjectInputStream inStream;
    public ObjectOutputStream outStream;
    public String clientNickname;
    private ConcurrentHashMap<String, ClientHandler> clientList;
    private ConcurrentHashMap<String, PublicKey> clientQueue;

    public ClientHandler(Socket cSocket, ConcurrentHashMap<String, ClientHandler> clientList, ConcurrentHashMap<String, ClientHandler> files, ConcurrentHashMap<String, PublicKey> clientKey) {
        this.clientSocket = cSocket;
        this.clientList = clientList;
        this.fileList = files;
        this.clientQueue = clientKey;
        try {
            this.outStream = new ObjectOutputStream(clientSocket.getOutputStream());
            this.inStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        System.out.println("Server --> ClientHandler started");
        String message = "";
        Message receiveMessage;
        Message sendMessage;
        Boolean exit = false;

        while (!exit) {
            try {
                receiveMessage = (Message) inStream.readObject();
                switch (receiveMessage.type) {
                    case (1): {
                        System.out.println("Received message of type 1");
                        if (clientList.containsKey(receiveMessage.message)) {
                            System.out.println("Name already in use");
                            sendMessage = new Message(6, "");
                            outStream.writeObject(sendMessage);
                            //send packet back asking for new name
                        } else {
                            System.out.println("Client " + receiveMessage.message + " added to list");
                            clientList.put(receiveMessage.message, this);
                            clientQueue.put(receiveMessage.message, receiveMessage.publicKey);
                            sendMessage = new Message(5, "");
                            outStream.writeObject(sendMessage);
                            outStream.writeObject(clientQueue);
                            clientNickname = receiveMessage.message;
                            for (Map.Entry<String, ClientHandler> aClient : clientList.entrySet()) {
                                if (aClient.getKey() != clientNickname) {
                                    sendMessage = new Message(8, receiveMessage.message);
                                    sendMessage.setPublicKey(receiveMessage.publicKey);
                                    aClient.getValue().outStream.writeObject(sendMessage);
                                }
                            }
                        }
                        break;
                    }
                    case (2): {
                        //Chat message
                        System.out.println("Received message of type 2");
                        break;
                    }
                    case (3): {
                        //File request
                        System.out.println("[Server]--> Client is requesting file " + receiveMessage.message);
                        for (Map.Entry<String, ClientHandler> aClient : clientList.entrySet()) {
                            if (aClient.getKey() != clientNickname) {
                                aClient.getValue().outStream.writeObject(receiveMessage);
                            }
                        }
                        break;
                    }
                    case (4): {
                        System.out.println("[Server]--> Client has accepted request for " + receiveMessage.message);
                        for (Map.Entry<String, ClientHandler> aClient : clientList.entrySet()) {
                            if (aClient.getKey() != clientNickname) {
                                aClient.getValue().outStream.writeObject(receiveMessage);
                            }
                        }
                        break;
                    }
                    case (7): {
                        System.out.println("[Server]--> Client has submitted " + receiveMessage.message + " for download");
                        for (Map.Entry<String, ClientHandler> aClient : clientList.entrySet()) {
                            if (aClient.getKey() != clientNickname) {
                                aClient.getValue().outStream.writeObject(receiveMessage);
                            }
                        }

                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {

                exit = closeClient();
            }
        }

    }

    public boolean closeClient() {
        if (clientNickname == null) {
            System.out.println("[Server]--> Client has disconnected");

        } else {
            System.out.println("[Server]--> Client " + clientNickname + " has disconnected");
            clientList.remove(clientNickname);
            clientQueue.remove(clientNickname);
        }
        try {
            inStream.close();
            outStream.close();
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

}
