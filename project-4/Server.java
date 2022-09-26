import java.io.*;
import java.net.*;
import java.util.*;

import static java.lang.System.err;

public final class Server {

    public static final int DEFAULT_PORT = 9090;

    private final Map<String, ClientHandler> handlers = new Hashtable<>();
    private final Map<InetAddress, Channel> channels = new Hashtable<>();
    private boolean running;

    public Server(int port)
    {
        Server server = this;
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                running = true;
                while (running) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler =
                        new ClientHandler(server, socket);
                    new Thread(handler).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void broadcast(String text)
    {
        broadcast(new Message().setText(text));
    }

    public void broadcastClientList()
    {
        broadcast(new Message().setText("client_list")
                .setClientIDs(handlers.keySet().stream().sorted().toList()));
    }

    public void broadcast(Message m)
    {
        handlers.values().stream().forEach(handler -> send(handler, m));
    }

    public void send(Channel channel, String text)
    {
        channel.getClientHandlers().stream()
            .forEach(handler -> send(handler, text));
    }

    public void send(ClientHandler handler, String text)
    {
        send(handler, new Message().setText(text));
    }

    public void send(ClientHandler handler, Message m)
    {
        handler.send(m.setSender("server"));
    }

    public Map<String, ClientHandler> getClientHandlers()
    {
        return handlers;
    }

    public Map<InetAddress, Channel> getChannels()
    {
        return channels;
    }

    public void close()
    {
        running = false;
        handlers.values().stream().forEach(ClientHandler::close);
    }

    public static void main(String[] args)
    {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Server(port);
    }
}
