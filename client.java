import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client
{
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9090;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws Exception
    {
        try {
            String message = "";
            String username = "";
        
            System.out.println("[Client] Connecting to server...");
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            
            ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
            boolean unique = false;

            while (!unique) {
                System.out.println("Please enter a username>");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                username = reader.readLine().trim();
                Packet packet = new Packet(username,"Server", "Username set");
                outStream.writeObject(packet);

                Packet recvPacket = (Packet)inStream.readObject();
                if (!recvPacket.message.equals("unique")) {
                    System.out.println("[Server] That username is already taken, please try another");
                } else {
                    unique = true;
                }
            }

            //Thread to recieve messages 
            try{
                messageHandler hearingAid = new messageHandler(inStream);
                threadPool.execute(hearingAid);
            } catch (IOException e){
                outStream.close();
                socket.close();
                System.exit(0);
            }

            while (!message.equals("exit")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                message = reader.readLine().trim();

                Packet packet;
                if(message.equals("Whisper")){
                    System.out.println("[Server] Type the name of the client you want to whisper to");
                    String reciever = reader.readLine().trim();
                    System.out.println("[Server] Please type your message");
                    message = reader.readLine().trim();
                    packet = new Packet(username,reciever,message);
                }else{
                    packet = new Packet(username,"all", message);
                }
                outStream.writeObject(packet);
            }   
            outStream.close();
            //inStream.close();
            socket.close();
            System.exit(0);
        } catch(IOException e) {
            System.out.println("[Client] " + e.getMessage());
        }
    }
}
