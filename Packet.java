import java.io.Serializable;

public class Packet implements Serializable {
    String message;
    String user;

    //create user to 
    

    public Packet(String user, String message){
        this.user = user;
        this.message = message;
    }
}
