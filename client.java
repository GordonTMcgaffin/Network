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
                Packet packet = new Packet(username, "Username set");
                outStream.writeObject(packet);

                Packet recvPacket = (Packet)inStream.readObject();
                if (!recvPacket.message.equals("unique")) {
                    System.out.println("[Server] That username is already taken, please try another");
                } else {
                    unique = true;
                }
            }

            // TODO: Add in a thread to recieve messages.
            messageHandler hearingAid = new messageHandler(inStream);
            threadPool.execute(hearingAid);

            while (!message.equals("exit")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                message = reader.readLine().trim();
                Packet packet = new Packet(username,message);
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
