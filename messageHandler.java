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
        while(!message.equals("close")){
            Packet recvPacket;
            try {
                recvPacket = (Packet)inStream.readObject();
                System.out.println("["+recvPacket.user+"] " + recvPacket.message );
                System.out.println("Type a message>");
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
            
        }
    }
    
}
