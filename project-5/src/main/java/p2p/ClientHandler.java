package p2p;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ContentHandler;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable{
    public Socket clientSocket;
    public ObjectInputStream inStream;
    public ObjectOutputStream outStream;
    public ConcurrentHashMap<String,ClientHandler> clientList;
    public String clientNickname;

    public ClientHandler(Socket cSocket, ConcurrentHashMap<String,ClientHandler> clientList){
        this.clientSocket = cSocket;
        this.clientList = clientList;
        try {
            this.inStream = new ObjectInputStream(clientSocket.getInputStream());
            this.outStream = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void run(){
        String message = "";
        Message receiveMessage;
        Message sendMessage;
        Boolean exit = false;

        while(!exit){
            try {
                receiveMessage = (Message)inStream.readObject();
                switch(receiveMessage.type){
                    case(1):{
                        System.out.println("Received message of type 1");
                        if(clientList.contains(receiveMessage.message)){
                            System.out.println("Name already in use");
                            //send packet back asking for new name
                        }else{
                            System.out.println("Client "+ receiveMessage.message+" added to list");
                            clientList.put(receiveMessage.message,this);
                        }
                        break;
                    }
                    case(2):{
                        //Chat message
                        System.out.println("Received message of type 2");
                        break;
                    }
                    case(3):{
                        //File request
                        System.out.println("Received message of type 3");
                        break;
                    }
                    case(4):{

                        System.out.println("Received message of type 4");
                        exit = true;
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
