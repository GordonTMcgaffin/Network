import java.io.*;
import java.net.*;
import java.util.*;

import static java.lang.System.err;

public final class ClientHandler
    implements Runnable {

    private final Server server;
    private final Map<String, ClientHandler> handlers;
    private final Map<InetAddress, Channel> channels;
    private final Socket socket;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private final String id;
    private boolean running;
    private Channel channel;

    public ClientHandler(Server server, Socket socket)
    {
        this.server = server;
        handlers = server.getClientHandlers();
        channels = server.getChannels();
        this.socket = socket;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String id = null;
        try {
            id = receive().getText();
        } catch (SocketException se) {
            se.printStackTrace();
        }
        this.id = id;
        if (id.equals("server")) {
            server.setServerClientHandler(this);
        } else {
            server.broadcast(this + " signed on");
            handlers.put(id, this);
            server.broadcastClientList();
        }
    }

    @Override
    public void run()
    {
        running = true;
        while (running) {
            try {
                Message m = receive();
                if (m == null) {
                    continue;
                }
                switch (m.getReceiver()) {
                    case "list":
                        if (inChannel()) {
                            serverSend("You are already in a channel." +
                                    "Channel creation aborted");
                            continue;
                        }
                        m.setText("channel_invite");
                        channel = new Channel(server);
                        m.setGroup(channel.getGroup());
                        List<String> clientIDs = m.getClientIDs();
                        m.setClientIDs(null);
                        clientIDs.forEach(id -> {
                            ClientHandler handler =
                                handlers.getOrDefault(id, null);
                            if (handler == null) {
                                serverSend(id + " could not be found");
                            } else if (handler.inChannel()) {
                                serverSend(id + " is already in a channel");
                            } else {
                                handler.send(m);
                            }
                        });
                        serverSend("You started a new channel");
                        channels.put(channel.getGroup(), channel);
                        channel.add(this);
                        send(m);
                        log("started " + channel);
                        break;
                    case "server":
                        String text = m.getText();
                        if (text == null) {
                            serverSend("Command not recognized: no text");
                            log("sent null command");
                        }
                        switch (text) {
                            case "decline_channel":
                                channels.get(m.getGroup())
                                    .serverBroadcast(this +
                                            " declined to join the channel");
                                log("declined " + m.getGroup());
                                break;
                            case "join_channel":
                                if (inChannel()) {
                                    serverSend("You are already in a channel");
                                } else {
                                    channel = channels.get(m.getGroup());
                                    channel.add(this);
                                    log("joined " + channel);
                                }
                                break;
                            case "exit_channel":
                                if (inChannel()) {
                                    channel.remove(this);
                                    log("left " + channel);
                                    serverSend("You left the channel");
                                    if (channel.isEmpty()) {
                                        channels.remove(channel);
                                    }
                                    channel = null;
                                } else {
                                    serverSend("You are not in a channel");
                                }
                                break;
                            default:
                                serverSend("Command not recognized: unknown");
                                log("sent unrecognized command");
                                break;
                        }
                        break;
                    case "all":
                        broadcast(m);
                        log("sent message to all");
                        break;
                    default:
                        String receiver = m.getReceiver();
                        ClientHandler handler =
                            handlers.getOrDefault(receiver, null);
                        if (handler == null) {
                            serverSend(receiver + " is not a recognized ID");
                        } else if ((!inChannel() && !handler.inChannel()) ||
                                    (channel != null &&
                                     channel.contains(handler))) {
                            handler.send(m);
                            log("sent message to " + m.getReceiver());
                        } else {
                            serverSend(handler + " could not be reached");
                        }
                        break;
                }
            } catch (SocketException se) {
                close();
            }
        }
    }

    private Message receive()
        throws SocketException
    {
        Message m = null;
        try {
            m = (Message) in.readObject(); 
        } catch (EOFException eofe) {
            // Lost connection to client.
            close();
        } catch (SocketException se) {
            throw se;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }

    public void send(Message m)
    {
        m.setReceiver(id);
        try {
            out.writeObject(m);
        } catch (Exception e) {
            // IGNORE
        }
    }

    public void broadcast(Message m)
    {
        if (inChannel()) {
            channel.broadcast(this, m);
        } else {
            handlers.values().stream()
                .filter(handler -> !handler.equals(this) &&
                        !handler.inChannel())
                .forEach(handler -> handler.send(m));
        }
    }

    private void serverSend(String text)
    {
        server.send(this, text);
    }

    private void log(String text)
    {
        server.log(this + " " + text);
    }

    public boolean inChannel()
    {
        return (channel != null);
    }

    @Override
    public String toString()
    {
        return id;
    }

    public void close()
    {
        if (!id.equals("server")) {
            if (inChannel()) {
                channel.remove(this);
            }
            handlers.remove(this.id);
            server.broadcast(this + " signed off");
            server.broadcastClientList();
        }
        running = false;
        try {
            out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
