import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private Socket client;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private ConcurrentHashMap<String,ClientHandler> clientList;

    public ClientHandler(Socket clientSocket, ConcurrentHashMap<String,ClientHandler> clients) throws Exception{
        this.client = clientSocket;
        this.clientList = clients;
        inStream = new ObjectInputStream(client.getInputStream());
        outStream = new ObjectOutputStream(client.getOutputStream());
        
    }

    @Override
    public void run() {
        String recvMessage = "";
        String message = "";
        String username = "";

        try{
            while(!recvMessage.equals("exit") && client.isConnected()){
                //This is what is recieved 
                Packet recvPacket = (Packet)inStream.readObject();
                System.out.println("["+recvPacket.user+"]> "+ recvPacket.message);
                recvMessage = recvPacket.message;
                
                //If the message sent to the server is "Username set" then the server 
                //will check the username given by recvPacket.user and set the thread's
                //username variable if it is unique
                if(recvMessage.equals("Username set")){
                    if(clientList.containsKey(recvPacket.user)){
                        message = "duplicate";
                    }else{
                        message = "unique";
                        clientList.put(recvPacket.user,this);
                        username = recvPacket.user;
                        System.out.println("HashMap > "+ clientList);
                        
                    }

                    //Server sends a message back if the username is Unique or not
                    System.out.println("[Server] Username set as " + username);
                    Packet packet = new Packet("Server",message);
                    outStream.writeObject(packet);
                    try {
                        Shout("Server",printClients());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }else{
                    System.out.println("[Server] Sending message to all users...");
                    Shout(recvPacket.user,recvMessage);
                    System.out.println("[Server] Message sent");
                }
            }
            clientList.remove(username);
            try {
                Shout("Server",printClients());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            client.close();
        } catch (IOException e){
            clientList.remove(username);
            try {
                Shout("Server",printClients());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.out.println("[Server] --- Client disconnected unexpectedly ---");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private String printClients(){
        String onlineUsers = "";
        onlineUsers = "Current online users:";
        for(Entry<String,ClientHandler> aClient : clientList.entrySet()){
            onlineUsers = onlineUsers + "\n"+ aClient.getKey();
        }
        return onlineUsers;
    }

    private void Shout(String username,String message) throws IOException{
        for(Entry<String,ClientHandler> aClient : clientList.entrySet()){
            Packet packet = new Packet(username,message);
            System.out.println(message + " sent to " + aClient.getKey());
            aClient.getValue().outStream.writeObject(packet);
        }
    }

    // private void hear(Packet packet) throws IOException{
    //     outStream.writeObject(packet);
    // }
    
}
