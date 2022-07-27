import java.io.Serializable;

public class Packet implements Serializable {
    String message;
    String sender;
    String reciever;

    //create user to 
    

    public Packet(String sendingUser,String recievingUser, String message){
        this.sender = sendingUser;
        this.reciever = recievingUser;
        this.message = message;
    }
}
