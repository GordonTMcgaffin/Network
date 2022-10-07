package Client;

import p2p.Message;

import java.io.*;
import java.util.HashMap;

public class ServerSender implements Runnable{

    public ObjectOutputStream outStream;
    public ObjectInputStream inStream;
    public BufferedReader inputReader;
    public HashMap<String, File> fileList;

    public ServerSender(ObjectOutputStream outputStream, ObjectInputStream inputStream ){
        this.outStream = outputStream;
        this.inStream = inputStream;

    }

    @Override
    public void run(){
        inputReader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        Message sendMessage;
        boolean exit = false;

        while(!exit){
            System.out.print("Enter command> ");

            try {
                input = inputReader.readLine();
                switch(input){
                    case("2"): {
                        System.out.print("Enter message> ");
                        input = inputReader.readLine();
                        sendMessage = new Message(2, input);
                        System.out.print("Who would you like to message> ");
                        input = inputReader.readLine();
                        sendMessage.setDest(input);

                        outStream.writeObject(sendMessage);

                        break;
                    }
                    case("3"):{
                        System.out.println("File will be selected here");
                        sendMessage = new Message(3,"File name");
                        break;
                    }
                    case("7"):{
                        System.out.print("Enter file location> ");
                        input = inputReader.readLine();
                        File newFile = new File(input);
                        fileList.put(newFile.getName(), newFile);
                        sendMessage = new Message(7,newFile.getName());
                        outStream.writeObject(sendMessage);
                        break;
                    }

                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
