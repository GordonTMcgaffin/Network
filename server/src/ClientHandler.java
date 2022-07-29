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
        outStream = new ObjectOutputStream(client.getOutputStream());
        inStream = new ObjectInputStream(client.getInputStream());
    }

    @Override
    public void run() {
        String recvMessage = "";
        String message = "";
        String username = "";

        try{
            while(!recvMessage.equals("exit")) {
                //This is what is received 
                Message recvPacket = (Message)inStream.readObject();
                System.out.println("["+recvPacket.sender+"]> "+ recvPacket.content);
                recvMessage = recvPacket.content;
                
                //If the message sent to the server is "Username set" then the server 
                //will check the username given by recvPacket.user and set the thread's
                //username variable if it is unique
                if(recvMessage.equals("Username set")){
                    if(clientList.containsKey(recvPacket.sender)){
                        message = "duplicate";
                        Message packet = new Message("Server",username,message);
                        outStream.writeObject(packet);
                    }else{
                        message = "unique";
                        clientList.put(recvPacket.sender,this);
                        username = recvPacket.sender;
                        System.out.println(printClients());
                        //Server sends a message back if the username is Unique or not
                        System.out.println("[Server] Username set as " + username);
                        Message packet = new Message("Server",username,message);
                        outStream.writeObject(packet);
                        Shout("Server", "User " + username + " has joined.");
                        showClients();
                    }
                }else if(recvMessage.equals("show_all_clients")){
                    Message packet = new Message("Server",username, printClients());
                    outStream.writeObject(packet);
                }else if(!recvPacket.receiver.equals("all")){
                    System.out.println("[Server] Whispering message " + recvPacket.content + " from " + recvPacket.sender + " to " + recvPacket.receiver);
                    if (!clientList.containsKey(recvPacket.receiver)) {
                        clientList.get(recvPacket.sender).outStream.writeObject(new Message("Server", recvPacket.sender, "User " + recvPacket.receiver + " is not available."));
                    } else {
                        clientList.get(recvPacket.sender).outStream.writeObject(recvPacket);
                        clientList.get(recvPacket.receiver).outStream.writeObject(recvPacket);
                    }
                }else if(!recvMessage.equals("exit")){
                    System.out.println("[Server] Sending message to all users...");
                    Shout(recvPacket.sender, recvMessage);
                    System.out.println("[Server] Message sent");
                }
            }
            clientList.remove(username);
            Shout("Server", "User " + username + " has left.");
            showClients();
            client.close();
        } catch (IOException e){
            clientList.remove(username);
            Shout("Server", "User " + username + " has left.");
            showClients();
            System.out.println("[Server] --- Client disconnected unexpectedly ---");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String printClients(){
        String onlineUsers = "online_users:";
        for(Entry<String,ClientHandler> aClient : clientList.entrySet()){
            onlineUsers = onlineUsers + "\n" + aClient.getKey();
        }
        return onlineUsers;
    }

    private void Shout(String username,String message) {
        try {
            for(Entry<String,ClientHandler> aClient : clientList.entrySet()){
                Message packet = new Message(username,"all",message);
                System.out.println(message + " sent to " + aClient.getKey());
                aClient.getValue().outStream.writeObject(packet);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void showClients(){
        Shout("Server",printClients());
    }

    
}
