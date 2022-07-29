import java.io.Serializable;

public class Message implements Serializable
{
    String sender, receiver, content;

    public Message(String sender, String receiver, String content)
    {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }
}
