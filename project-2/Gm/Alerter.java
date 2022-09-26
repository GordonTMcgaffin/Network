import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;

public final class Alerter {

    public static void showAlert(AlertType type, String titleAndHeader, String msg)
    {
        Alert alert = new Alert(type);
        alert.setResizable(true);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setTitle(titleAndHeader);
        alert.setHeaderText(titleAndHeader);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
