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
     * 9 - update new client
     * 10 - exit
     */
    public String message;
    public long fileSize;
    public PublicKey publicKey;
    private String destination = "";
    private String source = "";
    private String key = "";
    private String[] items = null;
    private byte[] encryptedMessage = null;

    public Message(int type, String message) {
        this.message = message;
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String dest) {
        if (this.destination.equals(""))
            this.destination = dest;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        if (this.key.equals(""))
            this.key = key;
    }

    public void setFileSize(long sz) {
        this.fileSize = sz;
    }

    public void setPublicKey(PublicKey key) {
        this.publicKey = key;
    }

    public String[] getItems() {
        return items;
    }

    public void setItems(String[] items) {
        if (this.items == null)
            this.items = items;
    }

    public byte[] getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(byte[] bytes) {
        if (this.encryptedMessage == null)
            this.encryptedMessage = bytes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String src) {
        if (this.source.equals("")) this.source = src;
    }


}
