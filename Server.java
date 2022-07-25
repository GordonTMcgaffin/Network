import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 9090;;
    private static ConcurrentHashMap<String,ClientHandler> clientsList = new ConcurrentHashMap<>();
    private static ExecutorService threadPool = Executors.newFixedThreadPool(4);

    public static void main(String[] args) throws Exception{
        //et up main server
        ServerSocket listener = new ServerSocket(PORT);
        System.out.println("[Server] Server stated");
        while(true){
            System.out.println("[Server] Waiting for clients. . . ");
            //Wait for a client to connect 
            Socket client = listener.accept();
            System.out.println("[Server] Client connected");
            //Set up client thread 
            ClientHandler clientThread = new ClientHandler(client,clientsList);
            //execute client thread
            threadPool.execute(clientThread);
            
        }
        //ToDo add in an input for server to command it to kill itself 
       // listener.close();
    }
}
