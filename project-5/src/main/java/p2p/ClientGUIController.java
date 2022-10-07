package p2p;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientGUIController {

    public static Socket serverSocket;
    public static ObjectInputStream inStream;
    public static ObjectOutputStream outStream;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(6);
    private static Stage primaryStage;
    public int packetSize = 400;
    private ConcurrentHashMap<String, File> fileList = new ConcurrentHashMap<>();
    private LinkedBlockingQueue<String> clients;
    private String key;
    private int downloadPort = 9091;
    private String downloadHost = "127.0.0.1";
    private String downloadDest = "";
    @FXML
    private Button UploadFile;
    @FXML
    private Button DownloadFile;
    @FXML
    private Button Pause;
    @FXML
    private ListView<String> OnlineClients;
    @FXML
    private Button SendMessage;
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

    public void init(Socket serverSocket, ObjectInputStream inStream, ObjectOutputStream outStream, ExecutorService threadPool, Stage stage) {
        this.serverSocket = serverSocket;
        this.inStream = inStream;
        this.outStream = outStream;
        this.threadPool = threadPool;
        this.primaryStage = stage;
        //UploadFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        try {
            clients = (LinkedBlockingQueue<String>) inStream.readObject();
            OnlineClients.getItems().addAll(0, clients);
        } catch (IOException | ClassNotFoundException e) {
            exit();
        }

        new Thread(() -> {
            boolean exit = false;
            while (!exit) {
                try {
                    Message receiveMessage = (Message) inStream.readObject();
                    switch (receiveMessage.type) {
                        case (1): {
                            System.out.println("Received message of type 1");
                            break;
                        }
                        case (2): {
                            //Chat message
                            System.out.println("Received message of type 2");
                            break;
                        }
                        case (3): {
                            //Uploader
                            if (fileList.containsKey(receiveMessage.message)) {
                                Message sendMessage = new Message(4, downloadHost + ":" + downloadPort + ":" + receiveMessage.message);
                                File file = fileList.get(receiveMessage.message);
                                long fileSize = Files.size(Path.of(file.getPath()));

                                System.out.println("==============================");
                                System.out.println("Setting up socket on " + downloadHost + ":" + downloadPort);
                                Path filePath = Path.of(file.getPath());
                                System.out.println(filePath);
                                System.out.println(file.getName());
                                System.out.println(fileSize);

                                sendMessage.setFileSize(fileSize);
                                sendMessage.setKey(receiveMessage.getKey());
                                outStream.writeObject(sendMessage);
                                new Thread(() -> {

                                    System.out.println("==============================");


                                    long bytesSent = 0L;
                                    double progress = 0.0;

                                    try {
                                        ServerSocket uploadSocket = new ServerSocket(downloadPort);
                                        Socket downloadClient = uploadSocket.accept();
                                        ObjectOutputStream uploadStream = new ObjectOutputStream(downloadClient.getOutputStream());
                                        ;
                                        System.out.println("Client connected");
                                        BufferedInputStream fileStream = new BufferedInputStream(Files.newInputStream(filePath));

                                        downloadPort++;

                                        byte[] data = new byte[packetSize];
                                        int bytesRead = 0;
                                        System.out.println("Uploading . . .");

                                        while ((bytesRead = fileStream.read(data, 0, packetSize)) != -1) {
                                            uploadStream.write(data, 0, bytesRead);
                                            System.out.println(bytesRead);
                                            bytesSent += bytesRead;
                                            progress = bytesSent / ((double) fileSize);
                                            double finalProgress = progress;

                                            System.out.println(finalProgress + "%");
                                            Platform.runLater(() -> {
                                                UploadFileProgress.setProgress(finalProgress);
                                            });

                                        }

                                        //ToDo wait for ack from client then close everything
                                        //Thread.sleep(1);
                                        System.out.println("Upload complete");

                                        downloadPort--;

                                    } catch (IOException e) {
                                        //ToDo catch disconnect error
                                    }


                                }).start();
                            }

                            //Do something here

                            break;
                        }
                        case (4): {

                            if (receiveMessage.getKey().equals(key)) {

                                new Thread(() -> {
                                    long bytesWritten = 0L;
                                    double progress = 0.0;

                                    String[] address = receiveMessage.message.split(":");
                                    long fileSize = receiveMessage.fileSize;
                                    String host = address[0];
                                    int port = Integer.parseInt(address[1]);
                                    String fileName = address[2];

                                    System.out.println("==============================");
                                    System.out.println("connecting to host: " + host + ":" + port + " . . . ");
                                    System.out.println(fileName);
                                    System.out.println(fileSize);
                                    System.out.println("==============================");

                                    Path filepath = Path.of(downloadDest, fileName);
                                    try {
                                        BufferedOutputStream fileOut =
                                                new BufferedOutputStream(
                                                        Files.newOutputStream(filepath));
                                        byte[] data = new byte[packetSize];
                                        int bytesRead = 0;
                                        Socket uploadClient = new Socket(host, port);
                                        System.out.println("Connected to sender");
                                        downloadPort++;
                                        ObjectInputStream downloadStream = new ObjectInputStream(uploadClient.getInputStream());
                                        while ((bytesRead = downloadStream.read(data, 0, packetSize))
                                                != -1) {
                                            System.out.println(bytesRead);
                                            fileOut.write(data, 0, bytesRead);

                                            bytesWritten += bytesRead;
                                            progress = bytesWritten / ((double) fileSize);
                                            double finalProgress = progress;

                                            System.out.println(finalProgress + "%");
                                            Platform.runLater(() -> {
                                                DownloadFileProgress.setProgress(finalProgress);
                                            });

                                        }

                                        downloadPort--;
                                        System.out.println("Download complete");

                                    } catch (IOException e) {
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
                            clients.put(receiveMessage.message);
                            Platform.runLater(() ->
                                    OnlineClients.getItems().add(receiveMessage.message));
                            break;
                        }
                    }
                } catch (IOException | InterruptedException | ClassNotFoundException e) {
                    exit();
                }
            }
        }).start();


    }

    public void updateClientList(String name) {
        OnlineClients.getItems().add(name);
    }

    public void exit() {
        //Todo send exit message along with file list
        try {
            inStream.close();
            outStream.close();
            serverSocket.close();
            System.out.println("Server disconnected");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.exit(1);

    }

    public void setDestination(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a File");
        File file = chooser.showDialog(primaryStage);
        if (file != null) {
            downloadDest = file.getPath();
            System.out.println(downloadDest);
        }
    }

    public void sendChat(ActionEvent event) {
        System.out.println(ChatInput.getText().strip());
    }

    public void pauseUpload(ActionEvent event) {

    }

    public void uploadFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a File");
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            System.out.println("Added file " + file.getName());
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
        //ToDo add check for file download path
        if (DownloadFilesList.getSelectionModel().getSelectedItems() != null) {
            String fileName = (String) DownloadFilesList.getSelectionModel().getSelectedItems().toArray()[0];
            Message sendMessage = new Message(3, fileName);
            key = randomKeyGen();
            sendMessage.setKey(key);
            try {
                outStream.writeObject(sendMessage);
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
        //System.out.println(key);
        return key;

    }

}
