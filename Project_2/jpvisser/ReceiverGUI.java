/**
 * @file    ReceiverGUI.java
 * @brief   GUI for receiving files via TCP or RBUDP
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
 * The {@code ReceiverGUI} class provides a graphical user interface for
 * receiving files via TCP and RBUDP.
 */
public class ReceiverGUI
    extends Application {

    /* --- Instance Variables ----------------------------------------------- */

    /** for checking whether a file is currently being received */
    boolean receiving;

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
        int _port, _socketTimeout;
        _port = _socketTimeout = -1;
        try {
            _port = Integer.parseInt(super.getParameters().getRaw().get(0));
            _socketTimeout = Integer.parseInt(
                    super.getParameters().getRaw().get(1));
        } catch (IndexOutOfBoundsException ioobe) {
            System.err.println("Usage: ReceiverGUI <port> <socketTimout>");
            System.exit(1);
        }
        int port = _port, socketTimeout = _socketTimeout;

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
        Label mainLabel = new Label("Select a directory to save to:");
        GridPane.setValignment(mainLabel, VPos.CENTER);
        GridPane.setHalignment(mainLabel, HPos.CENTER);
        GridPane.setHgrow(mainLabel, Priority.ALWAYS);
        GridPane.setVgrow(mainLabel, Priority.ALWAYS);
        GridPane.setConstraints(mainLabel, 0, 0);

        // file path input field
        TextField filePathField = new TextField();
        filePathField.setPromptText("Enter directory name to save to");
        // browse button
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> {
            if (receiving) {
                Alerter.showAlert(
                        AlertType.INFORMATION,
                        "Directory Already Set",
                        "The directory for saving files has already been set." +
                        "Restart the receiver to change it.");
                return;
            }
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select a File");
            File file = chooser.showDialog(primaryStage);
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

        // progress bar
        ProgressBar progressBar = new ProgressBar();
        GridPane.setValignment(progressBar, VPos.CENTER);
        GridPane.setHalignment(progressBar, HPos.CENTER);
        GridPane.setHgrow(progressBar, Priority.ALWAYS);
        GridPane.setVgrow(progressBar, Priority.ALWAYS);
        GridPane.setConstraints(progressBar, 0, 2);

        // receive button
        Button receiveButton = new Button("Receive");
        receiveButton.setOnAction(e -> {
            if (filePathField.getText().strip().equals("")) {
                Alerter.showAlert(
                        AlertType.INFORMATION,
                        "No Directory Selected",
                        "Enter a directory name or select a directory by " +
                        "clicking on the 'Browse' button.");
                return;
            }
            File file = new File(filePathField.getText().strip());
            if (!file.exists()) {
                Alerter.showAlert(
                        AlertType.INFORMATION,
                        "Directory Does Not Exist",
                        "The directory you have selected does not exist.");
                return;
            }
            receiving = true;
            receiveButton.setDisable(true);
            filePathField.setEditable(false);
            mainLabel.setText("Receiving files...");
            // thread for receiving
            new Thread() {
                @Override
                public void run()
                {
                    while (true) {
                        try {
                            Receiver.receive(port, socketTimeout, file.getPath());
                        } catch (SocketException se) {
                            // Transfer done or stopped.
                        } catch (Exception ex) {
                            Alerter.showAlert(
                                    AlertType.ERROR,
                                    "Something Went Wrong",
                                    "An unknown error has occurred.");
                            ex.printStackTrace();
                        }
                        boolean success = (Receiver.getProgress() >= 1.0);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run()
                            {
                                AlertType a = (success ?
                                        AlertType.INFORMATION :
                                        AlertType.ERROR);
                                String th, m;
                                th = "File " + (success ?
                                        "Successfully Received" :
                                        "Transfer Failed");
                                m = "File transfer has " +
                                    (success ?
                                     "completed successfully." :
                                     "failed: The file is incomplete.");
                                Alerter.showAlert(a, th, m);
                            }
                        });
                        receiving = false;
                    }
                }
            }.start();
            // thread for updating the progress bar
            new Thread() {
                @Override
                public void run()
                {
                    while (true) {
                        try {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run()
                                {
                                    progressBar.setProgress(
                                            Receiver.getProgress());
                                }
                            });
                            Thread.sleep(75);
                        } catch (InterruptedException ie) {
                            // Thread interrupted. Continue.
                        }
                    }
                }
            }.start();
        });
        GridPane.setValignment(receiveButton, VPos.CENTER);
        GridPane.setHalignment(receiveButton, HPos.CENTER);
        GridPane.setHgrow(receiveButton, Priority.ALWAYS);
        GridPane.setVgrow(receiveButton, Priority.ALWAYS);
        GridPane.setConstraints(receiveButton, 0, 3);

        // Complete window and show.
        Scene scene = new Scene(grid, 500, 250);
        grid.getChildren().addAll(mainLabel, fileSelectHBox, progressBar,
                receiveButton);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Receiver");
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    /* --- Main Method ------------------------------------------------------ */

    /**
     * Starts the {@code ReceiverGUI}.
     *
     * @param args  command-line arguments
     */
    public static void main(String[] args)
    {
        ReceiverGUI receiver = new ReceiverGUI();
        receiver.launch(args);
    }
}
