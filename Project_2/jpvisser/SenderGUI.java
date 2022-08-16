/**
 * @file    SenderGUI.java
 * @brief   GUI for sending files via TCP or RBUDP
 * @author  J. P. Visser (21553416@sun.ac.za)
 * @date    2022-08-16
 */

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javafx.application.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import static java.lang.System.out;

/**
 * The {@code SenderGUI} class provides a graphical user interface for sending
 * files via TCP and RBUDP.
 */
public class SenderGUI
    extends Application {

    /* --- Instance Variables ----------------------------------------------- */

    /** for checking whether a file is currently being sent */
    boolean sending;

    /* --- Instance Methods ------------------------------------------------- */

    /**
     * Starts the application.
     *
     * @param  primaryStage  primary Stage
     * @throws Exception if anything goes wrong
     */
    @Override
    public void start(Stage primaryStage)
        throws Exception
    {
        String _host = null;
        int _port, _blastSz, _packetSz;
        _port = _blastSz = _packetSz = -1;
        try {
            _host = super.getParameters().getRaw().get(0);
            _port = Integer.parseInt(super.getParameters().getRaw().get(1));
            _blastSz = Integer.parseInt(super.getParameters().getRaw().get(2));
            _packetSz = Integer.parseInt(super.getParameters().getRaw().get(3));
        } catch (IndexOutOfBoundsException ioobe) {
            System.err.println("Usage: SenderGUI <host> <port> <blast size> " +
                    "<packet size>");
            System.exit(1);
        }
        String host = _host;
        int port = _port, blastSz = _blastSz, packetSz = _packetSz;

        // grid
        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(15);
        grid.setPadding(new Insets(15, 15, 15, 15));
        grid.getRowConstraints().addAll(
                new RowConstraints(),
                new RowConstraints(),
                new RowConstraints(),
                new RowConstraints());

        // main label
        Label mainLabel = new Label("Select a file to send:");
        GridPane.setValignment(mainLabel, VPos.CENTER);
        GridPane.setHalignment(mainLabel, HPos.CENTER);
        GridPane.setHgrow(mainLabel, Priority.ALWAYS);
        GridPane.setVgrow(mainLabel, Priority.ALWAYS);
        GridPane.setConstraints(mainLabel, 0, 0);

        // file path input field
        TextField filePathField = new TextField();
        filePathField.setPromptText("Enter filename to send");
        // browse button
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select a File");
            File file = chooser.showOpenDialog(primaryStage);
            if (file != null) {
                filePathField.setText(file.getPath());
            }
        });
        HBox fileSelectHBox = new HBox();
        fileSelectHBox.setSpacing(5);
        fileSelectHBox.getChildren().addAll(filePathField, browseButton);
        HBox.setHgrow(filePathField, Priority.ALWAYS);
        HBox.setHgrow(browseButton, Priority.ALWAYS);
        GridPane.setValignment(fileSelectHBox, VPos.CENTER);
        GridPane.setHalignment(fileSelectHBox, HPos.CENTER);
        GridPane.setHgrow(fileSelectHBox, Priority.ALWAYS);
        GridPane.setVgrow(fileSelectHBox, Priority.ALWAYS);
        GridPane.setConstraints(fileSelectHBox, 0, 1);

        // protocol drop-down menu
        Label menuLabel = new Label("Select protocol:");
        MenuButton menuButton = new MenuButton("RBUDP");
        MenuItem mi1 = new MenuItem("RBUDP");
        MenuItem mi2 = new MenuItem("TCP");
        // TODO: Check if it is possible to turn this into a lambda.
        EventHandler<ActionEvent> menuItemSelectHandler = new EventHandler<>() {
            public void handle(ActionEvent e)
            {
                menuButton.setText(((MenuItem) e.getSource()).getText());
            }
        };
        mi1.setOnAction(menuItemSelectHandler);
        mi2.setOnAction(menuItemSelectHandler);
        menuButton.getItems().addAll(mi1, mi2);
        HBox menuHBox = new HBox();
        menuHBox.setAlignment(Pos.CENTER);
        menuHBox.setSpacing(5);
        HBox.setHgrow(menuLabel, Priority.ALWAYS);
        HBox.setHgrow(menuButton, Priority.ALWAYS);
        menuHBox.getChildren().addAll(menuLabel, menuButton);
        GridPane.setValignment(menuHBox, VPos.CENTER);
        GridPane.setHalignment(menuHBox, HPos.CENTER);
        GridPane.setHgrow(menuHBox, Priority.ALWAYS);
        GridPane.setVgrow(menuHBox, Priority.ALWAYS);
        GridPane.setConstraints(menuHBox, 0, 2);

        // send button
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            if (filePathField.getText().strip().equals("")) {
                Alerter.showAlert(
                        AlertType.INFORMATION,
                        "No File Selected",
                        "Enter a filename or select a file by clicking on " +
                        "the'Browse' button.");
                return;
            }
            File file = new File(filePathField.getText().strip());
            if (!file.exists()) {
                Alerter.showAlert(
                        AlertType.INFORMATION,
                        "File Does Not Exist",
                        "The file you have selected does not exist.");
                return;
            }
            mainLabel.setText("Sending...");
            filePathField.setEditable(false);
            browseButton.setDisable(true);
            menuButton.setDisable(true);
            sendButton.setDisable(true);
            // thread for sending
            new Thread() {
                @Override
                public void run()
                {
                    AlertType _alertType = AlertType.ERROR;
                    String _titleAndHeader, _msg;
                    try {
                        Sender.send(file.getPath(), host, port,
                                menuButton.getText(), blastSz, packetSz);
                        _alertType = AlertType.INFORMATION;
                        _titleAndHeader = "File Successfully Sent";
                        _msg = "The file transfer has completed successfully.";
                    } catch (ConnectException ce) {
                        // Could not connect to receiver.
                        _titleAndHeader = "Could Not Connect to Receiver";
                        _msg = "File transfer could not be initiated: The " +
                            "receiver could not be reached.";
                    } catch (EOFException eofe) {
                        // Lost connection to the receiver.
                        _titleAndHeader = "Lost Connection to Receiver";
                        _msg = "File transfer failed: Lost connection to the " +
                            "receiver.";
                    } catch (Exception ex) {
                        // Something went wrong.
                        _titleAndHeader = "Something Went Wrong";
                        _msg = "An unknown error has occurred.";
                        ex.printStackTrace();
                    }
                    AlertType alertType = _alertType;
                    String titleAndHeader = _titleAndHeader, msg = _msg;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run()
                        {
                            mainLabel.setText("Select a file to send:");
                            filePathField.setEditable(true);
                            browseButton.setDisable(false);
                            menuButton.setDisable(false);
                            sendButton.setDisable(false);
                            Alerter.showAlert(alertType, titleAndHeader, msg);
                        }
                    });
                    sending = false;
                }
            }.start();
        });
        GridPane.setValignment(sendButton, VPos.CENTER);
        GridPane.setHalignment(sendButton, HPos.CENTER);
        GridPane.setHgrow(sendButton, Priority.ALWAYS);
        GridPane.setVgrow(sendButton, Priority.ALWAYS);
        GridPane.setConstraints(sendButton, 0, 3);

        // Complete window and show.
        grid.getChildren().addAll(mainLabel, fileSelectHBox, menuHBox,
                sendButton);
        Scene scene = new Scene(grid, 500, 250);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Sender");
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    /* --- Main Method ------------------------------------------------------ */

    /**
     * Starts the {@code SenderGUI}.
     *
     * @param args  command-line arguments
     */
    public static void main(String[] args)
    {
        SenderGUI sender = new SenderGUI();
        sender.launch(args);
    }
}
