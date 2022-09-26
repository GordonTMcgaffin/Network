import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public final class Message
    implements Serializable {

    private String sender;
    private String receiver;
    private String text;
    private List<byte[]> audio;
    private List<String> clientIDs;
    private InetAddress group;

    public Message setSender(String sender)
    {
        this.sender = sender;
        return this;
    }

    public Message setReceiver(String receiver)
    {
        this.receiver = receiver;
        return this;
    }

    public Message setText(String text)
    {
        this.text = text;
        return this;
    }

    public Message setAudio(List<byte[]> audio)
    {
        this.audio = audio;
        return this;
    }

    public Message setClientIDs(List<String> clientIDs)
    {
        this.clientIDs = clientIDs;
        return this;
    }

    public Message setGroup(InetAddress group)
    {
        this.group = group;
        return this;
    }

    public String getSender()
    {
        return sender;
    }

    public String getReceiver()
    {
        return receiver;
    }

    public String getText()
    {
        return text;
    }

    public List<byte[]> getAudio()
    {
        return audio;
    }

    public InetAddress getGroup()
    {
        return group;
    }

    public List<String> getClientIDs()
    {
        return clientIDs;
    }
}
