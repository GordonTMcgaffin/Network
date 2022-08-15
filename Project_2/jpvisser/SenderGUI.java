import javafx.scene.layout.HBox;
import static java.lang.System.out;
import java.io.*;
import java.nio.file.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.*;
import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.event.*;
import java.net.*;

public class SenderGUI
    extends Application {

    boolean sending;

    @Override
    public void start(Stage primaryStage)
        throws Exception
    {
        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(15);
        grid.setPadding(new Insets(15, 15, 15, 15));
        // grid rows
        grid.getRowConstraints().addAll(
                new RowConstraints(),
                new RowConstraints(),
                new RowConstraints(),
                new RowConstraints());

        // prompt label
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

        // protocol select menu
        Label menuLabel = new Label("Select protocol:");
        MenuButton menuButton = new MenuButton("RBUDP");
        MenuItem mi1 = new MenuItem("RBUDP");
        MenuItem mi2 = new MenuItem("TCP");
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
                Alerter.showAlert(AlertType.INFORMATION, "No File Selected",
                        "Enter a filename or select a file by clicking on the 'Browse' button.");
                return;
            }
            File file = new File(filePathField.getText().strip());
            if (!file.exists()) {
                Alerter.showAlert(AlertType.INFORMATION, "File Does Not Exist",
                        "The file you have selected does not exist.");
                return;
            }
            mainLabel.setText("Sending...");
            filePathField.setEditable(false);
            browseButton.setDisable(true);
            menuButton.setDisable(true);
            sendButton.setDisable(true);
            new Thread() {
                @Override
                public void run()
                {
                    String th, m;
                    try {
                        Sender.send(file.getPath(), "localhost", 9090, menuButton.getText(), 1000, 1000);
                        th = "File Successfully Sent";
                        m = "The file transfer has completed successfully.";
                    } catch (ConnectException ce) {
                        // Could not connect to receiver.
                        th = "Could Not Connect to Receiver";
                        m = "File transfer could not be initiated: The receiver could not be reached.";
                    } catch (EOFException eofe) {
                        // Lost connection to the receiver.
                        th = "Lost Connection to Receiver";
                        m = "File transfer failed: Lost connection to the receiver.";
                    } catch (Exception ex) {
                        // Something went wrong.
                        th = "Something Went Wrong";
                        m = "An unknown error has occurred.";
                        ex.printStackTrace();
                    }
                    String titleAndHeader = th, msg = m;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run()
                        {
                            mainLabel.setText("Select a file to send:");
                            filePathField.setEditable(true);
                            browseButton.setDisable(false);
                            menuButton.setDisable(false);
                            sendButton.setDisable(false);
                            Alerter.showAlert(AlertType.ERROR, titleAndHeader, msg);
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
        grid.getChildren().addAll(mainLabel, fileSelectHBox, menuHBox, sendButton);
        Scene scene = new Scene(grid, 500, 250);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Sender");
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        SenderGUI sender = new SenderGUI();
        sender.launch(args);
    }
}
