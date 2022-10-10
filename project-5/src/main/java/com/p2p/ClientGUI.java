package com.p2p;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientGUI extends Application {

    public static Socket serverSocket;
    public static ObjectInputStream inStream;
    public static ObjectOutputStream outStream;
    public static Stage stage;
    public static String serverHost;
    public static String myHost;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(6);
    public String nickname;

    public static void main(String[] args) throws SocketException {
        serverHost = "127.0.1.1";
        myHost = "127.0.1.1";

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();

            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        int port = 9090;
        try {
            serverSocket = new Socket(serverHost, port);

            outStream = new ObjectOutputStream(serverSocket.getOutputStream());
            inStream = new ObjectInputStream(serverSocket.getInputStream());
        } catch (IOException e) {

            System.exit(1);
        }


        stage = primaryStage;
        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("nickname-view.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Gordon's bay");
        primaryStage.setScene(new Scene(root, 600, 400));

        ClientGUINicknameController nicknameController = (ClientGUINicknameController) loader.getController();
        nicknameController.init(serverSocket, inStream, outStream, threadPool, publicKey, privateKey, nickname);
        primaryStage.show();
    }

    public void changeView() throws IOException {
        FXMLLoader mainLoader = new FXMLLoader((getClass().getResource(("client-view.fxml"))));
        Parent mainRoot = mainLoader.load();
        ClientGUIController clientController = (ClientGUIController) mainLoader.getController();
        clientController.init(serverSocket, inStream, outStream, stage, privateKey, publicKey, nickname, myHost);
        stage.setOnCloseRequest(e -> {
            clientController.exit();
        });
        stage.setHeight(800);
        stage.setWidth(1400);
        stage.show();
        stage.getScene().setRoot(mainRoot);

    }
}