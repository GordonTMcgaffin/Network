package com.p2p;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ClientGUIController {

    public static Socket serverSocket;
    public static ObjectInputStream inStream;
    public static ObjectOutputStream outStream;
    private static Stage primaryStage;
    public int packetSize = 800;
    private ConcurrentHashMap<String, File> fileList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, PublicKey> clients;
    private String key;
    private int downloadPort = 9091;
    private String downloadHost = "127.0.0.1";
    private String downloadDest = "";
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private boolean pauseUpload = false;
    private boolean pauseDownload = false;
    //private SecretKey fileKey;
    @FXML
    private Button PauseUpload;
    @FXML
    private Button PauseDownload;
    @FXML
    private ListView<String> OnlineClients;
    @FXML
    private TextField ChatInput;
    @FXML
    private ListView<String> UploadFiles;
    @FXML
    private ListView<String> DownloadFilesList;
    @FXML
    private ProgressBar UploadFileProgress;
    @FXML
    private ProgressBar DownloadFileProgress;
    @FXML
    private ListView<String> ChatLog;
    @FXML
    private ListView<String> FileSearchResults;
    @FXML
    private TextField FileSearchInput;
    @FXML
    private Label Nickname;
    @FXML
    private Label UploadStatusMessage;
    @FXML
    private Label UploadErrorMessage;
    @FXML
    private Label DownloadErrorMessage;
    @FXML
    private Label DownloadStatusMessage;
    @FXML
    private Label ChatErrorMessage;

    public void init(Socket serverSocket, ObjectInputStream inStream, ObjectOutputStream outStream, Stage stage, PrivateKey priKey, PublicKey pubKey, String nickname) {
        this.serverSocket = serverSocket;
        this.inStream = inStream;
        this.outStream = outStream;
        this.primaryStage = stage;
        this.privateKey = priKey;
        this.publicKey = pubKey;

        InetAddress ip;
        try {
            ip = InetAddress.getLocalHost();
            downloadHost = ip.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Nickname.setText(nickname);
        ChatLog.getItems().add("Please select a clint from the clients list with ");
        ChatLog.getItems().add("whom you would like to send a message to ");

//        try {
//            KeyGenerator fileKeyGenerator = KeyGenerator.getInstance("AES");
//            fileKeyGenerator.init(128);
//            fileKey = fileKeyGenerator.generateKey();
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
//        }

        try {
            clients = (ConcurrentHashMap<String, PublicKey>) inStream.readObject();
            for (Map.Entry<String, PublicKey> aClient : clients.entrySet()) {
                OnlineClients.getItems().add(aClient.getKey());
            }
        } catch (IOException | ClassNotFoundException e) {
            exit();
        }


        new Thread(() -> {
            boolean exit = false;
            while (!exit) {
                try {
                    System.out.println("Receiving message . . .");
                    Message receiveMessage = (Message) inStream.readObject();
                    System.out.println("Message received");
                    switch (receiveMessage.type) {
                        case (2): {
                            //Chat message

                            Cipher decryptCipher = Cipher.getInstance("RSA");
                            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);


                            byte[] encryptedMessageBytes = receiveMessage.getEncryptedMessage();
                            byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);

                            String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);
                            Platform.runLater(() -> {
                                ChatLog.getItems().add(receiveMessage.getSource() + ": " + decryptedMessage);
                            });

                            break;
                        }
                        case (3): {
                            //Uploader

                            if (fileList.containsKey(receiveMessage.message)) {
                                //PublicKey downloaderPublicKey = receiveMessage.getPublicKey();

//================================================================================================================================
                                KeyGenerator fileKeyGenerator = KeyGenerator.getInstance("AES");
                                fileKeyGenerator.init(128);

                                byte[] iv = new byte[16];

                                for (byte b : iv) {
                                    System.out.println(b);
                                }

                                new SecureRandom().nextBytes(iv);


                                IvParameterSpec ivP = new IvParameterSpec(iv);
                                SecretKey fileKey = fileKeyGenerator.generateKey();
                                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                cipher.init(Cipher.ENCRYPT_MODE, fileKey, ivP);
                                //================================================================================================================================


                                Platform.runLater(() -> {
                                    UploadErrorMessage.setText("");
                                    UploadStatusMessage.setText("");
                                });
                                Message sendMessage = new Message(4, downloadHost + ":" + downloadPort + ":" + receiveMessage.message);
                                File file = fileList.get(receiveMessage.message);
                                long fileSize = Files.size(Path.of(file.getPath()));

                                sendMessage.setFileSize(fileSize);
                                sendMessage.setKey(receiveMessage.getKey());
                                //sendMessage.setFileKey(fileKey, ivP);
                                new Thread(() -> {
                                    long bytesSent = 0L;
                                    double progress;
                                    try {
                                        ServerSocket uploadSocket = new ServerSocket(downloadPort);

                                        outStream.writeObject(sendMessage);
                                        uploadSocket.setSoTimeout(10000);

                                        Socket downloadClient = uploadSocket.accept();
                                        ObjectOutputStream uploadStream = new ObjectOutputStream(downloadClient.getOutputStream());

                                        //================================================================================================================================
                                        uploadStream.writeObject(fileKey);
                                        System.out.println(fileKey);
                                        uploadStream.writeObject(iv);
                                        for (byte b : iv) {
                                            System.out.println(b);
                                        }
                                        System.out.println(ivP);
                                        //================================================================================================================================


                                        FileInputStream fileInputStream = new FileInputStream(file);
                                        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                                        downloadPort++;
                                        byte[] data;
                                        SealedObject packet;
                                        while (bytesSent != fileSize) {

                                            while (pauseUpload) {
                                                Thread.sleep(10);
                                            }

                                            int size = packetSize;
                                            if (fileSize - bytesSent >= size) {
                                                bytesSent += size;
                                            } else {
                                                size = (int) (fileSize - bytesSent);
                                                bytesSent = fileSize;
                                            }
                                            data = new byte[size];

                                            bufferedInputStream.read(data, 0, size);
                                            packet = new SealedObject(data, cipher);

                                            uploadStream.writeObject(packet);

                                            progress = bytesSent / ((double) fileSize);
                                            double finalProgress = progress;

                                            if (((int) Math.floor(progress * 100)) % 5 == 0.0) {
                                                Platform.runLater(() -> {
                                                    UploadFileProgress.setProgress(finalProgress);
                                                });
                                                Thread.sleep(1);
                                            }

                                        }


                                        uploadStream.flush();
                                        downloadClient.close();
                                        uploadSocket.close();
                                        downloadPort--;

                                        Platform.runLater(() -> {
                                            System.out.println("Upload Complete");
                                            UploadStatusMessage.setText("Upload Complete");
                                            UploadFileProgress.setProgress(0);
                                        });

                                    } catch (SocketTimeoutException ste) {
                                        System.out.println("Client timed out");
                                        Platform.runLater(() -> {
                                            UploadErrorMessage.setText("Client did not connect");
                                            UploadFileProgress.setProgress(0);
                                        });

                                    } catch (IOException | InterruptedException e) {
                                        Platform.runLater(() -> {
                                            UploadErrorMessage.setText("Upload Failed");
                                            UploadFileProgress.setProgress(0);
                                        });
                                    } catch (IllegalBlockSizeException e) {
                                        throw new RuntimeException(e);
                                    }
                                }).start();
                            }

                            break;
                        }
                        case (4): {


                            if (receiveMessage.getKey().equals(key)) {

                                Platform.runLater(() -> {
                                    DownloadErrorMessage.setText("");
                                    DownloadStatusMessage.setText("");
                                });
                                new Thread(() -> {
                                    long bytesWritten = 0L;
                                    double progress;
                                    String[] address = receiveMessage.message.split(":");
                                    long fileSize = receiveMessage.fileSize;
                                    String host = address[0];
                                    int port = Integer.parseInt(address[1]);
                                    String fileName = address[2];


                                    Path filepath = Path.of(downloadDest, fileName);
                                    try {

                                        FileOutputStream fileOut = new FileOutputStream(filepath.toString());

                                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOut);
                                        byte[] data = new byte[10000];
                                        int bytesRead = 0;
                                        Thread.sleep(100);
                                        Socket uploadClient = new Socket(host, port);

                                        downloadPort++;
                                        ObjectInputStream downloadStream = new ObjectInputStream(uploadClient.getInputStream());

                                        //================================================================================================================================
                                        SecretKey fileKey = (SecretKey) downloadStream.readObject();
                                        System.out.println(fileKey);
                                        byte[] iv = (byte[]) downloadStream.readObject();
                                        for (byte b : iv) {
                                            System.out.println(b);
                                        }
                                        IvParameterSpec ivP = new IvParameterSpec(iv);
                                        System.out.println(ivP);

                                        Cipher cipher = null;
                                        try {
                                            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                            cipher.init(Cipher.DECRYPT_MODE, fileKey, ivP);
                                        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                                                 InvalidAlgorithmParameterException | InvalidKeyException e) {
                                            throw new RuntimeException(e);
                                        }
                                        //================================================================================================================================

                                        SealedObject packet;
                                        while (bytesWritten != fileSize) {
                                            packet = (SealedObject) downloadStream.readObject();
                                            while (pauseDownload) {
                                                Thread.sleep(10);
                                            }

                                            data = (byte[]) packet.getObject(cipher);

                                            fileOut.write(data);


                                            bytesWritten += data.length;
                                            progress = bytesWritten / ((double) fileSize);
                                            double finalProgress = progress;
                                            Platform.runLater(() -> {
                                                DownloadFileProgress.setProgress(finalProgress);
                                            });
                                        }

                                        bufferedOutputStream.flush();
                                        fileOut.flush();
                                        fileOut.close();
                                        downloadPort--;

                                        Platform.runLater(() -> {
                                            DownloadStatusMessage.setText("Download Complete");
                                            DownloadFileProgress.setProgress(0);
                                        });
                                    } catch (IOException | InterruptedException e) {
                                        //throw new RuntimeException(e);
                                        Platform.runLater(() -> {
                                            DownloadErrorMessage.setText("Download Failed");
                                            DownloadFileProgress.setProgress(0);
                                        });
                                    } catch (ClassNotFoundException | IllegalBlockSizeException |
                                             BadPaddingException e) {
                                        throw new RuntimeException(e);
                                    }
                                }).start();
                            }
                            break;
                        }
                        case (7): {
                            Platform.runLater(() ->
                                    DownloadFilesList.getItems().add(receiveMessage.message));
                            break;
                        }
                        case (8): {
                            clients.put(receiveMessage.message, receiveMessage.getPublicKey());

                            Message sendMessage = new Message(9, receiveMessage.message);
                            sendMessage.setKey(receiveMessage.getKey());
                            String[] items = new String[UploadFiles.getItems().size()];
                            int i = 0;
                            for (String item : UploadFiles.getItems()) {

                                items[i] = item;
                                i++;
                            }
                            sendMessage.setItems(items);

                            outStream.writeObject(sendMessage);


                            Platform.runLater(() ->
                                    OnlineClients.getItems().add(receiveMessage.message));
                            break;
                        }
                        case (9): {
                            DownloadFilesList.getItems().addAll(receiveMessage.getItems());
                            break;
                        }
                        case (10): {
                            System.out.println("Exit client");
                            Platform.runLater(() -> {
                                OnlineClients.getItems().remove(receiveMessage.getSource());
                                String[] removeItems = receiveMessage.getItems();
                                System.out.println(removeItems.length);
                                for (int i = 0; i < removeItems.length; i++) {
                                    System.out.println(removeItems[i]);
                                    int ri = 0;
                                    for (String item : DownloadFilesList.getItems()) {
                                        System.out.println(item);

                                        if (item.equals(removeItems[i])) {

                                            DownloadFilesList.getItems().remove(ri);
                                            break;
                                        }
                                        ri++;
                                    }
                                }
                            });
                        }
                    }
                } catch (IOException | ClassNotFoundException | NoSuchPaddingException | NoSuchAlgorithmException |
                         InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                    //throw new RuntimeException(e);
                    exit();
                } catch (InvalidAlgorithmParameterException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }


    public void exit() {
        String[] items = new String[UploadFiles.getItems().size()];
        int i = 0;
        for (String item : UploadFiles.getItems()) {
            System.out.println(item);

            items[i] = item;
            i++;
        }

        Message sendMessage = new Message(10, "GoodBye");
        sendMessage.setItems(items);
        sendMessage.setSource(Nickname.getText());


        try {
            outStream.writeObject(sendMessage);
            inStream.close();
            outStream.close();
            serverSocket.close();

        } catch (IOException e) {
            System.exit(0);
        }
        System.exit(0);

    }

    public void setDestination(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a File");
        File file = chooser.showDialog(primaryStage);
        if (file != null) {
            downloadDest = file.getPath();

        }
    }

    public void searchFiles(ActionEvent event) {
        String search = FileSearchInput.getText().toLowerCase().strip();
        FileSearchResults.getItems().removeAll();
        if (search != null && search != "") {
            for (String item : DownloadFilesList.getItems()) {
                if (item.toLowerCase().strip().contains(search) || item.toLowerCase().compareTo(search) > 0) {
                    FileSearchResults.getItems().add(item);
                }
            }
        }
    }

    public void sendChat(ActionEvent event) {
        if (OnlineClients.getSelectionModel().getSelectedItems().size() != 0) {
            ChatErrorMessage.setText("");
            String client = (String) OnlineClients.getSelectionModel().getSelectedItems().toArray()[0];

            try {
                Cipher encryptCipher = Cipher.getInstance("RSA");
                encryptCipher.init(Cipher.ENCRYPT_MODE, clients.get(client));

                byte[] messageBytes = ChatInput.getText().getBytes(StandardCharsets.UTF_8);
                byte[] encryptedMessageBytes = encryptCipher.doFinal(messageBytes);
                ChatLog.getItems().add(ChatInput.getText());
                Message sendMessage = new Message(2, "");
                sendMessage.setDestination(client);
                sendMessage.setEncryptedMessage(encryptedMessageBytes);

                outStream.writeObject(sendMessage);

            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     IllegalBlockSizeException | BadPaddingException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            ChatErrorMessage.setText("A client must be selected");
        }
    }

    public void pauseUpload(ActionEvent event) {
        if (pauseUpload) {
            pauseUpload = false;
            PauseUpload.setText("Pause");
        } else {
            pauseUpload = true;
            PauseUpload.setText("Resume");
        }
    }

    public void pauseDownload(ActionEvent event) {
        if (pauseDownload) {
            pauseDownload = false;
            PauseDownload.setText("Pause");
        } else {
            pauseDownload = true;
            PauseDownload.setText("Resume");
        }
    }

    public void uploadFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a File");
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            fileList.put(file.getName(), file);
            UploadFiles.getItems().add(file.getName());
            Message message = new Message(7, file.getName());
            try {
                outStream.writeObject(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void downloadFile() {
        if (downloadDest == "") {
            DownloadErrorMessage.setText("A download destination must be set");
        } else if (DownloadFilesList.getSelectionModel().getSelectedItems().size() != 0 || FileSearchResults.getSelectionModel().getSelectedItems().size() != 0) {
            DownloadErrorMessage.setText("");
            String fileName = "";
            if (DownloadFilesList.getSelectionModel().getSelectedItems().size() != 0) {
                fileName = (String) DownloadFilesList.getSelectionModel().getSelectedItems().toArray()[0];
            } else {
                fileName = (String) FileSearchResults.getSelectionModel().getSelectedItems().toArray()[0];

            }
            Message sendMessage = new Message(3, fileName);
            key = randomKeyGen();
            sendMessage.setKey(key);
            sendMessage.setPublicKey(publicKey);
            try {
                System.out.println("Sending file request . . .");
                if (sendMessage == null) {
                    System.out.println("Oh no");
                }
                outStream.writeObject(sendMessage);
                System.out.println("Request sent");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String randomKeyGen() {
        byte[] stringBytes = new byte[20];
        new Random().nextBytes(stringBytes);
        String keyString = new String(stringBytes, Charset.forName("UTF-8"));

        String key = "";
        for (int i = 0; i < keyString.length(); i++) {
            int rand = new Random().nextInt();
            key = key + keyString.charAt(i) + rand;

        }
        return key;

    }

}
