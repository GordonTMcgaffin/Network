package p2p;

public class Message {
    public int type;
    /**
     * Types:
     * 1 - Nickname check
     * 2 - chat message
     * 3 - file request
     * 4 - exit
     */
    public String message;

    public Message(int type, String message){
        this.message = message;
        this.type = type;
    }


}
