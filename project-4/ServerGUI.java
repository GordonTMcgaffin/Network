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
        FXMLLoader loader =
            new FXMLLoader(getClass().getResource("server_gui.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Server");

        ServerGUIController controller =
            (ServerGUIController) loader.getController();
        int port = (super.getParameters().getRaw().size() > 0 ?
                Integer.parseInt(super.getParameters().getRaw().get(0)) :
                Server.DEFAULT_PORT);
        controller.initialize(port);

        primaryStage.setOnCloseRequest(e -> {
            controller.exit();
        });
        primaryStage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
