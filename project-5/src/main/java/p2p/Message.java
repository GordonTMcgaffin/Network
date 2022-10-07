package p2p;

import java.io.Serializable;
import java.security.PublicKey;

public class Message implements Serializable {
    public int type;
    /**
     * Types:
     * 1 - Nickname check
     * 2 - chat message
     * 3 - file request
     * 4 - file confirm
     * <p>
     * 5 - valid name
     * 6 - invalid name
     * 7 - new file
     * 8 - new client
     */
    public String message;
    public String destination;
    public long fileSize;
    public PublicKey publicKey;
    private String key = "";

    public Message(int type, String message) {
        this.message = message;
        this.type = type;
    }

    public void setDest(String dest) {
        this.destination = dest;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        //if(key == "")
        this.key = key;
    }

    public void setFileSize(long sz) {
        this.fileSize = sz;
    }

    public void setPublicKey(PublicKey key) {
        this.publicKey = key;
    }


}
