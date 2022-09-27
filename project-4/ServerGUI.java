import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

public final class ServerGUI
    extends Application {

    @Override
    public void start(Stage primaryStage)
        throws Exception
    {
        Parent root =
            FXMLLoader.load(getClass().getResource("server_gui.fxml"));
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Server");
        primaryStage.setOnCloseRequest(e -> {
            exit();
        });
        primaryStage.show();
    }

    public void exit()
    {
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
