import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class ServerCommand implements Runnable{
    private String command;
    private final ServerSocket listener;
    public ServerCommand(String command, ServerSocket listener){
        this.command = command;
        this.listener = listener;
    }
    @Override
    public void run(){
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        while(!command.equals("exit")){
            System.out.println("[Server] Enter command >");
            try {
                command = input.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            System.out.println("[Server] Shutting down...");
            listener.close();
            System.out.println("[Server] Goodbye");
            //System.exit(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
