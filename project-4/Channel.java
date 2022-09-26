import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class Channel
{
    private final Server server;
    private final InetAddress group;
    private final Set<ClientHandler> handlers = new HashSet<>();

    public Channel(Server server)
    {
        this.server = server;
        group = generateGroup(server.getChannels().keySet());
    }

    public static InetAddress generateGroup(Set<InetAddress> groups)
    {
        InetAddress group = null;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (group == null || groups.contains(group)) {
            StringBuilder builder = new StringBuilder(15);
            builder.append("" + random.nextInt(224, 240));
            for (int i = 0; i < 3; i++) {
                builder.append(".");
                builder.append(random.nextInt(0, 256));
            }
            try {
                group = InetAddress.getByName(builder.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return group;
    }

    public void add(ClientHandler handler)
    {
        handlers.add(handler);
        serverBroadcast(handler + " joined the channel.");
    }

    public void remove(ClientHandler handler)
    {
        handlers.remove(handler);
        serverBroadcast(handler + " left the channel.");
    }

    public void broadcast(ClientHandler handler, Message m)
    {
        handlers.stream().filter(h -> !h.equals(handler))
            .forEach(h -> h.send(m));
    }

    public void serverBroadcast(String text) {
        server.send(this, text);
    }

    public InetAddress getGroup()
    {
        return group;
    }

    public Set<ClientHandler> getClientHandlers()
    {
        return handlers;
    }

    public boolean contains(ClientHandler handler)
    {
        return handlers.contains(handler);
    }

    public boolean isEmpty()
    {
        return handlers.isEmpty();
    }
}
