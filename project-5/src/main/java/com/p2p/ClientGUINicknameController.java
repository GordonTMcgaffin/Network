package com.p2p;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;

public class ClientGUINicknameController {

    public static Socket serverSocket;
    public static ObjectInputStream inStream;
    public static ObjectOutputStream outStream;
    private static ExecutorService threadPool;
    public String nickname;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    @FXML
    private Button NicknameSubmit;
    @FXML
    private TextField NicknameField;
    @FXML
    private Text Error;

    public void init(Socket serverSocket, ObjectInputStream inStream, ObjectOutputStream outStream, ExecutorService threadPool, PublicKey pubKey, PrivateKey priKey, String nickname) {
        this.serverSocket = serverSocket;
        this.inStream = inStream;
        this.outStream = outStream;
        this.threadPool = threadPool;
        this.privateKey = priKey;
        this.publicKey = pubKey;
        this.nickname = nickname;
    }

    public void SendName(ActionEvent event) {

        String message = NicknameField.getText().strip();
        Message sendMessage;
        Message receiveMessage;

        if (!message.equals("")) {
            sendMessage = new Message(1, message);
            sendMessage.setPublicKey(publicKey);

            try {
                outStream.writeObject(sendMessage);

                receiveMessage = (Message) inStream.readObject();

                if (receiveMessage.type == 6) {
                    Error.setText("Nickname already in use, please enter another");
                } else {

                    ClientGUI m = new ClientGUI();
                    m.nickname = sendMessage.message;
                    m.stage.hide();
                    m.changeView();
                }

            } catch (IOException | ClassNotFoundException e) {
                exit();
            }
        } else {
            Error.setText("A nickname must be entered");
        }
    }

    public void exit() {
        System.exit(1);
    }
}
