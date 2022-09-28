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

        ClientGUIController controller =
            (ClientGUIController) loader.getController();
        String serverHostName = (super.getParameters().getRaw().size() > 0 ?
                super.getParameters().getRaw().get(0) : "localhost");
        int serverPort = (super.getParameters().getRaw().size() > 1 ?
                Integer.parseInt(super.getParameters().getRaw().get(1)) :
                Client.DEFAULT_SERVER_PORT);
        controller.initialize(serverHostName, serverPort);

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
