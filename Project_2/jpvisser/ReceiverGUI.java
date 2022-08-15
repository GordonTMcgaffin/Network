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

public class ReceiverGUI
    extends Application {

    boolean receiving;

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
                Alerter.showAlert(AlertType.INFORMATION, "Directory Already Set",
                        "The directory for saving files has already been set. Restart the receiver to change it.");
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
                Alerter.showAlert(AlertType.INFORMATION, "No Directory Selected",
                        "Enter a directory name or select a directory by clicking on the 'Browse' button.");
                return;
            }
            File file = new File(filePathField.getText().strip());
            if (!file.exists()) {
                Alerter.showAlert(AlertType.INFORMATION, "Directory Does Not Exist",
                        "The directory you have selected does not exist.");
                return;
            }
            receiving = true;
            receiveButton.setDisable(true);
            filePathField.setEditable(false);
            mainLabel.setText("Receiving files...");
            new Thread() {
                @Override
                public void run()
                {
                    while (true) {
                        try {
                            Receiver.receive(9090, 50, file.getPath());
                        } catch (SocketException se) {
                            // Transfer done. May or may not have been successful.
                        } catch (Exception ex) {
                            Alerter.showAlert(AlertType.ERROR, "Something Went Wrong", "An unknown error has occurred.");
                            ex.printStackTrace();
                        }
                        boolean success = (Receiver.getProgress() >= 1.0);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run()
                            {
                                AlertType a = (success ? AlertType.INFORMATION : AlertType.ERROR);
                                String th, m;
                                th = "File " + (success ? "Successfully Received" : "Transfer Failed");
                                m = "File transfer has " + (success ? "completed successfully." : "failed: The file is incomplete.");
                                Alerter.showAlert(a, th, m);
                            }
                        });
                    }
                }
            }.start();
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
                                    progressBar.setProgress(Receiver.getProgress());
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
        grid.getChildren().addAll(mainLabel, fileSelectHBox, progressBar, receiveButton);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Receiver");
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        ReceiverGUI receiver = new ReceiverGUI();
        receiver.launch(args);
    }
}
