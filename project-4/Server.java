import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.System.err;

public final class Server {

    public static final int DEFAULT_PORT = 9090;

    private ClientHandler serverClientHandler;
    private final Map<String, ClientHandler> handlers =
        new ConcurrentHashMap<>();
    private final Map<InetAddress, Channel> channels =
        new ConcurrentHashMap<>();
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

    public void setServerClientHandler(ClientHandler handler)
    {
        serverClientHandler = handler;
    }

    public void broadcast(String text)
    {
        broadcast(new Message().setText(text));
    }

    public void broadcastClientList()
    {
        Message m = new Message().setText("client_list")
            .setClientIDs(handlers.keySet().stream().sorted().toList());
        broadcast(m);
    }

    public void broadcast(Message m)
    {
        handlers.values().stream().forEach(handler -> send(handler, m));
        if (serverClientHandler != null) {
            serverClientHandler.send(m);
        }
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

    public void log(String text)
    {
        log(new Message().setText(text));
    }

    public void log(Message m)
    {
        if (serverClientHandler != null) {
            serverClientHandler.send(m);
        }
    }

    public void send(ClientHandler handler, Message m)
    {
        handler.send(m.setSender("server"));
    }

    public ClientHandler getServerClientHandler()
    {
        return serverClientHandler;
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
        if (serverClientHandler != null) {
            serverClientHandler.close();
        }
        handlers.values().stream().forEach(ClientHandler::close);
    }
}
