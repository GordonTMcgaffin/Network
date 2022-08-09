/**
 * @file    Message.java
 * @brief   message for passing between clients and server
 * @author  G. Mcgaffin (23565608@sun.ac.za)
 * @date    2022-08-02
 */

import java.io.Serializable;

/**
 * The {@code Message} class represents a message that can be sent between a
 * client and a server.
 */
public class Message
    implements Serializable {

    /* --- Instance Variables ----------------------------------------------- */

    /** message sender */
    String sender;

    /** message receiver */
    String receiver;

    /** the message */
    String content;

    /* --- Constructors ----------------------------------------------------- */

    /**
     * Creates and returns a new {@code Message}.
     *
     * @param sender    message sender
     * @param receiver  message receiver
     * @param content   the message
     */
    public Message(String sender, String receiver, String content)
    {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }
}
