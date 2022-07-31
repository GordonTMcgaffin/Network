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
        String message = "";
        String username = "";
        Message recvMessage;
        Message sendMessage;

        try{
            while(!message.equals("exit")) {
                //This is what is received 
                recvMessage = (Message)inStream.readObject();
                message = recvMessage.content;
                
                //If the message sent to the server is "Username set" then the server 
                //will check the username given by recvMessage.user and set the thread's
                //username variable if it is unique
                if(recvMessage.content.equals("Username set")){
                    if(clientList.containsKey(recvMessage.sender)){
                        message = "duplicate";
                        sendMessage = new Message("Server",username,message);
                        outStream.writeObject(sendMessage);
                    }else{
                        message = "unique";
                        clientList.put(recvMessage.sender,this);
                        username = recvMessage.sender;

                        //Server sends a message back if the username is Unique or not
                        System.out.println("[Server] User " + username + " has connected");
                        System.out.println(printClients());
                        sendMessage = new Message("Server",username,message);
                        outStream.writeObject(sendMessage);
                        Shout("Server", "User " + username + " has connected.");
                        showClients();
                    }
                }else if(recvMessage.content.equals("show_all_clients")){
                    sendMessage = new Message("Server",username, printClients());
                    outStream.writeObject(sendMessage);
                }else if(!recvMessage.receiver.equals("all")){

                    if (!clientList.containsKey(recvMessage.receiver)) {
                        clientList.get(recvMessage.sender).outStream.writeObject(new Message("Server", recvMessage.sender, "User " + recvMessage.receiver + " is not available."));
                    } else {
                        clientList.get(recvMessage.sender).outStream.writeObject(recvMessage);
                        clientList.get(recvMessage.receiver).outStream.writeObject(recvMessage);
                        System.out.println("[Server] Whispering message " + recvMessage.content + " from " + recvMessage.sender + " to " + recvMessage.receiver);

                    }
                }else if(!recvMessage.content.equals("exit")){
                    System.out.println("[Server] User " + username + " sent " + recvMessage.content + " to all users");
                    Shout(recvMessage.sender, recvMessage.content);
                }
            }
            endThread(username);
        } catch (IOException e){
            endThread(username);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void endThread(String username){
        clientList.remove(username);
        Shout("Server", "User " + username + " has left.");
        showClients();
        try {
            System.out.println("[Server] " + username + " has disconnected");
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
//                System.out.println(message + " sent to " + aClient.getKey());
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
