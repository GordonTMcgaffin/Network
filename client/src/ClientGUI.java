/**
 * @file    ClientGUI.java
 * @brief   chat client GUI
 * @author  J. P. Visser (21553416@sun.ac.za)
 * @date    2022-08-02
 */

import java.io.*;
import java.util.*;
import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

/**
 * The {@code ClientGUI} class instantiates the {@code Client} class and
 * provides a graphical user interface from which to receive and send messages
 * from/to other {@code Client} instances connected to the server.
 */
public class ClientGUI
    extends Application {

    /* --- Instance Variables ----------------------------------------------- */

    /** client instance for receiving and sending messages */
    private Client client;

    /** nickname chosen by the user */
    private String username;

    /** for displaying messages */
    private TextFlow chat;

    /** for displaying a list of connected users */
    private TextFlow connectedUsers;

    /** for user input */
    private TextArea chatInput;

    /** for sending messages typed in {@code chatInput} */
    private Button sendButton;

    /* --- Instance Methods ------------------------------------------------- */

    @Override
    public void start(Stage primaryStage)
        throws Exception
    {
        // client
        String host = super.getParameters().getRaw().get(0);
        int port = Integer.parseInt(super.getParameters().getRaw().get(1));
        client = new Client(host, port);

        // username
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
            client.send(new Message(username, "Server", "username_set"));
            Message msg = client.receive();
            if (!msg.content.equals("unique")) {
                headerText = "Username not available";
            } else {
                unique = true;
            }
        }

        // grid
        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(15);
        grid.setPadding(new Insets(15, 15, 15, 15));

        // grid columns
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

        // username label
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

        // header for online users list
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
                receiver = input.substring(input.indexOf(":") + 1,
                                           input.indexOf(" "));
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
                            if (msg.sender.equals("Server") &&
                                    msg.content.startsWith("online_users:")) {
                                Text txt = new Text(msg.content.substring(
                                            msg.content.indexOf("\n") + 1));
                                connectedUsers.getChildren().setAll(txt);
                                return;
                            }
                            String sender = (msg.sender.equals(username)) ?
                                            "You" : msg.sender;
                            Text txt = new Text(sender + ": " + msg.content +
                                                "\n");
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
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client");

        primaryStage.setOnCloseRequest(e -> {
            this.exit();
        });

        primaryStage.show();
    }

    /**
     * Disconnects the client, then closes the window and kills the program.
     */
    public void exit()
    {
        client.disconnect();
        Platform.exit();
        System.exit(0);
    }

    /* --- Main Method ------------------------------------------------------ */

    /**
     * Starts the {@code ClientGUI}.
     *
     * @param args  command-line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}
