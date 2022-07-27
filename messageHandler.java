import java.io.IOException;
import java.io.ObjectInputStream;

public class messageHandler implements Runnable {

    private ObjectInputStream inStream;
    public messageHandler(ObjectInputStream inStream) throws IOException, ClassNotFoundException{
        this.inStream = inStream;
    }

    @Override
    public void run() {
        String message = "";
        while(!message.equals("exit")){
            Packet recvPacket;
            try {
                recvPacket = (Packet)inStream.readObject();
                
            } catch (ClassNotFoundException | IOException e) {
                message = "exit";
                // e.printStackTrace();
                System.out.println("[Server] Lost connection to server");
                System.exit(0);
                break;
            }

            if(recvPacket.sender.equals("Disconected")){
                System.out.println("[Server] " + recvPacket.message );
                
            }else{
                System.out.println("["+recvPacket.sender+"] " + recvPacket.message );
                System.out.println("Type a message>");
            }
            
            
        }
        try {
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
