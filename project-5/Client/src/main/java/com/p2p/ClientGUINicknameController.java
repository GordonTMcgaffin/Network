package com.p2p;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;

public class ClientGUINicknameController {

    public static ObjectInputStream inStream;
    public static ObjectOutputStream outStream;
    private static ExecutorService threadPool;
    public Socket serverSocket;
    public String nickname;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String clientHost;
    @FXML
    private Button NicknameSubmit;
    @FXML
    private Label DialogueLabel;
    @FXML
    private TextField NicknameField;
    @FXML
    private Text Error;

    private boolean serverSet = false;
    private boolean hostSet = false;

    public void init(ObjectInputStream inStream, ObjectOutputStream outStream, ExecutorService threadPool, PublicKey pubKey, PrivateKey priKey, String nickname) {

        this.inStream = inStream;
        this.outStream = outStream;
        this.threadPool = threadPool;
        this.privateKey = priKey;
        this.publicKey = pubKey;
        this.nickname = nickname;

        InetAddress ip;
        try {
            ip = InetAddress.getLocalHost();
            NicknameField.setText(ip.getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void SendName(ActionEvent event) throws InterruptedException {
        if (!hostSet) {
            clientHost = NicknameField.getText().strip();
            if (!clientHost.equals("")) {
                DialogueLabel.setText("Enter Server IP");
                NicknameField.setText("");
                NicknameField.setPromptText("Enter IP");
                hostSet = true;
            } else {
                Error.setText("Please enter valid address");
            }
        } else if (!serverSet) {

            int port = 9090;
            String serverHost = NicknameField.getText().strip();
            int attempts = 0;

            while (attempts < 5) {
                try {

                    this.serverSocket = new Socket(serverHost, port);
                    outStream = new ObjectOutputStream(serverSocket.getOutputStream());
                    inStream = new ObjectInputStream(serverSocket.getInputStream());
                    serverSet = true;

                    Error.setText("");
                    DialogueLabel.setText("Enter Nickname");
                    NicknameField.setText("");
                    NicknameField.setPromptText("Enter Nickname");
                    break;
                } catch (IOException e) {
                    attempts++;
                    Thread.sleep(500);

                }
            }
            if (!serverSet) {
                Error.setText("Could not connect to address: " + serverHost);
            }

        } else {

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
                        m.setSocket(serverSocket, outStream, inStream);
                        m.myHost = clientHost;
                        m.mainView();
                    }

                } catch (IOException | ClassNotFoundException e) {
                    exit();
                }
            } else {
                Error.setText("A nickname must be entered");
            }
        }
    }

    public void exit() {
        System.exit(0);
    }
}
