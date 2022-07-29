import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import java.io.*;
import java.util.*;

public class ClientGUI extends Application {
    private Stage window;
    private TextFlow chat;
    private TextFlow connectedUsers;
    private TextArea chatInput;
    private Button sendButton;
    private String username;
    Client client;

    @Override
    public void start(Stage primaryStage)
        throws Exception
    {
        // Start client.
        String host = super.getParameters().getRaw().get(0);
        int port = Integer.parseInt(super.getParameters().getRaw().get(1));
        client = new Client(host, port);

        // Set username.
        boolean unique = false;
        String headerText = "Enter your username";
        while (!unique) {

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enter Username");
            dialog.setHeaderText(headerText);
            dialog.setContentText("Username:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(username -> this.username = username.strip());

            if (username == null || username.equals("")) {
                this.exit();
            }
            client.send(new Message(username, "Server", "Username set"));
            Message msg = client.receive();
            if (!msg.content.equals("unique")) {
                headerText = "Username not available";
            } else {
                unique = true;
            }
        }

        // window
        this.window = primaryStage;
        this.window.setTitle("Client");

        // grid
        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(15);
        grid.setPadding(new Insets(15, 15, 15, 15));

        ColumnConstraints column0 = new ColumnConstraints();
        column0.setPercentWidth(80);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(20);
        grid.getColumnConstraints().addAll(column0, column1);

        // grid rows
        RowConstraints row0 = new RowConstraints();
        row0.setPercentHeight(5);
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(70);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(20);
        RowConstraints row3 = new RowConstraints();
        row3.setPercentHeight(5);
        grid.getRowConstraints().addAll(row0, row1, row2, row3);

        Label usernameLabel = new Label("User: " + username);
        GridPane.setValignment(usernameLabel, VPos.CENTER);
        GridPane.setHalignment(usernameLabel, HPos.CENTER);
        GridPane.setHgrow(usernameLabel, Priority.ALWAYS);
        GridPane.setVgrow(usernameLabel, Priority.ALWAYS);
        GridPane.setConstraints(usernameLabel, 0, 0);

        // chat
        chat = new TextFlow();
        chat.setPadding(new Insets(5, 5, 5, 5));
        GridPane.setHgrow(chat, Priority.ALWAYS);
        GridPane.setVgrow(chat, Priority.ALWAYS);
        ScrollPane chatScrollPane = new ScrollPane();
        chatScrollPane.setContent(chat);
        GridPane.setConstraints(chatScrollPane, 0, 1);

        Label statusLabel = new Label("Online Users");
        GridPane.setValignment(statusLabel, VPos.CENTER);
        GridPane.setHalignment(statusLabel, HPos.CENTER);
        GridPane.setHgrow(statusLabel, Priority.ALWAYS);
        GridPane.setVgrow(statusLabel, Priority.ALWAYS);
        GridPane.setConstraints(statusLabel, 1, 0);

        // list of connected users
        connectedUsers = new TextFlow();
        connectedUsers.setPadding(new Insets(5, 5, 5, 5));
        GridPane.setHgrow(connectedUsers, Priority.ALWAYS);
        GridPane.setVgrow(connectedUsers, Priority.ALWAYS);
        ScrollPane connectedUsersScrollPane = new ScrollPane();
        connectedUsersScrollPane.setContent(connectedUsers);
        GridPane.setConstraints(connectedUsersScrollPane, 1, 1);

        // user input
        chatInput = new TextArea();
        chatInput.setPromptText("Type message here...");
        GridPane.setHgrow(chatInput, Priority.ALWAYS);
        GridPane.setVgrow(chatInput, Priority.ALWAYS);
        GridPane.setConstraints(chatInput, 0, 2);

        // send button
        sendButton = new Button("SEND");
        GridPane.setHgrow(sendButton, Priority.ALWAYS);
        GridPane.setVgrow(sendButton, Priority.ALWAYS);
        GridPane.setHalignment(sendButton, HPos.CENTER);
        GridPane.setValignment(sendButton, VPos.CENTER);
        GridPane.setConstraints(sendButton, 0, 3);

        sendButton.setOnAction(e -> {
            String input = chatInput.getText().strip();
            if (input.equals("")) {
                return;
            }
            String receiver = "all";
            if (input.startsWith("to:")) {
                receiver = input.substring(input.indexOf(":") + 1, input.indexOf(" "));
                input = input.substring(input.indexOf(" ") + 1);
            }
            client.send(new Message(username, receiver, input));
            if (input.equals("exit")) {
                this.exit();
            }
            chatInput.setText("");
        });

        // Receive messages and display them.
        new Thread() {
            @Override
            public void run()
            {
                while (true) {
                    final Message msg = client.receive();
                    Platform.runLater(new Runnable() {
                        public void run()
                        {
                            if (msg.sender.equals("Server") && msg.content.startsWith("online_users:")) {
                                Text txt = new Text(msg.content.substring(msg.content.indexOf("\n") + 1));
                                connectedUsers.getChildren().setAll(txt);
                                return;
                            }
                            String sender = (msg.sender.equals(username)) ? "You" : msg.sender;
                            Text txt = new Text(sender + ": " + msg.content + "\n");
                            if (msg.sender.equals("Server")) {
                                txt.setStyle("-fx-fill:#8C8C8C");
                            } else if (!msg.receiver.equals("all")) {
                                txt.setStyle("-fx-fill:#A056F7");
                            }
                            chat.getChildren().add(txt);
                        }
                    });
                }
            }
        }.start();

        // Complete window setup.
        grid.getChildren().addAll(usernameLabel, chatScrollPane, chatInput,
                sendButton, statusLabel, connectedUsersScrollPane);
        Scene scene = new Scene(grid, 500, 500);
        this.window.setScene(scene);
        this.window.show();

        // What to do when the window is closed.
        primaryStage.setOnCloseRequest(e -> {
            this.exit();
        });
    }

    public void exit()
    {
        client.disconnect();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
