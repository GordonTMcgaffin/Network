import java.io.Serializable;

public class RecieverInfo implements Serializable {
    int packetSize;


    public RecieverInfo(int packetSize) {
        this.packetSize = packetSize;

    }
}
