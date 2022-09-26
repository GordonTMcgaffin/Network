import javafx.application.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

public final class ClientGUI
    extends Application {

    @Override
    public void start(Stage primaryStage)
        throws Exception
    {
        FXMLLoader loader =
            new FXMLLoader(getClass().getResource("client_gui.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client");
        primaryStage.setOnCloseRequest(e -> {
            ClientGUIController controller =
                (ClientGUIController) loader.getController();
            controller.exit();
        });
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
