package Client;

import p2p.Message;

import java.io.IOException;
import java.io.ObjectInputStream;

public class ServerReceiver implements Runnable{

    public ObjectInputStream inStream;
    public ServerReceiver(ObjectInputStream InputStream){
        this.inStream = InputStream;
    }

    @Override
    public void run(){
       // System.out.println("Client --> Ready to receive packet");
        Message receiveMessage;
        boolean exit = false;

        while(!exit){
            try {
                receiveMessage = (Message) inStream.readObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
